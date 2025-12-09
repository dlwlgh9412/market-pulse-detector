package com.copago.marketpulsedetector.common.log

import com.copago.marketpulsedetector.domain.entity.DataAnomalyLogEntity
import com.copago.marketpulsedetector.domain.repository.DataAnomalyLogRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional

@Service
class AnomalyLogService(
    private val repository: DataAnomalyLogRepository
) {
    private val logger = LoggerFactory.getLogger(AnomalyLogService::class.java)

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun record(key: String, type: String, msg: String?) {
        try {
            val log = repository.findByTargetKeyAndAnomalyType(key, type)

            if (log != null) log.recur()
            else {
                repository.save(
                    DataAnomalyLogEntity(
                        targetKey = key,
                        anomalyType = type,
                        payload = msg
                    )
                )
            }
        } catch (e: Exception) {
            logger.error("Failed to record anomaly log: $key, $type", e)
        }
    }
}