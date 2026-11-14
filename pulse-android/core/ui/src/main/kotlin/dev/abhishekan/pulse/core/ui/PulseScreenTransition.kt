package dev.abhishekan.pulse.core.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.PathEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.hideFromAccessibility
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection

// Android 14's Activity open/close translation curve.
private val activitySlide =
    tween<IntOffset>(
        durationMillis = 450,
        easing =
            PathEasing(
                Path().apply {
                    cubicTo(0.05f, 0f, 0.133333f, 0.06f, 0.166666f, 0.4f)
                    cubicTo(0.208333f, 0.82f, 0.25f, 1f, 1f, 1f)
                },
            ),
    )

class PulseActivityTransitions(
    layoutDirection: LayoutDirection,
) {
    private val direction = if (layoutDirection == LayoutDirection.Ltr) 1 else -1

    val forwardEnter =
        slideInHorizontally(activitySlide) { direction * it / 10 } +
            fadeIn(tween(durationMillis = 83, delayMillis = 50, easing = LinearEasing))
    val forwardExit = slideOutHorizontally(activitySlide) { -direction * it / 10 }
    val backEnter = slideInHorizontally(activitySlide) { -direction * it / 10 }
    val backExit =
        slideOutHorizontally(activitySlide) { direction * it / 10 } +
            fadeOut(tween(durationMillis = 83, delayMillis = 35, easing = LinearEasing))
}

@Composable
fun <T> PulseScreenTransition(
    state: T,
    screenDepth: (T) -> Int,
    content: @Composable (state: T, active: Boolean) -> Unit,
) {
    val layoutDirection = LocalLayoutDirection.current
    val transitions = remember(layoutDirection) { PulseActivityTransitions(layoutDirection) }
    val depth = screenDepth(state)
    AnimatedContent(
        targetState = state,
        contentKey = screenDepth,
        modifier = Modifier.fillMaxSize().clipToBounds(),
        transitionSpec = {
            val targetDepth = screenDepth(targetState)
            val transform =
                if (targetDepth < screenDepth(initialState)) {
                    transitions.backEnter togetherWith transitions.backExit
                } else {
                    transitions.forwardEnter togetherWith transitions.forwardExit
                }
            transform.using(null).apply { targetContentZIndex = targetDepth.toFloat() }
        },
        label = "Screen transition",
    ) { screen ->
        val active = screenDepth(screen) == depth
        val outgoing =
            if (active) {
                Modifier
            } else {
                Modifier.clearAndSetSemantics { hideFromAccessibility() }.pointerInput(Unit) {
                    awaitPointerEventScope {
                        while (true) {
                            awaitPointerEvent(PointerEventPass.Initial).changes.forEach { it.consume() }
                        }
                    }
                }
            }
        Box(Modifier.fillMaxSize().then(outgoing)) {
            content(screen, active)
        }
    }
}
