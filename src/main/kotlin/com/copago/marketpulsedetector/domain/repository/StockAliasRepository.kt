package com.copago.marketpulsedetector.domain.repository

import com.copago.marketpulsedetector.domain.entity.StockAliasEntity
import org.springframework.data.jpa.repository.JpaRepository

interface StockAliasRepository: JpaRepository<StockAliasEntity, Long> {
    fun findAllByStockCode(stockCode: String): List<StockAliasEntity>
}