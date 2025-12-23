package com.copago.marketpulsedetector.core.repository

import com.copago.marketpulsedetector.core.domain.model.UserAgent
import org.springframework.data.jpa.repository.JpaRepository

interface UserAgentRepository: JpaRepository<UserAgent, Long> {
    fun findAllByIsActiveTrue(): List<UserAgent>
}