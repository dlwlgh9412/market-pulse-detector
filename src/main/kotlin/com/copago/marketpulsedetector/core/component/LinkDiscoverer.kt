package com.copago.marketpulsedetector.core.component

import com.copago.marketpulsedetector.domain.entity.CrawlTargetEntity
import com.copago.marketpulsedetector.common.utils.SecurityUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class LinkDiscoverer(
    private val dbService: LinkDiscovererService,
    private val htmlFetcher: HtmlFetcher,
    private val priorityAnalyzer: PriorityAnalyzer,
    private val errorHandler: CrawlTaskErrorHandler
) {
    private val logger = LoggerFactory.getLogger(LinkDiscoverer::class.java)

    /**
     * 목록형 페이지 크롤링
     * 다음 크롤링 대상을 찾는다.
     * @param targetId 코루틴 범위내의 작업으로 인해 엔티티를 넘기면 연관 엔티티를 조회할 수 없는 문제로 인해 targetId
     */
    suspend fun executeDiscovery(targetId: Long) = withContext(Dispatchers.IO) {
        // 작업 조회
        var target: CrawlTargetEntity? = null

        try {
            val (loadedTarget, templates, priorityRules) = dbService.prepareDiscovery(targetId)
            target = loadedTarget

            if (target.pageRule == null) {
                return@withContext
            }

            logger.info("[Discoverer] Processing Target: ${target.targetUrl}")

            val doc = htmlFetcher.fetch(target)

            val currentTemplate = target.pageRule!!
            val searchScope = if (!currentTemplate.linkSearchScope.isNullOrBlank()) {
                doc.select(currentTemplate.linkSearchScope)
            } else {
                doc.select("body")
            }

            val newTargets = mutableListOf<CrawlTargetEntity>()
            val links = searchScope.select("a[href]")

            for (link in links) {
                val rawHref = link.attr("href").trim()
                if (rawHref.startsWith("#") || rawHref.isBlank()) continue

                val absUrl = link.attr("abs:href")
                val cleanUrl = absUrl.substringBefore("#")

                if (!cleanUrl.contains(target.site.domain)) continue
                if (cleanUrl == target.targetUrl) continue

                val matchedTemplate = templates.firstOrNull {
                    it.urlPatternRegex?.toRegex()?.matches(cleanUrl) == true
                }

                if (matchedTemplate != null) {
                    val priority = priorityAnalyzer.calculatePriority(link, priorityRules)
                    newTargets.add(
                        CrawlTargetEntity(
                            site = target.site,
                            pageRule = matchedTemplate,
                            targetUrl = absUrl,
                            targetUrlHash = SecurityUtils.sha256(absUrl),
                            pageType = matchedTemplate.pageType,
                            priority = priority,
                            depth = target.depth + 1,
                            source = target.source
                        )
                    )
                }
            }

            dbService.completeDiscovery(target.id!!, newTargets)
            logger.info("[Discoverer] Finished. Discovered ${newTargets.size} new targets.")
        } catch (e: Exception) {
            target?.let { errorHandler.handleError(it, e) }
        }
    }
}