package com.copago.marketpulsedetector.core.fetcher

import com.copago.marketpulsedetector.core.service.ProxyService
import com.copago.marketpulsedetector.core.service.UserAgentService
import io.netty.handler.timeout.ReadTimeoutHandler
import io.netty.handler.timeout.WriteTimeoutHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.withContext
import org.springframework.http.client.reactive.ReactorClientHttpConnector
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.toEntity
import org.springframework.web.util.UriComponentsBuilder
import reactor.netty.http.client.HttpClient
import reactor.netty.resources.ConnectionProvider
import reactor.netty.transport.ProxyProvider
import java.time.Duration
import java.util.concurrent.TimeUnit

@Component
class WebClientFetcher(
    private val userAgentService: UserAgentService,
    private val proxyService: ProxyService,
) : Fetcher {
    private val connectionProvider = ConnectionProvider.builder("crawler-pool")
        .maxConnections(500)
        .pendingAcquireMaxCount(1000)
        .build()

    private val baseHttpClient = HttpClient.create(connectionProvider)
        .followRedirect(true)
        .compress(true)

    override suspend fun fetch(request: FetchRequest): FetchResponse {
        var httpClient = baseHttpClient
            .responseTimeout(Duration.ofMillis(request.timeout))
            .doOnConnected {
                it.addHandlerLast(ReadTimeoutHandler(request.timeout, TimeUnit.MILLISECONDS))
                it.addHandlerLast(WriteTimeoutHandler(request.timeout, TimeUnit.MILLISECONDS))
            }

        val proxy = withContext(Dispatchers.IO) {
            proxyService.allocateProxy()
        }

        if (proxy != null) {
            httpClient = httpClient.proxy {
                val builder = it.type(ProxyProvider.Proxy.HTTP)
                    .host(proxy.host)
                    .port(proxy.port)

                if (!proxy.username.isNullOrBlank() && !proxy.password.isNullOrBlank()) {
                    builder.username(proxy.username)
                    builder.password { proxy.password }
                }
            }
        }


        val client = WebClient.builder()
            .clientConnector(ReactorClientHttpConnector(httpClient))
            .codecs { it.defaultCodecs().maxInMemorySize(10 * 1024 * 1024) }
            .build()

        val uri = UriComponentsBuilder.fromUriString(request.url)
            .apply {
                if (request.queryParams.isNotEmpty()) {
                    request.queryParams.forEach { (key, value) -> queryParam(key, value) }
                }
            }
            .build().toUri()

        val spec = client.method(request.method)
            .uri(uri)
            .headers { header ->
                request.headers.forEach(header::add)
                header.add("User-Agent", userAgentService.getRandomUserAgent())
            }

        val finalSpec = if (request.body != null) spec.bodyValue(request.body) else spec

        val response = finalSpec.retrieve()
            .toEntity<String>()
            .map {
                FetchResponse(
                    url = request.url,
                    finalUrl = request.url,
                    status = it.statusCode.value(),
                    body = it.body ?: "",
                    headers = it.headers
                )
            }.awaitSingle()

        if (response.status in 200..299) {
            withContext(Dispatchers.IO) {
                proxy?.let { proxyService.reportSuccess(it.id) }
            }
        } else {
            withContext(Dispatchers.IO) {
                proxy?.let { proxyService.reportFailure(it.id) }
            }
        }

        return response
    }
}