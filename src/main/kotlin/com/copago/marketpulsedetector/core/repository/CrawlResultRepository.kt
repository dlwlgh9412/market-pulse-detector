package com.copago.marketpulsedetector.core.repository

import com.copago.marketpulsedetector.core.domain.model.CrawlResult
import org.springframework.data.jpa.repository.JpaRepository

interface CrawlResultRepository : JpaRepository<CrawlResult, Long> {
}