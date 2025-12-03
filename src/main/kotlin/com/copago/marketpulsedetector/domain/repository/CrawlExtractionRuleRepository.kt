package com.copago.marketpulsedetector.domain.repository

import com.copago.marketpulsedetector.domain.entity.CrawlExtractionRuleEntity
import com.copago.marketpulsedetector.domain.entity.CrawlPageRuleEntity
import org.springframework.data.jpa.repository.JpaRepository

interface CrawlExtractionRuleRepository : JpaRepository<CrawlExtractionRuleEntity, Long> {
//    fun findAllByTarget(target: CrawlTargetEntity): List<CrawlStructureEntity>
//    fun findAllByTargetAndStatus(target: CrawlTargetEntity, status: String): List<CrawlStructureEntity>
    fun findAllByPageRuleAndStatus(
    pageRule: CrawlPageRuleEntity,
    status: String
    ): List<CrawlExtractionRuleEntity>

    fun findAllByPageRule(pageRule: CrawlPageRuleEntity): List<CrawlExtractionRuleEntity>
}