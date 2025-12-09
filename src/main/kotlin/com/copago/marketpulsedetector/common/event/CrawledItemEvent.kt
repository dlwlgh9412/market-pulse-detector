package com.copago.marketpulsedetector.common.event

import java.time.LocalDateTime

data class CrawledItemEvent(
    val targetId: Long,
    val title: String,
    val content: String,
    val url: String,
    val crawledAt: LocalDateTime = LocalDateTime.now()
)
