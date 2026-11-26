package dev.abhishekan.pulse.core.domain.model
import java.time.Instant

data class APIKey(
    val id: Long,
    val name: String,
    val prefix: String,
    val createdAt: Instant,
)

// A secret is deliberately not a data class: generated toString must never expose it.
class CreatedAPIKey(
    val key: APIKey,
    val token: String,
)
