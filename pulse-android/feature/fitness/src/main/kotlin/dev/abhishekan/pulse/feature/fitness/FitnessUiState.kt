package dev.abhishekan.pulse.feature.fitness
import dev.abhishekan.pulse.core.domain.model.Failure
import dev.abhishekan.pulse.core.domain.model.FitnessOverview
import dev.abhishekan.pulse.core.ui.UiText
import java.time.LocalDate
import java.time.YearMonth

data class FitnessUiState(
    val selected: LocalDate,
    val month: YearMonth,
    val overview: FitnessOverview? = null,
    val loading: Boolean = true,
    val error: Failure? = null,
    val busy: Boolean = false,
    val message: UiText? = null,
    val needsReview: Boolean = false,
    val reviewReady: Boolean = false,
    val createdId: Long? = null,
)
