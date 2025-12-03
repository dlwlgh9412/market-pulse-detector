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
@Table(name = "tbl_crawl_page_rule")
data class CrawlPageRuleEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "site_id", nullable = false)
    val site: CrawlSiteEntity,

    @Column(nullable = false)
    val ruleName: String,

    @Column(nullable = false)
    val pageType: String,

    @Column(length = 255)
    val urlPatternRegex: String? = null,

    @Column(length = 255)
    val linkSearchScope: String? = null,

    @Column(nullable = false)
    val matchPriority: Int = 10,

    @Column(nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now()
)
