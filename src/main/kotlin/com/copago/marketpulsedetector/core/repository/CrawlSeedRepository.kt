package com.copago.marketpulsedetector.core.repository

import com.copago.marketpulsedetector.core.domain.model.CrawlSeed
import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface CrawlSeedRepository : JpaRepository<CrawlSeed, Long> {
    @EntityGraph(attributePaths = ["site", "pageDefinition"])
    @Query(
        """
        select s
        from CrawlSeed s
        where s.site.id = :siteId
        and s.isActive = true
        and s.nextRunAt <= :now
        and s.site.isActive = true
    """
    )
    fun findDueSeeds(@Param("siteId") siteId: Long, @Param("now") now: Long): List<CrawlSeed>
}