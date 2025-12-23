package com.copago.marketpulsedetector.core.service

import com.copago.marketpulsedetector.core.domain.model.CrawlPageDefinition
import com.copago.marketpulsedetector.core.domain.model.CrawlQueue
import com.copago.marketpulsedetector.core.domain.model.CrawlSeed
import com.copago.marketpulsedetector.core.domain.model.CrawlSite
import com.copago.marketpulsedetector.core.domain.model.CrawlStatus
import com.copago.marketpulsedetector.core.domain.model.RuleType
import com.copago.marketpulsedetector.core.domain.model.SelectorType
import com.copago.marketpulsedetector.core.executor.CrawlJobExecutor
import com.copago.marketpulsedetector.core.fetcher.FetchRequest
import com.copago.marketpulsedetector.core.fetcher.Fetcher
import com.copago.marketpulsedetector.core.properties.CrawlerProperties
import com.copago.marketpulsedetector.core.repository.CrawlQueueRepository
import com.copago.marketpulsedetector.core.repository.CrawlSelectorRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import org.slf4j.LoggerFactory
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Service

@Service
class CrawlQueueService(
    private val queueRepository: CrawlQueueRepository,
    private val resultService: CrawlResultService,
    private val selectorRepository: CrawlSelectorRepository,
    private val duplicationService: DuplicationService,
    private val fetcher: Fetcher,
    private val properties: CrawlerProperties,
    private val crawlJobExecutor: CrawlJobExecutor
) {
    private val logger = LoggerFactory.getLogger(CrawlQueueService::class.java)

    suspend fun createQueues(queues: List<QueueCreateCommand>) = coroutineScope {
        if (queues.isEmpty()) return@coroutineScope

        val filteredQueues = queues.filter { !duplicationService.isDuplicateOrMark(it.url) }

        if (filteredQueues.isEmpty()) return@coroutineScope

        val entities = filteredQueues.map { queue ->
            CrawlQueue(
                site = queue.site,
                seed = queue.seed,
                pageDefinition = queue.targetPageDefinition,
                url = queue.url,
                status = CrawlStatus.READY
            )
        }

        try {
            withContext(Dispatchers.IO) {
                queueRepository.saveAll(entities)
            }
        } catch (e: DataIntegrityViolationException) {
            logger.warn("Duplicate URL detected during bulk save")
        }
    }

    suspend fun process(queue: CrawlQueue) = coroutineScope {
        crawlJobExecutor.execute(queue, properties.workerId) {
            logger.info("Processing ${queue.url}")
            val request = FetchRequest(url = queue.url)
            val response = fetcher.fetch(request)

            val doc = Jsoup.parse(response.body)

            val allSelectors = selectorRepository.findAllByPageDefinitionId(queue.pageDefinition.id!!)
            val extractedData = mutableMapOf<String, Any>()

            allSelectors.forEach { selector ->
                if (selector.ruleType == RuleType.DATA &&
                    selector.selectorType == SelectorType.CSS &&
                    !selector.fieldName.isNullOrBlank() &&
                    !selector.selectorValue.isNullOrBlank()
                ) {
                    var element = doc.select(selector.selectorValue)
                    if (!selector.innerSelectorValue.isNullOrBlank())
                        element = element.select(selector.innerSelectorValue)

                    if (!selector.extractAttribute.isNullOrBlank()) {
                        var value: String? = null
                        when (selector.extractAttribute) {
                            "text" -> value = element.text()
                            else -> value = element.attr(selector.extractAttribute)
                        }

                        if (!value.isNullOrBlank()) extractedData[selector.fieldName!!] = value
                    }
                }
            }

            resultService.createResult(
                CrawlResultCreateCommand(
                    queue = queue,
                    seed = queue.seed,
                    data = extractedData,
                )
            )
        }
    }
}

data class QueueCreateCommand(
    val url: String,
    val targetPageDefinition: CrawlPageDefinition,
    val seed: CrawlSeed,
    val site: CrawlSite,
    val metadata: Map<String, Any> = mutableMapOf()
)