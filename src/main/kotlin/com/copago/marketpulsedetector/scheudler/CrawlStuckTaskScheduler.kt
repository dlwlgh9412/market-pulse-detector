package com.copago.marketpulsedetector.scheudler

import com.copago.marketpulsedetector.domain.repository.CrawlTargetRepository
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Component
class CrawlStuckTaskScheduler(
    private val crawlTargetRepository: CrawlTargetRepository,
) {
    private val logger = LoggerFactory.getLogger(CrawlStuckTaskScheduler::class.java)

    @Scheduled(fixedDelay = 600000) // 10분
    @SchedulerLock(name = "stuckTaskCleaner", lockAtMostFor = "PT5M", lockAtLeastFor = "PT1M")
    @Transactional
    fun cleanStuckTasks() {
        val cutoffTime = LocalDateTime.now().minusMinutes(30)

        val stuckTasks = crawlTargetRepository.findAllByStatusAndLastProcessedAtBefore("IN_PROGRESS", cutoffTime)

        if (stuckTasks.isNotEmpty()) {
            logger.warn("🧟 Found ${stuckTasks.size} stuck tasks. Marking them as FAILED.")

            for (task in stuckTasks) {
                task.status = "FAILED"
                task.retryCount++
            }
        }
    }
}