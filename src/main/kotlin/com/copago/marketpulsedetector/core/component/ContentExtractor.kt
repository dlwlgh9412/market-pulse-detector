package com.copago.marketpulsedetector.core.component

import com.copago.marketpulsedetector.common.exception.StructuralException
import com.copago.marketpulsedetector.domain.entity.CrawledDataEntity
import com.copago.marketpulsedetector.domain.repository.CrawlExtractionRuleRepository
import com.copago.marketpulsedetector.domain.repository.CrawlTargetRepository
import com.copago.marketpulsedetector.domain.repository.CrawledDataRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Component
class ContentExtractor(
    private val taskErrorHandler: CrawlTaskErrorHandler,
    private val htmlFetcher: HtmlFetcher,
    private val crawlTargetRepository: CrawlTargetRepository,
    private val crawlExtractionRuleRepository: CrawlExtractionRuleRepository,
    private val crawledDataRepository: CrawledDataRepository,
    private val errorHandler: CrawlTaskErrorHandler
) {
    private val logger = LoggerFactory.getLogger(ContentExtractor::class.java)

    @Transactional
    suspend fun executeExtraction(targetId: Long) = withContext(Dispatchers.IO) {
        val target = crawlTargetRepository.findWithSiteAndSourceAndPageRuleById(targetId)
            ?: throw RuntimeException("Target not found: $targetId")

        val pageRule = target.pageRule

        // 해당 작업에 대한 템플릿이 정의 되어있지 않으면 종료
        if (pageRule == null) {
            target.status = "ANALYSIS_REQUIRED"
            return@withContext
        }

        logger.info("[Extractor] Processing Target: ${target.targetUrl}")
        target.status = "IN_PROGRESS"
        target.lastProcessedAt = LocalDateTime.now()
        crawlTargetRepository.saveAndFlush(target)

        try {
            val rules = crawlExtractionRuleRepository.findAllByPageRuleAndStatus(target.pageRule!!, "ACTIVE")

            if (rules.isEmpty()) {
                logger.warn("No ACTIVE rules for template: ${target.pageRule?.ruleName}")
                target.status = "FAILED"
                return@withContext
            }

            val doc = htmlFetcher.fetch(target)

            val extractedData = mutableMapOf<String, Any>()
            var failureCount = 0

            for (rule in rules) {
                val element = doc.selectFirst(rule.cssSelector)
                var isSuccess = false

                if (element != null) {
                    var value = if (!rule.extractAttributes.isNullOrBlank()) {
                        element.attr(rule.extractAttributes)
                    } else {
                        element.text()
                    }

                    if (value.isNotBlank()) {
                        extractedData[rule.jsonKey] = value
                        isSuccess = true
                    } else {
                        if (rule.isRequired) {
                            logger.warn("Extracted value is empty...")
                        } else {
                            logger.debug("Optional: Found element but value is empty. [${rule.jsonKey}]")
                        }
                    }
                } else {
                    if (rule.isRequired) {
                        logger.warn("Critical: Selector failed. Element not found. [${rule.jsonKey}] ${rule.cssSelector}")
                    } else {
                        logger.debug("Optional: Selector failed. [${rule.jsonKey}]")
                    }
                }

                if (!isSuccess && rule.isRequired) {
                    failureCount++
                }
            }

            if (extractedData.isEmpty() || failureCount > 0) {
                throw StructuralException(
                    "Extraction failed. Required failures: $failureCount, Total extracted: ${extractedData.size}"
                )
            }
            val data = CrawledDataEntity(
                target = target,
                data = extractedData,
                sourceUrl = target.targetUrl
            )

            crawledDataRepository.save(data)
            target.status = "DONE"
            target.retryCount = 0
            logger.info("[Extractor] Success. Saved dataId: ${data.id}")

        } catch (e: Exception) {
            errorHandler.handleError(target, e)
        } finally {
            crawlTargetRepository.save(target)
        }
    }
}