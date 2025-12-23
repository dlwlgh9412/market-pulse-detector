package com.copago.marketpulsedetector.core.service

import com.copago.marketpulsedetector.core.domain.model.CrawlQueue
import com.copago.marketpulsedetector.core.domain.model.CrawlResult
import com.copago.marketpulsedetector.core.domain.model.CrawlSeed
import com.copago.marketpulsedetector.core.repository.CrawlResultRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import org.springframework.stereotype.Service

@Service
class CrawlResultService(
    private val crawlResultRepository: CrawlResultRepository
) {
    suspend fun createResult(result: CrawlResultCreateCommand) = coroutineScope {
        val result = CrawlResult(
            queue = result.queue,
            seedId = result.seed.id!!,
            extractedData = result.data
        )

        withContext(Dispatchers.IO) {
            crawlResultRepository.save(result)
        }
    }
}

    data class CrawlResultCreateCommand(
        val queue: CrawlQueue,
        val seed: CrawlSeed,
        val data: Map<String, Any>
    )