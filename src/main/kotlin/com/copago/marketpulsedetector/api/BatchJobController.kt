package com.copago.marketpulsedetector.api

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory
import org.springframework.batch.core.Job
import org.springframework.batch.core.JobParametersBuilder
import org.springframework.batch.core.launch.JobLauncher
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/batch")
class BatchJobController(
    private val jobLauncher: JobLauncher,
    private val stockSyncJob: Job
) {
    private val logger = LoggerFactory.getLogger(BatchJobController::class.java)
    private val batchScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    @PostMapping("/stock-sync")
    fun runStockSync(@RequestParam(defaultValue = "DAILY") mode: String): String {
        val timestamp = System.currentTimeMillis()

        batchScope.launch {
            try {
                logger.info("▶️ Triggering Stock Sync Job (Mode: $mode, Time: $timestamp)...")

                val params = JobParametersBuilder()
                    .addString("mode", mode)
                    .addLong("timestamp", timestamp)
                    .toJobParameters()

                val execution = jobLauncher.run(stockSyncJob, params)
                logger.info("✅ Job Finished. Status: ${execution.status}, ExitStatus: ${execution.exitStatus}")
            } catch (e: Exception) {
                logger.error("❌ Failed to execute batch job in background", e)
            }
        }

        return "🚀 Stock Sync Job Triggered Successfully! (Mode: $mode, Check logs for progress)"
    }
}