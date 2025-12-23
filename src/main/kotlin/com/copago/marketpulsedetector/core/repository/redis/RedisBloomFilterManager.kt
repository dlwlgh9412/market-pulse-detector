package com.copago.marketpulsedetector.core.repository.redis

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.data.redis.core.script.DefaultRedisScript
import org.springframework.stereotype.Component
import java.time.Duration
import java.util.Collections

@Component
class RedisBloomFilterManager(
    private val redisTemplate: StringRedisTemplate,
    private val hashProvider: BloomFilterHashProvider
) {
    companion object {
        private const val BITMAP_SIZE = 33_554_432L
        private const val HASH_COUNT = 7
    }

    private val ADD_SCRIPT = """
        local changed = 0
        for _, offset in ipairs(ARGV) do
            if redis.call('SETBIT', KEYS[1], offset, 1) == 0 then
                changed = 1
            end
        end
        return changed
    """

    private val CONTAINS_SCRIPT = """
        for _, offset in ipairs(ARGV) do
            if redis.call('GETBIT', KEYS[1], offset) == 0 then
                return 0
            end
        end
        return 1
    """

    suspend fun add(key: String, value: String, expireDuration: Duration): Boolean = withContext(Dispatchers.IO) {
        val offsets = hashProvider.calculateOffsets(value, BITMAP_SIZE, HASH_COUNT)
        val args = offsets.map { it.toString() }.toTypedArray()

        val result = redisTemplate.execute(
            DefaultRedisScript(ADD_SCRIPT, Long::class.java),
            Collections.singletonList(key),
            *args
        )

        val ttl = redisTemplate.getExpire(key)
        if (ttl == -1L) {
            redisTemplate.expire(key, expireDuration)
        }

        return@withContext result == 1L
    }

    suspend fun contains(key: String, value: String): Boolean = withContext(Dispatchers.IO) {
        val offsets = hashProvider.calculateOffsets(value, BITMAP_SIZE, HASH_COUNT)
        val args = offsets.map { it.toString() }.toTypedArray()

        val result = redisTemplate.execute(
            DefaultRedisScript(CONTAINS_SCRIPT, Long::class.java),
            Collections.singletonList(key),
            *args
        )

        return@withContext result == 1L
    }
}