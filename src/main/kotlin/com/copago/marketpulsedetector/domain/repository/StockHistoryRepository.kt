package com.copago.marketpulsedetector.domain.repository

import com.copago.marketpulsedetector.domain.entity.StockHistoryEntity
import org.springframework.data.jpa.repository.JpaRepository

interface StockHistoryRepository: JpaRepository<StockHistoryEntity, Long> {
}