package com.copago.marketpulsedetector.core.repository

import com.copago.marketpulsedetector.core.domain.model.CrawlQueue
import com.copago.marketpulsedetector.core.domain.model.CrawlStatus
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.LocalDateTime

interface CrawlQueueRepository : JpaRepository<CrawlQueue, Long> {
    @EntityGraph(attributePaths = ["seed", "pageDefinition"])
    fun findFirstBySiteIdAndStatusOrderByIdAsc(siteId: Long, status: CrawlStatus): CrawlQueue?

    @EntityGraph(attributePaths = ["seed", "pageDefinition"])
    @Query(
        """
        select q
        from CrawlQueue q
        where q.site.id = :siteId
        and q.status = :status
        and q.nextRunAt <= :now
        order by q.nextRunAt asc
    """
    )
    fun findNextCrawlJob(
        @Param("siteId") siteId: Long,
        @Param("status") status: CrawlStatus,
        @Param("now") now: LocalDateTime,
        pageable: Pageable
    ): List<CrawlQueue>

    @Query(
        """
        select q
        from CrawlQueue q
        where q.status in ('PROCESSING', 'FAIL')
        and q.expiredAt < now()
        order by q.expiredAt
    """
    )
    fun findTimeOutQueue(pageable: Pageable): List<CrawlQueue>

    @Modifying(clearAutomatically = true)
    @Query(
        """
        insert into tbl_crawl_queue_history (id, site_id, seed_id, page_definition_id, url, worker_id, last_processing_at)
        select q.id, q.site_id, q.seed_id, q.page_definition_id, q.url, q.worker_id, q.last_processing_at
        from tbl_crawl_queue q
        where q.status = 'DONE' and q.last_processing_at < :cutoffTime
    """, nativeQuery = true
    )
    fun migrateDoneJobs(@Param("cutoffTime") cutoffTime: LocalDateTime): Int

    @Modifying(clearAutomatically = true)
    @Query(
        """
        delete from CrawlQueue q
        where q.status = 'DONE' and q.lastProcessingAt < :cutoffTime
    """
    )
    fun deleteMigratedJobs(@Param("cutoffTime") cutoffTime: LocalDateTime): Int
}