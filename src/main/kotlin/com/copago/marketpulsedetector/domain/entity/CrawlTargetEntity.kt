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
@Table(name = "tbl_crawl_target")
data class CrawlTargetEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_target_id", nullable = false)
    val parentTarget: CrawlTargetEntity? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "site_id", nullable = false)
    val site: CrawlSiteEntity,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "page_rule_id", nullable = true)
    var pageRule: CrawlPageRuleEntity? = null,

    @Column(name = "target_name", length = 100)
    val targetName: String? = null,

    @Column(name = "target_url", length = 2048)
    val targetUrl: String,

    @Column(name = "target_url_hash", length = 64, unique = true)
    val targetUrlHash: String,

    @Column(name = "page_type", nullable = false)
    var pageType: String = "UNKNOWN",

    @Column(nullable = false)
    var status: String = "PENDING",

    @Column(nullable = false)
    val depth: Int = 1,

    @Column(nullable = false)
    val isActive: Boolean = true,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_id")
    val source: CrawlSourceEntity? = null,

    var nextTryAt: LocalDateTime? = null,

    var retryCount: Int = 0,

    var lastProcessedAt: LocalDateTime? = null,

    @Column(nullable = false)
    val priority: Int = 5,

    @Column(nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now()
)
