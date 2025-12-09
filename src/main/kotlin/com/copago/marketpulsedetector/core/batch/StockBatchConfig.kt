package com.copago.marketpulsedetector.core.batch

import com.copago.marketpulsedetector.core.batch.provider.StockDataProvider
import com.copago.marketpulsedetector.core.batch.provider.dto.StockDto
import com.copago.marketpulsedetector.core.batch.reader.StockItemReader
import com.copago.marketpulsedetector.core.batch.writer.StockItemWriter
import com.copago.marketpulsedetector.domain.entity.StockHistoryEntity
import com.copago.marketpulsedetector.domain.repository.StockHistoryRepository
import com.copago.marketpulsedetector.domain.repository.StockInfoRepository
import org.slf4j.LoggerFactory
import org.springframework.batch.core.Job
import org.springframework.batch.core.Step
import org.springframework.batch.core.configuration.annotation.StepScope
import org.springframework.batch.core.job.builder.JobBuilder
import org.springframework.batch.core.repository.JobRepository
import org.springframework.batch.core.step.builder.StepBuilder
import org.springframework.batch.core.step.tasklet.Tasklet
import org.springframework.batch.item.ItemReader
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.transaction.PlatformTransactionManager
import java.time.LocalDate

@Configuration
class StockBatchConfig(
    private val jobRepository: JobRepository,
    private val transactionManager: PlatformTransactionManager,
    private val stockItemWriter: StockItemWriter,
    private val krxStockProvider: StockDataProvider,
    private val stockInfoRepository: StockInfoRepository,
    private val stockHistoryRepository: StockHistoryRepository,
) {
    private val logger = LoggerFactory.getLogger(StockBatchConfig::class.java)

    @Bean
    fun stockSyncJob(): Job {
        return JobBuilder("stockSyncJob", jobRepository)
            .start(stockChunkStep())
            .next(stockDelistingStep())
            .build()
    }

    @Bean
    fun stockChunkStep(): Step {
        return StepBuilder("stockChunkStep", jobRepository)
            .chunk<StockDto, StockDto>(1000, transactionManager)
            .reader(stockItemReader())
            .writer(stockItemWriter)
            .build()
    }

    @Bean
    @StepScope
    fun stockItemReader(): ItemReader<StockDto> {
        return StockItemReader(krxStockProvider, pageSize = 1000)
    }

    @Bean
    fun stockDelistingStep(): Step {
        return StepBuilder("stockDelistingStep", jobRepository)
            .tasklet(delistingTasklet(), transactionManager)
            .build()
    }

    @Bean
    fun delistingTasklet(): Tasklet {
        return Tasklet { _, _ ->
            val today = LocalDate.now()
            // 오늘 업데이트되지 않은 활성 종목 조회 (updatedAt < Today Start)
            // (Repository에 findByIsActiveTrueAndUpdatedAtBefore 메서드 필요)
            val zombies = stockInfoRepository.findByIsActiveTrueAndUpdatedAtBefore(today.atStartOfDay())

            logger.info("💀 Checking for delisted stocks... Found potential: ${zombies.size}")

            for (zombie in zombies) {
                // 정말 상폐인지 더블 체크 로직이 있으면 좋으나, 여기서는 배치 로직상 상폐로 간주
                zombie.isActive = false
                zombie.delistedAt = today
                stockInfoRepository.save(zombie)

                stockHistoryRepository.save(
                    StockHistoryEntity(
                        stockCode = zombie.code,
                        changeType = "DELISTED",
                        changeDate = today
                    )
                )
            }
            logger.info("✅ Delisting Process Complete. Processed: ${zombies.size}")
            null
        }
    }
}