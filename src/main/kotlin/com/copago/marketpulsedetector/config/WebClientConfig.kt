package com.copago.marketpulsedetector.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.web.reactive.function.client.ExchangeStrategies
import org.springframework.web.reactive.function.client.WebClient

@Configuration
class WebClientConfig {

    @Bean
    @Primary
    fun webClient(): WebClient {
        return WebClient.create()
    }

    @Bean("krxWebClient")
    fun krxWebClient(): WebClient {
        val exchangeStrategies = ExchangeStrategies.builder()
            .codecs { configurer ->
                configurer.defaultCodecs().maxInMemorySize(10 * 1024 * 1024) // 10MB
            }
            .build()

        return WebClient.builder()
            .exchangeStrategies(exchangeStrategies)
            .build()
    }
}