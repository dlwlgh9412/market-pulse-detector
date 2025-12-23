package com.copago.marketpulsedetector.core.service

import com.copago.marketpulsedetector.core.fetcher.ProxyConfig
import com.copago.marketpulsedetector.core.repository.CrawlProxyRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
class ProxyService(
    private val proxyRepository: CrawlProxyRepository
) {
    private val logger = LoggerFactory.getLogger(ProxyService::class.java)
    private val MAX_FAIL_THRESHOLD = 5

    @Transactional
    fun allocateProxy(): ProxyConfig? {
        val proxy = proxyRepository.findFirstByIsActiveTrueOrderByLastUsedAtAsc() ?: return null

        proxy.lastUsedAt = LocalDateTime.now()

        return ProxyConfig(
            id = proxy.id!!,
            host = proxy.host,
            port = proxy.port,
            username = proxy.username,
            password = proxy.password,
        )
    }

    @Transactional
    fun reportSuccess(proxyId: Long) {
        proxyRepository.findById(proxyId).ifPresent {
            it.recordSuccess()
        }
    }

    @Transactional
    fun reportFailure(proxyId: Long, isHardFail: Boolean = false) {
        proxyRepository.findById(proxyId).ifPresent {
            val penalty = if (isHardFail) 3 else 1
            val isBanned = it.recordFailure(penalty, MAX_FAIL_THRESHOLD)

            if (isBanned) {
                logger.warn("⛔️ Proxy ${it.host} has been deactivated due to excessive failures.")
            }
        }
    }
}