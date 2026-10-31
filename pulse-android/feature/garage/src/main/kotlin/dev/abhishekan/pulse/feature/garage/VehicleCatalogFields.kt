package dev.abhishekan.pulse.feature.garage

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import dev.abhishekan.pulse.core.domain.model.VehicleCatalogOption
import dev.abhishekan.pulse.core.ui.PulseTheme
import dev.abhishekan.pulse.core.ui.components.PulseButton
import dev.abhishekan.pulse.core.ui.components.PulseButtonStyle
import dev.abhishekan.pulse.core.ui.components.PulseButtonVariant
import dev.abhishekan.pulse.core.ui.components.PulseCard
import dev.abhishekan.pulse.core.ui.components.PulseErrorState
import dev.abhishekan.pulse.core.ui.components.PulseRadioButton
import dev.abhishekan.pulse.core.ui.components.PulseText
import dev.abhishekan.pulse.core.ui.components.PulseTextField
import dev.abhishekan.pulse.core.ui.components.PulseTextStyle
import dev.abhishekan.pulse.core.ui.message

@Composable
internal fun VehicleCatalogFields(
    draft: GarageDraft,
    makes: GarageSection<List<VehicleCatalogOption>>,
    models: GarageSection<List<VehicleCatalogOption>>,
    enabled: Boolean,
    change: (GarageDraft) -> Unit,
    retry: () -> Unit,
) {
    val manual = draft["catalogMode"] == "manual"
    if (!manual && draft["vehicleKind"].isBlank()) return
    val make: @Composable () -> Unit = {
        if (manual) {
            PulseTextField(draft["makeName"], {
                if (it.codePointCount(0, it.length) <= 200) change(draft.set("makeName", it))
            }, stringResource(R.string.pulse_garage_manual_make), Modifier.fillMaxWidth(), enabled)
        } else {
            CatalogPicker(
                "make",
                draft["vehicleKind"],
                draft["makeSlug"],
                stringResource(R.string.pulse_garage_make),
                stringResource(R.string.pulse_garage_search_makes),
                draft["makeName"],
                draft["makeQuery"],
                makes,
                enabled,
                { change(draft.set("makeQuery", it)) },
                { change(draft.chooseMake(it)) },
                retry,
            )
        }
    }
    val model: @Composable () -> Unit = {
        if (manual) {
            PulseTextField(draft["modelName"], {
                if (it.codePointCount(0, it.length) <= 200) change(draft.set("modelName", it))
            }, stringResource(R.string.pulse_garage_manual_model), Modifier.fillMaxWidth(), enabled)
        } else if (draft["makeSlug"].isNotBlank()) {
            CatalogPicker(
                "model",
                "${draft["vehicleKind"]}/${draft["makeSlug"]}",
                draft["catalogId"],
                stringResource(R.string.pulse_garage_model),
                stringResource(R.string.pulse_garage_search_models),
                draft["modelName"],
                draft["modelQuery"],
                models,
                enabled,
                { change(draft.set("modelQuery", it)) },
                { change(draft.chooseModel(it)) },
                retry,
            )
        }
    }
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        if (maxWidth >= 480.dp && LocalDensity.current.fontScale <= 1.3f) {
            Row(horizontalArrangement = Arrangement.spacedBy(PulseTheme.spacing.medium)) {
                Column(Modifier.weight(1f)) { make() }
                Column(Modifier.weight(1f)) { model() }
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(PulseTheme.spacing.medium)) {
                make()
                model()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CatalogPicker(
    fieldKey: String,
    contextKey: String,
    selectedId: String,
    label: String,
    searchLabel: String,
    selected: String,
    query: String,
    section: GarageSection<List<VehicleCatalogOption>>,
    enabled: Boolean,
    search: (String) -> Unit,
    choose: (VehicleCatalogOption) -> Unit,
    retry: () -> Unit,
) {
    var open by rememberSaveable(fieldKey, contextKey) { mutableStateOf(false) }
    val options = section.value.orEmpty()
    val focus = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    val display = selected.ifBlank { stringResource(R.string.pulse_garage_select_option) }
    val dismiss = {
        focus.clearFocus()
        keyboard?.hide()
        open = false
    }
    Column(verticalArrangement = Arrangement.spacedBy(PulseTheme.spacing.small)) {
        PulseText(label, style = PulseTextStyle.Label)
        PulseButton(
            display,
            {
                focus.clearFocus()
                keyboard?.hide()
                open = true
            },
            Modifier.fillMaxWidth().heightIn(min = 56.dp).semantics { contentDescription = "$label: $display" },
            enabled = enabled,
            variant = PulseButtonVariant.Secondary,
        )
    }
    if (open && enabled) {
        val listState = rememberLazyListState()
        LaunchedEffect(query) { listState.scrollToItem(0) }
        ModalBottomSheet(
            onDismissRequest = dismiss,
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            sheetMaxWidth = 640.dp,
            containerColor = PulseTheme.colors.surface,
        ) {
            val sheetFocus = LocalFocusManager.current
            val sheetKeyboard = LocalSoftwareKeyboardController.current
            val dismissSheet = {
                sheetFocus.clearFocus()
                sheetKeyboard?.hide()
                open = false
            }
            Column(
                Modifier
                    .fillMaxWidth()
                    .heightIn(max = 640.dp)
                    .fillMaxHeight(0.85f)
                    .imePadding()
                    .padding(horizontal = PulseTheme.spacing.medium)
                    .padding(bottom = PulseTheme.spacing.cardPadding)
                    .testTag("garage-catalog-$fieldKey"),
                verticalArrangement = Arrangement.spacedBy(PulseTheme.spacing.medium),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    PulseText(
                        stringResource(
                            if (fieldKey ==
                                "make"
                            ) {
                                R.string.pulse_garage_choose_make
                            } else {
                                R.string.pulse_garage_choose_model
                            },
                        ),
                        Modifier.weight(1f).semantics { heading() },
                        PulseTextStyle.Title,
                    )
                    PulseButton(stringResource(R.string.pulse_garage_close_picker), dismissSheet, style = PulseButtonStyle.Text)
                }
                PulseTextField(
                    query,
                    { if (it.codePointCount(0, it.length) <= 100) search(it) },
                    searchLabel,
                    Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions =
                        KeyboardActions(onSearch = {
                            sheetFocus.clearFocus()
                            sheetKeyboard?.hide()
                        }),
                )
                when {
                    section.loading ->
                        PulseText(
                            stringResource(R.string.pulse_garage_catalog_loading),
                            Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                            color = PulseTheme.colors.textSecondary,
                        )
                    section.error != null -> PulseErrorState(section.error.message, retry)
                    options.isEmpty() ->
                        PulseText(
                            stringResource(R.string.pulse_garage_catalog_empty),
                            color = PulseTheme.colors.textSecondary,
                        )
                    options.size == 100 ->
                        PulseText(
                            stringResource(R.string.pulse_garage_catalog_limit),
                            color = PulseTheme.colors.textSecondary,
                        )
                }
                if (!section.loading && section.error == null && options.isNotEmpty() && options.size < 100) {
                    PulseText(
                        pluralStringResource(R.plurals.pulse_garage_catalog_count, options.size, options.size),
                        color = PulseTheme.colors.textSecondary,
                    )
                }
                LazyColumn(
                    Modifier.fillMaxWidth().weight(1f).selectableGroup(),
                    state = listState,
                    verticalArrangement = Arrangement.spacedBy(PulseTheme.spacing.small),
                ) {
                    items(options, key = { it.id }) { option ->
                        PulseCard(
                            Modifier.fillMaxWidth(),
                            highlighted = option.id == selectedId,
                            border =
                                BorderStroke(
                                    1.dp,
                                    if (option.id ==
                                        selectedId
                                    ) {
                                        PulseTheme.colors.primary
                                    } else {
                                        PulseTheme.colors.surface
                                    },
                                ),
                            contentPadding = PulseTheme.spacing.small,
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(PulseTheme.spacing.small),
                            ) {
                                Box(
                                    Modifier.size(40.dp).background(PulseTheme.colors.primarySoft, PulseTheme.shapes.pill),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    PulseText(
                                        option.name.take(2).uppercase(),
                                        color = PulseTheme.colors.onPrimarySoft,
                                        style = PulseTextStyle.Label,
                                    )
                                }
                                PulseRadioButton(
                                    option.id == selectedId,
                                    {
                                        dismissSheet()
                                        choose(option)
                                    },
                                    option.name,
                                    Modifier.weight(1f),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
