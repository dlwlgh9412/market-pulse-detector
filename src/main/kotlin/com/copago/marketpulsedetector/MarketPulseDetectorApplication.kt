package com.copago.marketpulsedetector

import net.javacrumbs.shedlock.spring.annotation.EnableSchedulerLock
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.ComponentScan
import org.springframework.scheduling.annotation.EnableScheduling

//@ComponentScan(basePackages = ["com.copago.marketpulsedetector.core.v2", "com.copago.marketpulsedetector.config"])
@EnableScheduling
@EnableSchedulerLock(defaultLockAtMostFor = "PT30S")
@SpringBootApplication
class MarketPulseDetectorApplication

fun main(args: Array<String>) {
    runApplication<MarketPulseDetectorApplication>(*args)
}
