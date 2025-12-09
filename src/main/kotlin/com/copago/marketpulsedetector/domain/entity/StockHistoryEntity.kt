package com.copago.marketpulsedetector.domain.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.LocalDate
import java.time.LocalDateTime

@Entity
@Table(name = "tbl_stock_history")
data class StockHistoryEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(nullable = false, length = 20)
    val stockCode: String,

    @Column
    val oldStockName: String? = null,

    @Column
    val oldMarketType: String? = null,

    @Column(nullable = false)
    val changeType: String,

    @Column(nullable = false)
    val changeDate: LocalDate,

    @Column
    val createdAt: LocalDateTime = LocalDateTime.now(),
)
