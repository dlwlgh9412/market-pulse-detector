package com.copago.marketpulsedetector.core.domain.model

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.LocalDateTime

@Entity
@Table(name = "tbl_user_agent")
data class UserAgent(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(length = 500, nullable = false)
    val agentString: String,

    @Enumerated(EnumType.STRING)
    @Column(length = 20, nullable = false)
    val deviceType: DeviceType,

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    val browserType: BrowserType? = null,

    var isActive: Boolean = true,

    @Column(updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),
)

enum class DeviceType {
    DESKTOP, MOBILE
}

enum class BrowserType {
    CHROME, FIREFOX, SAFARI
}
