package com.copago.marketpulsedetector.config

import com.copago.marketpulsedetector.domain.repository.CrawlSiteRepository
import org.slf4j.LoggerFactory
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.annotation.Configuration
import org.springframework.context.event.EventListener
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.transaction.annotation.Transactional

@Configuration
class CrawlSiteInitializer(
    private val crawlSiteRepository: CrawlSiteRepository,
    private val longValueRedisTemplate: RedisTemplate<String, Long>,
) {
    companion object {
        private const val ZSET_KEY: String = "crawler:scheduler:domains"
    }

    private val logger = LoggerFactory.getLogger(CrawlSiteInitializer::class.java)

    /**
     * 도메인별 요청제한 값(현재시간 + 요청주기) 레디스 ZSet 등록
     */
    @EventListener(ApplicationReadyEvent::class)
    @Transactional(readOnly = true)
    fun initCrawlDomains() {
        logger.info(">>> Initializing CrawlDomains")

        val domains = crawlSiteRepository.findAll()

        if (domains.isEmpty()) {
            logger.warn(">>> Not Found CrawlDomains")
            return
        }

        val now = System.currentTimeMillis()
        val score: Double = now.toDouble()
        var newCount = 0

        for (domain in domains) {
            val isAdded = longValueRedisTemplate.opsForZSet().addIfAbsent(ZSET_KEY, domain.id!!, score)

            if (isAdded == true) {
                newCount++
            }
        }

        logger.info(">>> [Initializing] Complete Synchronizing CrawlDomains: {} New Add: {}", domains.size, newCount)
    }
}