package com.copago.marketpulsedetector.domain.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table

@Entity
@Table(name = "tbl_market_theme")
data class MarketThemeEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(name = "theme_name", nullable = false)
    val themeName: String,

    val description: String? = null,

    val isActive: Boolean = true
)