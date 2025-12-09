package com.copago.marketpulsedetector.domain.repository

import com.copago.marketpulsedetector.domain.entity.MarketThemeStockMapEntity
import org.springframework.data.jpa.repository.JpaRepository

interface MarketThemeStockMapRepository: JpaRepository<MarketThemeStockMapEntity, Long> {
    fun findAllByThemeId(themeId: Long): List<MarketThemeStockMapEntity>
}