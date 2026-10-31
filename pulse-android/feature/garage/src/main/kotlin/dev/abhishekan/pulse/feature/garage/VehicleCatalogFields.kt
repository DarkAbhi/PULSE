package dev.abhishekan.pulse.feature.garage

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import dev.abhishekan.pulse.core.domain.model.VehicleCatalogOption
import dev.abhishekan.pulse.core.ui.PulseTheme
import dev.abhishekan.pulse.core.ui.components.PulseErrorState
import dev.abhishekan.pulse.core.ui.components.PulseSelectionField
import dev.abhishekan.pulse.core.ui.components.PulseText
import dev.abhishekan.pulse.core.ui.components.PulseTextField
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
    if (draft["catalogMode"] == "manual") {
        PulseTextField(draft["makeName"], {
            if (it.length <=
                200
            ) {
                change(draft.set("makeName", it))
            }
        }, stringResource(R.string.pulse_garage_manual_make), Modifier.fillMaxWidth(), enabled)
        PulseTextField(draft["modelName"], {
            if (it.length <=
                200
            ) {
                change(draft.set("modelName", it))
            }
        }, stringResource(R.string.pulse_garage_manual_model), Modifier.fillMaxWidth(), enabled)
    } else if (draft["vehicleKind"].isNotBlank()) {
        CatalogSearch(
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
        if (draft["makeSlug"].isNotBlank()) {
            CatalogSearch(
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
}

@Composable
private fun CatalogSearch(
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
    val options = section.value.orEmpty()
    Column(verticalArrangement = Arrangement.spacedBy(PulseTheme.spacing.small)) {
        PulseTextField(query, { if (it.codePointCount(0, it.length) <= 100) search(it) }, searchLabel, Modifier.fillMaxWidth(), enabled)
        PulseSelectionField(
            label,
            selected.ifBlank { stringResource(R.string.pulse_garage_select_option) },
            options.map { it.name },
            { choose(options[it]) },
            enabled =
                enabled && options.isNotEmpty() && !section.loading,
            labelAbove = true,
        )
        when {
            section.loading ->
                PulseText(
                    stringResource(R.string.pulse_garage_catalog_loading),
                    Modifier.semantics {
                        liveRegion =
                            LiveRegionMode.Polite
                    },
                    color = PulseTheme.colors.textSecondary,
                )
            section.error != null -> PulseErrorState(section.error.message, retry)
            options.isEmpty() -> PulseText(stringResource(R.string.pulse_garage_catalog_empty), color = PulseTheme.colors.textSecondary)
            options.size == 100 -> PulseText(stringResource(R.string.pulse_garage_catalog_limit), color = PulseTheme.colors.textSecondary)
        }
    }
}
