package dev.abhishekan.pulse.core.domain.repository

import dev.abhishekan.pulse.core.domain.model.Outcome
import kotlinx.coroutines.flow.Flow

interface DashboardPreferencesRepository {
    val compact: Flow<Boolean>

    suspend fun setCompact(compact: Boolean): Outcome<Unit>
}
