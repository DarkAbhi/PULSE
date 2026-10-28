package dev.abhishekan.pulse.feature.home

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.MotionDurationScale
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import dev.abhishekan.pulse.core.domain.model.UserTimeZone
import dev.abhishekan.pulse.core.ui.PulseTheme
import dev.abhishekan.pulse.core.ui.PulseTokens
import dev.abhishekan.pulse.core.ui.components.PulseIconButton
import dev.abhishekan.pulse.core.ui.components.PulseText
import dev.abhishekan.pulse.core.ui.components.PulseTextStyle
import kotlinx.coroutines.delay
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.PI
import kotlin.math.cos
import dev.abhishekan.pulse.core.ui.R as UiR

@Composable
internal fun DashboardGreeting(
    name: String,
    compact: Boolean,
    onNotifications: () -> Unit,
    onProfile: (() -> Unit)?,
) {
    val darkTheme = PulseTheme.isDark
    val greetingInk = if (darkTheme) Color.White else Color(0xFF191633)
    val greetingMuted = if (darkTheme) Color.White.copy(alpha = 0.85f) else Color(0xFF605C78)
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val now by produceState(Instant.now(), lifecycle) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            while (true) {
                value = Instant.now()
                delay(60_000 - value.toEpochMilli() % 60_000)
            }
        }
    }
    Box(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(32.dp))
            .testTag("dashboard-greeting"),
    ) {
        GreetingAurora(Modifier.matchParentSize(), compact, darkTheme)
        Column(
            Modifier
                .fillMaxWidth()
                .heightIn(min = if (compact) 360.dp else 400.dp)
                .padding(if (compact) PulseTokens.cardPadding else PulseTokens.large),
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    PulseText(formatDashboardTime(now), style = PulseTextStyle.Heading2, color = greetingInk)
                    PulseText(formatDashboardDate(now), color = greetingMuted)
                }
                PulseIconButton(stringResource(R.string.pulse_home_notifications), onNotifications) {
                    Icon(painterResource(UiR.drawable.pulse_bell), null, tint = greetingInk)
                }
                if (onProfile != null) {
                    PulseIconButton(stringResource(R.string.pulse_home_profile), onProfile) {
                        Icon(painterResource(UiR.drawable.pulse_person), null, tint = greetingInk)
                    }
                }
            }
            Spacer(Modifier.weight(1f).heightIn(min = 48.dp))
            Column(verticalArrangement = Arrangement.spacedBy(PulseTokens.small)) {
                PulseText(stringResource(R.string.pulse_home_good_to_see_you), style = PulseTextStyle.Title2, color = greetingMuted)
                Text(
                    name,
                    Modifier.fillMaxWidth().semantics { heading() },
                    color = greetingInk,
                    style =
                        PulseTheme.typography.heading1.copy(
                            fontSize = if (compact) 48.sp else 64.sp,
                            lineHeight = if (compact) 52.sp else 68.sp,
                        ),
                )
                Spacer(Modifier.height(4.dp))
                PulseText(
                    stringResource(R.string.pulse_home_a_little_perspective_on_your_everyday),
                    Modifier.widthIn(max = 360.dp),
                    color = greetingInk,
                )
            }
        }
    }
}

@Composable
private fun GreetingAurora(
    modifier: Modifier,
    compact: Boolean,
    darkTheme: Boolean,
) {
    val rippleColor = if (darkTheme) Color.White.copy(alpha = 0.55f) else Color(0xFF4C6BFF).copy(alpha = 0.3f)
    val largeText = LocalDensity.current.fontScale > 1.3f
    val motionScale = rememberCoroutineScope().coroutineContext[MotionDurationScale]
    val transition = rememberInfiniteTransition(label = "Greeting pulse")
    val phase =
        transition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(6_000, easing = LinearEasing), RepeatMode.Restart),
            label = "Orb breath and ripples",
        )
    Canvas(modifier) {
        if (darkTheme) {
            drawRect(
                Brush.linearGradient(
                    0f to Color(0xFF2B1B8F),
                    0.55f to Color(0xFF5B2CE0),
                    1f to Color(0xFFB22BD8),
                    start = Offset(size.width * 0.25f, 0f),
                    end = Offset(size.width * 0.75f, size.height),
                ),
            )
        } else {
            drawRect(Color.White)
        }
        val reducedMotion = motionScale?.scaleFactor == 0f
        val t = if (reducedMotion) 0f else phase.value
        val center =
            if (largeText) {
                Offset(size.width * 0.98f, 0f)
            } else {
                Offset(size.width * if (compact) 0.92f else 0.84f, size.height * if (compact) 0.25f else 0.46f)
            }
        val baseRadius =
            when {
                largeText -> minOf(size.width * 0.3f, 96.dp.toPx())
                compact -> minOf(size.width * 0.42f, size.height * 0.3f)
                else -> maxOf(size.width * 0.35f, size.height * 0.72f)
            }
        val breath = if (reducedMotion) 1f else 0.99f - 0.05f * cos(2f * PI.toFloat() * t)
        val radius = baseRadius * breath
        drawCircle(
            Brush.radialGradient(listOf(Color(0x334C6BFF), Color.Transparent), center, baseRadius * 1.5f),
            baseRadius * 1.5f,
            center,
        )
        if (!reducedMotion) {
            repeat(3) { index ->
                val ripple = (t + index / 3f) % 1f
                drawCircle(
                    rippleColor.copy(alpha = rippleColor.alpha * (1f - ripple)),
                    baseRadius * (0.95f + 0.55f * LinearOutSlowInEasing.transform(ripple)),
                    center,
                    style = Stroke(2.dp.toPx()),
                )
            }
        }
        drawCircle(
            Brush.radialGradient(
                0f to Color(0xFF19D3F3),
                0.45f to Color(0xFF4C6BFF),
                0.85f to Color(0xFFFF2FD0),
                center = center - Offset(radius * 0.2f, radius * 0.2f),
                radius = radius * 1.15f,
            ),
            radius,
            center,
        )
        drawCircle(
            Brush.radialGradient(
                listOf(Color.White.copy(alpha = 0.85f), Color.Transparent),
                center - Offset(radius * 0.4f, radius * 0.44f),
                radius * 0.7f,
            ),
            radius,
            center,
        )
        drawCircle(Color.White.copy(alpha = 0.5f), radius, center, style = Stroke(1.dp.toPx()))
        // Reserve quiet space for text without washing out the right-hand orb.
        val textBackdrop = if (darkTheme) Color(0xFF2B1B8F) else Color.White
        drawRect(
            Brush.horizontalGradient(
                0f to textBackdrop,
                (if (compact) 0.35f else 0.3f) to textBackdrop.copy(alpha = 0.96f),
                0.55f to Color.Transparent,
            ),
        )
    }
}

internal fun formatDashboardDate(
    instant: Instant,
    locale: Locale = Locale.getDefault(),
): String = DateTimeFormatter.ofPattern("EEEE, d MMM yyyy", locale).withZone(UserTimeZone.zone).format(instant)

internal fun formatDashboardTime(
    instant: Instant,
    locale: Locale = Locale.getDefault(),
): String = DateTimeFormatter.ofPattern("h:mm a", locale).withZone(UserTimeZone.zone).format(instant)
