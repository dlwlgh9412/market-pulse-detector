package com.copago.marketpulsedetector.domain.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import java.time.LocalDateTime

@Entity
@Table(name = "tbl_crawl_analysis_result")
data class CrawlAnalysisResultEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(name = "target_id", nullable = false)
    val targetId: Long,

    @ManyToOne
    @JoinColumn(name = "stock_code", nullable = false)
    val stock: StockInfoEntity,

    @Column(length = 20)
    val matchType: String = "DIRECT",

    @Column(nullable = false)
    val confidence: Float = 1.0f,

    @Column(length = 10)
    var sentiment: String = "NEUTRAL",

    @Column(name = "created_at", updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now()
)
