package dev.abhishekan.pulse.core.domain.usecase

import dev.abhishekan.pulse.core.domain.model.*
import dev.abhishekan.pulse.core.domain.repository.FitnessRepository
class SearchExercises(private val repository: FitnessRepository) {
    suspend operator fun invoke(query: String): Outcome<List<CatalogExercise>> {
        val value = query.trim()
        return if (value.isEmpty() || value.codePointCount(0, value.length) > 100) Outcome.Error(Failure.InvalidInput)
        else repository.search(value)
    }
}
