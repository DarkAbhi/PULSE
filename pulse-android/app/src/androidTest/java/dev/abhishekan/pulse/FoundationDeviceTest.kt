package dev.abhishekan.pulse

import android.content.Context
import androidx.test.platform.app.InstrumentationRegistry
import dev.abhishekan.pulse.core.data.AndroidSessionKey
import dev.abhishekan.pulse.core.data.EncryptedSessionStore
import dev.abhishekan.pulse.core.domain.*
import dev.abhishekan.pulse.core.network.NetworkConfig
import io.ktor.client.HttpClient
import java.io.File
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.koin.core.context.GlobalContext

class FoundationDeviceTest {
    private val context: Context get() = InstrumentationRegistry.getInstrumentation().targetContext

    @Test fun productionKoinGraphResolvesEveryFoundationRoot() {
        val koin = GlobalContext.get()
        assertSame(koin.get<HttpClient>(), koin.get<HttpClient>())
        assertNotNull(koin.get<PreferencesRepository>())
        assertNotNull(koin.get<AndroidSessionKey>())
        assertNotNull(koin.get<EncryptedSessionStore>())
        assertNotNull(koin.get<ObserveTheme>())
        assertNotNull(koin.get<SetTheme>())
        assertNotNull(koin.get<AppearanceViewModel>())
        assertTrue(koin.get<NetworkConfig>().debug)
    }

    @Test fun keystoreSessionSurvivesStoreRecreationAndRejectsTampering() = runBlocking {
        val store = EncryptedSessionStore(context, AndroidSessionKey())
        val secret = "instrumentation-only-session".toByteArray()
        try {
            store.clear()
            assertNull(store.read())
            store.write(secret)
            val file = File(context.noBackupFilesDir, "session.enc")
            assertTrue(file.exists())
            assertFalse(String(file.readBytes()).contains(String(secret)))
            val recreated = EncryptedSessionStore(context, AndroidSessionKey())
            assertArrayEquals(secret, recreated.read())
            val bytes = file.readBytes()
            bytes[bytes.lastIndex] = (bytes.last().toInt() xor 1).toByte()
            file.writeBytes(bytes)
            assertThrows(Exception::class.java) { runBlocking { recreated.read() } }
            assertArrayEquals(bytes, file.readBytes())
        } finally { store.clear() }
        assertNull(store.read())
    }

    @Test fun preferencesPersistExplicitChoice() = runBlocking {
        val repository = GlobalContext.get().get<PreferencesRepository>()
        val initial = repository.theme.first()
        try {
            repository.setTheme(ThemePreference.Dark)
            assertEquals(ThemePreference.Dark, GlobalContext.get().get<ObserveTheme>()().first())
        } finally { repository.setTheme(initial) }
    }
}
