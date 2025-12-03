package com.copago.marketpulsedetector.core.generator

import com.copago.marketpulsedetector.common.utils.SecurityUtils
import com.copago.marketpulsedetector.domain.entity.CrawlTargetEntity
import com.copago.marketpulsedetector.domain.repository.CrawlSourceRepository
import com.copago.marketpulsedetector.domain.repository.CrawlTargetRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
class CrawlSeedTaskGenerator(
    private val crawlSourceRepository: CrawlSourceRepository,
    private val crawlTargetRepository: CrawlTargetRepository
) {
    private val logger = LoggerFactory.getLogger(CrawlSeedTaskGenerator::class.java)

    @Transactional
    fun injectSources() {
        val now = LocalDateTime.now()

        val activeSources = crawlSourceRepository.findAll().filter { it.isActive }
        var injectedCount = 0

        for (source in activeSources) {
            val nextRunTime = source.lastGeneratedAt?.plusSeconds(source.crawlIntervalSec.toLong()) ?: LocalDateTime.MIN

            if (now.isAfter(nextRunTime)) {
                val newTarget = CrawlTargetEntity(
                    source = source,
                    site = source.site,
                    pageRule = source.pageRule,
                    targetUrl = source.sourceUrl,
                    targetUrlHash = SecurityUtils.Companion.sha256(source.sourceUrl),
                    pageType = source.pageRule.pageType,
                    status = "PENDING",
                    priority = 100,
                    depth = 1
                )

                crawlTargetRepository.upsert(
                    parentTargetId = null,
                    sourceId = newTarget.source?.id,
                    pageRuleId = newTarget.pageRule?.id,
                    siteId = newTarget.site.id!!,
                    targetUrl = newTarget.targetUrl,
                    targetUrlHash = newTarget.targetUrlHash,
                    pageType = newTarget.pageType,
                    status = newTarget.status,
                    priority = newTarget.priority,
                    depth = newTarget.depth
                )

                source.lastGeneratedAt = now
                injectedCount++
                logger.info("💉 Injected Seed: ${source.description} (${source.sourceUrl})")
            }
        }

        if (injectedCount > 0) {
            logger.info("Finished injecting $injectedCount sources.")
        }
    }
}