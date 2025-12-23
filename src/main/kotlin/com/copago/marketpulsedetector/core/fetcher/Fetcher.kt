package com.copago.marketpulsedetector.core.fetcher

import com.copago.marketpulsedetector.core.domain.model.DeviceType
import org.springframework.http.HttpMethod

interface Fetcher {
    suspend fun fetch(request: FetchRequest): FetchResponse
}

data class ProxyConfig (
    val id: Long,
    val host: String,
    val port: Int,
    val username: String? = null,
    val password: String? = null,
)

data class FetchRequest(
    val url: String,
    val method: HttpMethod = HttpMethod.GET,
    val headers: Map<String, String> = mutableMapOf(),
    val queryParams: Map<String, Any> = mutableMapOf(),
    val body: String? = null,
    val timeout: Long = 10_000,
    val proxyConfig: ProxyConfig? = null,
    val deviceType: DeviceType = DeviceType.DESKTOP
)

data class FetchResponse(
    val url: String,
    val finalUrl: String,
    val status: Int,
    val body: String,
    val headers: Map<String, List<String>> = emptyMap()
)