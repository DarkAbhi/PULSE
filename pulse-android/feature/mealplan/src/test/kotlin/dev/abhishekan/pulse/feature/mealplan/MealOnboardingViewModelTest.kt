package dev.abhishekan.pulse.feature.mealplan

import androidx.lifecycle.ViewModelStore
import dev.abhishekan.pulse.core.domain.model.Failure
import dev.abhishekan.pulse.core.domain.model.MealOnboarding
import dev.abhishekan.pulse.core.domain.model.MealOnboardingStatus
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
        var hold: CompletableDeferred<Unit>? = null

        override suspend fun onboarding(): Outcome<MealOnboarding> = failure?.let { Outcome.Error(it) } ?: Outcome.Success(record)

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
            first.update(complete)
            first.next()
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
            model.next()
            assertEquals(0, model.uiState.value.invalidStep)
            model.update(complete)
            repeat(6) { model.next() }
            assertEquals(6, model.uiState.value.step)
            model.reviewStep(4)
            model.update(complete.copy(meals = emptyList()))
            model.next()
            assertEquals(4, model.uiState.value.invalidStep)
            assertEquals(0, repo.writes)
            model.update(complete)
            model.next()
            assertEquals(6, model.uiState.value.step)
        }

    @Test fun androidSaveAndSkipPersistForNextClientAndPreventDuplicateSubmission() =
        runTest {
            val model = vm()
            runCurrent()
            model.update(complete)
            repo.hold = CompletableDeferred()
            model.save()
            model.save()
            model.skip()
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
            skipped.skip()
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
            model.refresh()
            runCurrent()
            assertFalse(model.uiState.value.showJourney)
            assertEquals(0, repo.writes)
        }

    @Test fun failedWritesKeepAnswersAndLostSuccessfulResponsesReconcile() =
        runTest {
            val model = vm()
            runCurrent()
            model.update(complete)
            repo.writeFailure = Failure.Connection
            model.save()
            runCurrent()
            assertTrue(model.uiState.value.showJourney)
            assertEquals(complete, model.uiState.value.draft)
            assertEquals(Failure.Connection, model.uiState.value.error)
            assertFalse(model.uiState.value.saving)
            repo.commitBeforeFailure = true
            model.save()
            runCurrent()
            assertFalse(model.uiState.value.showJourney)
            assertEquals(MealOnboardingStatus.Completed, repo.record.status)
        }

    @Test fun returningFromWebRefreshesStatusAndAnOpposingDecisionIsAuthoritative() =
        runTest {
            val model = vm()
            runCurrent()
            model.update(complete)
            repo.record = MealOnboarding(MealOnboardingStatus.Skipped)
            model.save()
            runCurrent()
            assertFalse(model.uiState.value.showJourney)
            assertTrue(model.uiState.value.resolvedElsewhere)
            repo.record = MealOnboarding(MealOnboardingStatus.Pending)
            val pending = vm("resume")
            runCurrent()
            repo.record = MealOnboarding(MealOnboardingStatus.Completed, complete)
            pending.refresh()
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
            model.editPreferences()
            model.update(complete)
            model.cancelEditing()
            assertEquals(0, repo.writes)
            assertFalse(model.uiState.value.showJourney)
            model.editPreferences()
            model.update(complete)
            model.save()
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
            model.editPreferences()
            val updated = complete.copy(goal = "food_calm")
            model.update(updated)
            repo.writeFailure = Failure.Timeout
            model.save()
            runCurrent()
            assertTrue(model.uiState.value.editing)
            assertEquals(updated, model.uiState.value.draft)
            repo.commitBeforeFailure = true
            model.save()
            runCurrent()
            assertFalse(model.uiState.value.editing)
        }
}
