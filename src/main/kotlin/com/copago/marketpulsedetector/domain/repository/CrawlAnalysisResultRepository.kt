package com.copago.marketpulsedetector.domain.repository

import com.copago.marketpulsedetector.domain.entity.CrawlAnalysisResultEntity
import org.springframework.data.jpa.repository.JpaRepository

interface CrawlAnalysisResultRepository: JpaRepository<CrawlAnalysisResultEntity, Long> {
    fun findAllByTargetId(targetId: Long): List<CrawlAnalysisResultEntity>
}