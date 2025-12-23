package com.copago.marketpulsedetector.core.service

import com.copago.marketpulsedetector.core.domain.model.CrawlSeed
import com.copago.marketpulsedetector.core.domain.model.RuleType
import com.copago.marketpulsedetector.core.domain.model.SelectorType
import com.copago.marketpulsedetector.core.fetcher.FetchRequest
import com.copago.marketpulsedetector.core.fetcher.Fetcher
import com.copago.marketpulsedetector.core.repository.CrawlSeedRepository
import com.copago.marketpulsedetector.core.repository.CrawlSelectorRepository
import com.copago.marketpulsedetector.core.utils.forEachWithInterval
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import org.jsoup.select.Elements
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.concurrent.TimeUnit
import kotlin.collections.mapNotNull

@Service
class CrawlSeedService(
    private val crawlSeedRepository: CrawlSeedRepository,
    private val crawlSelectorRepository: CrawlSelectorRepository,
    private val crawlQueueService: CrawlQueueService,
    private val fetcher: Fetcher
) {
    private val logger = LoggerFactory.getLogger(CrawlSeedService::class.java)

    suspend fun process(seeds: List<CrawlSeed>, rateLimitMs: Long) = coroutineScope {
        val pageDefIds = seeds.mapNotNull { it.pageDefinition.id }.distinct()
        if (pageDefIds.isEmpty()) return@coroutineScope

        val allSelectors = withContext(Dispatchers.IO) {
            crawlSelectorRepository.findAllByPageDefinitionIdIn(pageDefIds)
        }

        val selectorMap = allSelectors.groupBy { it.pageDefinition.id }

        seeds.forEachWithInterval(rateLimitMs) { seed ->
            logger.info("❗️Processing Seed.. {}", seed)

            val request = FetchRequest(
                url = seed.url,
                timeout = seed.site.timeout
            )

            try {
                val response = fetcher.fetch(request)

                val doc = Jsoup.parse(response.body)

                val selectors = selectorMap.getValue(seed.pageDefinition.id)

                val discoveredLinks = mutableListOf<QueueCreateCommand>()

                selectors.forEach { selector ->
                    var containers: Elements? = null

                    if (selector.ruleType == RuleType.LINK && selector.selectorType == SelectorType.CSS) {
                        selector.selectorValue?.let { value -> containers = doc.select(value) }
                    }

                    containers?.forEach { container ->
                        val linkEl = if (!selector.innerSelectorValue.isNullOrBlank()) {
                            container.select(selector.innerSelectorValue).first()
                        } else {
                            container.select("a").first()
                        }

                        if (linkEl != null) {
                            val url = linkEl.absUrl("href")

                            if (url.isNotBlank() && selector.targetPageDefinition != null) {
                                discoveredLinks.add(
                                    QueueCreateCommand(
                                        url = url,
                                        targetPageDefinition = selector.targetPageDefinition!!,
                                        seed = seed,
                                        site = seed.site
                                    )
                                )
                            }
                        }
                    }
                }

                if (discoveredLinks.isNotEmpty()) {
                    val uniqueLinks = discoveredLinks.distinctBy { it.url }
                    crawlQueueService.createQueues(uniqueLinks)
                }

                seed.nextRunAt = System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(seed.intervalSeconds.toLong())
                crawlSeedRepository.save(seed)
            } catch (e: Exception) {
                logger.error("Fetch failed for seed ${seed.id}", e)
            }
        }
    }
}