package com.copago.marketpulsedetector.core.component

import com.copago.marketpulsedetector.domain.entity.CrawlTargetEntity
import com.copago.marketpulsedetector.domain.repository.CrawlPageRuleRepository
import com.copago.marketpulsedetector.domain.repository.CrawlPriorityRuleRepository
import com.copago.marketpulsedetector.domain.repository.CrawlTargetRepository
import com.copago.marketpulsedetector.common.utils.SecurityUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Component
class LinkDiscoverer(
    private val htmlFetcher: HtmlFetcher,
    private val crawlTargetRepository: CrawlTargetRepository,
    private val crawlPageRuleRepository: CrawlPageRuleRepository,
    private val crawlPriorityRuleRepository: CrawlPriorityRuleRepository,
    private val priorityAnalyzer: PriorityAnalyzer,
    private val errorHandler: CrawlTaskErrorHandler
) {
    private val logger = LoggerFactory.getLogger(LinkDiscoverer::class.java)

    /**
     * 목록형 페이지 크롤링
     * 다음 크롤링 대상을 찾는다.
     * @param targetId 코루틴 범위내의 작업으로 인해 엔티티를 넘기면 연관 엔티티를 조회할 수 없는 문제로 인해 targetId
     */
    @Transactional
    suspend fun executeDiscovery(targetId: Long) = withContext(Dispatchers.IO) {
        // 작업 조회
        val target = crawlTargetRepository.findWithSiteAndSourceAndPageRuleById(targetId)
            ?: throw RuntimeException("Target not found: $targetId")

        val currentTemplate = target.pageRule

        // 해당 작업에 대한 템플릿이 정의 되어있지 않으면 종료
        if (currentTemplate == null) {
            target.status = "ANALYSIS_REQUIRED"
            return@withContext
        }

        logger.info("[Discoverer] Processing Target: ${target.targetUrl} (Depth: ${target.depth})")

        // 작업 상태 변경(진행중)
        target.status = "IN_PROGRESS"
        target.lastProcessedAt = LocalDateTime.now()
        crawlTargetRepository.saveAndFlush(target)

        try {
            // 작업 템플릿 조회
            val allTemplates = crawlPageRuleRepository.findAllBySiteOrderByMatchPriorityDesc(target.site)

            val priorityRules = if (target.pageRule != null) {
                crawlPriorityRuleRepository.findAllByPageRule(target.pageRule!!)
            } else {
                emptyList()
            }
            // HTML 요청
            val doc = htmlFetcher.fetch(target)

            val searchScope = if (currentTemplate.linkSearchScope!!.isNotBlank()) {
                doc.select(currentTemplate.linkSearchScope)
            } else {
                doc.select("body")
            }

            var discoveredCount = 0

            val links = searchScope.select("a[href]")

            for (link in links) {
                val rawHref = link.attr("href").trim()
                if (rawHref.startsWith("#") || rawHref.isBlank()) continue

                val absUrl = link.attr("abs:href")
                val cleanUrl = absUrl.substringBefore("#")
                // 유효성 검사 (빈 값, 외부 도메인 제외)
                if (cleanUrl.isBlank() || !cleanUrl.contains(target.site.domain)) continue
                if (cleanUrl == target.targetUrl || cleanUrl == target.targetUrl.dropLast(1)) continue

                val matchedTemplate = allTemplates.firstOrNull { template ->
                    template.urlPatternRegex!!.toRegex().matches(cleanUrl)
                }

                if (matchedTemplate != null) {
                    val calculatedPriority = priorityAnalyzer.calculatePriority(link, priorityRules, basePriority = 50)

                    val newTarget = CrawlTargetEntity(
                        site = target.site,
                        pageRule = matchedTemplate,
                        targetUrl = absUrl,
                        pageType = matchedTemplate.pageType,
                        targetUrlHash = SecurityUtils.sha256(absUrl),
                        status = "PENDING",
                        depth = target.depth + 1,
                        source = target.source,
                        priority = calculatedPriority
                    )

                    crawlTargetRepository.upsert(
                        parentTargetId = target.id,
                        sourceId = target.source?.id
                            ?: throw IllegalStateException("Source is missing for target ${target.id}"),
                        pageRuleId = matchedTemplate.id,
                        siteId = target.site.id,
                        targetUrl = newTarget.targetUrl,
                        targetUrlHash = SecurityUtils.sha256(newTarget.targetUrl),
                        pageType = matchedTemplate.pageType,
                        status = "PENDING",
                        priority = newTarget.priority,
                        depth = target.depth + 1,
                    )
                    discoveredCount++
                    logger.debug("  Found [${matchedTemplate.pageType}]: $absUrl")
                }
            }

            target.status = "DONE"
            logger.info("[Discoverer] Finished. Discovered $discoveredCount new targets.")
        } catch (e: Exception) {
            errorHandler.handleError(target, e)
        } finally {
            crawlTargetRepository.save(target)
        }
    }
}