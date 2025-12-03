package com.copago.marketpulsedetector.core.component

import com.copago.marketpulsedetector.common.exception.FatalException
import com.copago.marketpulsedetector.common.exception.RetryableException
import com.copago.marketpulsedetector.domain.entity.CrawlTargetEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jsoup.HttpStatusException
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.springframework.stereotype.Component
import java.io.IOException
import java.net.SocketTimeoutException

@Component
class HtmlFetcher {
    suspend fun fetch(target: CrawlTargetEntity): Document = withContext(Dispatchers.IO) {
        try {
            return@withContext Jsoup.connect(target.targetUrl)
                .userAgent(target.site.userAgent)
                .timeout(target.site.timeout.toInt() ?: 10000)
                .execute()
                .parse()
        } catch (e: HttpStatusException) {
            when (e.statusCode) {
                429, 500, 502, 503, 504 -> throw RetryableException("Server Error: ${e.statusCode}", e)
                400, 401, 403, 404 -> throw FatalException("Client Error: ${e.statusCode}", e)
                else -> throw FatalException("Unknown HTTP Error: ${e.statusCode}", e)
            }
        } catch (e: SocketTimeoutException) {
            throw RetryableException("Connection Timeout", e)
        } catch (e: IOException) {
            throw RetryableException("Network Error", e)
        } catch (e: Exception) {
            throw FatalException("Unexpected Error", e)
        }
    }
}