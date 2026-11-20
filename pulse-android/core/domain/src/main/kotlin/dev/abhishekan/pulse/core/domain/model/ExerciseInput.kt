package dev.abhishekan.pulse.core.domain.model

import java.math.BigDecimal

data class ExerciseInput(
    val name: String,
    val sets: List<ExerciseSetInput>,
    val catalogId: String?,
    val rememberAlias: Boolean,
) {
    val issue: ExerciseIssue? get() =
        when {
            name.trim().let {
                it.isEmpty() || it.codePointCount(0, it.length) > 100
            } -> ExerciseIssue.Name
            sets.size !in 1..20 -> ExerciseIssue.Sets
            sets.any { it.reps !in 1..1000 } -> ExerciseIssue.Reps
            sets.any {
                it.weight != null && (it.weight.signum() < 0 || it.weight > MaxWeight)
            } -> ExerciseIssue.Weight
            catalogId != null &&
                (
                    catalogId.isBlank() ||
                        catalogId
                            .toByteArray(
                                Charsets.UTF_8,
                            ).size > 200
                ) -> ExerciseIssue.Catalog
            rememberAlias && catalogId == null -> ExerciseIssue.Alias
            else -> null
        }

    private companion object {
        val MaxWeight = BigDecimal("1.7976931348623157E+308")
    }
}
