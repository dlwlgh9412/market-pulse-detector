package com.copago.marketpulsedetector.domain.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import java.time.LocalDateTime

@Entity
@Table(name = "tbl_crawl_theme_result")
data class CrawlThemeResultEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "target_id", nullable = false)
    val target: CrawlTargetEntity,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "theme_id", nullable = false)
    val theme: MarketThemeEntity,

    @Column(nullable = false)
    val confidence: Float = 1.0f,

    @Column(name = "created_at", updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now()
)
