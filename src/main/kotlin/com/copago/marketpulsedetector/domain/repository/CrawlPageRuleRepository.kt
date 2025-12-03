package com.copago.marketpulsedetector.domain.repository

import com.copago.marketpulsedetector.domain.entity.CrawlSiteEntity
import com.copago.marketpulsedetector.domain.entity.CrawlPageRuleEntity
import org.springframework.data.jpa.repository.JpaRepository

interface CrawlPageRuleRepository: JpaRepository<CrawlPageRuleEntity, Long> {
    fun findAllBySiteOrderByMatchPriorityDesc(site: CrawlSiteEntity): List<CrawlPageRuleEntity>
}