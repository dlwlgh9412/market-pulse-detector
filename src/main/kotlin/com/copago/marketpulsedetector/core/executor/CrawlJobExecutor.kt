package com.copago.marketpulsedetector.core.executor

import com.copago.marketpulsedetector.core.domain.model.CrawlQueue
import com.copago.marketpulsedetector.core.repository.CrawlQueueRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.hibernate.query.sqm.ParsingException
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.HttpServerErrorException
import java.io.IOException
import java.net.ConnectException
import java.net.MalformedURLException
import java.net.SocketTimeoutException

@Component
class CrawlJobExecutor(
    private val queueRepository: CrawlQueueRepository
) {
    private val logger = LoggerFactory.getLogger(CrawlJobExecutor::class.java)

    suspend fun execute(queue: CrawlQueue, workerId: String, block: suspend () -> Unit) {
        try {
            queue.markAsProcessing(workerId)
            withContext(Dispatchers.IO) {
                queueRepository.save(queue)
            }

            block()

            queue.markAsDone()
            withContext(Dispatchers.IO) {
                queueRepository.save(queue)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            logger.error("Job Failed [${queue.id}]: ${e.message}", e)
            handleFailure(queue, e)
        }
    }

    private fun handleFailure(queue: CrawlQueue, e: Exception) {
        try {
            val action = classifyException(e)
            when (action) {
                FailureAction.RETRY -> {
                    queue.markAsRetry()
                    logger.info("-> Retrying queue [${queue.id}] (Count: ${queue.retryCount})")
                }

                FailureAction.FAIL_IMMEDIATELY -> {
                    queue.markAsFailed()
                    logger.error("-> Fatal Error. Stopped queue [${queue.id}] immediately.")
                }
            }

            queueRepository.save(queue)
        } catch (e: Exception) {
            logger.error("Failed to save queue status", e)
        }
    }

    private fun classifyException(e: Exception): FailureAction {
        return when (e) {
            is NullPointerException,
            is IllegalArgumentException,
            is MalformedURLException,
            is ParsingException -> FailureAction.FAIL_IMMEDIATELY

            is HttpClientErrorException -> {
                when (e.statusCode.value()) {
                    400, 401, 403, 404 -> FailureAction.FAIL_IMMEDIATELY
                    else -> FailureAction.RETRY
                }
            }

            is SocketTimeoutException,
            is ConnectException,
            is HttpServerErrorException,
            is IOException -> FailureAction.RETRY

            else -> FailureAction.RETRY
        }
    }

    private enum class FailureAction {
        RETRY, FAIL_IMMEDIATELY
    }
}