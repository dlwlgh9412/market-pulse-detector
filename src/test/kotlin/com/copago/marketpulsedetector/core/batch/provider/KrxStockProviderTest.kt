package com.copago.marketpulsedetector.core.batch.provider

import com.copago.marketpulsedetector.scheudler.CrawlHealingScheduler
import com.copago.marketpulsedetector.scheudler.CrawlScheduler
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.bean.override.mockito.MockitoBean

@SpringBootTest
class KrxStockProviderTest {
    @Autowired
    private lateinit var krxStockProvider: KrxStockProvider

    @MockitoBean
    private lateinit var crawlScheduler: CrawlScheduler

    @MockitoBean
    private lateinit var crawlHealingScheduler: CrawlHealingScheduler

    //    @Disabled
    @Test
    @DisplayName("실제 종목정보 요청")
    fun requestKrx() {
        val result = krxStockProvider.fetchPage(1000)

        assertTrue(result.isNotEmpty())
        assertEquals(result.size, 1000)
    }

}