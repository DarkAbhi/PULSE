package dev.abhishekan.pulse.feature.horizon

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.abhishekan.pulse.core.domain.model.HorizonChange
import dev.abhishekan.pulse.core.domain.model.HorizonRecord
import dev.abhishekan.pulse.core.ui.PulseTheme
import dev.abhishekan.pulse.core.ui.PulseTokens
import dev.abhishekan.pulse.core.ui.components.PulseButton
import dev.abhishekan.pulse.core.ui.components.PulseButtonStyle
import dev.abhishekan.pulse.core.ui.components.PulseDialog
import dev.abhishekan.pulse.core.ui.components.PulseDropdownMenu
import dev.abhishekan.pulse.core.ui.components.PulseDropdownMenuItem
import dev.abhishekan.pulse.core.ui.components.PulseHorizontalDivider
import dev.abhishekan.pulse.core.ui.components.PulseText

/** Shares the navigation entry's ViewModel with the screen, including its draft safeguards. */
@Composable
fun HorizonTopBarActions(viewModel: HorizonViewModel) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    if (state.draft?.kind == HorizonForm.Purchase) return
    if (state.draft?.kind == HorizonForm.Subscription) return
    if (state.draft?.kind == HorizonForm.Deduction) {
        state.draft?.id?.let { ObligationTopBarActions(state, viewModel, it) }
        return
    }
    var expanded by remember { mutableStateOf(false) }
    val canCreate = !state.busy && !state.reviewRequired && state.overview.value != null && state.draft == null
    LaunchedEffect(canCreate) {
        if (!canCreate) expanded = false
    }
    Box {
        PulseButton(
            stringResource(R.string.pulse_horizon_quick_actions),
            { expanded = true },
            modifier = Modifier.testTag("horizon-quick-actions"),
            enabled = canCreate,
            style = PulseButtonStyle.Icon,
        ) {
            Icon(painterResource(R.drawable.pulse_horizon_more), contentDescription = null)
        }
        PulseDropdownMenu(
            expanded = expanded && canCreate,
            onDismissRequest = { expanded = false },
            modifier = Modifier.testTag("horizon-quick-actions-menu"),
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
                if (form == HorizonForm.Budget) PulseHorizontalDivider(color = PulseTheme.colors.border)
                PulseDropdownMenuItem(
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

@Composable
private fun ObligationTopBarActions(
    state: HorizonUiState,
    viewModel: HorizonViewModel,
    id: Long,
) {
    var expanded by remember(id) { mutableStateOf(false) }
    var confirming by rememberSaveable(id) { mutableStateOf(false) }
    val enabled = !state.busy && !state.reviewRequired && state.overview.value != null
    Box {
        PulseButton(
            stringResource(R.string.pulse_horizon_obligation_more_actions),
            { expanded = true },
            enabled = enabled,
            style = PulseButtonStyle.Icon,
        ) { Icon(painterResource(R.drawable.pulse_horizon_more), null) }
        PulseDropdownMenu(expanded && enabled, { expanded = false }) {
            PulseDropdownMenuItem(
                text = { PulseText(stringResource(R.string.pulse_horizon_obligation_delete), color = PulseTheme.colors.error) },
                onClick = {
                    expanded = false
                    confirming = true
                },
            )
        }
    }
    if (confirming) {
        PulseDialog(
            stringResource(R.string.pulse_horizon_confirm_deletion),
            stringResource(
                R.string.pulse_horizon_delete_1_s_this_removes_the_fixed_obligation_and,
                state.overview.value
                    ?.deductions
                    ?.find { it.id == id }
                    ?.name ?: state.draft?.name.orEmpty(),
            ),
            stringResource(R.string.pulse_horizon_delete),
            {
                confirming = false
                if (enabled) viewModel.mutate(HorizonChange.Delete(HorizonRecord.Deduction, id), closeDraft = true)
            },
            { confirming = false },
        )
    }
}
