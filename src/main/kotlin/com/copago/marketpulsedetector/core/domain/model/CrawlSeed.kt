package com.copago.marketpulsedetector.core.domain.model

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table

@Entity
@Table(name = "tbl_crawl_seed")
data class CrawlSeed(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "site_id", nullable = false)
    val site: CrawlSite,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "page_definition_id", nullable = false)
    val pageDefinition: CrawlPageDefinition,

    @Column(nullable = false, length = 500)
    val url: String,

    @Column(name = "is_active", nullable = false)
    var isActive: Boolean = true,

    @Column(name = "interval_seconds", nullable = false)
    var intervalSeconds: Int = 3600,

    @Column(name = "next_run_at", nullable = false)
    var nextRunAt: Long = 0,

    @Column(name = "last_processed_at")
    var lastProcessedAt: Long? = null,

    ) {
    fun deactivate() {
        this.isActive = false
    }

    override fun toString(): String {
        return "CrawlSeed(id=$id)"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is CrawlSeed) return false

        if (id != other.id) return false

        return true
    }

    override fun hashCode(): Int {
        return id?.hashCode() ?: 0
    }
}