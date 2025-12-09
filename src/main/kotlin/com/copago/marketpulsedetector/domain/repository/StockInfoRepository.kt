package com.copago.marketpulsedetector.domain.repository

import com.copago.marketpulsedetector.domain.entity.StockInfoEntity
import org.springframework.data.jpa.repository.JpaRepository
import java.time.LocalDateTime

interface StockInfoRepository : JpaRepository<StockInfoEntity, String> {
    fun findAllByIsActiveTrue(): List<StockInfoEntity>

    fun findByIsActiveTrueAndUpdatedAtBefore(startOfDay: LocalDateTime): List<StockInfoEntity>
}