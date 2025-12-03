package com.copago.marketpulsedetector.domain.repository

import com.copago.marketpulsedetector.domain.entity.CrawlSiteEntity
import org.springframework.data.jpa.repository.JpaRepository

interface CrawlSiteRepository: JpaRepository<CrawlSiteEntity, Int> {
    fun findByDomain(domain: String): CrawlSiteEntity?
}