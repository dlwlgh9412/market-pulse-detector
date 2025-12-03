package com.copago.marketpulsedetector.domain.repository

import com.copago.marketpulsedetector.domain.entity.CrawledDataEntity
import org.springframework.data.jpa.repository.JpaRepository

interface CrawledDataRepository : JpaRepository<CrawledDataEntity, Long> {
}