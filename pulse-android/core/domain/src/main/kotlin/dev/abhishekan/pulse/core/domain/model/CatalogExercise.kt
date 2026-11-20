package dev.abhishekan.pulse.core.domain.model

data class CatalogExercise(
    val id: String,
    val name: String,
    val equipment: String?,
    val muscles: List<String>,
    val instructions: List<String>,
    val matchType: String? = null,
)
