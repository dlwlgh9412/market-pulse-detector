package com.copago.marketpulsedetector.domain.repository

import com.copago.marketpulsedetector.domain.entity.CrawlPageRuleEntity
import com.copago.marketpulsedetector.domain.entity.CrawlPriorityRuleEntity
import org.springframework.data.jpa.repository.JpaRepository

interface CrawlPriorityRuleRepository : JpaRepository<CrawlPriorityRuleEntity, Long> {
    fun findAllByPageRule(pageRule: CrawlPageRuleEntity): List<CrawlPriorityRuleEntity>
}