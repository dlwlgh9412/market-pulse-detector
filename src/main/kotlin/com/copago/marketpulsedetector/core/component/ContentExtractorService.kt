package com.copago.marketpulsedetector.core.component

import com.copago.marketpulsedetector.common.event.CrawledItemEvent
import com.copago.marketpulsedetector.core.producer.CrawlerKafkaProducer
import com.copago.marketpulsedetector.domain.entity.CrawlExtractionRuleEntity
import com.copago.marketpulsedetector.domain.entity.CrawlTargetEntity
import com.copago.marketpulsedetector.domain.entity.CrawledDataEntity
import com.copago.marketpulsedetector.domain.repository.CrawlExtractionRuleRepository
import com.copago.marketpulsedetector.domain.repository.CrawlTargetRepository
import com.copago.marketpulsedetector.domain.repository.CrawledDataRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
class ContentExtractorService(
    private val crawlTargetRepository: CrawlTargetRepository,
    private val crawlExtractionRuleRepository: CrawlExtractionRuleRepository,
    private val crawledDataRepository: CrawledDataRepository,
    private val crawlerKafkaProducer: CrawlerKafkaProducer
) {
    @Transactional
    fun prepareExtraction(targetId: Long): Pair<CrawlTargetEntity, List<CrawlExtractionRuleEntity>> {
        val target = crawlTargetRepository.findWithSiteAndSourceAndPageRuleById(targetId)
            ?: throw RuntimeException("Target not found: $targetId")

        target.status = "IN_PROGRESS"
        target.lastProcessedAt = LocalDateTime.now()

        val rules = if (target.pageRule != null) {
            crawlExtractionRuleRepository.findAllByPageRuleAndStatus(target.pageRule!!, "ACTIVE")
        } else emptyList()

        return Pair(target, rules)
    }

    @Transactional
    fun markAsFailed(targetId: Long, reason: String) {
        val target = crawlTargetRepository.findById(targetId).orElseThrow()
        target.status = "FAILED"
    }

    @Transactional
    fun completeExtraction(targetId: Long, extractedData: Map<String, Any>) {
        val target = crawlTargetRepository.findById(targetId).orElseThrow()

        val dataEntity = CrawledDataEntity(
            target = target,
            data = extractedData,
            sourceUrl = target.targetUrl
        )
        crawledDataRepository.save(dataEntity)

        target.status = "DONE"
        target.retryCount = 0

        val title = extractedData["title"]?.toString() ?: ""
        val content = extractedData["content"]?.toString() ?: ""

        if (title.isNotBlank()) {
            val event = CrawledItemEvent(
                targetId = targetId,
                title = title,
                content = content,
                url = target.targetUrl
            )

            crawlerKafkaProducer.send(event)
        }
    }
}