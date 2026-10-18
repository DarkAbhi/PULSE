package dev.abhishekan.pulse.core.domain.model

import java.time.Instant

data class Notification(
    val id: Long,
    val title: String,
    val body: String?,
    val createdAt: Instant?,
    val source: String = "",
    val priority: Int = 0,
    val targetPath: String? = null,
)
