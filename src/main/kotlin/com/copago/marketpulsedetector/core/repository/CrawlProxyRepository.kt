package com.copago.marketpulsedetector.core.repository

import com.copago.marketpulsedetector.core.domain.model.CrawlProxy
import org.springframework.data.jpa.repository.JpaRepository

interface CrawlProxyRepository: JpaRepository<CrawlProxy, Long> {
    fun findFirstByIsActiveTrueOrderByLastUsedAtAsc(): CrawlProxy?
}