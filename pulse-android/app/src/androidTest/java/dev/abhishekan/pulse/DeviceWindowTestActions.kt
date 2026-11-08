package dev.abhishekan.pulse

import android.accessibilityservice.AccessibilityService
import android.app.Activity
import android.os.Build
import android.view.View
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.view.inspector.WindowInspector
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertTrue

private fun windowRoots(activity: Activity): List<View> =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        WindowInspector.getGlobalWindowViews().filter { it.isAttachedToWindow && it.isShown }
    } else {
        listOf(activity.window.decorView)
    }

internal fun <A : Activity> ComposeTestRule.awaitAppWindowFocus(scenario: ActivityScenario<A>) {
    waitUntil(15_000) {
        var focused = false
        scenario.onActivity { activity ->
            val focusable =
                windowRoots(activity).filter {
                    val flags = (it.layoutParams as? WindowManager.LayoutParams)?.flags ?: 0
                    flags and WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE == 0
                }
            val overlays = focusable.filter { it !== activity.window.decorView }
            focused = overlays.ifEmpty { focusable }.any { it.hasWindowFocus() && !it.isLayoutRequested }
        }
        focused
    }
}

internal fun <A : Activity> ComposeTestRule.hideAppKeyboard(scenario: ActivityScenario<A>) {
    awaitAppWindowFocus(scenario)
    scenario.onActivity { activity ->
        // A dialog's IME belongs to its window token, not the activity's decor view.
        windowRoots(activity).forEach { root ->
            root.context.getSystemService(InputMethodManager::class.java).hideSoftInputFromWindow(root.windowToken, 0)
        }
    }
    waitUntil(15_000) {
        var hidden = false
        scenario.onActivity { activity ->
            hidden =
                windowRoots(activity).none {
                    ViewCompat.getRootWindowInsets(it)?.isVisible(WindowInsetsCompat.Type.ime()) == true
                }
        }
        hidden
    }
}

internal fun <A : Activity> ComposeTestRule.pressSystemBack(scenario: ActivityScenario<A>) {
    hideAppKeyboard(scenario)
    awaitAppWindowFocus(scenario)
    // System Back targets the active window without Espresso choosing an unfocused root.
    assertTrue(
        "Android must accept the system Back action",
        InstrumentationRegistry.getInstrumentation().uiAutomation.performGlobalAction(AccessibilityService.GLOBAL_ACTION_BACK),
    )
    waitForIdle()
}

internal fun <A : Activity> ComposeTestRule.awaitWindowConfiguration(
    scenario: ActivityScenario<A>,
    width: Int,
    height: Int,
    densityDpi: Int,
    fontScale: Float? = null,
) {
    // Compose idleness alone does not wait for Android's display override to arrive.
    waitUntil(15_000) {
        var settled = false
        scenario.onActivity { activity ->
            val configuration = activity.resources.configuration
            val decor = activity.window.decorView
            settled = decor.width == width &&
                decor.height == height &&
                configuration.densityDpi == densityDpi &&
                (fontScale == null || kotlin.math.abs(configuration.fontScale - fontScale) < 0.01f) &&
                !decor.isLayoutRequested
        }
        settled
    }
    awaitAppWindowFocus(scenario)
    waitForIdle()
}
