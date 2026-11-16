package dev.abhishekan.pulse.feature.mealplan.onboarding

import androidx.lifecycle.ViewModelStore
import dev.abhishekan.pulse.core.domain.model.Failure
import dev.abhishekan.pulse.core.domain.model.MealOnboarding
import dev.abhishekan.pulse.core.domain.model.MealOnboardingStatus
import dev.abhishekan.pulse.core.domain.model.MealPreferenceChoices
import dev.abhishekan.pulse.core.domain.model.MealPreferences
import dev.abhishekan.pulse.core.domain.model.Outcome
import dev.abhishekan.pulse.core.domain.repository.MealPreferencesRepository
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalTime

@OptIn(ExperimentalCoroutinesApi::class)
class MealOnboardingViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val models = ViewModelStore()
    private val complete = MealPreferences(goal = "feel_satisfied", dietaryPattern = "vegetarian")

    private class Repo : MealPreferencesRepository {
        var record = MealOnboarding(MealOnboardingStatus.Pending)
        var failure: Failure? = null
        var writeFailure: Failure? = null
        var commitBeforeFailure = false
        var writes = 0
        var reads = 0
        var hold: CompletableDeferred<Unit>? = null

        override suspend fun onboarding(): Outcome<MealOnboarding> {
            reads++
            return failure?.let { Outcome.Error(it) } ?: Outcome.Success(record)
        }

        override suspend fun resolve(preferences: MealPreferences?): Outcome<MealOnboarding> {
            writes++
            hold?.await()
            if (record.status != MealOnboardingStatus.Pending) return Outcome.Success(record)
            if (writeFailure == null ||
                commitBeforeFailure
            ) {
                record =
                    MealOnboarding(if (preferences == null) MealOnboardingStatus.Skipped else MealOnboardingStatus.Completed, preferences)
            }
            return writeFailure?.let { Outcome.Error(it) } ?: Outcome.Success(record)
        }

        override suspend fun save(preferences: MealPreferences): Outcome<MealOnboarding> {
            writes++
            if (writeFailure == null || commitBeforeFailure) record = record.copy(preferences = preferences)
            return writeFailure?.let { Outcome.Error(it) } ?: Outcome.Success(record)
        }
    }

    private val repo = Repo()

    @Before fun setup() {
        Dispatchers.setMain(dispatcher)
    }

    @After fun cleanup() {
        models.clear()
        dispatcher.scheduler.runCurrent()
        Dispatchers.resetMain()
    }

    private fun vm(key: String = "setup") = MealOnboardingViewModel(repo).also { models.put(key, it) }

    private fun MealOnboardingViewModel.answerRequiredQuestions() {
        onAction(MealOnboardingAction.SelectGoal(complete.goal))
        onAction(MealOnboardingAction.SelectDiet(complete.dietaryPattern))
    }

    @Test fun firstResumeUsesInitialReadAndLaterResumeReadsServerStatus() =
        runTest {
            val model = vm()
            runCurrent()
            model.onAction(MealOnboardingAction.Resume)
            runCurrent()
            assertEquals(1, repo.reads)

            repo.record = MealOnboarding(MealOnboardingStatus.Skipped)
            model.onAction(MealOnboardingAction.Resume)
            runCurrent()
            assertEquals(2, repo.reads)
            assertFalse(model.uiState.value.showJourney)
        }

    @Test fun consecutiveAnswersUseTheLatestDraft() =
        runTest {
            val model = vm()
            runCurrent()
            val barriers = MealPreferenceChoices.barriers.take(2)
            val avoidance = MealPreferenceChoices.avoidances.first()
            barriers.forEach { model.onAction(MealOnboardingAction.ToggleBarrier(it)) }
            model.onAction(MealOnboardingAction.ToggleAvoidance(avoidance))
            assertEquals(barriers, model.uiState.value.draft.barriers)
            assertEquals(listOf(avoidance), model.uiState.value.draft.foodAvoidances)
        }

    @Test fun webCompletionAndWebSkipBypassAndroidJourney() =
        runTest {
            for (status in listOf(MealOnboardingStatus.Completed, MealOnboardingStatus.Skipped)) {
                repo.record = MealOnboarding(status, if (status == MealOnboardingStatus.Completed) complete else null)
                val model = vm(status.name)
                assertNull(model.uiState.value.record)
                runCurrent()
                assertFalse(model.uiState.value.showJourney)
                assertEquals(0, repo.writes)
            }
        }

    @Test fun partialAnswersNeverDismissAndNewViewModelStartsPending() =
        runTest {
            val first = vm()
            runCurrent()
            first.answerRequiredQuestions()
            first.onAction(MealOnboardingAction.Next)
            runCurrent()
            assertEquals(0, repo.writes)
            assertTrue(first.uiState.value.showJourney)
            val restarted = vm("restarted")
            runCurrent()
            assertTrue(restarted.uiState.value.showJourney)
            assertEquals("", restarted.uiState.value.draft.goal)
        }

    @Test fun questionsRequireGoalDietAndMealsButAllowEmptyOptionalAnswers() =
        runTest {
            val model = vm()
            runCurrent()
            model.onAction(MealOnboardingAction.Next)
            assertEquals(0, model.uiState.value.invalidStep)
            model.answerRequiredQuestions()
            repeat(6) { model.onAction(MealOnboardingAction.Next) }
            assertEquals(6, model.uiState.value.step)
            model.onAction(MealOnboardingAction.ReviewStep(4))
            complete.meals.forEach { model.onAction(MealOnboardingAction.SetMealEnabled(it.kind, false)) }
            model.onAction(MealOnboardingAction.Next)
            assertEquals(4, model.uiState.value.invalidStep)
            assertEquals(0, repo.writes)
            complete.meals.forEach { model.onAction(MealOnboardingAction.SetMealEnabled(it.kind, true)) }
            model.onAction(MealOnboardingAction.Next)
            assertEquals(6, model.uiState.value.step)
        }

    @Test fun androidSaveAndSkipPersistForNextClientAndPreventDuplicateSubmission() =
        runTest {
            val model = vm()
            runCurrent()
            model.answerRequiredQuestions()
            repo.hold = CompletableDeferred()
            model.onAction(MealOnboardingAction.Save)
            model.onAction(MealOnboardingAction.Save)
            model.onAction(MealOnboardingAction.Skip)
            runCurrent()
            assertEquals(1, repo.writes)
            assertTrue(model.uiState.value.saving)
            repo.hold!!.complete(Unit)
            runCurrent()
            assertEquals(MealOnboardingStatus.Completed, repo.record.status)
            assertEquals(complete, repo.record.preferences)
            assertFalse(model.uiState.value.showJourney)
            repo.record = MealOnboarding(MealOnboardingStatus.Pending)
            val skipped = vm("skip")
            runCurrent()
            skipped.onAction(MealOnboardingAction.Skip)
            runCurrent()
            assertEquals(MealOnboardingStatus.Skipped, repo.record.status)
            assertNull(repo.record.preferences)
            assertFalse(skipped.uiState.value.showJourney)
        }

    @Test fun failedReadIsRetryableAndDoesNotAssumeFirstVisit() =
        runTest {
            repo.failure = Failure.Connection
            val model = vm()
            runCurrent()
            assertNull(model.uiState.value.record)
            assertEquals(Failure.Connection, model.uiState.value.error)
            repo.failure = null
            repo.record = MealOnboarding(MealOnboardingStatus.Skipped)
            model.onAction(MealOnboardingAction.Refresh)
            runCurrent()
            assertFalse(model.uiState.value.showJourney)
            assertEquals(0, repo.writes)
        }

    @Test fun failedWritesKeepAnswersAndLostSuccessfulResponsesReconcile() =
        runTest {
            val model = vm()
            runCurrent()
            model.answerRequiredQuestions()
            repo.writeFailure = Failure.Connection
            model.onAction(MealOnboardingAction.Save)
            runCurrent()
            assertTrue(model.uiState.value.showJourney)
            assertEquals(complete, model.uiState.value.draft)
            assertEquals(Failure.Connection, model.uiState.value.error)
            assertFalse(model.uiState.value.saving)
            repo.commitBeforeFailure = true
            model.onAction(MealOnboardingAction.Save)
            runCurrent()
            assertFalse(model.uiState.value.showJourney)
            assertEquals(MealOnboardingStatus.Completed, repo.record.status)
        }

    @Test fun returningFromWebRefreshesStatusAndAnOpposingDecisionIsAuthoritative() =
        runTest {
            val model = vm()
            runCurrent()
            model.answerRequiredQuestions()
            repo.record = MealOnboarding(MealOnboardingStatus.Skipped)
            model.onAction(MealOnboardingAction.Save)
            runCurrent()
            assertFalse(model.uiState.value.showJourney)
            assertTrue(model.uiState.value.resolvedElsewhere)
            repo.record = MealOnboarding(MealOnboardingStatus.Pending)
            val pending = vm("resume")
            runCurrent()
            repo.record = MealOnboarding(MealOnboardingStatus.Completed, complete)
            pending.onAction(MealOnboardingAction.Refresh)
            runCurrent()
            assertFalse(pending.uiState.value.showJourney)
            assertEquals(
                complete,
                pending.uiState.value.record
                    ?.preferences,
            )
        }

    @Test fun editingAfterSkipPreservesDecisionAndCancelDoesNotWrite() =
        runTest {
            repo.record = MealOnboarding(MealOnboardingStatus.Skipped)
            val model = vm()
            runCurrent()
            model.onAction(MealOnboardingAction.EditPreferences)
            model.answerRequiredQuestions()
            model.onAction(MealOnboardingAction.CancelEditing)
            assertEquals(0, repo.writes)
            assertFalse(model.uiState.value.showJourney)
            model.onAction(MealOnboardingAction.EditPreferences)
            model.answerRequiredQuestions()
            model.onAction(MealOnboardingAction.Save)
            runCurrent()
            assertEquals(MealOnboardingStatus.Skipped, repo.record.status)
            assertEquals(complete, repo.record.preferences)
            assertFalse(model.uiState.value.showJourney)
        }

    @Test fun failedSettingsWriteDoesNotDiscardDraftBecauseOldStatusIsTerminal() =
        runTest {
            repo.record = MealOnboarding(MealOnboardingStatus.Completed, complete)
            val model = vm()
            runCurrent()
            model.onAction(MealOnboardingAction.EditPreferences)
            val updated = complete.copy(goal = "food_calm")
            model.onAction(MealOnboardingAction.SelectGoal(updated.goal))
            repo.writeFailure = Failure.Timeout
            model.onAction(MealOnboardingAction.Save)
            runCurrent()
            assertTrue(model.uiState.value.editing)
            assertEquals(updated, model.uiState.value.draft)
            repo.commitBeforeFailure = true
            model.onAction(MealOnboardingAction.Save)
            runCurrent()
            assertFalse(model.uiState.value.editing)
        }

    @Test fun disabledMealTimeIsRestoredWithinAnEditAndClearedOnCancel() =
        runTest {
            val breakfast = complete.meals.first()
            val committed = complete.copy(meals = complete.meals - breakfast, planningSlotsConfigured = true)
            repo.record = MealOnboarding(MealOnboardingStatus.Completed, committed)
            val model = vm()
            runCurrent()
            val kind = breakfast.kind
            val editedTime = LocalTime.of(8, 15)

            model.onAction(MealOnboardingAction.EditPreferences)
            model.onAction(MealOnboardingAction.SetMealEnabled(kind, true))
            model.onAction(MealOnboardingAction.SetMealTime(kind, editedTime))
            model.onAction(MealOnboardingAction.SetMealEnabled(kind, false))
            model.onAction(MealOnboardingAction.SetMealEnabled(kind, true))
            assertEquals(
                editedTime,
                model.uiState.value.draft.meals
                    .first()
                    .time,
            )

            model.onAction(MealOnboardingAction.SetMealEnabled(kind, false))
            model.onAction(MealOnboardingAction.CancelEditing)
            model.onAction(MealOnboardingAction.EditPreferences)
            model.onAction(MealOnboardingAction.SetMealEnabled(kind, true))
            assertEquals(
                breakfast,
                model.uiState.value.draft.meals
                    .first(),
            )
        }
}
