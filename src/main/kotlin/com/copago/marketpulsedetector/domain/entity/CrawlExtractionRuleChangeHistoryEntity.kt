package com.copago.marketpulsedetector.domain.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.LocalDateTime

@Entity
@Table(name = "tbl_crawl_extraction_rule_change_history")
data class CrawlExtractionRuleChangeHistoryEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(nullable = false)
    val ruleId: Long, // 변경된 Extraction Rule ID

    @Column(nullable = false)
    val targetId: Long, // 문제를 일으킨 Target ID

    @Column(nullable = false)
    val oldSelector: String,

    @Column(nullable = false)
    val newSelector: String,

    @Column(nullable = false)
    val changeReason: String, // 예: "LLM Self-Healing"

    val isVerified: Boolean = false, // 검증 성공 여부

    @Column(nullable = false, updatable = false)
    val changedAt: LocalDateTime = LocalDateTime.now()
)
