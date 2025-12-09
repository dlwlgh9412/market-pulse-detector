package com.copago.marketpulsedetector.common.extension

import com.copago.marketpulsedetector.common.log.AnomalyLogService

fun <T> Result<T>.logOnAnomaly(
    logService: AnomalyLogService,
    key: String,
    type: String,
    msg: String? = null
): T? {
    return this.onFailure { exception ->
        val finalMsg = msg ?: exception.message ?: "Unknown Error"
        logService.record(key, type, msg)
    }.getOrNull()
}