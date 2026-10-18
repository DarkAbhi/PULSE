package dev.abhishekan.pulse.core.ui.gallery

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.tooling.preview.Preview
import dev.abhishekan.pulse.core.ui.PulseTheme
import dev.abhishekan.pulse.core.ui.components.PulseButton
import dev.abhishekan.pulse.core.ui.components.PulseButtonVariant
import dev.abhishekan.pulse.core.ui.components.PulseCard
import dev.abhishekan.pulse.core.ui.components.PulseCheckbox
import dev.abhishekan.pulse.core.ui.components.PulseFilterChip
import dev.abhishekan.pulse.core.ui.components.PulseRadioButton
import dev.abhishekan.pulse.core.ui.components.PulseSwitch
import dev.abhishekan.pulse.core.ui.components.PulseText
import dev.abhishekan.pulse.core.ui.components.PulseTextButton
import dev.abhishekan.pulse.core.ui.components.PulseTextField
import dev.abhishekan.pulse.core.ui.components.PulseTextStyle

/** Token specimens are debug fixtures, never production copy or navigation. */
@Composable
internal fun PulseTokenShowcase() {
    val c = PulseTheme.colors
    val spacing = PulseTheme.spacing
    Column(verticalArrangement = Arrangement.spacedBy(spacing.medium)) {
        PulseFoundationBoard()
        PulseText("Foundation / typography", style = PulseTextStyle.Editorial)
        PulseText("DisplayXL · 64 · Bold · Barlow Condensed", style = PulseTextStyle.DisplayXL)
        PulseText("DisplayLarge · 48 · Bold · Barlow Condensed", style = PulseTextStyle.DisplayLarge)
        PulseText("Metric · 40 · Bold · Barlow Condensed", style = PulseTextStyle.Metric)
        PulseText("Heading1 · 32 · Semibold · DM Sans", style = PulseTextStyle.Heading1)
        PulseText("Heading2 · 28 · Semibold · DM Sans", style = PulseTextStyle.Heading2)
        PulseText("Heading3 · 24 · Semibold · DM Sans", style = PulseTextStyle.Heading3)
        PulseText("Title · 22 · Semibold · DM Sans", style = PulseTextStyle.Title)
        PulseText("Title2 · 20 · Medium · DM Sans", style = PulseTextStyle.Title2)
        PulseText("Title3 · 18 · Medium · DM Sans", style = PulseTextStyle.Title3)
        PulseText("Body · 16 · Regular · DM Sans", style = PulseTextStyle.Body)
        PulseText("Body2 · 14 · Regular · DM Sans", style = PulseTextStyle.Body2)
        PulseText("Body3 · 13 · Regular · DM Sans", style = PulseTextStyle.Body3)
        PulseText("Label · 14 · Semibold · DM Sans", style = PulseTextStyle.Label)
        PulseText("Label2 · 12 · Semibold · DM Sans", style = PulseTextStyle.Label2)
        PulseText("Caption · 12 · Regular · DM Sans", style = PulseTextStyle.Caption)
        PulseText("Overline · 11 · Bold · DM Sans", style = PulseTextStyle.Overline)
        PulseText("Editorial · 32 · Regular · Instrument Serif", style = PulseTextStyle.Editorial)
        PulseText("Foundation / palette", style = PulseTextStyle.Heading2)
        val palette =
            listOf(
                "background" to c.background,
                "surface" to c.surface,
                "surfaceMuted" to c.surfaceMuted,
                "surfaceStrong" to c.surfaceStrong,
                "textPrimary" to c.textPrimary,
                "textSecondary" to c.textSecondary,
                "textTertiary" to c.textTertiary,
                "primary" to c.primary,
                "primaryPressed" to c.primaryPressed,
                "onPrimary" to c.onPrimary,
                "primarySoft" to c.primarySoft,
                "onPrimarySoft" to c.onPrimarySoft,
                "action" to c.action,
                "border" to c.border,
                "outline" to c.outline,
                "divider" to c.divider,
                "success" to c.success,
                "successSoft" to c.successSoft,
                "warning" to c.warning,
                "warningSoft" to c.warningSoft,
                "error" to c.error,
                "errorSoft" to c.errorSoft,
                "info" to c.info,
                "infoSoft" to c.infoSoft,
                "coral" to c.coral,
                "sage" to c.sage,
                "sky" to c.sky,
                "lavender" to c.lavender,
                "inverseSurface" to c.inverseSurface,
                "inverseText" to c.inverseText,
                "inverseAction" to c.inverseAction,
            )
        palette.forEach { (name, color) ->
            Row(horizontalArrangement = Arrangement.spacedBy(spacing.small)) {
                Box(Modifier.size(spacing.target).background(color, PulseTheme.shapes.small))
                PulseText("$name · #%06X".format(color.toArgb() and 0xFFFFFF), style = PulseTextStyle.Body2)
            }
        }
        PulseText("Actions", style = PulseTextStyle.Heading2)
        PulseButtonVariant.entries.forEach { variant ->
            PulseButton(variant.name, {}, variant = variant)
        }
        PulseButton("Disabled", {}, enabled = false)
        PulseButton("Loading", {}, loading = true)
        PulseTextButton("Text action", {})
        PulseCard(highlighted = true) {
            PulseText("A little room to breathe", style = PulseTextStyle.Editorial)
            PulseText("08:24", style = PulseTextStyle.DisplayLarge)
            PulseText("Dark ink on a warm accent.")
        }
        listOf("Success" to c.success, "Warning" to c.warning, "Error" to c.error, "Information" to c.info).forEach { (label, color) ->
            PulseText("● $label", color = color, style = PulseTextStyle.Label)
        }
        PulseTextField("", {}, "Input")
        PulseFilterChip("Selected", true, {})
        PulseCheckbox(true, {}, "Checkbox")
        PulseRadioButton(true, {}, "Radio button")
        PulseSwitch(true, {}, "Switch")
        Spacer(Modifier.size(spacing.small))
    }
}

@Preview(name = "Foundation · light", widthDp = 390, heightDp = 900)
@Preview(name = "Foundation · large text", widthDp = 390, heightDp = 900, fontScale = 2f)
@Composable
private fun FoundationLightPreview() {
    PulseTheme(false) { FoundationSpecimen() }
}

@Preview(name = "Foundation · dark", widthDp = 390, heightDp = 900)
@Composable
private fun FoundationDarkPreview() {
    PulseTheme(true) { FoundationSpecimen() }
}

@Composable
private fun FoundationSpecimen() {
    Surface(color = PulseTheme.colors.background, contentColor = PulseTheme.colors.textPrimary) {
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(PulseTheme.spacing.medium)) {
            PulseFoundationBoard()
        }
    }
}

@Composable
fun PulseFoundationBoard() {
    Column(verticalArrangement = Arrangement.spacedBy(PulseTheme.spacing.medium)) {
        PulseText("PULSE / FOUNDATION", style = PulseTextStyle.Overline, color = PulseTheme.colors.textSecondary)
        PulseText("Room for everyday life.", style = PulseTextStyle.Editorial)
        PulseText("Warm surfaces. Clear choices. A little personality.", style = PulseTextStyle.Body2)
        PulseCard(Modifier.fillMaxWidth(), highlighted = true) {
            PulseText("YOUR DAILY RHYTHM", style = PulseTextStyle.Label2)
            PulseText("08:24", style = PulseTextStyle.DisplayXL)
            PulseText("Make time for what matters.", style = PulseTextStyle.Body2)
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(PulseTheme.spacing.small)) {
            PulseButton("Continue", {})
            PulseButton("Review", {}, variant = PulseButtonVariant.Secondary)
        }
        PulseTextField("", {}, "A small intention")
        FlowRow(horizontalArrangement = Arrangement.spacedBy(PulseTheme.spacing.small)) {
            PulseFilterChip("Today", true, {})
            PulseFilterChip("This week", false, {})
        }
        PulseText("● Ready when you are", style = PulseTextStyle.Label, color = PulseTheme.colors.success)
    }
}
