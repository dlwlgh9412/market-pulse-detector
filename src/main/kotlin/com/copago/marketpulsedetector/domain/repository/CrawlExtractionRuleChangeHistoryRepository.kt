package com.copago.marketpulsedetector.domain.repository

import com.copago.marketpulsedetector.domain.entity.CrawlExtractionRuleChangeHistoryEntity
import org.springframework.data.jpa.repository.JpaRepository

interface CrawlExtractionRuleChangeHistoryRepository: JpaRepository<CrawlExtractionRuleChangeHistoryEntity, Long> {
}