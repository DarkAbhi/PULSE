package dev.abhishekan.pulse.feature.horizon.presentation.editor

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import dev.abhishekan.pulse.core.ui.PulseTheme
import dev.abhishekan.pulse.core.ui.PulseTokens
import dev.abhishekan.pulse.core.ui.components.PulseButton
import dev.abhishekan.pulse.core.ui.components.PulseButtonVariant
import dev.abhishekan.pulse.core.ui.components.PulseDropdownMenu
import dev.abhishekan.pulse.core.ui.components.PulseDropdownMenuItem
import dev.abhishekan.pulse.core.ui.components.PulseText
import dev.abhishekan.pulse.core.ui.components.PulseTextField
import dev.abhishekan.pulse.core.ui.components.PulseTextStyle
import dev.abhishekan.pulse.feature.horizon.R
import dev.abhishekan.pulse.feature.horizon.presentation.components.horizonOption

@Composable
internal fun TransactionPickerField(
    label: Int,
    value: String,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val title = stringResource(label)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        PulseText(title, style = PulseTextStyle.Label)
        PulseButton(
            value,
            onClick,
            Modifier.fillMaxWidth().heightIn(min = 56.dp).semantics {
                contentDescription = "$title: $value"
            },
            enabled = enabled,
            variant = PulseButtonVariant.Secondary,
        )
    }
}

@Composable
internal fun HorizonFormSection(
    title: Int,
    hint: Int?,
    icon: Int,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = PulseTheme.colors
    Surface(Modifier.fillMaxWidth(), shape = PulseTokens.cardShape, color = colors.surface, border = BorderStroke(1.dp, colors.border)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Surface(shape = PulseTheme.shapes.medium, color = colors.primarySoft) {
                    Box(Modifier.size(40.dp), contentAlignment = Alignment.Center) {
                        Icon(painterResource(icon), null, Modifier.size(20.dp), tint = colors.onPrimarySoft)
                    }
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    PulseText(stringResource(title), Modifier.semantics { heading() }, style = PulseTextStyle.Title3)
                    hint?.let { PulseText(stringResource(it), style = PulseTextStyle.Body2, color = colors.textSecondary) }
                }
            }
            content()
        }
    }
}

@Composable
internal fun SubscriptionFieldPair(
    wide: Boolean,
    first: @Composable () -> Unit,
    second: @Composable () -> Unit,
) {
    if (wide) {
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Box(Modifier.weight(1f)) { first() }
            Box(Modifier.weight(1f)) { second() }
        }
    } else {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            first()
            second()
        }
    }
}

@Composable
internal fun SubscriptionInput(
    value: String,
    onChange: (String) -> Unit,
    label: String,
    hint: String,
    enabled: Boolean,
    type: KeyboardType = KeyboardType.Text,
    leadingIcon: (@Composable () -> Unit)? = null,
) {
    PulseTextField(
        value,
        onChange,
        label,
        Modifier.fillMaxWidth(),
        enabled,
        labelAbove = true,
        placeholder = { PulseText(hint, color = PulseTheme.colors.textSecondary) },
        keyboardOptions = KeyboardOptions(keyboardType = type, imeAction = ImeAction.Next),
        leadingIcon = leadingIcon,
    )
}

@Composable
internal fun SubscriptionSegments(
    label: String,
    value: String,
    options: List<String>,
    enabled: Boolean,
    onSelect: (String) -> Unit,
) {
    val colors = PulseTheme.colors
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        PulseText(label, style = PulseTextStyle.Label)
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val stacked = maxWidth < (options.size * 80).dp * LocalDensity.current.fontScale.coerceAtLeast(1f)
            val option: @Composable (String, Modifier) -> Unit = { key, modifier ->
                val selected = value == key
                val active = key == "active"
                Surface(
                    modifier.heightIn(min = 48.dp).selectable(selected, enabled, Role.RadioButton) { onSelect(key) },
                    shape = PulseTokens.controlShape,
                    color =
                        if (selected) {
                            if (active) colors.successSoft else colors.primarySoft
                        } else {
                            colors.surface
                        },
                    border =
                        BorderStroke(
                            1.dp,
                            if (selected) {
                                if (active) colors.success else colors.action
                            } else {
                                colors.border
                            },
                        ),
                ) {
                    Box(Modifier.padding(horizontal = 8.dp, vertical = 12.dp), contentAlignment = Alignment.Center) {
                        PulseText(
                            horizonOption(key),
                            color =
                                if (selected &&
                                    active
                                ) {
                                    colors.success
                                } else {
                                    colors.textPrimary
                                },
                            style = PulseTextStyle.Label,
                        )
                    }
                }
            }
            if (stacked) {
                Column(Modifier.selectableGroup(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    options.forEach { option(it, Modifier.fillMaxWidth()) }
                }
            } else {
                Row(Modifier.selectableGroup(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    options.forEach { option(it, Modifier.weight(1f)) }
                }
            }
        }
    }
}

@Composable
internal fun SubscriptionRelation(
    label: Int,
    id: Long?,
    options: List<Pair<Long, String>>,
    enabled: Boolean,
    onSelect: (Long?) -> Unit,
) {
    val none = stringResource(R.string.pulse_horizon_none)
    val title = stringResource(label)
    val selected =
        if (id ==
            null
        ) {
            none
        } else {
            options.find { it.first == id }?.second ?: stringResource(R.string.pulse_horizon_unavailable_selection)
        }
    val colors = PulseTheme.colors
    val focus = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    var expanded by remember { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        PulseText(title, style = PulseTextStyle.Label)
        Box {
            PulseButton(
                text = selected,
                onClick = {
                    focus.clearFocus()
                    keyboard?.hide()
                    expanded = true
                },
                modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp).semantics { contentDescription = "$title: $selected" },
                enabled = enabled,
                variant = PulseButtonVariant.Secondary,
                trailingIcon = { Icon(painterResource(R.drawable.pulse_obligation_chevron), null, Modifier.size(20.dp)) },
            )
            PulseDropdownMenu(expanded && enabled, { expanded = false }) {
                (listOf(null to none) + options).forEach { (optionId, name) ->
                    PulseDropdownMenuItem(
                        text = { PulseText(name) },
                        onClick = {
                            expanded = false
                            onSelect(optionId)
                        },
                    )
                }
            }
        }
    }
}
