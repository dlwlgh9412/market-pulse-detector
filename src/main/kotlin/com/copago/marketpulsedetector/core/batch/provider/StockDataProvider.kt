package com.copago.marketpulsedetector.core.batch.provider

import com.copago.marketpulsedetector.core.batch.provider.dto.StockDto

interface StockDataProvider {
    fun fetchPage(pageNo: Int, pageSize: Int): StockPageResult
}

data class StockPageResult(
    val items: List<StockDto>,
    val totalCount: Long
)
