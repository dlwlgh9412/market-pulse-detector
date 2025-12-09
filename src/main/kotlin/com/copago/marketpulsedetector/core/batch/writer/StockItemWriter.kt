package com.copago.marketpulsedetector.core.batch.writer

import com.copago.marketpulsedetector.core.batch.component.AliasGenerator
import com.copago.marketpulsedetector.core.batch.provider.dto.StockDto
import com.copago.marketpulsedetector.domain.entity.StockAliasEntity
import com.copago.marketpulsedetector.domain.entity.StockHistoryEntity
import com.copago.marketpulsedetector.domain.entity.StockInfoEntity
import com.copago.marketpulsedetector.domain.repository.StockAliasRepository
import com.copago.marketpulsedetector.domain.repository.StockHistoryRepository
import com.copago.marketpulsedetector.domain.repository.StockInfoRepository
import org.springframework.batch.item.Chunk
import org.springframework.batch.item.ItemWriter
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

@Component
class StockItemWriter(
    private val stockInfoRepository: StockInfoRepository,
    private val stockAliasRepository: StockAliasRepository,
    private val stockHistoryRepository: StockHistoryRepository,
    private val aliasGenerator: AliasGenerator,
) : ItemWriter<StockDto> {

    @Transactional
    override fun write(chunk: Chunk<out StockDto?>) {
        val today = LocalDate.now()

        val items = chunk.filterNotNull()
        if (items.isEmpty()) return

        val codes = items.map { it.code }
        val existingStocks = stockInfoRepository.findAllById(codes).associateBy { it.code }

        for (dto in items) {
            val existing = existingStocks[dto.code]

            if (existing == null) {
                val newStock = StockInfoEntity(
                    code = dto.code,
                    stockName = dto.name,
                    marketType = dto.market,
                    country = dto.country,
                    isActive = true
                )

                val saved = stockInfoRepository.save(newStock)

                stockHistoryRepository.save(
                    StockHistoryEntity(stockCode = saved.code, changeType = "NEW", changeDate = today)
                )

                saveAliases(saved, dto)
            } else {
                if (existing.stockName != dto.name) {
                    val oldName = existing.stockName
                    val updated = existing.copy(stockName = dto.name, isActive = true)
                    stockInfoRepository.save(updated)

                    stockHistoryRepository.save(
                        StockHistoryEntity(
                            stockCode = updated.code,
                            changeType = "RENAMED",
                            changeDate = today,
                            oldStockName = oldName,
                        )
                    )

                    stockAliasRepository.save(StockAliasEntity(stock = updated, alias = oldName))
                    saveAliases(updated, dto)
                }
                // 시장 정보 변경 (코스닥 -> 코스피) 체크 등 추가
            }
        }
    }

    private fun saveAliases(stock: StockInfoEntity, dto: StockDto) {
        val generated = aliasGenerator.generate(dto)
        val existingAliases = stockAliasRepository.findAllByStockCode(stock.code).map { it.alias }.toSet()

        generated.filter { !existingAliases.contains(it) }.forEach { alias ->
            stockAliasRepository.save(StockAliasEntity(stock = stock, alias = alias))
        }
    }
}