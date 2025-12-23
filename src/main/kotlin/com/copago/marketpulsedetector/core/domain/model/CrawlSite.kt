package com.copago.marketpulsedetector.core.domain.model

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.LocalDateTime

@Entity
@Table(name = "tbl_crawl_site")
data class CrawlSite(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(name = "name", length = 100, nullable = false)
    val name: String,

    @Column(name = "base_url", length = 255, nullable = false)
    val baseUrl: String,

    @Column(name = "is_active")
    val isActive: Boolean = true,

    @Column(name = "rate_limit_ms")
    val rateLimitMs: Long = 1000,

    @Column(name = "headers", columnDefinition = "TEXT")
    val headers: String? = null,

    @Column(name = "timeout", nullable = false)
    val timeout: Long = 60000,

    @Column(name = "created_at")
    val createdAt: LocalDateTime = LocalDateTime.now(),
)
