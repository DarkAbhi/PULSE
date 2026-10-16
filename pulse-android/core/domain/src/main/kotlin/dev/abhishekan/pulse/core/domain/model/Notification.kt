package dev.abhishekan.pulse.core.domain.model

import java.time.Instant

data class Notification(val id: Long, val title: String, val body: String?, val createdAt: Instant?)
