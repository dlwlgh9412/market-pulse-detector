package com.copago.marketpulsedetector.core.dispatcher

import com.copago.marketpulsedetector.core.component.ContentExtractor
import com.copago.marketpulsedetector.core.component.LinkDiscoverer
import com.copago.marketpulsedetector.domain.repository.CrawlTargetRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class CrawlDispatcher(
    private val crawlTargetRepository: CrawlTargetRepository,
    private val linkDiscoverer: LinkDiscoverer,
    private val contentExtractor: ContentExtractor
) {
    private val logger = LoggerFactory.getLogger(CrawlDispatcher::class.java)

    /**
     * 크롤링 대상 타입 (LIST, CONTENT) 분기
     */
    @Transactional
    suspend fun dispatch(siteId: Long) {
        // 도메인 아이디를 기준으로 가능한 작업 조회 (내림차순 우선순위)
        val target = withContext(Dispatchers.IO) {
            crawlTargetRepository.findTopBySiteIdAndStatusOrderByPriorityDescIdAsc(siteId, "PENDING")
        } ?: return

        logger.info("🚀 Dispatching Target[${target.id}] Type=${target.pageType} Priority=${target.priority}")

        try {
            when (target.pageType) {
                // 목록형 작업
                "LIST" -> linkDiscoverer.executeDiscovery(target.id!!)
                // 컨텐츠형 작업
                "CONTENT" -> contentExtractor.executeExtraction(target.id!!)
                else -> logger.warn("Unknown target type: ${target.pageType}")
            }
        } catch (e: Exception) {
            logger.error("Error during dispatch for target ${target.id}", e)
        }
    }
}