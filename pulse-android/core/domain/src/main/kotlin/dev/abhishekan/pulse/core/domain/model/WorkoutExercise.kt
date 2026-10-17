package dev.abhishekan.pulse.core.domain.model

data class WorkoutExercise(val id: Long, val name: String, val catalogId: String?, val catalog: CatalogExercise?, val sets: List<WorkoutSet>)
