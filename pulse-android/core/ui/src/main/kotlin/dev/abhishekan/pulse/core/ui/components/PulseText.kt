package dev.abhishekan.pulse.core.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import dev.abhishekan.pulse.core.ui.UiText
import dev.abhishekan.pulse.core.ui.asString

enum class PulseTextStyle { Body, Label, Title, Display, Metric }

@Composable
fun PulseText(
    text: String,
    modifier: Modifier = Modifier,
    style: PulseTextStyle = PulseTextStyle.Body,
    color: Color = Color.Unspecified,
) {
    val typography = MaterialTheme.typography
    Text(
        text,
        modifier,
        color = color,
        style =
            when (style) {
                PulseTextStyle.Body -> typography.bodyLarge
                PulseTextStyle.Label -> typography.labelLarge
                PulseTextStyle.Title -> typography.titleLarge
                PulseTextStyle.Display -> typography.displaySmall
                PulseTextStyle.Metric ->
                    typography.displaySmall.copy(
                        fontFamily = FontFamily.Monospace,
                        fontFeatureSettings = "tnum",
                    )
            },
    )
}

@Composable
fun PulseText(
    text: UiText,
    modifier: Modifier = Modifier,
    style: PulseTextStyle = PulseTextStyle.Body,
    color: Color = Color.Unspecified,
) = PulseText(text.asString(), modifier, style, color)
