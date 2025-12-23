package com.copago.marketpulsedetector.core.scheduler

import com.copago.marketpulsedetector.core.v2.scheduler.CrawlJobCleanupScheduler
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest
class CrawlJobCleanupSchedulerTest {
    @Autowired
    private lateinit var crawlJobCleanupScheduler: CrawlJobCleanupScheduler

    @Test
    fun archive() {
        crawlJobCleanupScheduler.archive()
    }
}