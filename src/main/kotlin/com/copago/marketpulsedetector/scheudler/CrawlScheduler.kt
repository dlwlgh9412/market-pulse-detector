package com.copago.marketpulsedetector.scheudler

import com.copago.marketpulsedetector.config.RedisRateLimiter
import com.copago.marketpulsedetector.core.dispatcher.CrawlDispatcher
import com.copago.marketpulsedetector.core.generator.CrawlSeedTaskGenerator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.lang.Exception

@Component
class CrawlScheduler(
    private val sourceInjector: CrawlSeedTaskGenerator,
    private val redisRateLimiter: RedisRateLimiter,
    private val crawlDispatcher: CrawlDispatcher,
) {
    private val logger = LoggerFactory.getLogger(CrawlScheduler::class.java)
    private val crawlerScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /**
     * 주기: 60초마다 체크
     * 역할: CrawlSource를 확인하여 실행 시간이 된 시드 URL을 큐에 넣음 (새로운 기사 목록이 있는지 확인하는 용도로 다른 작업과 개별로 동작)
     */
    @Scheduled(fixedDelay = 60000)
    fun runInjectorJob() {
        try {
            sourceInjector.injectSources()
        } catch (e: Exception) {
        }
    }

    /**
     * 크롤링 작업 시작
     */
    @Scheduled(fixedDelay = 100)
    fun runCrawlWorker() {
        val batchSize = 10
        // 요청 가능한(요청 주기 범위 안에 포함된) 도메인 아이디 조회
        val availableSiteIds = redisRateLimiter.popAvailableDomains(limit = batchSize, defaultDelayMs = 5000)

        if (availableSiteIds.isEmpty()) return

        logger.info("⚡ Fetched ${availableSiteIds.size} available sites from Redis: $availableSiteIds")

        // 가져온 도메인들을 코루틴 크롤링 작업 시작
        for (siteId in availableSiteIds) {
            crawlerScope.launch {
                try {
                    crawlDispatcher.dispatch(siteId)
                } catch (e: Exception) {
                    logger.error("Coroutine failed for siteId: $siteId", e)
                }
            }
        }
    }
}