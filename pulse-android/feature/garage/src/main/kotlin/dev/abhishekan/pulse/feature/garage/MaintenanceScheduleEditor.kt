package dev.abhishekan.pulse.feature.garage

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.abhishekan.pulse.core.domain.model.MaintenanceInterval
import dev.abhishekan.pulse.core.domain.model.MaintenanceSchedule
import dev.abhishekan.pulse.core.ui.PulseTheme
import dev.abhishekan.pulse.core.ui.PulseTokens
import dev.abhishekan.pulse.core.ui.components.PulseFormAction
import dev.abhishekan.pulse.core.ui.components.PulseFormActions
import dev.abhishekan.pulse.core.ui.components.PulseFormLayout
import dev.abhishekan.pulse.core.ui.components.PulseRadioButton
import dev.abhishekan.pulse.core.ui.components.PulseText
import dev.abhishekan.pulse.core.ui.components.PulseTextField
import dev.abhishekan.pulse.core.ui.components.PulseTextStyle
import dev.abhishekan.pulse.core.ui.uiText
import dev.abhishekan.pulse.core.ui.R as CoreUiR

@Composable internal fun scheduleSourceLabel(source: String): String =
    stringResource(if (source == "manual") R.string.pulse_garage_schedule_manual else R.string.pulse_garage_schedule_personal)

private fun reminderNumber(
    value: String,
    max: Int,
): Int? = if (value.isBlank()) 0 else value.toIntOrNull()?.takeIf { it in 1..max }

internal fun reminderInterval(
    km: String,
    months: String,
): MaintenanceInterval? {
    val distance = reminderNumber(km, 1000000) ?: return null
    val duration = reminderNumber(months, 1200) ?: return null
    return MaintenanceInterval(distance, duration)
}

@Composable
internal fun MaintenanceScheduleEditor(
    setting: MaintenanceSchedule,
    enabled: Boolean,
    busy: Boolean,
    pagePadding: Dp,
    prefix: @Composable () -> Unit,
    cancel: () -> Unit,
    save: (MaintenanceSchedule) -> Unit,
) {
    fun Int.input() = takeIf { it > 0 }?.toString().orEmpty()
    var source by rememberSaveable(setting) { mutableStateOf(setting.activeSource) }
    var personalKm by rememberSaveable(setting) { mutableStateOf(setting.personal.kilometers.input()) }
    var personalTime by rememberSaveable(setting) { mutableStateOf(setting.personal.duration.input()) }
    var manualKm by rememberSaveable(setting) { mutableStateOf(setting.manual.kilometers.input()) }
    var manualTime by rememberSaveable(setting) { mutableStateOf(setting.manual.duration.input()) }
    var reference by rememberSaveable(setting) { mutableStateOf(setting.manualReference) }
    val colors = PulseTheme.colors
    val focus = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    val personal = reminderInterval(personalKm, personalTime)
    val manual = reminderInterval(manualKm, manualTime)
    val chosen = if (source == "personal") personal else manual
    val referenceValid = reference.toByteArray().size <= 160
    val valid = personal != null && manual != null && referenceValid && chosen != null && (chosen.kilometers > 0 || chosen.duration > 0)

    @Composable fun numberField(
        key: String,
        value: String,
        update: (String) -> Unit,
        months: Boolean,
    ) {
        val max = if (months) 1200 else 1000000
        PulseTextField(
            value,
            update,
            stringResource(if (months) R.string.pulse_garage_schedule_time else R.string.pulse_garage_schedule_km),
            Modifier.fillMaxWidth().testTag("schedule-${setting.kind}-$key"),
            enabled = enabled,
            labelAbove = true,
            error =
                if (reminderNumber(value, max) == null) {
                    uiText(
                        if (months) R.string.pulse_garage_schedule_time_error else R.string.pulse_garage_schedule_km_error,
                    )
                } else {
                    null
                },
            placeholder = {
                PulseText(
                    stringResource(
                        if (months) R.string.pulse_garage_schedule_time_example else R.string.pulse_garage_schedule_km_example,
                    ),
                    color = colors.textSecondary,
                )
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
            keyboardActions = KeyboardActions(onNext = { focus.moveFocus(FocusDirection.Next) }),
        )
    }

    @Composable fun intervalFields(
        key: String,
        km: String,
        time: String,
        changeKm: (String) -> Unit,
        changeTime: (String) -> Unit,
    ) {
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            if (maxWidth >= 600.dp * LocalDensity.current.fontScale.coerceAtLeast(1f)) {
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Box(Modifier.weight(1f)) { numberField("$key-km", km, changeKm, false) }
                    Box(Modifier.weight(1f)) { numberField("$key-time", time, changeTime, true) }
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    numberField("$key-km", km, changeKm, false)
                    numberField("$key-time", time, changeTime, true)
                }
            }
        }
    }
    PulseFormLayout(
        scrollContentModifier = Modifier.padding(pagePadding),
        footer = {
            PulseFormActions(
                primary =
                    PulseFormAction(
                        stringResource(CoreUiR.string.pulse_ui_save),
                        {
                            focus.clearFocus()
                            keyboard?.hide()
                            save(setting.copy(activeSource = source, personal = personal!!, manual = manual!!, manualReference = reference))
                        },
                        enabled = enabled && valid,
                        loading = busy,
                        modifier = Modifier.testTag("schedule-save-${setting.kind}"),
                    ),
                secondary =
                    PulseFormAction(
                        stringResource(CoreUiR.string.pulse_ui_cancel),
                        {
                            focus.clearFocus()
                            keyboard?.hide()
                            source = setting.activeSource
                            personalKm = setting.personal.kilometers.input()
                            personalTime = setting.personal.duration.input()
                            manualKm = setting.manual.kilometers.input()
                            manualTime = setting.manual.duration.input()
                            reference = setting.manualReference
                            cancel()
                        },
                        enabled = enabled,
                    ),
                modifier = Modifier.align(Alignment.Start).padding(horizontal = pagePadding).widthIn(max = 760.dp),
            )
        },
    ) {
        prefix()
        Column(
            Modifier.widthIn(max = 760.dp).fillMaxWidth().testTag("schedule-fields-${setting.kind}"),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            PulseText(stringResource(R.string.pulse_garage_schedule_hint), style = PulseTextStyle.Body2, color = colors.textSecondary)
            MaintenanceSection(R.string.pulse_garage_schedule_active, R.string.pulse_garage_schedule_active_hint, R.drawable.garage_clock) {
                BoxWithConstraints(Modifier.fillMaxWidth()) {
                    @Composable fun option(
                        key: String,
                        modifier: Modifier,
                    ) {
                        val selected = source == key
                        Surface(
                            modifier,
                            shape = PulseTokens.controlShape,
                            color = if (selected) colors.primarySoft else colors.surfaceMuted,
                            border = BorderStroke(1.dp, if (selected) colors.action else colors.border),
                        ) {
                            PulseRadioButton(
                                selected,
                                {
                                    focus.clearFocus()
                                    keyboard?.hide()
                                    source = key
                                },
                                scheduleSourceLabel(
                                    key,
                                ),
                                Modifier.fillMaxWidth().padding(horizontal = 12.dp).testTag("schedule-source-$key"),
                                enabled = enabled,
                            )
                        }
                    }
                    if (maxWidth >= 400.dp * LocalDensity.current.fontScale.coerceAtLeast(1f)) {
                        Row(Modifier.selectableGroup(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            option("personal", Modifier.weight(1f))
                            option("manual", Modifier.weight(1f))
                        }
                    } else {
                        Column(Modifier.selectableGroup(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            option("personal", Modifier.fillMaxWidth())
                            option("manual", Modifier.fillMaxWidth())
                        }
                    }
                }
            }
            MaintenanceSection(
                R.string.pulse_garage_schedule_personal,
                R.string.pulse_garage_schedule_personal_hint,
                R.drawable.garage_odometer,
            ) {
                intervalFields("personal", personalKm, personalTime, { personalKm = it }, { personalTime = it })
            }
            MaintenanceSection(
                R.string.pulse_garage_schedule_manual,
                R.string.pulse_garage_schedule_manual_section_hint,
                R.drawable.garage_document,
            ) {
                intervalFields("manual", manualKm, manualTime, { manualKm = it }, { manualTime = it })
                PulseTextField(
                    reference,
                    { reference = it },
                    stringResource(R.string.pulse_garage_schedule_reference),
                    Modifier.fillMaxWidth(),
                    enabled = enabled,
                    labelAbove = true,
                    error = if (!referenceValid) uiText(R.string.pulse_garage_schedule_reference_error) else null,
                    placeholder = {
                        PulseText(stringResource(R.string.pulse_garage_schedule_reference_example), color = colors.textSecondary)
                    },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { focus.clearFocus() }),
                )
            }
            if (chosen != null && chosen.kilometers == 0 && chosen.duration == 0) {
                PulseText(
                    stringResource(R.string.pulse_garage_schedule_missing_interval),
                    style = PulseTextStyle.Body2,
                    color = colors.textSecondary,
                )
            }
        }
    }
}
