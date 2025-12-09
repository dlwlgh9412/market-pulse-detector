package com.copago.marketpulsedetector.domain.repository

import com.copago.marketpulsedetector.domain.entity.MarketThemeKeywordEntity
import org.springframework.data.jpa.repository.JpaRepository

interface MarketThemeKeywordRepository: JpaRepository<MarketThemeKeywordEntity, Long> {
}