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
import dev.abhishekan.pulse.core.domain.usecase.RevokeAPIKey
import dev.abhishekan.pulse.core.domain.usecase.SaveProfileCurrency
import dev.abhishekan.pulse.core.domain.usecase.SaveProfileName
import dev.abhishekan.pulse.core.domain.usecase.SaveProfileTimeZone
import dev.abhishekan.pulse.core.ui.uiText
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
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
        var keyReads = 0
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
            keyReads++
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
        repo,
        SaveProfileName(repo),
        ChangePassword(repo),
        CreateAPIKey(repo),
        RevokeAPIKey(repo),
        saved,
        SaveProfileCurrency(repo),
        SaveProfileTimeZone(repo),
    )

    private fun enterPasswords(
        vm: ProfileViewModel,
        current: String,
        next: String = "",
        confirmation: String = "",
    ) {
        vm.onAction(ProfileAction.ChangePasswordField(PasswordField.Current, current))
        vm.onAction(ProfileAction.ChangePasswordField(PasswordField.Next, next))
        vm.onAction(ProfileAction.ChangePasswordField(PasswordField.Confirmation, confirmation))
    }

    @Test fun navigationRequestDoesNotReopenTheNameEditorAfterRecreation() =
        runTest {
            val repo = Repo()
            val saved = SavedStateHandle()
            val first = vm(repo, saved)
            runCurrent()
            first.onAction(ProfileAction.OpenInitialNameEditor)
            assertEquals(AccountEditor.Name, first.uiState.value.editor)
            first.onAction(ProfileAction.CloseEditor)
            val recreated = vm(repo, saved)
            runCurrent()
            recreated.onAction(ProfileAction.OpenInitialNameEditor)
            assertNull(recreated.uiState.value.editor)
        }

    @Test fun currencySavePreservesProfileAndFailureKeepsPreference() =
        runTest {
            val repo = Repo()
            val vm = vm(repo)
            runCurrent()
            vm.onAction(ProfileAction.SaveCurrency("INR"))
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
            vm.onAction(ProfileAction.SaveCurrency("ZZZ"))
            runCurrent()
            assertEquals(writes, repo.writes)
            assertEquals(
                "INR",
                vm.uiState.value.profile
                    ?.currency,
            )
            repo.failure = Failure.InvalidInput
            vm.onAction(ProfileAction.SaveCurrency("USD"))
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
            vm.onAction(ProfileAction.SaveTimeZone("America/New_York"))
            runCurrent()
            assertEquals(
                "America/New_York",
                vm.uiState.value.profile
                    ?.timezone,
            )
            repo.failure = Failure.InvalidInput
            vm.onAction(ProfileAction.SaveTimeZone("Europe/London"))
            runCurrent()
            assertEquals(
                "America/New_York",
                vm.uiState.value.profile
                    ?.timezone,
            )
            assertNotNull(vm.uiState.value.timeZoneError)
            assertNull(vm.uiState.value.currencyError)
        }

    @Test fun uncertainCurrencyRequiresAFreshProfileEvenWhileKeysAreStillLoading() =
        runTest {
            val keysGate = CompletableDeferred<Unit>()
            val repo = Repo().apply { keysReadGate = keysGate }
            val vm = vm(repo)
            runCurrent()
            repo.failure = Failure.Timeout
            repo.loadFailure = Failure.Connection
            vm.onAction(ProfileAction.SaveCurrency("INR"))
            runCurrent()

            assertEquals(ProfileMutation.Currency, vm.uiState.value.review)
            assertFalse(vm.uiState.value.reviewed)
            assertTrue(vm.uiState.value.keysLoading)
            assertNotNull(vm.uiState.value.currencyError)
            assertNull(vm.uiState.value.timeZoneError)
            vm.onAction(ProfileAction.AcknowledgeReview)
            assertTrue(vm.uiState.value.uncertain)

            repo.loadFailure = null
            vm.onAction(ProfileAction.RefreshReview)
            runCurrent()
            assertTrue(vm.uiState.value.reviewed)
            assertEquals(1, repo.keyReads)
            vm.onAction(ProfileAction.AcknowledgeReview)
            assertFalse(vm.uiState.value.uncertain)
            keysGate.complete(Unit)
            runCurrent()
        }

    @Test fun pendingCurrencyRestoresUsingTheExistingSavedStateValue() =
        runTest {
            val keysGate = CompletableDeferred<Unit>()
            val repo = Repo().apply { keysReadGate = keysGate }
            val saved = SavedStateHandle(mapOf("pending" to "Currency"))
            val vm = vm(repo, saved)
            runCurrent()
            assertEquals(ProfileMutation.Currency, vm.uiState.value.review)
            assertTrue(vm.uiState.value.reviewed)
            assertTrue(vm.uiState.value.keysLoading)
            vm.onAction(ProfileAction.AcknowledgeReview)
            assertNull(saved.get<String>("pending"))
            keysGate.complete(Unit)
            runCurrent()
        }

    @Test fun invalidTimeZoneNeverSendsAWrite() =
        runTest {
            val repo = Repo()
            val vm = vm(repo)
            runCurrent()
            vm.onAction(ProfileAction.SaveTimeZone("Not/AZone"))
            runCurrent()
            assertEquals(0, repo.writes)
        }

    @Test fun repeatedEditorOpenPreservesDraftAndSavedState() =
        runTest {
            val saved = SavedStateHandle()
            val vm = vm(Repo(), saved)
            runCurrent()
            vm.onAction(ProfileAction.Edit(AccountEditor.Name))
            vm.onAction(ProfileAction.ChangeName(" 😀 Draft "))
            vm.onAction(ProfileAction.Edit(AccountEditor.Name))
            assertEquals(" 😀 Draft ", vm.uiState.value.name)
            assertEquals(" 😀 Draft ", saved.get<String>("name"))
            vm.onAction(ProfileAction.CloseEditor)
            vm.onAction(ProfileAction.Edit(AccountEditor.Password))
            enterPasswords(vm, "current", "next password", "next password")
            vm.onAction(ProfileAction.Edit(AccountEditor.Password))
            assertEquals("next password", vm.uiState.value.passwords.next)
        }

    @Test fun nameValidationAndSavePreserveUnicodeAndTrimOnlyEdges() =
        runTest {
            val repo = Repo()
            val vm = vm(repo)
            runCurrent()
            vm.onAction(ProfileAction.Edit(AccountEditor.Name))
            vm.onAction(ProfileAction.ChangeName(" "))
            vm.onAction(ProfileAction.SaveEditor)
            runCurrent()
            assertEquals(0, repo.writes)
            vm.onAction(ProfileAction.ChangeName(" 😀 Asha "))
            vm.onAction(ProfileAction.SaveEditor)
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
            vm.onAction(ProfileAction.RefreshKeys)
            runCurrent()
            assertEquals(1, vm.uiState.value.keys.size)
        }

    @Test fun incorrectPasswordPreservesInputButSuccessAndDismissalClearIt() =
        runTest {
            val repo = Repo().apply { failure = Failure.InvalidCredentials }
            val vm = vm(repo)
            runCurrent()
            vm.onAction(ProfileAction.Edit(AccountEditor.Password))
            enterPasswords(vm, " old ", " new password ", " new password ")
            vm.onAction(ProfileAction.SaveEditor)
            runCurrent()
            assertEquals(" old | new password ", repo.received)
            assertNotNull(vm.uiState.value.error)
            repo.failure = null
            vm.onAction(ProfileAction.SaveEditor)
            runCurrent()
            assertEquals("", vm.uiState.value.passwords.current)
            assertNull(vm.uiState.value.editor)
            vm.onAction(ProfileAction.Edit(AccountEditor.Password))
            enterPasswords(vm, "secret")
            vm.onAction(ProfileAction.CloseEditor)
            assertEquals("", vm.uiState.value.passwords.current)
        }

    @Test fun duplicateSubmissionsAreBlockedAndSecretsAreNeverSaved() =
        runTest {
            val repo = Repo().apply { gate = CompletableDeferred() }
            val saved = SavedStateHandle()
            val vm = vm(repo, saved)
            runCurrent()
            vm.onAction(ProfileAction.Edit(AccountEditor.Password))
            enterPasswords(vm, "current-secret", "next-secret", "next-secret")
            vm.onAction(ProfileAction.SaveEditor)
            vm.onAction(ProfileAction.SaveEditor)
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
            vm.onAction(ProfileAction.Edit(AccountEditor.Key))
            vm.onAction(ProfileAction.ChangeKeyName(" Personal "))
            vm.onAction(ProfileAction.SaveEditor)
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
            vm.onAction(ProfileAction.CloseToken)
            assertNull(vm.uiState.value.token)
        }

    @Test fun closedFlowCannotReceiveATokenFromLateCreationResponse() =
        runTest {
            val repo = Repo().apply { gate = CompletableDeferred() }
            val vm = vm(repo)
            runCurrent()
            vm.onAction(ProfileAction.Edit(AccountEditor.Key))
            vm.onAction(ProfileAction.ChangeKeyName("Late"))
            vm.onAction(ProfileAction.SaveEditor)
            runCurrent()
            vm.onAction(ProfileAction.ClearSecrets)
            repo.gate!!.complete(Unit)
            runCurrent()
            assertNull(vm.uiState.value.token)
            assertEquals(ProfileMutation.Key, vm.uiState.value.review)
        }

    @Test fun leavingAnOpenTokenRequiresKeyReviewOnReturn() =
        runTest {
            val saved = SavedStateHandle()
            val vm = vm(Repo(), saved)
            runCurrent()
            vm.onAction(ProfileAction.Edit(AccountEditor.Key))
            vm.onAction(ProfileAction.ChangeKeyName("Personal"))
            vm.onAction(ProfileAction.SaveEditor)
            runCurrent()

            vm.onAction(ProfileAction.ClearSecrets)

            assertNull(vm.uiState.value.token)
            assertEquals(ProfileMutation.Key, vm.uiState.value.review)
            assertTrue(saved.get<Boolean>("tokenNeedsReview") == true)
            vm.onAction(ProfileAction.AcknowledgeReview)
            assertNull(saved.get<Boolean>("tokenNeedsReview"))
        }

    @Test fun lostCreationResponseRefreshesAndRequiresReviewBeforeAnotherMutation() =
        runTest {
            val repo = Repo().apply { failure = Failure.Timeout }
            val vm = vm(repo)
            runCurrent()
            vm.onAction(ProfileAction.Edit(AccountEditor.Key))
            vm.onAction(ProfileAction.ChangeKeyName("Lost"))
            vm.onAction(ProfileAction.SaveEditor)
            runCurrent()
            assertTrue(vm.uiState.value.uncertain)
            assertEquals(1, vm.uiState.value.keys.size)
            vm.onAction(ProfileAction.SaveEditor)
            runCurrent()
            assertEquals(1, repo.writes)
            vm.onAction(ProfileAction.AcknowledgeReview)
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
            vm.onAction(ProfileAction.Edit(AccountEditor.Name))
            vm.onAction(ProfileAction.ChangeName("Updated"))
            vm.onAction(ProfileAction.SaveEditor)
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
            vm.onAction(ProfileAction.Edit(AccountEditor.Key))
            vm.onAction(ProfileAction.ChangeKeyName("New"))
            vm.onAction(ProfileAction.SaveEditor)
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

    @Test fun repeatedIdenticalSuccessMessagesAreDeliveredAsSeparateEvents() =
        runTest {
            val vm = vm(Repo())
            runCurrent()
            vm.onAction(ProfileAction.SaveCurrency("INR"))
            runCurrent()
            vm.onAction(ProfileAction.SaveTimeZone("Europe/London"))
            runCurrent()

            val expected = ProfileEvent(uiText(R.string.pulse_profile_saved))
            assertEquals(expected, vm.events.first())
            assertEquals(expected, vm.events.first())
        }

    @Test fun copyFailureCannotBeReportedAsSuccess() =
        runTest {
            val vm = vm(Repo())
            runCurrent()
            vm.onAction(ProfileAction.Edit(AccountEditor.Key))
            vm.onAction(ProfileAction.ChangeKeyName("Personal"))
            vm.onAction(ProfileAction.SaveEditor)
            runCurrent()
            vm.onAction(ProfileAction.TokenCopied(false))
            assertEquals(
                uiText(R.string.pulse_profile_copy_failed),
                vm.uiState.value.tokenCopyMessage,
            )
            vm.onAction(ProfileAction.TokenCopied(true))
            assertEquals(
                uiText(R.string.pulse_profile_copied),
                vm.uiState.value.tokenCopyMessage,
            )
        }
}
