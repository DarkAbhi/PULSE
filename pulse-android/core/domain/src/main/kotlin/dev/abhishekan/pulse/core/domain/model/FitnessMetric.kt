package dev.abhishekan.pulse.core.domain.model

data class FitnessMetric(
    val id: String,
    val label: String,
    val value: String,
    val subtitle: String,
    val note: String?,
)
