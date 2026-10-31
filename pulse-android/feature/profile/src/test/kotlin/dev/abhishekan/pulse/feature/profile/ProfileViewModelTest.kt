package dev.abhishekan.pulse.feature.profile
import androidx.lifecycle.SavedStateHandle
import dev.abhishekan.pulse.core.domain.model.APIKey
import dev.abhishekan.pulse.core.domain.model.CreatedAPIKey
import dev.abhishekan.pulse.core.domain.model.Failure
import dev.abhishekan.pulse.core.domain.model.Outcome
import dev.abhishekan.pulse.core.domain.model.Profile
import dev.abhishekan.pulse.core.domain.repository.ProfileRepository
import dev.abhishekan.pulse.core.domain.usecase.ChangePassword
import dev.abhishekan.pulse.core.domain.usecase.CreateAPIKey
import dev.abhishekan.pulse.core.domain.usecase.GetAPIKeys
import dev.abhishekan.pulse.core.domain.usecase.GetAccountProfile
import dev.abhishekan.pulse.core.domain.usecase.RevokeAPIKey
import dev.abhishekan.pulse.core.domain.usecase.SaveProfileCurrency
import dev.abhishekan.pulse.core.domain.usecase.SaveProfileName
import dev.abhishekan.pulse.core.domain.usecase.SaveProfileTimeZone
import dev.abhishekan.pulse.core.ui.uiText
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
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class ProfileViewModelTest {
    @Before fun before() {
        Dispatchers.setMain(StandardTestDispatcher())
    }

    @After fun after() {
        Dispatchers.resetMain()
    }

    private class Repo : ProfileRepository {
        var value = Profile("Asha")
        var loadFailure: Failure? = null
        var profileReadGate: CompletableDeferred<Unit>? = null
        var keysReadGate: CompletableDeferred<Unit>? = null
        var keyValues = listOf(APIKey(1, "Fixture", "lt_aaaa", Instant.EPOCH))
        var writes = 0
        var gate: CompletableDeferred<Unit>? = null
        var failure: Failure? = null
        var received = ""
        val token = "lt_" + "a".repeat(64)

        override suspend fun profile(): Outcome<Profile> {
            val snapshot = value
            profileReadGate?.await()
            return loadFailure?.let { Outcome.Error(it) } ?: Outcome.Success(snapshot)
        }

        override suspend fun saveCurrency(currency: String): Outcome<String> {
            writes++
            failure?.let { return Outcome.Error(it) }
            value = value.copy(currency = currency)
            return Outcome.Success(currency)
        }

        override suspend fun saveTimeZone(timezone: String): Outcome<String> {
            writes++
            failure?.let { return Outcome.Error(it) }
            value = value.copy(timezone = timezone)
            return Outcome.Success(timezone)
        }

        override suspend fun saveName(name: String): Outcome<Profile> {
            writes++
            received = name
            value = Profile(name)
            return failure?.let { Outcome.Error(it) }
                ?: Outcome.Success(value)
        }

        override suspend fun changePassword(
            current: String,
            next: String,
        ): Outcome<Unit> {
            writes++
            received = current + "|" + next
            gate?.await()
            return failure?.let { Outcome.Error(it) }
                ?: Outcome.Success(Unit)
        }

        override suspend fun keys(): Outcome<List<APIKey>> {
            val snapshot = keyValues
            keysReadGate?.await()
            return Outcome.Success(snapshot)
        }

        override suspend fun createKey(name: String): Outcome<CreatedAPIKey> {
            writes++
            gate?.await()
            failure?.let { return Outcome.Error(it) }
            val key = APIKey(2, name, "lt_aaaa", Instant.EPOCH)
            keyValues = keyValues + key
            return Outcome.Success(CreatedAPIKey(key, token))
        }

        override suspend fun revokeKey(id: Long) = Outcome.Success(Unit)
    }

    private fun vm(
        repo: Repo,
        saved: SavedStateHandle = SavedStateHandle(),
    ) = ProfileViewModel(
        GetAccountProfile(repo),
        SaveProfileName(repo),
        ChangePassword(repo),
        GetAPIKeys(repo),
        CreateAPIKey(repo),
        RevokeAPIKey(repo),
        saved,
        SaveProfileCurrency(repo),
        SaveProfileTimeZone(repo),
    )

    @Test fun currencySavePreservesProfileAndFailureKeepsPreference() =
        runTest {
            val repo = Repo()
            val vm = vm(repo)
            runCurrent()
            vm.currency("INR")
            runCurrent()
            assertEquals(
                "INR",
                vm.uiState.value.profile
                    ?.currency,
            )
            assertEquals(
                repo.value.name,
                vm.uiState.value.profile
                    ?.name,
            )
            val writes = repo.writes
            vm.currency("ZZZ")
            runCurrent()
            assertEquals(writes, repo.writes)
            assertEquals(
                "INR",
                vm.uiState.value.profile
                    ?.currency,
            )
            repo.failure = Failure.InvalidInput
            vm.currency("USD")
            runCurrent()
            assertEquals(
                "INR",
                vm.uiState.value.profile
                    ?.currency,
            )
        }

    @Test fun timeZoneSaveUpdatesProfileAndFailureKeepsPreviousPreference() =
        runTest {
            val repo = Repo()
            val vm = vm(repo)
            runCurrent()
            vm.timeZone("America/New_York")
            runCurrent()
            assertEquals(
                "America/New_York",
                vm.uiState.value.profile
                    ?.timezone,
            )
            repo.failure = Failure.InvalidInput
            vm.timeZone("Europe/London")
            runCurrent()
            assertEquals(
                "America/New_York",
                vm.uiState.value.profile
                    ?.timezone,
            )
            assertNotNull(vm.uiState.value.error)
        }

    @Test fun invalidTimeZoneNeverSendsAWrite() =
        runTest {
            val repo = Repo()
            val vm = vm(repo)
            runCurrent()
            vm.timeZone("Not/AZone")
            runCurrent()
            assertEquals(0, repo.writes)
        }

    @Test fun repeatedEditorOpenPreservesDraftAndSavedState() =
        runTest {
            val saved = SavedStateHandle()
            val vm = vm(Repo(), saved)
            runCurrent()
            vm.edit(AccountEditor.Name)
            vm.name(" 😀 Draft ")
            vm.edit(AccountEditor.Name)
            assertEquals(" 😀 Draft ", vm.uiState.value.name)
            assertEquals(" 😀 Draft ", saved.get<String>("name"))
            vm.closeEditor()
            vm.edit(AccountEditor.Password)
            vm.passwords(PasswordFields("current", "next password", "next password"))
            vm.edit(AccountEditor.Password)
            assertEquals("next password", vm.uiState.value.passwords.next)
        }

    @Test fun nameValidationAndSavePreserveUnicodeAndTrimOnlyEdges() =
        runTest {
            val repo = Repo()
            val vm = vm(repo)
            runCurrent()
            vm.edit(AccountEditor.Name)
            vm.name(" ")
            vm.save()
            runCurrent()
            assertEquals(0, repo.writes)
            vm.name(" 😀 Asha ")
            vm.save()
            runCurrent()
            assertEquals("😀 Asha", repo.received)
            assertEquals(
                "😀 Asha",
                vm.uiState.value.profile
                    ?.name,
            )
        }

    @Test fun failedProfileFetchIsNeverMissingProfileAndOtherSectionsStillLoad() =
        runTest {
            val repo = Repo().apply { loadFailure = Failure.Connection }
            val vm = vm(repo)
            runCurrent()
            assertNull(vm.uiState.value.profile)
            assertEquals(Failure.Connection, vm.uiState.value.profileError)
            vm.refreshKeys()
            runCurrent()
            assertEquals(1, vm.uiState.value.keys.size)
        }

    @Test fun incorrectPasswordPreservesInputButSuccessAndDismissalClearIt() =
        runTest {
            val repo = Repo().apply { failure = Failure.InvalidCredentials }
            val vm = vm(repo)
            runCurrent()
            vm.edit(AccountEditor.Password)
            vm.passwords(PasswordFields(" old ", " new password ", " new password "))
            vm.save()
            runCurrent()
            assertEquals(" old | new password ", repo.received)
            assertNotNull(vm.uiState.value.error)
            repo.failure = null
            vm.save()
            runCurrent()
            assertEquals("", vm.uiState.value.passwords.current)
            assertNull(vm.uiState.value.editor)
            vm.edit(AccountEditor.Password)
            vm.passwords(PasswordFields("secret"))
            vm.closeEditor()
            assertEquals("", vm.uiState.value.passwords.current)
        }

    @Test fun duplicateSubmissionsAreBlockedAndSecretsAreNeverSaved() =
        runTest {
            val repo = Repo().apply { gate = CompletableDeferred() }
            val saved = SavedStateHandle()
            val vm = vm(repo, saved)
            runCurrent()
            vm.edit(AccountEditor.Password)
            vm.passwords(PasswordFields("current-secret", "next-secret", "next-secret"))
            vm.save()
            vm.save()
            runCurrent()
            assertEquals(1, repo.writes)
            assertFalse(saved.keys().any { saved.get<Any?>(it).toString().contains("secret") })
            repo.gate!!.complete(Unit)
            runCurrent()
            assertEquals("", vm.uiState.value.passwords.next)
        }

    @Test fun oneTimeTokenSurvivesStateReadsAndIsClearedOnFlowClose() =
        runTest {
            val repo = Repo()
            val saved = SavedStateHandle()
            val vm = vm(repo, saved)
            runCurrent()
            vm.edit(AccountEditor.Key)
            vm.keyName(" Personal ")
            vm.save()
            runCurrent()
            assertEquals(
                repo.token,
                vm.uiState.value.token
                    ?.token,
            )
            assertFalse(
                vm.uiState.value
                    .toString()
                    .contains(repo.token),
            )
            assertFalse(saved.keys().any { saved.get<Any?>(it).toString().contains(repo.token) })
            vm.clearToken()
            assertNull(vm.uiState.value.token)
        }

    @Test fun closedFlowCannotReceiveATokenFromLateCreationResponse() =
        runTest {
            val repo = Repo().apply { gate = CompletableDeferred() }
            val vm = vm(repo)
            runCurrent()
            vm.edit(AccountEditor.Key)
            vm.keyName("Late")
            vm.save()
            runCurrent()
            vm.clearSecrets()
            repo.gate!!.complete(Unit)
            runCurrent()
            assertNull(vm.uiState.value.token)
            assertNotNull(vm.uiState.value.message)
        }

    @Test fun lostCreationResponseRefreshesAndRequiresReviewBeforeAnotherMutation() =
        runTest {
            val repo = Repo().apply { failure = Failure.Timeout }
            val vm = vm(repo)
            runCurrent()
            vm.edit(AccountEditor.Key)
            vm.keyName("Lost")
            vm.save()
            runCurrent()
            assertTrue(vm.uiState.value.uncertain)
            assertEquals(1, vm.uiState.value.keys.size)
            vm.save()
            runCurrent()
            assertEquals(1, repo.writes)
            vm.acknowledge()
            assertFalse(vm.uiState.value.uncertain)
        }

    @Test fun processRecreationCannotRestoreFullTokenOrPasswordsAndRequiresReviewForLostToken() =
        runTest {
            val repo = Repo()
            val saved = SavedStateHandle(mapOf("tokenNeedsReview" to true))
            val vm = vm(repo, saved)
            runCurrent()
            assertNull(vm.uiState.value.token)
            assertEquals("", vm.uiState.value.passwords.current)
            assertTrue(vm.uiState.value.uncertain)
            assertTrue(vm.uiState.value.reviewed)
        }

    @Test fun olderProfileReadCannotOverwriteASavedName() =
        runTest {
            val repo = Repo().apply { profileReadGate = CompletableDeferred() }
            val vm = vm(repo)
            runCurrent()
            vm.edit(AccountEditor.Name)
            vm.name("Updated")
            vm.save()
            runCurrent()
            repo.profileReadGate!!.complete(Unit)
            runCurrent()
            assertEquals(
                "Updated",
                vm.uiState.value.profile
                    ?.name,
            )
            assertFalse(vm.uiState.value.loading)
        }

    @Test fun olderKeyReadCannotHideANewlyCreatedKey() =
        runTest {
            val repo = Repo().apply { keysReadGate = CompletableDeferred() }
            val vm = vm(repo)
            runCurrent()
            vm.edit(AccountEditor.Key)
            vm.keyName("New")
            vm.save()
            runCurrent()
            repo.keysReadGate!!.complete(Unit)
            runCurrent()
            assertEquals(
                listOf("Fixture", "New"),
                vm.uiState.value.keys
                    .map { it.name },
            )
            assertFalse(vm.uiState.value.keysLoading)
        }

    @Test fun copyFailureCannotBeReportedAsSuccess() =
        runTest {
            val vm = vm(Repo())
            runCurrent()
            vm.copied(false)
            assertEquals(
                uiText(R.string.pulse_profile_copy_failed),
                vm.uiState.value.message,
            )
            vm.copied(true)
            assertEquals(
                uiText(R.string.pulse_profile_copied),
                vm.uiState.value.message,
            )
        }
}
