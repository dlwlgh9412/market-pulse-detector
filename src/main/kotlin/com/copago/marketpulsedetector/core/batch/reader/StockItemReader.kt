package com.copago.marketpulsedetector.core.batch.reader

import com.copago.marketpulsedetector.core.batch.provider.StockDataProvider
import com.copago.marketpulsedetector.core.batch.provider.dto.StockDto
import org.springframework.batch.item.ItemReader

class StockItemReader(
    private val provider: StockDataProvider,
    private val pageSize: Int = 1000
) : ItemReader<StockDto> {
    private var pageNo = 1
    private var buffer = mutableListOf<StockDto>()
    private var totalCount = -1L
    private var fetchedCount = 0L

    override fun read(): StockDto? {
        if (buffer.isNotEmpty()) {
            return buffer.removeAt(0)
        }

        if (totalCount != -1L && fetchedCount >= totalCount) {
            return null
        }

        fetchNextPage()

        return if (buffer.isNotEmpty()) buffer.removeAt(0) else null
    }

    private fun fetchNextPage() {
        val result = provider.fetchPage(pageNo, pageSize)

        if (totalCount == -1L) {
            totalCount = result.totalCount
        }

        if (result.items.isNotEmpty()) {
            buffer.addAll(result.items)
            fetchedCount += result.items.size
            pageNo++
        } else {
            fetchedCount = totalCount + 1 // 강제 종료
        }
    }
}

