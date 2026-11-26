package dev.abhishekan.pulse.core.data

import dev.abhishekan.pulse.core.domain.model.DailyQuest
import dev.abhishekan.pulse.core.domain.model.QuestActivity
import dev.abhishekan.pulse.core.domain.model.QuestActivityDay
import dev.abhishekan.pulse.core.domain.model.QuestDay
import dev.abhishekan.pulse.core.domain.model.QuestOverview
import dev.abhishekan.pulse.core.domain.model.QuestProgress
import dev.abhishekan.pulse.core.domain.model.QuestSettings
import dev.abhishekan.pulse.core.domain.model.QuestSettingsView
import dev.abhishekan.pulse.core.domain.repository.QuestRepository
import dev.abhishekan.pulse.core.network.QuestApi
import dev.abhishekan.pulse.core.network.dto.QuestSettingsDto
import io.ktor.client.plugins.ClientRequestException
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.time.Instant
import java.time.LocalDate

class PulseQuestRepository(
    private val api: QuestApi,
    private val auth: PulseAuthRepository,
) : QuestRepository {
    private val revision = MutableStateFlow(0L)
    override val changes = revision.asStateFlow()

    override suspend fun overview(week: LocalDate?) =
        auth.authenticatedRequest {
            val response =
                try {
                    api.overview(week?.toString())
                } catch (error: ClientRequestException) {
                    if (week == null || error.response.status != HttpStatusCode.BadRequest) throw error
                    api.overview(null)
                }
            val progress = response.progress
            QuestOverview(
                QuestSettingsView(
                    response.configured,
                    response.settings.domain(),
                    response.effective?.let(LocalDate::parse),
                    response.hasWorkoutDays,
                ),
                LocalDate.parse(response.today),
                Instant.parse(response.nextResetAt),
                response.week.map { QuestDay(LocalDate.parse(it.date), it.status, it.completed, it.total) },
                response.previousWeek?.let(LocalDate::parse),
                response.nextWeek?.let(LocalDate::parse),
                response.todayQuests.orEmpty().map { DailyQuest(it.id, it.target, it.progress, it.xp, it.hearts, it.completed) },
                QuestProgress(
                    progress.lifetimeXp,
                    progress.hearts,
                    progress.level,
                    progress.todayXp,
                    progress.todayXpTarget,
                    progress.todayComplete,
                    progress.weekXp,
                    progress.weekHearts,
                    progress.weekActive,
                    progress.streak,
                    progress.bestStreak,
                ),
            )
        }

    override suspend fun activity() =
        auth.authenticatedRequest {
            val response = api.activity()
            QuestActivity(
                LocalDate.parse(response.today),
                response.days.orEmpty().map { QuestActivityDay(LocalDate.parse(it.date), it.completedIds.orEmpty()) },
            )
        }

    override suspend fun settings() =
        auth.authenticatedRequest {
            val response = api.settings()
            QuestSettingsView(
                response.configured,
                response.settings.domain(),
                response.effective?.let(LocalDate::parse),
                response.hasWorkoutDays,
            )
        }

    override suspend fun saveSettings(settings: QuestSettings) =
        auth.authenticatedRequest(onResult = { revision.update { it + 1 } }) {
            api.saveSettings(
                QuestSettingsDto(
                    settings.gymWorkout,
                    settings.stepTarget,
                    settings.activityRings,
                    settings.sleepMinutes,
                    settings.dailyMeals,
                ),
            )
        }

    private fun QuestSettingsDto.domain() = QuestSettings(gymWorkout, stepTarget, activityRings, sleepMinutes, dailyMeals)
}
