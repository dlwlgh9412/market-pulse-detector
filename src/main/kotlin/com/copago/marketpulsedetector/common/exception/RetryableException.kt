package com.copago.marketpulsedetector.common.exception

class RetryableException(message: String, cause: Throwable? = null) : CrawlerException(message, cause)