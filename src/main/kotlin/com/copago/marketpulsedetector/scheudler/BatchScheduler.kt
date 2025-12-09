package com.copago.marketpulsedetector.scheudler

import org.springframework.batch.core.Job
import org.springframework.batch.core.JobParametersBuilder
import org.springframework.batch.core.launch.JobLauncher
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class BatchScheduler(
    private val jobLauncher: JobLauncher,
    private val stockSyncJob: Job
) {
    @Scheduled(cron = "0 0 4 * * *")
    fun runDailySync() {
        val params = JobParametersBuilder()
            .addString("mode", "DAILY")
            .addLong("time", System.currentTimeMillis())
            .toJobParameters()

        jobLauncher.run(stockSyncJob, params)
    }
}