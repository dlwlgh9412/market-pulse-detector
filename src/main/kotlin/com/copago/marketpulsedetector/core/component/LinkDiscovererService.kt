package com.copago.marketpulsedetector.core.component

import com.copago.marketpulsedetector.domain.entity.CrawlPageRuleEntity
import com.copago.marketpulsedetector.domain.entity.CrawlPriorityRuleEntity
import com.copago.marketpulsedetector.domain.entity.CrawlTargetEntity
import com.copago.marketpulsedetector.domain.repository.CrawlPageRuleRepository
import com.copago.marketpulsedetector.domain.repository.CrawlPriorityRuleRepository
import com.copago.marketpulsedetector.domain.repository.CrawlTargetRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
class LinkDiscovererService(
    private val crawlTargetRepository: CrawlTargetRepository,
    private val crawlPageRuleRepository: CrawlPageRuleRepository,
    private val crawlPriorityRuleRepository: CrawlPriorityRuleRepository
) {
    @Transactional
    fun prepareDiscovery(targetId: Long): Triple<CrawlTargetEntity, List<CrawlPageRuleEntity>, List<CrawlPriorityRuleEntity>> {
        val target = crawlTargetRepository.findWithSiteAndSourceAndPageRuleById(targetId)
            ?: throw RuntimeException("Target not found: $targetId")

        target.status = "IN_PROGRESS"
        target.lastProcessedAt = LocalDateTime.now()

        val templates = crawlPageRuleRepository.findAllBySiteOrderByMatchPriorityDesc(target.site)
        val priorityRules = target.pageRule?.let {
            crawlPriorityRuleRepository.findAllByPageRule(it)
        } ?: emptyList()

        return Triple(target, templates, priorityRules)
    }

    @Transactional
    fun completeDiscovery(parentId: Long, newTargets: List<CrawlTargetEntity>) {
        val parentTarget = crawlTargetRepository.findById(parentId).orElseThrow()
        parentTarget.status = "DONE"

        for (target in newTargets) {
            crawlTargetRepository.upsert(
                parentTargetId = parentId,
                sourceId = target.source?.id,
                pageRuleId = target.pageRule?.id,
                siteId = target.site.id,
                targetUrl = target.targetUrl,
                targetUrlHash = target.targetUrlHash,
                pageType = target.pageType,
                status = "PENDING",
                priority = target.priority,
                depth = target.depth
            )
        }
    }
}