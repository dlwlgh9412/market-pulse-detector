package com.copago.marketpulsedetector.core.domain.model

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.LocalDateTime

@Entity
@Table(name = "tbl_crawl_queue_history")
data class CrawlQueueHistory(
    @Id
    val id: Long,

    @Column(name = "site_id")
    val siteId: Long,

    @Column(name = "seed_id")
    val seedId: Long,

    @Column(name = "page_definition_id")
    val pageDefinitionId: Long,

    @Column(name = "url")
    val url: String,

    @Column(name = "worker_id")
    val workerId: String? = null,

    @Column(name = "last_processing_at")
    val lastProcessingAt: LocalDateTime? = null,
)
