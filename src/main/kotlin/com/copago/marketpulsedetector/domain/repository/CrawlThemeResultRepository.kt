package com.copago.marketpulsedetector.domain.repository

import com.copago.marketpulsedetector.domain.entity.CrawlThemeResultEntity
import org.springframework.data.jpa.repository.JpaRepository

interface CrawlThemeResultRepository: JpaRepository<CrawlThemeResultEntity, Long> {
}