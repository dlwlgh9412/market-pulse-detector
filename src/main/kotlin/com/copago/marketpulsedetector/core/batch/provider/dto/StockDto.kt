package com.copago.marketpulsedetector.core.batch.provider.dto

data class StockDto(
    val code: String,
    val name: String,
    val market: String, // KOSPI, KOSDAQ, NASDAQ...
    val country: String, // KR, US
    val englishName: String? = null
)
