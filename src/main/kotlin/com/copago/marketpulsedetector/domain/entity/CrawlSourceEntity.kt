package com.copago.marketpulsedetector.domain.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToOne
import jakarta.persistence.Table
import java.time.LocalDateTime

@Entity
@Table(name = "tbl_crawl_source")
data class CrawlSourceEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "site_id", nullable = false)
    val site: CrawlSiteEntity,

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "page_rule_id", nullable = false)
    val pageRule: CrawlPageRuleEntity,

    @Column(name = "source_url", nullable = false)
    val sourceUrl: String,

    @Column(name = "description")
    var description: String? = null,

    @Column(name = "crawl_interval_sec")
    var crawlIntervalSec: Int = 3600,

    @Column(name = "is_active")
    var isActive: Boolean = true,

    @Column(name = "last_generated_at")
    var lastGeneratedAt: LocalDateTime? = null,
)
