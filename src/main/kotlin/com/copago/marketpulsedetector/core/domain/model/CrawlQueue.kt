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
import java.time.LocalDateTime

@Entity
@Table(name = "tbl_crawl_queue")
data class CrawlQueue(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "site_id", nullable = false)
    val site: CrawlSite,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seed_id", nullable = false)
    val seed: CrawlSeed,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "page_definition_id", nullable = false)
    val pageDefinition: CrawlPageDefinition,

    @Column(name = "url", nullable = false, unique = true, length = 500)
    val url: String,

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20)
    var status: CrawlStatus = CrawlStatus.READY,

    @Column(name = "retry_count")
    var retryCount: Int = 0,

    @Column(name = "priority")
    var priority: Int = 10,

    @Column(name = "worker_id", length = 20)
    var workerId: String? = null,

    @Column(name = "timeout_sec")
    val timeoutSec: Int = 60,

    @Column(name = "last_processing_at")
    var lastProcessingAt: LocalDateTime? = null,

    @Column(name = "next_run_at")
    var nextRunAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "expired_at")
    var expiredAt: LocalDateTime? = null,

    @Column(name = "created_at", updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "updated_at")
    var updatedAt: LocalDateTime = LocalDateTime.now(),
) {
    override fun toString(): String {
        return "CrawlQueue(id=$id, url='$url', status=$status)"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is CrawlQueue) return false

        if (id != other.id) return false

        return true
    }

    override fun hashCode(): Int {
        return id?.hashCode() ?: 0
    }

    fun markAsProcessing(workerId: String) {
        if (this.status == CrawlStatus.DONE) {
            throw IllegalStateException("Cannot start processing. this queue is done")
        }

        this.status = CrawlStatus.PROCESSING
        this.workerId = workerId
        this.lastProcessingAt = LocalDateTime.now()
        this.expiredAt = LocalDateTime.now().plusSeconds(this.timeoutSec.toLong())
    }

    fun markAsDone() {
        this.status = CrawlStatus.DONE
        this.updatedAt = LocalDateTime.now()
    }

    fun markAsFailed(maxRetry: Int = 3) {
        this.retryCount++
        this.updatedAt = LocalDateTime.now()

        if (this.retryCount < maxRetry) {
            this.status = CrawlStatus.READY
            this.nextRunAt = LocalDateTime.now().plusMinutes(5L * retryCount)
        } else {
            this.status = CrawlStatus.FAIL
        }
    }

    fun markAsRetry(maxRetry: Int = 3) {
        this.retryCount++
        this.updatedAt = LocalDateTime.now()

        if (this.retryCount < maxRetry) {
            this.status = CrawlStatus.READY

            val waitMinutes = 5L * this.retryCount
            this.nextRunAt = LocalDateTime.now().plusMinutes(waitMinutes)
        } else {
            this.status = CrawlStatus.FAIL
        }
    }

}

enum class CrawlStatus {
    READY, PROCESSING, DONE, FAIL
}
