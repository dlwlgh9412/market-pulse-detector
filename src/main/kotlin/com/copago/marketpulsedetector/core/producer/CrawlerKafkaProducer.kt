package com.copago.marketpulsedetector.core.producer

import com.copago.marketpulsedetector.common.event.CrawledItemEvent
import org.slf4j.LoggerFactory
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Component
import org.springframework.transaction.support.TransactionSynchronization
import org.springframework.transaction.support.TransactionSynchronizationManager

@Component
class CrawlerKafkaProducer(
    private val kafkaTemplate: KafkaTemplate<String, Any>
) {
    private val logger = LoggerFactory.getLogger(CrawlerKafkaProducer::class.java)
    private val TOPIC = "crawled.article"

    fun send(event: CrawledItemEvent) {
        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            TransactionSynchronizationManager.registerSynchronization(object : TransactionSynchronization {
                override fun afterCommit() {
                    sendMessage(event)
                }
            })
        } else {

        }
    }

    private fun sendMessage(event: CrawledItemEvent) {
        try {
            kafkaTemplate.send(TOPIC, event.url, event)
            logger.debug("📡 Published to Kafka (After Commit): ${event.url}")
        } catch (e: Exception) {
            logger.error("Failed to send Kafka message", e)
        }
    }
}