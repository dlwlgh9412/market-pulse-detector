package com.copago.marketpulsedetector.core.repository

import com.copago.marketpulsedetector.core.domain.model.CrawlSite
import org.springframework.data.jpa.repository.JpaRepository

interface CrawlSiteRepository: JpaRepository<CrawlSite, Long> {
}