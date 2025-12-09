package com.copago.marketpulsedetector.domain.entity

import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table

@Entity
@Table(name = "tbl_market_theme_stock_map")
data class MarketThemeStockMapEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "theme_id", nullable = false)
    val theme: MarketThemeEntity,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "stock_code", nullable = false)
    val stock: StockInfoEntity,

    val reason: String? = null
)
