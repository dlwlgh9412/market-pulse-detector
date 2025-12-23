package com.copago.marketpulsedetector.core.properties

import org.springframework.boot.context.properties.ConfigurationProperties
import java.util.UUID

@ConfigurationProperties(prefix = "app.crawler")
data class CrawlerProperties(
    val workerId: String = System.getenv("HOSTNAME") + "_" + UUID.randomUUID().toString().substring(0, 5),
    val batchFetchSize: Int? = null,
    val workerConcurrency: Int? = null
)
