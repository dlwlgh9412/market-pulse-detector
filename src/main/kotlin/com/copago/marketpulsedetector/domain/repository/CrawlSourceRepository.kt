package com.copago.marketpulsedetector.domain.repository

import com.copago.marketpulsedetector.domain.entity.CrawlSourceEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import java.time.LocalDateTime

interface CrawlSourceRepository : JpaRepository<CrawlSourceEntity, Long> {
    @Query("select s from CrawlSourceEntity s where s.isActive = true and (s.lastGeneratedAt is null or s.lastGeneratedAt <= :cutoffTime)")
    fun findDueSources(cutoffTime: LocalDateTime): List<CrawlSourceEntity>
}