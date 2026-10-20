package dev.abhishekan.pulse.core.ui.components

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import dev.abhishekan.pulse.core.ui.PulseTheme
import dev.abhishekan.pulse.core.ui.UiText
import dev.abhishekan.pulse.core.ui.asString

enum class PulseTextStyle {
    Body,
    Label,
    Title,
    Display,
    Metric,
    DisplayXL,
    DisplayLarge,
    Heading1,
    Heading2,
    Heading3,
    Title2,
    Title3,
    Body2,
    Body3,
    Label2,
    Caption,
    Overline,
    Editorial,
}

@Composable
fun PulseText(
    text: String,
    modifier: Modifier = Modifier,
    style: PulseTextStyle = PulseTextStyle.Body,
    color: Color = Color.Unspecified,
) {
    val typography = PulseTheme.typography
    Text(
        text,
        modifier,
        color = color,
        style =
            when (style) {
                PulseTextStyle.Body -> typography.body1
                PulseTextStyle.Label -> typography.label1
                PulseTextStyle.Title -> typography.title1
                PulseTextStyle.Display -> typography.heading1
                PulseTextStyle.Metric -> typography.displayMedium
                PulseTextStyle.DisplayXL -> typography.displayXL
                PulseTextStyle.DisplayLarge -> typography.displayLarge
                PulseTextStyle.Heading1 -> typography.heading1
                PulseTextStyle.Heading2 -> typography.heading2
                PulseTextStyle.Heading3 -> typography.heading3
                PulseTextStyle.Title2 -> typography.title2
                PulseTextStyle.Title3 -> typography.title3
                PulseTextStyle.Body2 -> typography.body2
                PulseTextStyle.Body3 -> typography.body3
                PulseTextStyle.Label2 -> typography.label2
                PulseTextStyle.Caption -> typography.caption
                PulseTextStyle.Overline -> typography.overline
                PulseTextStyle.Editorial -> typography.editorial
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
