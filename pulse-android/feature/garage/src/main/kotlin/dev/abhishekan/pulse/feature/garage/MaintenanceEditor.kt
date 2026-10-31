package dev.abhishekan.pulse.feature.garage

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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import dev.abhishekan.pulse.core.domain.model.UserCurrency
import dev.abhishekan.pulse.core.domain.model.UserTimeZone
import dev.abhishekan.pulse.core.domain.model.garageCategories
import dev.abhishekan.pulse.core.ui.PulseTheme
import dev.abhishekan.pulse.core.ui.PulseTokens
import dev.abhishekan.pulse.core.ui.UiText
import dev.abhishekan.pulse.core.ui.components.PulseButton
import dev.abhishekan.pulse.core.ui.components.PulseButtonStyle
import dev.abhishekan.pulse.core.ui.components.PulseButtonVariant
import dev.abhishekan.pulse.core.ui.components.PulseText
import dev.abhishekan.pulse.core.ui.components.PulseTextField
import dev.abhishekan.pulse.core.ui.components.PulseTextStyle
import dev.abhishekan.pulse.core.ui.components.PulseTimePickerDialog
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@Composable
internal fun MaintenanceEditor(
    draft: GarageDraft,
    busy: Boolean,
    blocked: Boolean,
    error: UiText?,
    change: (GarageDraft) -> Unit,
    save: () -> Unit,
    cancel: () -> Unit,
) {
    var picker by rememberSaveable { mutableStateOf<String?>(null) }
    val focus = LocalFocusManager.current
    val colors = PulseTheme.colors
    val enabled = !busy
    val open: (String) -> Unit = {
        focus.clearFocus()
        picker = it
    }

    @Composable fun input(
        key: String,
        label: Int,
        numeric: Boolean = false,
        placeholder: Int? = null,
    ) {
        PulseTextField(
            draft[key],
            { change(draft.set(key, it)) },
            stringResource(label),
            Modifier.fillMaxWidth().testTag("maintenance-$key"),
            enabled = enabled,
            labelAbove = true,
            placeholder = placeholder?.let { { PulseText(stringResource(it), color = colors.textSecondary) } },
            keyboardOptions =
                KeyboardOptions(
                    keyboardType = if (numeric) KeyboardType.Decimal else KeyboardType.Text,
                    imeAction = ImeAction.Next,
                ),
            keyboardActions = KeyboardActions(onNext = { focus.moveFocus(FocusDirection.Next) }),
        )
    }
    Column(Modifier.widthIn(max = 760.dp).fillMaxWidth().testTag("maintenance-editor"), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        PulseText(
            stringResource(
                if (draft.id ==
                    null
                ) {
                    R.string.pulse_garage_add_care
                } else {
                    R.string.pulse_garage_edit_care
                },
            ),
            Modifier.semantics { heading() },
            PulseTextStyle.Display,
        )
        PulseText(stringResource(R.string.pulse_garage_care_subtitle), color = colors.textSecondary)
        MaintenanceSection(R.string.pulse_garage_care_service_details, R.string.pulse_garage_care_service_hint, R.drawable.garage_service) {
            MaintenancePickerField(
                stringResource(R.string.pulse_garage_maintenance_category),
                garageOption(draft["category"]),
                R.drawable.garage_service,
                enabled,
                "category",
            ) {
                open("category")
            }
            input("title", R.string.pulse_garage_title, placeholder = R.string.pulse_garage_care_title_hint)
            PulseTextField(
                draft["amount"],
                { change(draft.set("amount", it)) },
                stringResource(R.string.pulse_garage_amount, UserCurrency.symbol()),
                Modifier.fillMaxWidth().testTag("maintenance-amount"),
                enabled = enabled,
                labelAbove = true,
                textStyle = PulseTheme.typography.heading1,
                leadingIcon = { PulseText(UserCurrency.symbol(), style = PulseTextStyle.Title3) },
                placeholder = { PulseText("0.00", style = PulseTextStyle.Heading1, color = colors.textSecondary) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions(onNext = { focus.moveFocus(FocusDirection.Next) }),
            )
        }
        MaintenanceSection(R.string.pulse_garage_care_when, R.string.pulse_garage_care_when_hint, R.drawable.garage_calendar) {
            BoxWithConstraints {
                val date: @Composable () -> Unit = {
                    MaintenancePickerField(
                        stringResource(R.string.pulse_garage_care_date),
                        runCatching {
                            LocalDate
                                .parse(draft["date"])
                                .format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM))
                        }.getOrDefault(draft["date"]),
                        R.drawable.garage_calendar,
                        enabled,
                        "date",
                    ) { open("date") }
                }
                val time: @Composable () -> Unit = {
                    MaintenancePickerField(
                        stringResource(R.string.pulse_garage_care_time),
                        runCatching {
                            LocalTime
                                .parse(draft["time"])
                                .format(DateTimeFormatter.ofPattern("HH:mm"))
                        }.getOrDefault(draft["time"]),
                        R.drawable.garage_clock,
                        enabled,
                        "time",
                    ) { open("time") }
                }
                if (maxWidth >= 360.dp * LocalDensity.current.fontScale.coerceAtLeast(1f)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        Box(Modifier.weight(1f)) { date() }
                        Box(Modifier.weight(1f)) { time() }
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        date()
                        time()
                    }
                }
            }
        }
        MaintenanceSection(R.string.pulse_garage_care_extra, R.string.pulse_garage_care_extra_hint, R.drawable.garage_document) {
            input("odometer", R.string.pulse_garage_odometer_km_optional, true)
            input("provider", R.string.pulse_garage_provider_optional, placeholder = R.string.pulse_garage_care_provider_hint)
            PulseTextField(
                draft["notes"],
                { change(draft.set("notes", it)) },
                stringResource(R.string.pulse_garage_notes_optional),
                Modifier.fillMaxWidth().testTag("maintenance-notes"),
                enabled = enabled,
                labelAbove = true,
                singleLine = false,
                minLines = 3,
                maxLines = 6,
                placeholder = { PulseText(stringResource(R.string.pulse_garage_care_notes_hint), color = colors.textSecondary) },
            )
            Surface(shape = PulseTheme.shapes.medium, color = colors.surfaceMuted) {
                Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Icon(painterResource(R.drawable.garage_attach), null, Modifier.size(20.dp), tint = colors.textSecondary)
                    PulseText(
                        stringResource(R.string.pulse_garage_save_this_record_first_then_attach_receipts_from_its),
                        Modifier.weight(1f),
                        PulseTextStyle.Body2,
                        color = colors.textSecondary,
                    )
                }
            }
        }
        error?.let { PulseText(it, Modifier.semantics { liveRegion = LiveRegionMode.Polite }, color = colors.error) }
        PulseButton(
            stringResource(R.string.pulse_garage_save_care),
            {
                focus.clearFocus()
                save()
            },
            Modifier.fillMaxWidth().heightIn(min = 56.dp),
            enabled =
                enabled && !blocked,
            loading = busy,
        )
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            PulseButton(stringResource(R.string.pulse_garage_cancel_editing), {
                focus.clearFocus()
                cancel()
            }, enabled = enabled, style = PulseButtonStyle.Text)
        }
    }
    if (enabled) {
        when (picker) {
            "category" ->
                MaintenanceCategorySheet(draft["category"], {
                    change(draft.set("category", it))
                    picker = null
                }, { picker = null })
            "date" ->
                MaintenanceDateSheet(runCatching { LocalDate.parse(draft["date"]) }.getOrDefault(LocalDate.now(UserTimeZone.zone)), {
                    change(draft.set("date", it.toString()))
                    picker =
                        null
                }, { picker = null })
            "time" ->
                PulseTimePickerDialog(runCatching { LocalTime.parse(draft["time"]) }.getOrDefault(LocalTime.MIDNIGHT), {
                    change(draft.set("time", it.toString()))
                    picker =
                        null
                }, { picker = null })
        }
    }
}

@Composable
private fun MaintenanceSection(
    title: Int,
    hint: Int,
    icon: Int,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = PulseTheme.colors
    Surface(Modifier.fillMaxWidth(), shape = PulseTokens.cardShape, color = colors.surface, border = BorderStroke(1.dp, colors.border)) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Surface(shape = PulseTheme.shapes.medium, color = colors.primarySoft) {
                    Box(Modifier.size(40.dp), contentAlignment = Alignment.Center) {
                        Icon(painterResource(icon), null, Modifier.size(20.dp), tint = colors.onPrimarySoft)
                    }
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    PulseText(stringResource(title), Modifier.semantics { heading() }, PulseTextStyle.Title3)
                    PulseText(stringResource(hint), style = PulseTextStyle.Body2, color = colors.textSecondary)
                }
            }
            content()
        }
    }
}

@Composable
private fun MaintenancePickerField(
    label: String,
    value: String,
    icon: Int,
    enabled: Boolean,
    tag: String,
    open: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        PulseText(label, style = PulseTextStyle.Label)
        PulseButton(
            value,
            open,
            Modifier.fillMaxWidth().heightIn(min = 56.dp).testTag("maintenance-$tag").semantics {
                contentDescription =
                    "$label: $value"
            },
            enabled = enabled,
            variant = PulseButtonVariant.Secondary,
            leadingIcon = { Icon(painterResource(icon), null, Modifier.size(20.dp)) },
            trailingIcon = { Icon(painterResource(R.drawable.garage_chevron_down), null, Modifier.size(20.dp)) },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MaintenanceCategorySheet(
    selected: String,
    select: (String) -> Unit,
    dismiss: () -> Unit,
) {
    val colors = PulseTheme.colors
    ModalBottomSheet(
        onDismissRequest = dismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        sheetMaxWidth = 640.dp,
        containerColor = colors.surface,
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            PulseText(stringResource(R.string.pulse_garage_care_choose_category), Modifier.semantics { heading() }, PulseTextStyle.Title3)
            Column(Modifier.selectableGroup(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                garageCategories.forEach { category ->
                    val active = selected == category
                    Surface(
                        Modifier
                            .fillMaxWidth()
                            .selectable(active, role = Role.RadioButton) {
                                select(category)
                            }.testTag(
                                "maintenance-category-$category",
                            ),
                        shape = PulseTokens.controlShape,
                        color = if (active) colors.primarySoft else colors.surfaceMuted,
                        border =
                            BorderStroke(
                                1.dp,
                                if (active) colors.action else colors.border,
                            ),
                    ) {
                        Row(
                            Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            PulseText(garageOption(category), Modifier.weight(1f), PulseTextStyle.Label)
                            RadioButton(
                                selected = active,
                                onClick = null,
                                colors = RadioButtonDefaults.colors(selectedColor = colors.action),
                            )
                        }
                    }
                }
            }
            PulseButton(stringResource(R.string.pulse_garage_close_picker), dismiss, Modifier.fillMaxWidth(), style = PulseButtonStyle.Text)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MaintenanceDateSheet(
    selected: LocalDate,
    select: (LocalDate) -> Unit,
    dismiss: () -> Unit,
) {
    val colors = PulseTheme.colors
    val state =
        rememberDatePickerState(
            initialSelectedDateMillis = selected.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
            yearRange =
                0..9999,
        )
    ModalBottomSheet(
        onDismissRequest = dismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        sheetMaxWidth = 640.dp,
        containerColor = colors.surface,
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 24.dp)
                .testTag("maintenance-date-sheet"),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            PulseText(
                stringResource(R.string.pulse_garage_care_choose_date),
                Modifier.padding(horizontal = 24.dp).semantics {
                    heading()
                },
                PulseTextStyle.Title3,
            )
            DatePicker(
                state,
                colors =
                    DatePickerDefaults.colors(
                        containerColor = colors.surface,
                        selectedDayContainerColor = colors.primary,
                        selectedDayContentColor = colors.onPrimary,
                        todayContentColor = colors.action,
                        todayDateBorderColor = colors.action,
                    ),
            )
            PulseButton(
                stringResource(R.string.pulse_garage_care_choose_date),
                {
                    state.selectedDateMillis?.let {
                        select(Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate())
                    }
                },
                Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                enabled =
                    state.selectedDateMillis != null,
            )
            PulseButton(stringResource(R.string.pulse_garage_close_picker), dismiss, Modifier.fillMaxWidth(), style = PulseButtonStyle.Text)
        }
    }
}
