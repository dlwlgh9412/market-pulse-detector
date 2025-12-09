package com.copago.marketpulsedetector.domain.repository

import com.copago.marketpulsedetector.domain.entity.DataAnomalyLogEntity
import org.springframework.data.jpa.repository.JpaRepository

interface DataAnomalyLogRepository: JpaRepository<DataAnomalyLogEntity, Long> {
    fun findByTargetKeyAndAnomalyType(targetKey: String, type: String): DataAnomalyLogEntity?
}