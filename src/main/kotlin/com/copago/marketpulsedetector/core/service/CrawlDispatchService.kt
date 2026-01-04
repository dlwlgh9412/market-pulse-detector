package com.copago.marketpulsedetector.core.service

import com.copago.marketpulsedetector.core.domain.model.CrawlStatus
import com.copago.marketpulsedetector.core.repository.CrawlQueueRepository
import com.copago.marketpulsedetector.core.repository.CrawlSeedRepository
import com.copago.marketpulsedetector.core.repository.redis.CrawlQueueManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service
class CrawlDispatchService(
    private val seedRepository: CrawlSeedRepository,
    private val queueRepository: CrawlQueueRepository,

    private val seedService: CrawlSeedService,
    private val queueService: CrawlQueueService,
) {
    suspend fun dispatch(siteInfo: CrawlQueueManager.Site): Boolean {
        val now = System.currentTimeMillis()

        val dueSeeds = withContext(Dispatchers.IO) {
            seedRepository.findDueSeeds(siteInfo.id, now)
        }

        if (dueSeeds.isNotEmpty()) {
            seedService.process(dueSeeds, siteInfo.metadata.rateLimitMs)
            return true
        }

        val nextTask = withContext(Dispatchers.IO) {
            queueRepository.findNextCrawlJob(
                siteInfo.id,
                CrawlStatus.READY,
                LocalDateTime.now(),
                PageRequest.of(0, 1)
            ).firstOrNull()
        }

        if (nextTask != null) {
            queueService.process(nextTask)
            return true
        }

        return false
    }
}