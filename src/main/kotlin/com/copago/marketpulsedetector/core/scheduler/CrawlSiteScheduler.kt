package com.copago.marketpulsedetector.core.scheduler

import com.copago.marketpulsedetector.core.repository.CrawlSiteRepository
import com.copago.marketpulsedetector.core.repository.redis.CrawlQueueManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class CrawlSiteScheduler(
    private val crawlSiteRepository: CrawlSiteRepository,
    private val schedulingQueue: CrawlQueueManager
) {
    private val logger = LoggerFactory.getLogger(CrawlSiteScheduler::class.java)
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    @Scheduled(fixedDelay = 60000)
    fun syncSite() {
        scope.launch {

            val allSites = withContext(Dispatchers.IO) {
                crawlSiteRepository.findAll()
            }

            // DB에 존재하는 ID Set
            val activeIds = allSites
                .filter { it.id != null && it.isActive }
                .map { it.id.toString() }
                .toSet()
            // Redis에 존재하는 ID Set
            val redisIds = withContext(Dispatchers.IO) {
                schedulingQueue.getAllSiteIds()
            }

            // 삭제 대상 (Redis에 있지만 DB에 없는 것)
            val removeIds = redisIds - activeIds

            // 추가 대상 (DB에는 있지만 Redis에는 없는 것)
            val addIds = activeIds - removeIds

            // 데이터 삭제 (DB에는 없지만 Redis에만 있는 것)
            if (removeIds.isNotEmpty()) {
                val removedCount = withContext(Dispatchers.IO) {
                    schedulingQueue.removeSites(removeIds.toList())
                }
                logger.info("🗑️ Removed $removedCount inactive/deleted sites from Redis: $removeIds")
            }

            // 누락 데이터 추가 (DB에는 있지만 Redis에는 없는 것)
            if (addIds.isNotEmpty()) {
                val syncInfos = allSites
                    .filter { it.id.toString() in addIds }
                    .map {
                        CrawlQueueManager.SiteInfo(
                            id = it.id!!,
                            rateLimitMs = it.rateLimitMs,
                            timeoutMs = it.timeout,
                            isActive = it.isActive
                        )
                    }

                withContext(Dispatchers.IO) {
                    schedulingQueue.syncSites(syncInfos)
                }
            }
        }
    }
}