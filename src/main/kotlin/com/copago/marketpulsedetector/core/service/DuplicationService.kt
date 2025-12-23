package com.copago.marketpulsedetector.core.service

import com.copago.marketpulsedetector.core.repository.redis.RedisBloomFilterManager
import org.springframework.stereotype.Service
import java.time.Duration
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Service
class DuplicationService(
    private val bloomFilterManager: RedisBloomFilterManager
) {
    private val RETENTION_DAYS = Duration.ofDays(60)

    suspend fun isDuplicateOrMark(url: String): Boolean {
        val today = LocalDate.now()
        val currentKey = getKey(today)
        val prevKey = getKey(today.minusMonths(1))

        if (bloomFilterManager.contains(prevKey, url)) {
            return true
        }

        if (bloomFilterManager.contains(currentKey, url)) {
            return true
        }

        bloomFilterManager.add(currentKey, url, RETENTION_DAYS)

        return false
    }

    private fun getKey(date: LocalDate): String {
        val monthStr = date.format(DateTimeFormatter.ofPattern("yyyyMM"))
        return "crawler:visited:$monthStr"
    }
}