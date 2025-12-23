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
@Table(name = "tbl_crawl_page_definition")
data class CrawlPageDefinition(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "site_id", nullable = false)
    val site: com.copago.marketpulsedetector.core.domain.model.CrawlSite,

    @Enumerated(EnumType.STRING)
    @Column(name = "page_type", length = 20)
    val pageType: com.copago.marketpulsedetector.core.domain.model.PageType,

//    @Column(name = "url_pattern", length = 255)
//    val urlPattern: String,

    @Column(name = "fetch_method", length = 20)
    val fetchMethod: String = "JSOUP",

    @Column(name = "description", length = 200)
    val description: String,
)

enum class PageType {
    LIST, DATA
}