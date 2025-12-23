package com.copago.marketpulsedetector.core.domain.model

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.LocalDateTime

@Entity
@Table(name = "tbl_crawl_proxy")
data class CrawlProxy(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(nullable = false, length = 50)
    val host: String,

    @Column(nullable = false)
    val port: Int,

    @Enumerated(EnumType.STRING)
    @Column(length = 10)
    val type: ProxyType = ProxyType.HTTP,

    @Column(length = 50)
    val username: String? = null,

    @Column(length = 50)
    val password: String? = null,

    @Column(name = "is_active", nullable = false)
    var isActive: Boolean = true,

    @Column(name = "fail_count", nullable = false)
    var failCount: Int = 0,

    @Column(name = "success_count", nullable = false)
    var successCount: Long = 0,

    @Column(name = "last_used_at")
    var lastUsedAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "created_at", updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now()
) {
    fun recordSuccess() {
        this.failCount = 0
        this.successCount++
    }

    fun recordFailure(penalty: Int = 1, threshold: Int = 5): Boolean {
        this.failCount += penalty
        if (this.failCount >= threshold) {
            this.isActive = false
            return true
        }

        return false
    }
}

enum class ProxyType {
    HTTP
}
