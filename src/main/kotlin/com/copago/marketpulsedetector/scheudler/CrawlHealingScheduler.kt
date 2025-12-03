package com.copago.marketpulsedetector.scheudler

import com.copago.marketpulsedetector.core.healing.SelfHealingService
import com.copago.marketpulsedetector.domain.repository.CrawlTargetRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory
import org.springframework.data.domain.PageRequest
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class CrawlHealingScheduler(
    private val targetRepository: CrawlTargetRepository,
    private val selfHealingService: SelfHealingService
) {
    private val logger = LoggerFactory.getLogger(CrawlHealingScheduler::class.java)
    private val healingScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    @Scheduled(fixedDelay = 60000)
    fun runHealingJob() {
        val pageable = PageRequest.of(0, 5)
        val brokenTargets = targetRepository.findAllByStatus("BROKEN", pageable)

        if (brokenTargets.isEmpty()) return

        logger.info("🏥 Found ${brokenTargets.size} broken tasks. Starting healing process...")

        // 2. 비동기 처리
        for (target in brokenTargets) {
            healingScope.launch {
                try {
                    selfHealingService.healBrokenTask(target.id!!)
                } catch (e: Exception) {
                    logger.error("Healing failed for target ${target.id}", e)
                }
            }
        }
    }
}