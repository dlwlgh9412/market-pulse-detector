package com.copago.marketpulsedetector.core.repository.redis

import com.fasterxml.jackson.databind.ObjectMapper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory
import org.springframework.data.redis.connection.RedisZSetCommands
import org.springframework.data.redis.connection.StringRedisConnection
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.data.redis.core.script.DefaultRedisScript
import org.springframework.stereotype.Component

@Component
class CrawlQueueManager(
    private val redisTemplate: StringRedisTemplate,
    private val objectMapper: ObjectMapper
) {
    private val logger = LoggerFactory.getLogger(CrawlQueueManager::class.java)

    companion object {
        private const val Z_SET_KEY: String = "crawler:scheduler:queue"
        private const val HASH_KEY: String = "crawler:site:info"
        private const val DEFAULT_LEASE_MS = 60_000L
    }

    data class SiteMetadata(
        val rateLimitMs: Long,
        val timeoutMs: Long,
        val priority: Int = 1,
        val isActive: Boolean = true
    )

    data class Site(
        val id: Long,
        val metadata: SiteMetadata,
    )


    data class SiteInfo(
        val id: Long,
        val rateLimitMs: Long,
        val timeoutMs: Long,
        val priority: Int = 1,
        val isActive: Boolean = true
    )

    private val POP_SCRIPT = """
        local zset_key = KEYS[1]
        local hash_key = KEYS[2]
        local now = tonumber(ARGV[1])
        local limit = tonumber(ARGV[2])
        local default_lease = tonumber(ARGV[3])

        -- 1. 실행 시간(Score)이 된 사이트 조회
        local sites = redis.call('ZRANGEBYSCORE', zset_key, '-inf', now, 'LIMIT', 0, limit)
        local result = {}

        for i, site_id in ipairs(sites) do
            local meta_json = redis.call('HGET', hash_key, site_id)
            
            if meta_json then
                -- 2. 메타데이터 파싱은 앱에서 하도록 JSON 그대로 반환
                -- 3. [Lock] 일단 default_lease 만큼 미래로 밀어둠 (다른 스레드 접근 차단)
                --    앱에서 Heartbeat를 통해 이 시간을 계속 연장해야 함
                redis.call('ZADD', zset_key, now + default_lease, site_id)
                
                table.insert(result, site_id)
                table.insert(result, meta_json)
            else
                -- 메타데이터가 없는(삭제된) 사이트는 큐에서 제거
                redis.call('ZREM', zset_key, site_id)
            end
        end

        return result
    """

    suspend fun popAvailableSites(limit: Int = 1): List<Site> = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val script = DefaultRedisScript(POP_SCRIPT, List::class.java)

        val rawResult = redisTemplate.execute(
            script,
            listOf(Z_SET_KEY, HASH_KEY),
            now.toString(),
            limit.toString(),
            DEFAULT_LEASE_MS.toString()
        ) as? List<String> ?: emptyList()

        val poppedList = mutableListOf<Site>()
        for (i in rawResult.indices step 2) {
            try {
                val siteId = rawResult[i].toLong()
                val json = rawResult[i + 1]
                val metadata = objectMapper.readValue(json, SiteMetadata::class.java)
                poppedList.add(Site(id = siteId, metadata = metadata))
            } catch (e: Exception) {
                logger.error(e.message, e)
                continue
            }
        }
        return@withContext poppedList
    }

    // 작업 중임을 알리고 연장
    suspend fun extendLease(siteId: Long, extensionMs: Long) = withContext(Dispatchers.IO) {
        val nextExpire = System.currentTimeMillis() + extensionMs
        redisTemplate.opsForZSet().add(Z_SET_KEY, siteId.toString(), nextExpire.toDouble())
    }

    // 작업 완료 후 다음 스케줄링
    suspend fun completeAndReschedule(siteId: Long, rateLimitMs: Long) = withContext(Dispatchers.IO) {
        val nextExecutionTime = System.currentTimeMillis() + rateLimitMs
        redisTemplate.opsForZSet().add(Z_SET_KEY, siteId.toString(), nextExecutionTime.toDouble())
    }

    suspend fun syncSites(sites: List<SiteInfo>) = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()

        redisTemplate.executePipelined { connection ->
            val stringConn = connection as StringRedisConnection

            sites.forEach { site ->
                val siteId = site.id.toString()

                if (site.isActive) {
                    val meta = SiteMetadata(
                        rateLimitMs = site.rateLimitMs,
                        timeoutMs = site.timeoutMs,
                        priority = site.priority,
                        isActive = true
                    )

                    stringConn.hSet(HASH_KEY, siteId, objectMapper.writeValueAsString(meta))
                    stringConn.zAdd(Z_SET_KEY, now.toDouble(), siteId, RedisZSetCommands.ZAddArgs.ifNotExists())
                } else {
                    // 비활성화 처리
                    stringConn.zRem(Z_SET_KEY, siteId)
                    stringConn.hDel(HASH_KEY, siteId)
                }
            }
            null
        }
    }

    suspend fun getAllSiteIds(): Set<String> = withContext(Dispatchers.IO) {
        return@withContext redisTemplate.opsForZSet()
            .range(Z_SET_KEY, 0, -1)
            ?.toSet()
            ?: emptySet()
    }

    fun removeSites(siteIds: List<String>): Int {
        if (siteIds.isEmpty()) return 0

        val count = redisTemplate.opsForZSet()
            .remove(Z_SET_KEY, *siteIds.toTypedArray())

        return count?.toInt() ?: 0
    }

    suspend fun <T> runWithHeartbeat(siteId: Long, timeoutMs: Long, block: suspend () -> T): T = coroutineScope {
        val job = launch {
            while (isActive) {
                delay(timeoutMs / 2)
                extendLease(siteId, timeoutMs)
            }
        }

        try {
            block()
        } finally {
            job.cancelAndJoin()
        }
    }
}

