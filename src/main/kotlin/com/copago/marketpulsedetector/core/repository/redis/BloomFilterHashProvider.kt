package com.copago.marketpulsedetector.core.repository.redis

import org.springframework.stereotype.Component
import java.nio.charset.StandardCharsets
import java.security.MessageDigest

@Component
class BloomFilterHashProvider {
    fun calculateOffsets(value: String, bitmapSize: Long, hashCount: Int): LongArray {
        val (hash1, hash2) = getHashPair(value)
        val offsets = LongArray(hashCount)

        for (i in 0 until hashCount) {
            var combinedHash = hash1 + (i * hash2)
            if (combinedHash < 0) combinedHash = combinedHash.inv()

            offsets[i] = combinedHash % bitmapSize
        }

        return offsets
    }

    private fun getHashPair(value: String): Pair<Long, Long> {
        val md = MessageDigest.getInstance("MD5")
        val digest = md.digest(value.toByteArray(StandardCharsets.UTF_8))

        var hash1 = 0L
        var hash2 = 0L

        for (i in 0..7) {
            hash1 = hash1 or ((digest[i].toLong() and 0xFF) shl (i * 8))
        }

        for (i in 8..15) {
            hash2 = hash2 or ((digest[i].toLong() and 0xFF) shl ((i - 8) * 8))
        }

        return Pair(hash1, hash2)
    }
}