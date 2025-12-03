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
@Table(name = "tbl_crawl_extraction_rule")
data class CrawlExtractionRuleEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "page_rule_id", nullable = false)
    val pageRule: CrawlPageRuleEntity,

    @Column(name = "is_required", nullable = false)
    val isRequired: Boolean = false,

    @Column(name = "json_key", nullable = false, length = 100)
    val jsonKey: String,

    @Column(name = "css_selector", nullable = false, length = 1024)
    var cssSelector: String,

    @Column(name = "selector_type", length = 5)
    var selectorType: String = "CSS",

    @Column(name = "extract_attribute")
    var extractAttributes: String? = null,

    @Column(name = "status", length = 10)
    var status: String = "ACTIVE",

    @Column(name = "failure_count")
    var failureCount: Int = 0,

    var lastUsedAt: LocalDateTime? = null,

    var lastSuccessAt: LocalDateTime? = null,

    @Column(nullable = false, length = 10)
    var updatedBy: String = "USER",

    @Column(nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now()
)