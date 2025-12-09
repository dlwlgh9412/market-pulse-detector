package com.copago.marketpulsedetector.domain.repository

import com.copago.marketpulsedetector.domain.entity.MarketThemeEntity
import org.springframework.data.jpa.repository.JpaRepository

interface MarketThemeRepository: JpaRepository<MarketThemeEntity, Long> {
    fun findAllByIsActiveTrue(): List<MarketThemeEntity>
}