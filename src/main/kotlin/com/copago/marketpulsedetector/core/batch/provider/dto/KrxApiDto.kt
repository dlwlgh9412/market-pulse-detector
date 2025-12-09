package com.copago.marketpulsedetector.core.batch.provider.dto

import com.fasterxml.jackson.annotation.JsonProperty

data class KrxApiResponse(
    val response: KrxApiResponseWrapper
)

data class KrxApiResponseWrapper(
    val header: KrxResponseHeader,
    val body: KrxResponseBody
)

data class KrxResponseHeader(
    val resultCode: String,
    val resultMsg: String
)

data class KrxResponseBody(
    val numOfRows: Int,
    val pageNo: Int,
    val totalCount: Long,
    val items: KrxItems,
)

data class KrxItems(
    val item: List<KrxItem>
)

data class KrxItem(
    @JsonProperty("srtnCd")
    val shortCode: String,

    @JsonProperty("itmsNm")
    val itemName: String,

    @JsonProperty("mrktCtg")
    val marketCategory: String
)
