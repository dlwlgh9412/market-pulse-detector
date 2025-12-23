package com.copago.marketpulsedetector.core.domain.model

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
@Table(name = "tbl_crawl_selector")
data class CrawlSelector(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "page_definition_id", nullable = false)
    val pageDefinition: CrawlPageDefinition,

    @Enumerated(EnumType.STRING)
    @Column(name = "rule_type")
    val ruleType: RuleType = RuleType.DATA,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "target_page_definition_id", nullable = true)
    val targetPageDefinition: CrawlPageDefinition? = null,

    @Column(name = "field_name", length = 50)
    var fieldName: String? = null,

    @Enumerated(EnumType.STRING)
    @Column(name = "selector_type", length = 20)
    var selectorType: SelectorType = SelectorType.CSS,

    @Column(name = "selector_value", columnDefinition = "TEXT")
    var selectorValue: String? = null,

    @Column(name = "inner_selector_value", columnDefinition = "TEXT")
    var innerSelectorValue: String? = null,

    @Column(name = "extract_attribute", length = 50)
    var extractAttribute: String? = null,

    @Column(name = "is_multiple")
    var isMultiple: Boolean = false
)

enum class SelectorType {
    CSS, XPATH, REGEX, JSON_PATH
}

enum class RuleType {
    DATA, LINK
}
