package com.copago.marketpulsedetector.core.analysis

import com.copago.marketpulsedetector.common.event.CrawledItemEvent
import com.copago.marketpulsedetector.common.extension.logOnAnomaly
import com.copago.marketpulsedetector.common.log.AnomalyLogService
import com.copago.marketpulsedetector.domain.entity.CrawlAnalysisResultEntity
import com.copago.marketpulsedetector.domain.entity.StockInfoEntity
import com.copago.marketpulsedetector.domain.repository.CrawlAnalysisResultRepository
import com.copago.marketpulsedetector.domain.repository.MarketThemeStockMapRepository
import org.slf4j.LoggerFactory
import org.springframework.kafka.annotation.DltHandler
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.annotation.RetryableTopic
import org.springframework.kafka.retrytopic.DltStrategy
import org.springframework.retry.annotation.Backoff
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class AnalysisConsumer(
    private val stockExtractor: StockExtractor,
    private val themeExtractor: ThemeExtractor,
    private val themeStockMapRepository: MarketThemeStockMapRepository,
    private val stockAnalysisRepository: CrawlAnalysisResultRepository,
    private val logService: AnomalyLogService
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    @RetryableTopic(
        attempts = "4",
        backoff = Backoff(delay = 1000, multiplier = 2.0),
        dltStrategy = DltStrategy.FAIL_ON_ERROR
    )
    @KafkaListener(topics = ["crawled.article"], groupId = "market-pulse-analyzer-group")
    @Transactional
    fun analyze(event: CrawledItemEvent) {
        val fullText = "${event.title} ${event.content}"

        // 1. 직접 언급된 종목 추출
        val directStocks = stockExtractor.extractStocks(fullText)

        // 2. 언급된 정치/정책 테마 추출
        val themes = themeExtractor.extractThemes(fullText)

        // 3. 테마를 통한 간접 종목 추론 (Inference)
        val inferredStocks = mutableSetOf<StockInfoEntity>()

        if (themes.isNotEmpty()) {
            logger.info("🏛️ Policy/Theme Detected: ${themes.map { it.themeName }}")

            // 발견된 테마에 매핑된 종목들을 가져옴
            for (theme in themes) {
                val maps = themeStockMapRepository.findAllByThemeId(theme.id!!)
                maps.forEach { map ->
                    runCatching {
                        map.stock.stockName
                        map.stock
                    }.logOnAnomaly(logService = logService,
                        key = "ThemeMap:${map.id}",
                        type = "MISSING_STOCK_INFO",
                        msg = "Theme: ${theme.themeName}"
                    )?.let { inferredStocks.add(it) }
                }
            }
            // (선택) 테마 발견 사실 저장 로직 추가 (CrawlThemeResultEntity)
        }

        // 중복 방지
        val existingResults = stockAnalysisRepository.findAllByTargetId(event.targetId)
        val existingStockCodes = existingResults.map { it.stock.code }.toSet()

        // 4. 결과 통합 (직접 언급 + 간접 추론)
        // 직접 언급은 신뢰도 1.0, 테마 추론은 신뢰도 0.7 정도로 차등을 둘 수 있음
        val finalResults = mutableListOf<CrawlAnalysisResultEntity>()

        directStocks.forEach { stock ->
            if (!existingStockCodes.contains(stock.code)) {
                finalResults.add(
                    CrawlAnalysisResultEntity(
                        targetId = event.targetId,
                        stock = stock,
                        confidence = 1.0f
                    )
                )
            }
        }

        // 이미 직접 언급된 종목은 제외하고 추가
        inferredStocks.forEach { stock ->
            if (!directStocks.contains(stock) && !existingStockCodes.contains(stock.code)) {
                finalResults.add(
                    CrawlAnalysisResultEntity(
                        targetId = event.targetId,
                        stock = stock,
                        confidence = 0.7f, // 간접 추론
                        matchType = "THEME"
                    )
                )
            }
        }

        if (finalResults.isNotEmpty()) {
            stockAnalysisRepository.saveAll(finalResults)
            logger.info("✅ Saved Analysis: ${finalResults.size} new items (Direct/Inferred).")
        } else {
            logger.info("ℹ️ Analysis skipped (Already extracted or empty).")
        }
    }

    @DltHandler
    fun handleDlt(event: CrawledItemEvent, exception: Exception) {
        logger.error("💀 Analysis Completely Failed for URL: ${event.url}", exception)

        // 사용자님의 logService를 활용하여 실패 기록 저장
        logService.record(
            key = "KafkaDLT:${event.targetId}",
            type = "ANALYSIS_FAILURE",
            msg = "Max retry reached. Error: ${exception.message}",
        )
    }
}