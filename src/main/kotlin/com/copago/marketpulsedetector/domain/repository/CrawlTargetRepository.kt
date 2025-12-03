package com.copago.marketpulsedetector.domain.repository

import com.copago.marketpulsedetector.domain.entity.CrawlTargetEntity
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.transaction.annotation.Transactional

interface CrawlTargetRepository : JpaRepository<CrawlTargetEntity, Long> {
    fun findTopBySiteIdAndStatusOrderByPriorityDescIdAsc(siteId: Long, status: String): CrawlTargetEntity?

    @EntityGraph(attributePaths = ["site", "pageRule"])
    fun findWithMetadataById(id: Long): CrawlTargetEntity?

    @EntityGraph(attributePaths = ["site", "source", "pageRule"])
    fun findWithSiteAndSourceAndPageRuleById(targetId: Long): CrawlTargetEntity?

    @Transactional
    @Modifying
    @Query(
        value = """
        insert into tbl_crawl_target(
        parent_target_id,
        source_id, page_rule_id, site_id,
        target_url, target_url_hash, page_type,
        status, priority, depth, created_at
        ) values 
        (:parentTargetId,
        :sourceId, :pageRuleId, :siteId,
        :targetUrl, :targetUrlHash, :pageType,
        :status, :priority, :depth, now()
        ) on duplicate key update
        status = 'PENDING',
        priority = :priority,
        page_rule_id = :pageRuleId,
        created_at = now(),
        retry_count = 0,
        last_processed_at = null
    """, nativeQuery = true
    )
    fun upsert(
        @Param("parentTargetId") parentTargetId: Long?,
        @Param("sourceId") sourceId: Long?,
        @Param("pageRuleId") pageRuleId: Long?,
        @Param("siteId") siteId: Long?,
        @Param("targetUrl") targetUrl: String?,
        @Param("targetUrlHash") targetUrlHash: String?,
        @Param("pageType") pageType: String?,
        @Param("status") status: String,
        @Param("priority") priority: Int?,
        @Param("depth") depth: Int?
    )

    @Query("""
        select t from CrawlTargetEntity t
        where t.site.id = :siteId
        and t.status = 'PENDING'
        and (t.nextTryAt is null or t.nextTryAt <= current timestamp)
        order by t.priority desc
        limit 1
    """)
    fun findNextTask(siteId: Long): CrawlTargetEntity?

    fun findAllByStatus(status: String, pageable: Pageable): Page<CrawlTargetEntity>
}