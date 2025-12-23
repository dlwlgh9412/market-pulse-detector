package com.copago.marketpulsedetector.core.scheduler

import com.copago.marketpulsedetector.core.properties.CrawlerProperties
import com.copago.marketpulsedetector.core.repository.CrawlQueueRepository
import com.copago.marketpulsedetector.core.service.CrawlQueueService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Component
class CrawlJobCleanupScheduler(
    private val queueRepository: CrawlQueueRepository,
    private val queueService: CrawlQueueService,
    private val properties: CrawlerProperties
) {
    private val logger = LoggerFactory.getLogger(CrawlJobCleanupScheduler::class.java)

    @Scheduled(fixedDelay = 60000)
    suspend fun cleanup() {
        val queue = withContext(Dispatchers.IO) {
            queueRepository.findTimeOutQueue()
        }

        if (queue != null) {
            queue.markAsProcessing(properties.workerId)
            queueService.process(queue)
        }
    }

    @Scheduled(cron = "0 0 3 * * *")
    @Transactional
    fun archive() {
        val cutoffTime = LocalDateTime.now()

        val migratedCount = queueRepository.migrateDoneJobs(cutoffTime)
        val deletedCount = queueRepository.deleteMigratedJobs(cutoffTime)

        if (migratedCount != deletedCount) {
            logger.warn("Archiving mismatch! Inserted: {}, Deleted: {}", migratedCount, deletedCount);
        } else {
            logger.info("Successfully archived {} jobs.", migratedCount);
        }
    }
}