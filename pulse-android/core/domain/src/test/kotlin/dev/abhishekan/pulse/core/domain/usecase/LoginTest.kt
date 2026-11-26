package dev.abhishekan.pulse.core.domain.usecase
import dev.abhishekan.pulse.core.domain.model.Failure
import dev.abhishekan.pulse.core.domain.model.Outcome
import dev.abhishekan.pulse.core.domain.model.SessionState
import dev.abhishekan.pulse.core.domain.repository.AuthRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class LoginTest {
    private class Repository : AuthRepository {
        override val session = MutableStateFlow<SessionState>(SessionState.SignedOut())
        val credentials = mutableListOf<Pair<String, String>>()
        var result: Outcome<Unit> = Outcome.Success(Unit)
        var cancel = false

        override suspend fun login(
            username: String,
            password: String,
        ): Outcome<Unit> {
            credentials += username to password
            if (cancel) throw CancellationException("Cancelled")
            return result
        }

        override suspend fun restore() = Unit

        override suspend fun logout() = result

        override suspend fun forget() = result

        override suspend fun checkExpiry() = Unit
    }

    @Test fun rejectsMissingInputBeforeCallingRepository() =
        runTest {
            val repository = Repository()
            val login = Login(repository)
            assertEquals(Outcome.Error(Failure.InvalidInput), login(" \t", "secret"))
            assertEquals(Outcome.Error(Failure.InvalidInput), login("user", ""))
            assertTrue(repository.credentials.isEmpty())
        }

    @Test fun normalizesOnlyUsernameAndPreservesDomainFailure() =
        runTest {
            val repository = Repository().apply { result = Outcome.Error(Failure.InvalidCredentials) }
            assertEquals(repository.result, Login(repository)(" user \n", "  secret \t"))
            assertEquals(listOf("user" to "  secret \t"), repository.credentials)
        }

    @Test fun propagatesCancellation() =
        runTest {
            val login = Login(Repository().apply { cancel = true })
            try {
                login("user", "secret")
                fail("Cancellation must propagate")
            } catch (_: CancellationException) {
            }
        }
}
