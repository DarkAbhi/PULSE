package dev.abhishekan.pulse.feature.horizon

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.abhishekan.pulse.core.ui.PulseTheme
import dev.abhishekan.pulse.core.ui.PulseTokens
import dev.abhishekan.pulse.core.ui.components.PulseIconButton
import dev.abhishekan.pulse.core.ui.components.PulseText

/** Shares the navigation entry's ViewModel with the screen, including its draft safeguards. */
@Composable
fun HorizonTopBarActions(viewModel: HorizonViewModel) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var expanded by remember { mutableStateOf(false) }
    val canCreate = !state.busy && !state.reviewRequired && state.overview.value != null && state.draft == null
    LaunchedEffect(canCreate) {
        if (!canCreate) expanded = false
    }
    Box {
        PulseIconButton(
            stringResource(R.string.pulse_horizon_quick_actions),
            { expanded = true },
            modifier = Modifier.testTag("horizon-quick-actions"),
            enabled = canCreate,
        ) {
            Icon(painterResource(R.drawable.pulse_horizon_more), contentDescription = null)
        }
        DropdownMenu(
            expanded = expanded && canCreate,
            onDismissRequest = { expanded = false },
            modifier = Modifier.testTag("horizon-quick-actions-menu"),
            shape = PulseTheme.shapes.medium,
            containerColor = PulseTheme.colors.surface,
            tonalElevation = PulseTheme.elevation.none,
            shadowElevation = PulseTheme.elevation.low,
        ) {
            val actions =
                listOf(
                    HorizonForm.Transaction to R.string.pulse_horizon_log_transaction,
                    HorizonForm.Deduction to R.string.pulse_horizon_add_deduction,
                    HorizonForm.Purchase to R.string.pulse_horizon_add_planned_purchase,
                    HorizonForm.Subscription to R.string.pulse_horizon_add_subscription,
                    HorizonForm.Budget to R.string.pulse_horizon_add_monthly_budget,
                    HorizonForm.Category to R.string.pulse_horizon_add_category_tag,
                )
            actions.forEach { (form, label) ->
                if (form == HorizonForm.Budget) HorizontalDivider(color = PulseTheme.colors.border)
                DropdownMenuItem(
                    text = { PulseText(stringResource(label)) },
                    onClick = {
                        expanded = false
                        if (canCreate) viewModel.newForm(form)
                    },
                    modifier = Modifier.heightIn(min = PulseTokens.target),
                )
            }
        }
    }
}
