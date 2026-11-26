package dev.abhishekan.pulse.core.domain.repository

import dev.abhishekan.pulse.core.domain.model.Outcome
import dev.abhishekan.pulse.core.domain.model.QuestActivity
import dev.abhishekan.pulse.core.domain.model.QuestOverview
import dev.abhishekan.pulse.core.domain.model.QuestSettings
import dev.abhishekan.pulse.core.domain.model.QuestSettingsView
import kotlinx.coroutines.flow.StateFlow
import java.time.LocalDate

interface QuestRepository {
    val changes: StateFlow<Long>

    suspend fun overview(week: LocalDate?): Outcome<QuestOverview>

    suspend fun activity(): Outcome<QuestActivity>

    suspend fun settings(): Outcome<QuestSettingsView>

    suspend fun saveSettings(settings: QuestSettings): Outcome<Unit>
}
