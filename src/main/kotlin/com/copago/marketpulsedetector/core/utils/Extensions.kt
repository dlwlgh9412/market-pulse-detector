package com.copago.marketpulsedetector.core.utils

import kotlinx.coroutines.delay
import kotlin.system.measureTimeMillis

suspend fun <T> Iterable<T>.forEachWithInterval(
    intervalMs: Long,
    action: suspend (T) -> Unit
) {
    val iterator = this.iterator()

    while (iterator.hasNext()) {
        val item = iterator.next()

        val executionTime = measureTimeMillis {
            action(item)
        }

        if (iterator.hasNext()) {
            val waitTime = (intervalMs - executionTime).coerceAtLeast(0)
            if (waitTime > 0) {
                delay(waitTime)
            }
        }
    }
}