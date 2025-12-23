package com.copago.marketpulsedetector.core.config

import com.copago.marketpulsedetector.core.properties.CrawlerProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Configuration
import org.springframework.core.env.Environment
import org.springframework.core.env.getProperty
import kotlin.math.max

@Configuration
@EnableConfigurationProperties(CrawlerProperties::class)
class CrawlerConfig(
    private val environment: Environment,
    private val crawlerProperties: CrawlerProperties,
) {
    val concurrencyLimit: Int
        get() {
            if (crawlerProperties.workerConcurrency != null) return crawlerProperties.workerConcurrency

            val dbPoolSize = environment.getProperty<Int>("spring.datasource.hikari.maximum-pool-size", 10)
            return dbPoolSize
        }

    val batchFetchSize: Int
        get() {
            if (crawlerProperties.batchFetchSize != null) return crawlerProperties.batchFetchSize

            return max(5, concurrencyLimit / 3)
        }

}