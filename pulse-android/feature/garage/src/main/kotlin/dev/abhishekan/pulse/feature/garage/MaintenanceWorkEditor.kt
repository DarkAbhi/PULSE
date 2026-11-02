package dev.abhishekan.pulse.feature.garage

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.abhishekan.pulse.core.domain.model.MaintenanceAction
import dev.abhishekan.pulse.core.domain.model.garageMaintenanceActions
import dev.abhishekan.pulse.core.domain.model.garageMaintenanceComponents
import dev.abhishekan.pulse.core.ui.components.PulseButton
import dev.abhishekan.pulse.core.ui.components.PulseButtonVariant
import dev.abhishekan.pulse.core.ui.components.PulseSelectionField
import dev.abhishekan.pulse.core.ui.components.PulseText
import dev.abhishekan.pulse.core.ui.components.PulseTextField
import dev.abhishekan.pulse.core.ui.components.PulseTextStyle

@Composable
internal fun MaintenanceWorkEditor(
    draft: GarageDraft,
    enabled: Boolean,
    change: (GarageDraft) -> Unit,
) {
    val actions = draft.maintenanceActions
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        PulseText(stringResource(R.string.pulse_garage_work_performed), style = PulseTextStyle.Title2)
        PulseText(stringResource(R.string.pulse_garage_work_hint), style = PulseTextStyle.Body2)
        actions.forEachIndexed { index, a ->
            fun update(value: MaintenanceAction) {
                change(draft.withMaintenanceActions(actions.toMutableList().also { it[index] = value }))
            }
            val components = (garageMaintenanceComponents + listOfNotNull(a.component.takeIf(String::isNotBlank))).distinct() + "custom"
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                PulseSelectionField(
                    stringResource(
                        R.string.pulse_garage_component,
                    ),
                    garageOption(a.component),
                    components.map { garageOption(it) },
                    {
                        update(
                            a.copy(
                                component = components[it],
                                action =
                                    if (components[it] ==
                                        "general_service"
                                    ) {
                                        "completed"
                                    } else {
                                        a.action
                                    },
                            ),
                        )
                    },
                    enabled = enabled,
                    labelAbove = true,
                )
                if (a.component == "custom" || a.component !in garageMaintenanceComponents) {
                    PulseTextField(
                        a.component.takeUnless { it == "custom" }.orEmpty(),
                        {
                            update(a.copy(component = it.lowercase(java.util.Locale.ROOT).replace(' ', '_')))
                        },
                        stringResource(R.string.pulse_garage_custom_component),
                        enabled = enabled,
                        supportingText = { PulseText(stringResource(R.string.pulse_garage_custom_component_hint)) },
                    )
                }
                PulseSelectionField(
                    stringResource(
                        R.string.pulse_garage_work_action,
                    ),
                    garageOption(a.action),
                    garageMaintenanceActions.map {
                        garageOption(it)
                    },
                    {
                        update(a.copy(action = garageMaintenanceActions[it]))
                    },
                    enabled = enabled,
                    labelAbove = true,
                )
                PulseTextField(
                    a.description,
                    { update(a.copy(description = it)) },
                    stringResource(R.string.pulse_garage_work_description),
                    Modifier.fillMaxWidth(),
                    enabled = enabled,
                    singleLine = false,
                    maxLines = 4,
                )
                PulseButton(stringResource(R.string.pulse_garage_remove_work), {
                    change(draft.withMaintenanceActions(actions.filterIndexed { i, _ -> i != index }))
                }, enabled = enabled, variant = PulseButtonVariant.Secondary)
            }
        }
        PulseButton(stringResource(R.string.pulse_garage_add_work), {
            change(draft.withMaintenanceActions(actions + MaintenanceAction("engine_oil", "replaced")))
        }, Modifier.testTag("maintenance-add-work"), enabled = enabled && actions.size < 100, variant = PulseButtonVariant.Secondary)
    }
}
