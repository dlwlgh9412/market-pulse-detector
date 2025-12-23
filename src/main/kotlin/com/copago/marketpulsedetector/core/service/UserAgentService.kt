package com.copago.marketpulsedetector.core.service

import com.copago.marketpulsedetector.core.domain.model.DeviceType
import com.copago.marketpulsedetector.core.repository.UserAgentRepository
import jakarta.annotation.PostConstruct
import org.slf4j.LoggerFactory
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.concurrent.CopyOnWriteArrayList

@Service
class UserAgentService(
    private val userAgentRepository: UserAgentRepository,
    private val redisTemplate: RedisTemplate<String, String>
) {
    private val logger = LoggerFactory.getLogger(UserAgentService::class.java)
    private val CHANNEL_TOPIC = "ua-block-channel"

    private val desktopAgents = CopyOnWriteArrayList<UserAgentCacheEntry>()
    private val mobileAgents = CopyOnWriteArrayList<UserAgentCacheEntry>()

    private val FALLBACK_UA =
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"

    @PostConstruct
    fun init() {
        refreshUserAgents()
    }

    @Scheduled(fixedRate = 3_600_000)
    fun refreshUserAgents() {
        logger.info("Refreshing User-Agent list from Database...")
        val allAgents = userAgentRepository.findAllByIsActiveTrue()

        val newDesktopList = allAgents.filter { it.deviceType == DeviceType.DESKTOP }
            .map { UserAgentCacheEntry(it.id!!, it.agentString) }

        val newMobileList = allAgents.filter { it.deviceType == DeviceType.MOBILE }
            .map { UserAgentCacheEntry(it.id!!, it.agentString) }

        desktopAgents.clear()
        desktopAgents.addAll(newDesktopList)

        mobileAgents.clear()
        mobileAgents.addAll(newMobileList)

        logger.info("Loaded ${desktopAgents.size} Desktop UAs, ${mobileAgents.size} Mobile UAs.")
    }

    fun getRandomUserAgent(deviceType: DeviceType = DeviceType.DESKTOP): String {
        val targetList = if (deviceType == DeviceType.DESKTOP) desktopAgents else mobileAgents

        if (targetList.isEmpty()) {
            logger.warn("UserAgent list is empty! Using fallback.")
            return FALLBACK_UA
        }

        return targetList.random().agentString
    }

    @Transactional
    fun blockUserAgent(userAgentId: Long) {
        val ua = userAgentRepository.findById(userAgentId)
            .orElseThrow { IllegalArgumentException("UserAgent not found: $userAgentId") }
        ua.isActive = false

        removeLocalUserAgent(userAgentId)

        redisTemplate.convertAndSend(CHANNEL_TOPIC, userAgentId)

        logger.info("Blocked UserAgent ID: $userAgentId and published event.")
    }

    fun handleBlockMessage(message: String) {
        try {
            val id = message.toLong()
            removeLocalUserAgent(id)
            logger.info("[Sync] Removed blocked UA ID: $id from local cache.")
        } catch (e: NumberFormatException) {
            logger.error("Invalid message format from Redis: $message")
        }
    }

    private fun removeLocalUserAgent(id: Long) {
        desktopAgents.removeIf { it.id == id }
        mobileAgents.removeIf { it.id == id }
    }
}

data class UserAgentCacheEntry(
    val id: Long,
    val agentString: String
)