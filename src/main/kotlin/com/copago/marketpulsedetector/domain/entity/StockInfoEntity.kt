package com.copago.marketpulsedetector.domain.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.LocalDate
import java.time.LocalDateTime

@Entity
@Table(name = "tbl_stock_info")
data class StockInfoEntity(
    @Id
    @Column(length = 20)
    val code: String,

    @Column(name = "stock_name", nullable = false)
    val stockName: String,

    @Column(name = "market_type", nullable = false)
    val marketType: String,

    @Column(length = 2)
    val country: String,

    var isActive: Boolean = true,

    val listedAt: LocalDate?= null,

    var delistedAt: LocalDate? = null,

    @Column(name = "created_at", updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    val updatedAt: LocalDateTime = LocalDateTime.now(),
)
