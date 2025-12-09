package com.copago.marketpulsedetector.core.batch.provider

import com.copago.marketpulsedetector.core.batch.provider.dto.KrxApiResponse
import com.copago.marketpulsedetector.core.batch.provider.dto.StockDto
import org.slf4j.LoggerFactory
import org.springframework.batch.core.configuration.annotation.StepScope
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.util.UriComponentsBuilder
import java.net.URLEncoder
import java.time.LocalDate

@Component
@StepScope
class KrxStockProvider(
    @param:Qualifier("krxWebClient") private val webClient: WebClient,
    @param:Value("\${api.public-krx-data.url}") private val apiUrl: String,
    @param:Value("\${api.public-krx-data.decode-key}") private val decodeKey: String,
    @param:Value("#{jobParameters['mode']}") private val mode: String?
) : StockDataProvider {
    private val logger = LoggerFactory.getLogger(KrxStockProvider::class.java)
    private val targetDate: LocalDate? = if (mode == "INIT") null else LocalDate.now()

    override fun fetchPage(pageNo: Int, pageSize: Int): StockPageResult {
        logger.info("📡 Fetching Domestic Stocks from KRX/PublicData...")

        val encodeKey = URLEncoder.encode(decodeKey, "UTF-8")

        val uri = UriComponentsBuilder.fromUriString(apiUrl)
            .queryParam("serviceKey", encodeKey)
            .queryParam("resultType", "json")
            .queryParam("pageNo", pageNo)
            .queryParam("numOfRows", pageSize)
            .queryParam("basDt", targetDate)
            .build(true)
            .toUri()

        val result = webClient.get()
            .uri(uri)
            .retrieve()
            .bodyToMono(KrxApiResponse::class.java)
            .block()

        if (result == null || result.response.body.totalCount == 0L) {
            logger.warn("⚠️ KRX API returned empty result.")
            return StockPageResult(emptyList(), 0)
        }

        val items = result.response.body.items.item
        logger.info("✅ Fetched ${items.size} items from API.")

        val stockItems = items.map { it ->
            val cleanCode = if (it.shortCode.startsWith("A")) {
                it.shortCode.substring(1)
            } else  it.shortCode

            StockDto(
                code = cleanCode,
                name = it.itemName,
                market = it.marketCategory,
                country = "KR"
            )
        }

        return StockPageResult(items = stockItems, totalCount = result.response.body.totalCount)
    }
}