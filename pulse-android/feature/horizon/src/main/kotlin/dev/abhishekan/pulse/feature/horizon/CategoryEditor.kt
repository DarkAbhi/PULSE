package dev.abhishekan.pulse.feature.horizon

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import dev.abhishekan.pulse.core.ui.PulseLightColors
import dev.abhishekan.pulse.core.ui.PulseTheme
import dev.abhishekan.pulse.core.ui.PulseWindowSize
import dev.abhishekan.pulse.core.ui.UiText
import dev.abhishekan.pulse.core.ui.components.PulseCard
import dev.abhishekan.pulse.core.ui.components.PulseFilterChip
import dev.abhishekan.pulse.core.ui.components.PulseText
import dev.abhishekan.pulse.core.ui.components.PulseTextField
import dev.abhishekan.pulse.core.ui.components.PulseTextStyle
import dev.abhishekan.pulse.core.ui.pulseWindowSize
import dev.abhishekan.pulse.core.ui.uiText
import android.graphics.Color as GraphicsColor
import dev.abhishekan.pulse.core.ui.R as CoreUiR

private data class CategoryIcon(
    val value: String,
    val label: Int,
    val drawable: Int,
)

private val categoryIcons =
    listOf(
        CategoryIcon("tag", R.string.pulse_horizon_category_icon_tag, R.drawable.pulse_obligation_tag),
        CategoryIcon("home", R.string.pulse_horizon_category_icon_home, R.drawable.pulse_obligation_home),
        CategoryIcon("shopping-cart", R.string.pulse_horizon_category_icon_shopping, R.drawable.pulse_purchase_cart),
        CategoryIcon("wallet", R.string.pulse_horizon_category_icon_wallet, R.drawable.pulse_obligation_wallet),
        CategoryIcon("calendar", R.string.pulse_horizon_category_icon_calendar, R.drawable.pulse_obligation_calendar),
        CategoryIcon("car", R.string.pulse_horizon_category_icon_transport, CoreUiR.drawable.pulse_garage),
    )

@Composable
internal fun CategoryEditor(
    draft: HorizonDraft,
    busy: Boolean,
    error: UiText?,
    canSave: Boolean,
    onChange: (HorizonDraft) -> Unit,
    onSave: () -> Unit,
) {
    val colors = PulseTheme.colors
    val spacing = PulseTheme.spacing
    val nameFocus = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current
    val originalColor = rememberSaveable(draft.id) { draft.color }
    val originalIcon = rememberSaveable(draft.id) { draft.icon }
    val trimmedName = draft.name.trim()
    val nameError = error != null && (trimmedName.isEmpty() || trimmedName.codePointCount(0, trimmedName.length) > 100)
    LaunchedEffect(error) {
        if (nameError) nameFocus.requestFocus()
    }
    BoxWithConstraints(Modifier.fillMaxWidth().testTag("category-editor")) {
        val wide =
            pulseWindowSize(maxWidth / LocalDensity.current.fontScale.coerceAtLeast(1f)) == PulseWindowSize.Expanded
        val details: @Composable () -> Unit = {
            PulseCard(Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(spacing.cardPadding)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(spacing.compact), verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier.size(spacing.target).background(colors.primarySoft, PulseTheme.shapes.medium),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                painterResource(R.drawable.pulse_obligation_tag),
                                null,
                                Modifier.size(spacing.cardPadding),
                                tint = colors.onPrimarySoft,
                            )
                        }
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(spacing.tiny)) {
                            PulseText(
                                stringResource(R.string.pulse_horizon_category_details),
                                Modifier.semantics { heading() },
                                style = PulseTextStyle.Title3,
                            )
                            PulseText(
                                stringResource(R.string.pulse_horizon_category_details_hint),
                                style = PulseTextStyle.Body2,
                                color = colors.textSecondary,
                            )
                        }
                    }
                    PulseTextField(
                        draft.name,
                        { onChange(draft.copy(name = it)) },
                        stringResource(R.string.pulse_horizon_category_name),
                        Modifier.fillMaxWidth().focusRequester(nameFocus).testTag("category-name"),
                        enabled = !busy,
                        labelAbove = true,
                        accessibilityLabel = stringResource(R.string.pulse_horizon_name),
                        error = if (nameError) uiText(R.string.pulse_horizon_category_name_error) else null,
                        placeholder = {
                            PulseText(
                                stringResource(R.string.pulse_horizon_category_name_hint),
                                color = colors.textSecondary,
                            )
                        },
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions =
                            KeyboardActions(onDone = {
                                if (canSave) {
                                    keyboard?.hide()
                                    onSave()
                                }
                            }),
                    )
                    if (error != null && !nameError) {
                        PulseText(error, Modifier.semantics { liveRegion = LiveRegionMode.Polite }, color = colors.error)
                    }
                }
            }
        }
        val appearance: @Composable () -> Unit = {
            CategoryAppearance(draft, busy, originalColor, originalIcon, onChange)
        }
        if (wide) {
            Row(horizontalArrangement = Arrangement.spacedBy(spacing.cardPadding)) {
                Box(Modifier.weight(1f)) { details() }
                Box(Modifier.weight(1f)) { appearance() }
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(spacing.cardPadding)) {
                details()
                appearance()
            }
        }
    }
}

@Composable
private fun CategoryAppearance(
    draft: HorizonDraft,
    busy: Boolean,
    originalColor: String,
    originalIcon: String,
    onChange: (HorizonDraft) -> Unit,
) {
    val colors = PulseTheme.colors
    val spacing = PulseTheme.spacing
    val focus = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    val select: (HorizonDraft) -> Unit = {
        focus.clearFocus()
        keyboard?.hide()
        onChange(it)
    }
    // Store stable light-palette values so changing the app theme never changes category data.
    val palette =
        listOf(
            R.string.pulse_horizon_category_color_gold to PulseLightColors.primary,
            R.string.pulse_horizon_category_color_coral to PulseLightColors.coral,
            R.string.pulse_horizon_category_color_sage to PulseLightColors.sage,
            R.string.pulse_horizon_category_color_sky to PulseLightColors.sky,
            R.string.pulse_horizon_category_color_lavender to PulseLightColors.lavender,
        ).map { (label, color) -> Triple(stringResource(label), color, "#%06x".format(color.toArgb() and 0xFFFFFF)) }
    val choices =
        if (palette.any { it.third.equals(originalColor, ignoreCase = true) }) {
            palette
        } else {
            val current = runCatching { Color(GraphicsColor.parseColor(originalColor)) }.getOrDefault(colors.action)
            listOf(Triple(stringResource(R.string.pulse_horizon_category_color_current), current, originalColor)) + palette
        }
    PulseCard(Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(spacing.medium)) {
            PulseText(
                stringResource(R.string.pulse_horizon_category_appearance),
                Modifier.semantics { heading() },
                style = PulseTextStyle.Title3,
            )
            PulseText(
                stringResource(R.string.pulse_horizon_category_appearance_hint),
                style = PulseTextStyle.Body2,
                color = colors.textSecondary,
            )
            PulseText(stringResource(R.string.pulse_horizon_category_icon), style = PulseTextStyle.Label)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(spacing.small),
                verticalArrangement = Arrangement.spacedBy(spacing.small),
            ) {
                if (categoryIcons.none { it.value == originalIcon }) {
                    PulseFilterChip(originalIcon, draft.icon == originalIcon, { select(draft.copy(icon = originalIcon)) }, enabled = !busy)
                }
                categoryIcons.forEach { option ->
                    PulseFilterChip(
                        stringResource(option.label),
                        draft.icon == option.value,
                        { select(draft.copy(icon = option.value)) },
                        Modifier.testTag("category-icon-${option.value}"),
                        enabled = !busy,
                        leadingIcon = { Icon(painterResource(option.drawable), null, Modifier.size(spacing.comfortable)) },
                    )
                }
            }
            PulseText(stringResource(R.string.pulse_horizon_category_color), style = PulseTextStyle.Label)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(spacing.small),
                verticalArrangement = Arrangement.spacedBy(spacing.small),
            ) {
                choices.forEach { (label, color, value) ->
                    PulseFilterChip(
                        label,
                        draft.color.equals(value, ignoreCase = true),
                        { select(draft.copy(color = value)) },
                        Modifier.testTag("category-color-$value"),
                        enabled = !busy,
                        leadingIcon = { Box(Modifier.size(spacing.medium).background(color, PulseTheme.shapes.pill)) },
                    )
                }
            }
        }
    }
}
