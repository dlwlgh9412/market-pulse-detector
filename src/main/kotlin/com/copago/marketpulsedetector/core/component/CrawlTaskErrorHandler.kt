package com.copago.marketpulsedetector.core.component

import com.copago.marketpulsedetector.common.exception.FatalException
import com.copago.marketpulsedetector.common.exception.RetryableException
import com.copago.marketpulsedetector.common.exception.StructuralException
import com.copago.marketpulsedetector.domain.entity.CrawlTargetEntity
import com.copago.marketpulsedetector.domain.repository.CrawlTargetRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import kotlin.math.pow

@Component
class CrawlTaskErrorHandler(
    private val repository: CrawlTargetRepository
) {
    private val logger = LoggerFactory.getLogger(CrawlTaskErrorHandler::class.java)

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun handleError(target: CrawlTargetEntity, e: Throwable) {
        when (e) {
            is RetryableException -> handleRetry(target, e)
            is FatalException -> handleFatal(target, e)
            is StructuralException -> handleBroken(target, e)
            else -> handleFatal(target, e)
        }
        repository.save(target)
    }

    private fun handleRetry(target: CrawlTargetEntity, e: Throwable) {
        val maxRetry = 5;
        if (target.retryCount < maxRetry) {
            target.retryCount++
            target.status = "PENDING"

            val backoffSeconds = 5 * 2.0.pow((target.retryCount - 1).toDouble()).toLong()
            target.nextTryAt = LocalDateTime.now().plusSeconds(backoffSeconds)
            logger.warn("Retry scheduled for ${target.targetUrl} (Count: ${target.retryCount}, Delay: ${backoffSeconds}s). Reason: ${e.message}")
        } else {
            target.status = "FAILED"
            logger.error("Max retry reached for ${target.targetUrl}. Marking as FAILED.")
        }
    }

    private fun handleFatal(target: CrawlTargetEntity, e: Throwable) {
        target.status = "FAILED"
        logger.error("Fatal error for ${target.targetUrl}: ${e.message}")
    }

    private fun handleBroken(target: CrawlTargetEntity, e: Throwable) {
        target.status = "BROKEN"
        logger.error("Structure broken for ${target.targetUrl}: ${e.message}")
    }
}