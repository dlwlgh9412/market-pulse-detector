package com.copago.marketpulsedetector.core.domain.model

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.OneToOne
import jakarta.persistence.Table
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.time.LocalDateTime

@Entity
@Table(name = "tbl_crawl_result")
data class CrawlResult(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "queue_id", nullable = false)
    val queue: CrawlQueue,

    @Column(name = "seed_id")
    val seedId: Long,

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "extracted_data", columnDefinition = "json")
    var extractedData: Map<String, Any> = mutableMapOf(),

    @Column(name = "crawled_at")
    val crawledAt: LocalDateTime = LocalDateTime.now()
) {
    fun getValue(key: String): Any? {
        return extractedData[key]
    }
}
