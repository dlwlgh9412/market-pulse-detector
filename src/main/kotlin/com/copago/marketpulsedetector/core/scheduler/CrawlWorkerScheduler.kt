package com.copago.marketpulsedetector.core.scheduler

import com.copago.marketpulsedetector.core.config.CrawlerConfig
import com.copago.marketpulsedetector.core.repository.redis.CrawlQueueManager
import com.copago.marketpulsedetector.core.service.CrawlDispatchService
import jakarta.annotation.PostConstruct
import jakarta.annotation.PreDestroy
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import kotlin.coroutines.cancellation.CancellationException

@Component
class CrawlWorkerScheduler(
    private val siteQueue: CrawlQueueManager,
    private val dispatchService: CrawlDispatchService,
    private val config: CrawlerConfig
) {
    private val logger = LoggerFactory.getLogger(javaClass)
    private val fetchSize by lazy { config.batchFetchSize }
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private val siteChannel = Channel<CrawlQueueManager.Site>(Channel.BUFFERED)

    @PostConstruct
    fun start() {
        startProducer()
        repeat(config.concurrencyLimit) {
            startConsumer(it)
        }
    }

    private fun startProducer() = scope.launch {
        while (isActive) {
            try {
                if (siteChannel.isEmpty) {
                    val sites = siteQueue.popAvailableSites(fetchSize)

                    if (sites.isEmpty()) {
                        delay(1000)
                    } else {
                        sites.forEach { siteChannel.send(it) }
                    }
                } else {
                    delay(100)
                }
            } catch (e: Exception) {
                logger.error("Worker Producer Error", e)
                delay(1000)
            }
        }
    }

    private fun startConsumer(id: Int) = scope.launch {
        for (site in siteChannel) {
            try {
                process(site)
            } catch (e: Exception) {
                logger.error("Worker $id Consumer Error", e)
            }
        }
    }

    private suspend fun process(site: CrawlQueueManager.Site) {
        val siteId = site.id
        val metadata = site.metadata
        var nextDelay = site.metadata.rateLimitMs

        try {
            siteQueue.runWithHeartbeat(siteId, metadata.timeoutMs) {
                if (!dispatchService.dispatch(site))
                    nextDelay = 1000L
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            logger.error("❌ Error processing site $siteId", e)
        } finally {
            withContext(NonCancellable + Dispatchers.IO) {
                try {
                    siteQueue.completeAndReschedule(siteId, nextDelay)
                } catch (e: Exception) {
                    logger.error("Failed to reschedule site $siteId", e)
                }
            }
        }
    }

    @PreDestroy
    fun destroy() {
        scope.cancel()
    }
}