package com.copago.marketpulsedetector.config

import org.springframework.data.redis.core.RedisTemplate
import org.springframework.data.redis.core.script.DefaultRedisScript
import org.springframework.stereotype.Component
import java.util.Collections

@Component
class RedisRateLimiter(
    private val redisTemplate: RedisTemplate<String, Long>,
) {
    companion object {
        private const val ZSET_KEY: String = "crawler:scheduler:domains"
    }

    private val POP_SCRIPT = """
        local key = KEYS[1]
        local now = tonumber(ARGV[1])
        local delay = tonumber(ARGV[2])
        local limit = tonumber(ARGV[3])
        
        -- 1. 현재 시간(now)보다 점수가 낮은(실행 가능한) 멤버를 limit만큼 조회
        local members = redis.call('ZRANGEBYSCORE', key, '-inf', now, 'LIMIT', 0, limit)
        
        -- 2. 조회된 멤버가 있으면 점수를 미래(now + delay)로 업데이트 (선점)
        for i, member in ipairs(members) do
            redis.call('ZADD', key, now + delay, member)
        end
        
        return members
    """

    fun popAvailableDomains(limit: Int, defaultDelayMs: Long = 5000): List<Long> {
        val now = System.currentTimeMillis()
        val script = DefaultRedisScript(POP_SCRIPT, List::class.java)

        val result = redisTemplate.execute(
            script,
            Collections.singletonList(ZSET_KEY),
            now.toString(),
            defaultDelayMs.toString(),
            limit.toString()
        ) as List<*>

        return result.map { it as Long }
    }

    fun updateScore(siteId: Long, nextAvailableTime: Long) {
        redisTemplate.opsForZSet().add(ZSET_KEY, siteId, nextAvailableTime.toDouble())
    }
}