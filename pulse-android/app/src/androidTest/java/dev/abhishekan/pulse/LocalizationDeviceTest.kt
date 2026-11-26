package dev.abhishekan.pulse
import android.content.res.Configuration
import android.content.res.Resources
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.platform.app.InstrumentationRegistry
import dev.abhishekan.pulse.core.ui.PulseTheme
import dev.abhishekan.pulse.core.ui.UiText
import dev.abhishekan.pulse.core.ui.components.PulseText
import dev.abhishekan.pulse.core.ui.resolve
import dev.abhishekan.pulse.core.ui.uiPlural
import dev.abhishekan.pulse.core.ui.uiText
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import java.util.Locale
import dev.abhishekan.pulse.test.R as TestR

class LocalizationDeviceTest {
    @get:Rule val compose = createComposeRule()

    private fun resources(locale: Locale): Resources {
        val context = InstrumentationRegistry.getInstrumentation().context
        return context
            .createConfigurationContext(
                Configuration(context.resources.configuration).apply {
                    setLocale(locale)
                },
            ).resources
    }

    @Test fun retainedMessageAndNestedArgumentsFollowCurrentLocale() {
        val retained =
            uiText(
                TestR.string.localization_nested,
                uiText(TestR.string.localization_greeting, "Asha"),
            )
        var current by mutableStateOf(resources(Locale.ENGLISH))
        compose.setContent {
            CompositionLocalProvider(LocalResources provides current) {
                PulseTheme(false) {
                    Column {
                        PulseText(stringResource(TestR.string.localization_greeting, "Mira"))
                        PulseText(pluralStringResource(TestR.plurals.localization_items, 2, 2))
                        PulseText(retained)
                    }
                }
            }
        }
        compose.onNodeWithText("Hello, Mira").assertIsDisplayed()
        compose.onNodeWithText("2 items").assertIsDisplayed()
        compose.onNodeWithText("Notice: Hello, Asha").assertIsDisplayed()
        compose.runOnIdle { current = resources(Locale.FRENCH) }
        compose.onNodeWithText("Bonjour, Mira").assertIsDisplayed()
        compose.onNodeWithText("2 éléments").assertIsDisplayed()
        compose.onNodeWithText("Information : Bonjour, Asha").assertIsDisplayed()
    }

    @Test fun quantitiesAndUserProvidedValuesRemainCorrect() {
        val english = resources(Locale.ENGLISH)
        val french = resources(Locale.FRENCH)
        val singular = uiPlural(TestR.plurals.localization_items, 1, 1)
        val plural = uiPlural(TestR.plurals.localization_items, 2, 2)
        assertEquals("1 item", singular.resolve(english))
        assertEquals("2 items", plural.resolve(english))
        assertEquals("1 élément", singular.resolve(french))
        assertEquals("2 éléments", plural.resolve(french))
        assertEquals("User-provided meal", UiText.Literal("User-provided meal").resolve(french))
    }
}
