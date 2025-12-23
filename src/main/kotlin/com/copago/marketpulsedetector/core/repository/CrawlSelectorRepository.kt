package com.copago.marketpulsedetector.core.repository

import com.copago.marketpulsedetector.core.domain.model.CrawlSelector
import org.springframework.data.jpa.repository.JpaRepository

interface CrawlSelectorRepository : JpaRepository<CrawlSelector, Long> {
    fun findAllByPageDefinitionIdIn(pageDefIds: List<Long>): List<CrawlSelector>
    fun findAllByPageDefinitionId(id: Long): List<CrawlSelector>
}