package com.copago.marketpulsedetector.common.exception

sealed class CrawlerException(
    message: String,
    cause: Throwable? = null
) : RuntimeException(message, cause)