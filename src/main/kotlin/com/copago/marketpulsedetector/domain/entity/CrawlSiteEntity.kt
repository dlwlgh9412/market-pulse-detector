package com.copago.marketpulsedetector.domain.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.LocalDateTime

@Entity
@Table(name = "tbl_crawl_site")
data class CrawlSiteEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(name = "site_name", length = 100, nullable = false)
    val siteName: String,

    @Column(name = "seed_url", length = 2048, nullable = false)
    val seedUrl: String? = null,

    @Column(name = "domain", length = 255, unique = true, nullable = false)
    val domain: String,

    @Column(name = "crawl_policy", columnDefinition = "TEXT")
    val crawlPolicy: String? = null,

    @Column(name = "crawl_delay_ms")
    var crawlDelayMs: Long = 10000,

    @Column(name = "user_agent", columnDefinition = "TEXT", nullable = false)
    val userAgent: String? = null,

    @Column(name = "timeout")
    val timeout: Long = 60000,

    @Column(name = "created_at", updatable = false)
    val createdAt: LocalDateTime? = null,
)
