package dev.abhishekan.pulse.feature.garage.maintenance

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.abhishekan.pulse.core.domain.model.GarageChange
import dev.abhishekan.pulse.core.domain.model.GarageReceipt
import dev.abhishekan.pulse.core.domain.model.VehicleHistory
import dev.abhishekan.pulse.core.ui.PulseTokens
import dev.abhishekan.pulse.core.ui.UiText
import dev.abhishekan.pulse.core.ui.components.PulseEmptyState
import dev.abhishekan.pulse.core.ui.components.PulseText
import dev.abhishekan.pulse.core.ui.components.PulseTextField
import dev.abhishekan.pulse.feature.garage.R
import dev.abhishekan.pulse.feature.garage.editor.GarageDraft

@Composable
internal fun MaintenanceCards(
    data: VehicleHistory,
    vehicle: Long,
    enabled: Boolean,
    busy: Boolean,
    edit: (GarageDraft) -> Unit,
    remove: (GarageChange.Delete, UiText) -> Unit,
    upload: (Long) -> Unit,
    open: (Long, GarageReceipt) -> Unit,
    download: (Long, GarageReceipt) -> Unit,
    uploadRecord: Long?,
    progress: Float?,
) {
    var workQuery by rememberSaveable(vehicle) { mutableStateOf("") }
    PulseTextField(workQuery, { workQuery = it }, stringResource(R.string.pulse_garage_filter_work), Modifier.fillMaxWidth())
    val query = workQuery.trim()
    val matches =
        remember(data.maintenance, query) {
            if (query.isEmpty()) {
                data.maintenance
            } else {
                data.maintenance.filter { record ->
                    record.actions.any { action ->
                        val work = "${action.component} ${action.action} ${action.description}".replace('_', ' ')
                        work.contains(query, ignoreCase = true)
                    }
                }
            }
        }
    if (matches.isEmpty() && workQuery.isNotBlank()) PulseText(stringResource(R.string.pulse_garage_no_matching_work))
    if (data.maintenance.isEmpty()) {
        PulseEmptyState(
            stringResource(R.string.pulse_garage_no_maintenance_yet),
            stringResource(
                R.string.pulse_garage_keep_services_repairs_insurance_washes_and_tyres_together,
            ),
        )
    }
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val columns = if (maxWidth >= 840.dp && LocalDensity.current.fontScale <= 1.3f) 2 else 1
        Column(verticalArrangement = Arrangement.spacedBy(PulseTokens.medium)) {
            matches.chunked(columns).forEach { entries ->
                Row(horizontalArrangement = Arrangement.spacedBy(PulseTokens.medium)) {
                    entries.forEach { item ->
                        key(item.id) {
                            MaintenanceHistoryCard(
                                item,
                                vehicle,
                                enabled,
                                busy,
                                edit,
                                remove,
                                upload,
                                open,
                                download,
                                uploadRecord,
                                progress,
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                    if (entries.size < columns) Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}
