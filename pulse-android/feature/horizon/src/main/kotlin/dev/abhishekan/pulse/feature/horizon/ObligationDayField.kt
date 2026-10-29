package dev.abhishekan.pulse.feature.horizon

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import dev.abhishekan.pulse.core.ui.PulseTheme
import dev.abhishekan.pulse.core.ui.components.PulseButton
import dev.abhishekan.pulse.core.ui.components.PulseButtonStyle
import dev.abhishekan.pulse.core.ui.components.PulseButtonVariant
import dev.abhishekan.pulse.core.ui.components.PulseText
import dev.abhishekan.pulse.core.ui.components.PulseTextStyle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ObligationDayField(
    value: String,
    enabled: Boolean,
    onChange: (String) -> Unit,
) {
    var open by rememberSaveable { mutableStateOf(false) }
    val colors = PulseTheme.colors
    val label = stringResource(R.string.pulse_horizon_obligation_due_label)
    val selected = value.ifBlank { stringResource(R.string.pulse_horizon_not_set) }
    val description = "$label: $selected"
    val focus = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        PulseText(label, style = PulseTextStyle.Label)
        PulseButton(
            text = selected,
            onClick = {
                focus.clearFocus()
                keyboard?.hide()
                open = true
            },
            modifier =
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = 56.dp)
                    .testTag("obligation-due-day")
                    .semantics { contentDescription = description },
            enabled = enabled,
            variant = PulseButtonVariant.Secondary,
            leadingIcon = { Icon(painterResource(R.drawable.pulse_obligation_calendar), null, Modifier.size(20.dp)) },
        )
    }
    if (open && enabled) {
        ModalBottomSheet(
            onDismissRequest = { open = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            sheetMaxWidth = 520.dp,
            containerColor = colors.surface,
        ) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .heightIn(max = 640.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 24.dp)
                    .testTag("obligation-day-picker"),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                PulseText(stringResource(R.string.pulse_horizon_obligation_choose_day), style = PulseTextStyle.Title3)
                PulseText(
                    stringResource(R.string.pulse_horizon_obligation_short_month),
                    style = PulseTextStyle.Body2,
                    color = colors.textSecondary,
                )
                BoxWithConstraints(Modifier.fillMaxWidth().selectableGroup()) {
                    val target = 48.dp * LocalDensity.current.fontScale.coerceAtLeast(1f)
                    val columns = ((maxWidth + 4.dp) / (target + 4.dp)).toInt().coerceIn(3, 7)
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        (1..31).toList().chunked(columns).forEach { days ->
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                days.forEach { day ->
                                    val checked = value == day.toString()
                                    Surface(
                                        Modifier
                                            .weight(1f)
                                            .heightIn(min = 48.dp)
                                            .testTag("obligation-day-$day")
                                            .selectable(checked, role = Role.RadioButton) {
                                                onChange(day.toString())
                                                open = false
                                            },
                                        shape = PulseTheme.shapes.small,
                                        color = if (checked) colors.primarySoft else colors.surface,
                                        border = BorderStroke(1.dp, if (checked) colors.action else colors.border),
                                    ) {
                                        Box(Modifier.padding(vertical = 12.dp), contentAlignment = Alignment.Center) {
                                            PulseText(day.toString(), style = PulseTextStyle.Label)
                                        }
                                    }
                                }
                                repeat(columns - days.size) { Spacer(Modifier.weight(1f)) }
                            }
                        }
                    }
                }
                PulseButton(stringResource(R.string.pulse_horizon_obligation_clear_day), {
                    onChange("")
                    open = false
                }, Modifier.fillMaxWidth(), style = PulseButtonStyle.Text)
            }
        }
    }
}
