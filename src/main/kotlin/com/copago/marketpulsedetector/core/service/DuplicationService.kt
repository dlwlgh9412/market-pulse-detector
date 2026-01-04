package com.copago.marketpulsedetector.core.service

import org.springframework.data.redis.connection.RedisStringCommands.SetOption
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.data.redis.core.types.Expiration
import org.springframework.stereotype.Service
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.time.Duration

@Service
class DuplicationService(
    private val redisTemplate: StringRedisTemplate,
) {
    private val TTL = Duration.ofDays(1)

    fun filterNewQueues(queues: List<QueueCreateCommand>): List<QueueCreateCommand> {
        if (queues.isEmpty()) return emptyList()

        val firstByKey = LinkedHashMap<String, QueueCreateCommand>(queues.size)
        for (queue in queues) {
            val key = toKeyString(queue.url)
            firstByKey.putIfAbsent(key, queue)
        }

        val uniqueKeys = firstByKey.keys.toList()
        val expiration = Expiration.from(TTL)
        val valueBytes = "1".toByteArray(StandardCharsets.UTF_8)

        val results: List<Any?> = redisTemplate.executePipelined { connection ->
            val cmd = connection.stringCommands()
            for (key in uniqueKeys) {
                cmd.set(
                    key.toByteArray(StandardCharsets.UTF_8),
                    valueBytes,
                    expiration,
                    SetOption.ifAbsent()
                )
            }
            null
        } ?: emptyList()

        val out = ArrayList<QueueCreateCommand>()
        for (i in uniqueKeys.indices) {
            val result = results.getOrNull(i)
            val firstSeen = when (result) {
                is Boolean -> result
                is String -> result.equals("OK", ignoreCase = true)
                is ByteArray -> result.toString(Charsets.UTF_8).equals("OK", ignoreCase = true)
                else -> false
            }

            if (firstSeen) {
                out.add(firstByKey.getValue(uniqueKeys[i]))
            }
        }

        return out
    }

    private fun toKeyString(url: String): String {
        val normalized = normalize(url)
        return "crawl:visited:url:${sha256(normalized)}"
    }

    private fun normalize(url: String): String {
        val trimmed = url.trim()
        return trimmed.substringBefore("#").removeSuffix("/")
    }

    private fun sha256(value: String): String {
        val md = MessageDigest.getInstance("SHA-256")
        val bytes = md.digest(value.toByteArray(StandardCharsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }
}