package com.copago.marketpulsedetector.core.component

import com.copago.marketpulsedetector.common.event.CrawledItemEvent
import com.copago.marketpulsedetector.common.exception.StructuralException
import com.copago.marketpulsedetector.domain.entity.CrawlTargetEntity

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class ContentExtractor(
    private val dbService: ContentExtractorService,
    private val htmlFetcher: HtmlFetcher,
    private val errorHandler: CrawlTaskErrorHandler
) {
    private val logger = LoggerFactory.getLogger(ContentExtractor::class.java)

    suspend fun executeExtraction(targetId: Long) = withContext(Dispatchers.IO) {
        var target: CrawlTargetEntity? = null

        try {
            val (loadedTarget, extractionRules) = dbService.prepareExtraction(targetId)
            target = loadedTarget

            if (target.pageRule == null || extractionRules.isEmpty()) {
                logger.warn("No extraction rules for target: ${target.targetUrl}")
                dbService.markAsFailed(targetId, "No Rules")
                return@withContext
            }

            logger.info("[Extractor] Processing: ${target.targetUrl}")

            val doc = htmlFetcher.fetch(target)

            val extractedData = mutableMapOf<String, Any>()
            var failureCount = 0

            for (rule in extractionRules) {
                val element = doc.selectFirst(rule.cssSelector)
                var isSuccess = false

                if (element != null) {
                    val value = if (!rule.extractAttributes.isNullOrBlank()) {
                        element.attr(rule.extractAttributes)
                    } else {
                        element.text()
                    }
                    if (value.isNotBlank()) {
                        extractedData[rule.jsonKey] = value
                        isSuccess = true
                    }
                }

                if (!isSuccess && rule.isRequired) failureCount++
            }

            if (extractedData.isEmpty() || failureCount > 0) {
                throw StructuralException("Extraction failed. Failures: $failureCount")
            }

            // 4. [DB Transaction] 저장
            dbService.completeExtraction(target.id!!, extractedData)
            logger.info("[Extractor] Success.")
        } catch (e: Exception) {
            target?.let { errorHandler.handleError(it, e) }
        }
    }
}