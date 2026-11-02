package dev.abhishekan.pulse.feature.garage

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import dev.abhishekan.pulse.core.domain.model.MaintenanceInterval
import dev.abhishekan.pulse.core.domain.model.MaintenanceSchedule
import dev.abhishekan.pulse.core.ui.components.PulseButton
import dev.abhishekan.pulse.core.ui.components.PulseButtonVariant
import dev.abhishekan.pulse.core.ui.components.PulseSelectionField
import dev.abhishekan.pulse.core.ui.components.PulseText
import dev.abhishekan.pulse.core.ui.components.PulseTextField
import dev.abhishekan.pulse.core.ui.components.PulseTextStyle

@Composable internal fun scheduleSourceLabel(source: String): String =
    stringResource(
        when (source) {
            "personal" -> R.string.pulse_garage_schedule_personal
            "manual" -> R.string.pulse_garage_schedule_manual
            else -> R.string.pulse_garage_schedule_default
        },
    )

@Composable
internal fun MaintenanceScheduleEditor(
    settings: List<MaintenanceSchedule>,
    enabled: Boolean,
    save: (MaintenanceSchedule) -> Unit,
) {
    settings.forEach { setting ->
        var expanded by rememberSaveable(setting.kind) { mutableStateOf(false) }
        PulseButton(
            stringResource(R.string.pulse_garage_schedule_edit, garageOption(setting.kind)),
            { expanded = !expanded },
            Modifier.fillMaxWidth().testTag("schedule-edit-${setting.kind}"),
            enabled,
            variant = PulseButtonVariant.Secondary,
        )
        if (expanded) ScheduleFields(setting, enabled, save)
    }
}

@Composable
private fun ScheduleFields(
    setting: MaintenanceSchedule,
    enabled: Boolean,
    save: (MaintenanceSchedule) -> Unit,
) {
    var source by rememberSaveable(setting) { mutableStateOf(setting.activeSource) }
    var personalKm by rememberSaveable(setting) {
        mutableStateOf(
            setting.personal.kilometers
                .takeIf { it > 0 }
                ?.toString()
                .orEmpty(),
        )
    }
    var personalTime by rememberSaveable(setting) {
        mutableStateOf(
            setting.personal.duration
                .takeIf { it > 0 }
                ?.toString()
                .orEmpty(),
        )
    }
    var personalUnit by rememberSaveable(setting) { mutableStateOf(setting.personal.unit) }
    var manualKm by rememberSaveable(setting) {
        mutableStateOf(
            setting.manual.kilometers
                .takeIf { it > 0 }
                ?.toString()
                .orEmpty(),
        )
    }
    var manualTime by rememberSaveable(setting) {
        mutableStateOf(
            setting.manual.duration
                .takeIf { it > 0 }
                ?.toString()
                .orEmpty(),
        )
    }
    var manualUnit by rememberSaveable(setting) { mutableStateOf(setting.manual.unit) }
    var reference by rememberSaveable(setting) { mutableStateOf(setting.manualReference) }
    val sources = listOf("default", "personal", "manual")

    fun interval(
        km: String,
        time: String,
        unit: String,
    ): MaintenanceInterval? {
        val distance = if (km.isBlank())0 else km.toIntOrNull() ?: return null
        val duration = if (time.isBlank())0 else time.toIntOrNull() ?: return null
        if (distance !in 0..1000000 || duration !in 0..(if (unit == "days")36500 else 1200))return null
        return MaintenanceInterval(distance, duration, unit)
    }
    val personal = interval(personalKm, personalTime, personalUnit)
    val manual = interval(manualKm, manualTime, manualUnit)
    val chosen = if (source == "personal")personal else manual
    val valid =
        personal != null &&
            manual != null &&
            reference.toByteArray().size <= 160 &&
            (source == "default" || chosen != null && (chosen.kilometers > 0 || chosen.duration > 0))
    Column(Modifier.fillMaxWidth().testTag("schedule-fields-${setting.kind}"), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        PulseText(stringResource(R.string.pulse_garage_schedule_hint), style = PulseTextStyle.Body2)
        PulseSelectionField(
            stringResource(R.string.pulse_garage_schedule_active),
            scheduleSourceLabel(source),
            sources.map { scheduleSourceLabel(it) },
            { source = sources[it] },
            enabled = enabled,
            labelAbove = true,
        )
        val default = setting.defaultInterval
        PulseText(
            stringResource(
                R.string.pulse_garage_schedule_default_values,
                default.kilometers,
                default.duration,
                stringResource(
                    if (default.unit ==
                        "days"
                    ) {
                        R.string.pulse_garage_schedule_days
                    } else {
                        R.string.pulse_garage_schedule_months
                    },
                ),
            ),
            style = PulseTextStyle.Caption,
        )

        @Composable fun inputs(
            key: String,
            label: String,
            km: String,
            time: String,
            unit: String,
            onKm: (String) -> Unit,
            onTime: (String) -> Unit,
            onUnit: (String) -> Unit,
        ) {
            PulseText(label, style = PulseTextStyle.Title3)
            PulseTextField(
                km,
                onKm,
                stringResource(R.string.pulse_garage_schedule_km),
                Modifier.fillMaxWidth().testTag("schedule-${setting.kind}-$key-km"),
                enabled = enabled,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            )
            PulseTextField(
                time,
                onTime,
                stringResource(R.string.pulse_garage_schedule_time),
                Modifier.fillMaxWidth().testTag("schedule-${setting.kind}-$key-time"),
                enabled = enabled,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            )
            PulseSelectionField(
                stringResource(R.string.pulse_garage_schedule_unit),
                stringResource(
                    if (unit ==
                        "days"
                    ) {
                        R.string.pulse_garage_schedule_days
                    } else {
                        R.string.pulse_garage_schedule_months
                    },
                ),
                listOf(stringResource(R.string.pulse_garage_schedule_days), stringResource(R.string.pulse_garage_schedule_months)),
                {
                    onUnit(
                        if (it ==
                            0
                        ) {
                            "days"
                        } else {
                            "months"
                        },
                    )
                },
                enabled = enabled,
            )
        }
        inputs(
            "personal",
            scheduleSourceLabel(
                "personal",
            ),
            personalKm,
            personalTime,
            personalUnit,
            { personalKm = it },
            { personalTime = it },
            {
                personalUnit =
                    it
            },
        )
        inputs("manual", scheduleSourceLabel("manual"), manualKm, manualTime, manualUnit, {
            manualKm = it
        }, { manualTime = it }, { manualUnit = it })
        PulseTextField(reference, {
            reference = it
        }, stringResource(R.string.pulse_garage_schedule_reference), Modifier.fillMaxWidth(), enabled = enabled)
        PulseText(stringResource(R.string.pulse_garage_schedule_manual_hint), style = PulseTextStyle.Caption)
        PulseButton(stringResource(R.string.pulse_garage_schedule_save), {
            save(setting.copy(activeSource = source, personal = personal!!, manual = manual!!, manualReference = reference))
        }, Modifier.testTag("schedule-save-${setting.kind}"), enabled = enabled && valid)
    }
}
