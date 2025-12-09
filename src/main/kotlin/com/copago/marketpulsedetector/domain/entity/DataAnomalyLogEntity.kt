package com.copago.marketpulsedetector.domain.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.LocalDateTime

@Entity
@Table(name = "tbl_data_anomaly_log")
data class DataAnomalyLogEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(name = "target_key", nullable = false)
    val targetKey: String,

    @Column(name = "anomaly_type", nullable = false)
    val anomalyType: String,

    @Column(columnDefinition = "TEXT")
    val payload: String? = null,

    @Column(length = 20)
    var status: String = "NEW",

    var occurrence: Int = 1,

    @Column(updatable = false)
    val firstSeenAt: LocalDateTime = LocalDateTime.now(),

    var lastSeenAt: LocalDateTime = LocalDateTime.now()
) {
    fun recur() {
        if (this.status == "RESOLVED") {
            this.status = "NEW"
        }
        this.occurrence += 1
        this.lastSeenAt = LocalDateTime.now()
    }
}
