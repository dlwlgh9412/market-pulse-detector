package com.copago.marketpulsedetector.domain.entity

import com.copago.marketpulsedetector.domain.enums.PriorityConditionType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table

@Entity
@Table(name = "tbl_crawl_priority_rule")
data class CrawlPriorityRuleEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "page_rule_id", nullable = false)
    val pageRule: CrawlPageRuleEntity,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    val conditionType: PriorityConditionType,

    @Column(nullable = false)
    val conditionExpression: String,

    @Column(nullable = false)
    val priorityBonus: Int,

    val isActive: Boolean = true
)
