package dev.abhishekan.pulse.feature.garage
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import dev.abhishekan.pulse.core.domain.model.garageCategories
import dev.abhishekan.pulse.core.ui.PulseTheme
import dev.abhishekan.pulse.core.ui.PulseTokens
import dev.abhishekan.pulse.core.ui.UiText
import dev.abhishekan.pulse.core.ui.components.PulseButton
import dev.abhishekan.pulse.core.ui.components.PulseCard
import dev.abhishekan.pulse.core.ui.components.PulseDatePickerDialog
import dev.abhishekan.pulse.core.ui.components.PulseSelectionField
import dev.abhishekan.pulse.core.ui.components.PulseText
import dev.abhishekan.pulse.core.ui.components.PulseTextButton
import dev.abhishekan.pulse.core.ui.components.PulseTextField
import dev.abhishekan.pulse.core.ui.components.PulseTextStyle
import dev.abhishekan.pulse.core.ui.components.PulseTimePickerDialog
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

@Composable
internal fun GarageEditor(
    draft: GarageDraft,
    busy: Boolean,
    blocked: Boolean,
    error: UiText?,
    change: (GarageDraft) -> Unit,
    save: () -> Unit,
    cancel: () -> Unit,
) {
    if (draft.kind == GarageForm.Vehicle) {
        VehicleEditor(draft, busy, blocked, error, change, save, cancel)
        return
    }
    if (draft.kind == GarageForm.Fuel) {
        FuelEditor(draft, busy, error, change)
        return
    }
    var datePicker by remember { mutableStateOf(false) }
    var timePicker by remember { mutableStateOf(false) }
    val focus = LocalFocusManager.current
    val enabled = !busy

    @Composable fun field(
        key: String,
        label: String,
        numeric: Boolean = false,
    ) {
        PulseTextField(
            draft[key],
            {
                change(draft.set(key, it))
            },
            label,
            Modifier.fillMaxWidth(),
            enabled,
            keyboardOptions =
                KeyboardOptions(
                    keyboardType = if (numeric) KeyboardType.Decimal else KeyboardType.Text,
                    imeAction = ImeAction.Next,
                ),
            keyboardActions = KeyboardActions(onNext = { focus.moveFocus(FocusDirection.Next) }),
        )
    }

    @Composable fun choice(
        key: String,
        label: String,
        options: List<String>,
    ) {
        GarageChoice(label, draft[key], options, { change(draft.set(key, it)) }, enabled)
    }
    PulseCard(Modifier.widthIn(max = PulseTokens.readingWidth).fillMaxWidth()) {
        PulseText(
            stringResource(
                draft.kind.editorLabel(
                    if (draft.id == null &&
                        draft.kind != GarageForm.Pressure
                    ) {
                        EditorAction.Add
                    } else {
                        EditorAction.Edit
                    },
                ),
            ),
            style = PulseTextStyle.Title,
        )
        when (draft.kind) {
            GarageForm.Vehicle -> Unit
            GarageForm.Pressure ->
                PulseText(
                    stringResource(
                        R.string.pulse_garage_target_tire_pressures_in_psi_leave_a_field_empty,
                    ),
                )
            GarageForm.Care -> {
                choice(
                    "category",
                    stringResource(R.string.pulse_garage_maintenance_category),
                    garageCategories,
                )
                field("title", stringResource(R.string.pulse_garage_title))
                field("amount", stringResource(R.string.pulse_garage_amount), true)
            }
            GarageForm.Fuel -> {
                field("odometer", stringResource(R.string.pulse_garage_odometer_km), true)
            }
        }
        if (draft.kind == GarageForm.Pressure) {
            field("frontSolo", stringResource(R.string.pulse_garage_front_solo_psi), true)
            field("rearSolo", stringResource(R.string.pulse_garage_rear_solo_psi), true)
            field("frontPillion", stringResource(R.string.pulse_garage_front_pillion_psi), true)
            field("rearPillion", stringResource(R.string.pulse_garage_rear_pillion_psi), true)
        }
        if (draft.kind in listOf(GarageForm.Fuel, GarageForm.Care)) {
            PulseTextButton(stringResource(R.string.pulse_garage_date_1_s, draft["date"]), {
                datePicker =
                    true
            }, enabled = enabled)
            PulseTextButton(stringResource(R.string.pulse_garage_time_1_s, draft["time"]), {
                timePicker =
                    true
            }, enabled = enabled)
            PulseText(
                stringResource(
                    R.string.pulse_garage_date_and_time_use_1_s,
                    draft["zone"].ifBlank {
                        ZoneId.systemDefault().id
                    },
                ),
                style = PulseTextStyle.Label,
            )
        }
        if (draft.kind ==
            GarageForm.Care
        ) {
            field("odometer", stringResource(R.string.pulse_garage_odometer_km_optional), true)
            field("provider", stringResource(R.string.pulse_garage_provider_optional))
            PulseText(
                stringResource(
                    R.string.pulse_garage_save_this_record_first_then_attach_receipts_from_its,
                ),
            )
        }
        if (draft.kind in
            listOf(GarageForm.Fuel, GarageForm.Care)
        ) {
            field("notes", stringResource(R.string.pulse_garage_notes_optional))
        }
        error?.let {
            PulseText(
                it,
                Modifier.semantics {
                    liveRegion = LiveRegionMode.Polite
                },
                color = PulseTheme.colors.error,
            )
        }
        PulseButton(
            stringResource(draft.kind.editorLabel(EditorAction.Save)),
            {
                focus.clearFocus()
                save()
            },
            Modifier.fillMaxWidth(),
            enabled =
                !blocked && enabled,
            loading = busy,
        )
        PulseTextButton(stringResource(R.string.pulse_garage_cancel_editing), {
            focus.clearFocus()
            cancel()
        }, enabled = enabled)
    }
    if (datePicker) {
        PulseDatePickerDialog(
            runCatching {
                LocalDate.parse(draft["date"])
            }.getOrDefault(LocalDate.now()),
            {
                change(draft.set("date", it.toString()))
                datePicker = false
            },
            { datePicker = false },
        )
    }
    if (timePicker) {
        PulseTimePickerDialog(
            runCatching {
                LocalTime.parse(draft["time"])
            }.getOrDefault(LocalTime.MIDNIGHT),
            {
                change(draft.set("time", it.toString()))
                timePicker = false
            },
            { timePicker = false },
        )
    }
}

@Composable
internal fun GarageChoice(
    label: String,
    value: String,
    options: List<String>,
    select: (String) -> Unit,
    enabled: Boolean,
) {
    PulseSelectionField(
        label,
        garageOption(value),
        options.map {
            garageOption(it)
        },
        { select(options[it]) },
        Modifier.fillMaxWidth(),
        enabled,
    )
}
