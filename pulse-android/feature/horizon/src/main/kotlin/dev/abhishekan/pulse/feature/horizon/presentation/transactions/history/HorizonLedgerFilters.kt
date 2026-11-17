package dev.abhishekan.pulse.feature.horizon.presentation.transactions.history

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.abhishekan.pulse.core.ui.PulseTheme
import dev.abhishekan.pulse.core.ui.components.PulseBottomSheet
import dev.abhishekan.pulse.core.ui.components.PulseErrorState
import dev.abhishekan.pulse.core.ui.components.PulseFilterChip
import dev.abhishekan.pulse.core.ui.components.PulseLoadingState
import dev.abhishekan.pulse.core.ui.components.PulseRadioButton
import dev.abhishekan.pulse.core.ui.components.PulseTextField
import dev.abhishekan.pulse.core.ui.message
import dev.abhishekan.pulse.feature.horizon.R
import dev.abhishekan.pulse.feature.horizon.presentation.components.categoryCards

@Composable
internal fun LedgerFilters(
    state: TransactionHistoryUiState,
    search: String,
    onAction: (TransactionHistoryAction) -> Unit,
) {
    val categoryId = state.historyCategoryId
    var sheet by rememberSaveable { mutableStateOf<String?>(null) }
    var categorySearch by rememberSaveable { mutableStateOf("") }
    val categories =
        state.overview.value
            ?.categories
            .orEmpty()
    val allCategories = stringResource(R.string.pulse_horizon_ledger_all_categories)
    val categoryLabel =
        categories.find { it.id == categoryId }?.displayName
            ?: if (categoryId == null) allCategories else stringResource(R.string.pulse_horizon_filter_categories)
    FlowRow(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        LedgerFilterPill(
            ledgerTypeLabel(state.historyType),
            state.historyType != null,
            Modifier.testTag("ledger-type-filter"),
            !state.busy,
        ) { sheet = "type" }
        LedgerFilterPill(
            categoryLabel,
            categoryId != null,
            Modifier.testTag("ledger-category-filter"),
            !state.busy,
        ) {
            categorySearch = ""
            sheet = "category"
        }
    }
    if (sheet != null) {
        PulseBottomSheet(
            title =
                stringResource(
                    if (sheet ==
                        "category"
                    ) {
                        R.string.pulse_horizon_filter_categories
                    } else {
                        R.string.pulse_horizon_filter_type
                    },
                ),
            onDismissRequest = {
                sheet = null
            },
            fullScreen = sheet == "category",
        ) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .testTag("ledger-category-sheet")
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (sheet == "category") {
                    PulseTextField(categorySearch, {
                        categorySearch = it
                    }, stringResource(R.string.pulse_horizon_filter_search_categories), Modifier.fillMaxWidth())
                    LazyColumn(
                        Modifier.weight(1f, fill = false),
                        verticalArrangement = Arrangement.spacedBy(PulseTheme.spacing.small),
                    ) {
                        item {
                            PulseRadioButton(categoryId == null, {
                                onAction(TransactionHistoryAction.Filter(search, state.historyType, null))
                                sheet = null
                            }, allCategories, Modifier.fillMaxWidth())
                        }
                        state.overview.error?.let { error ->
                            item { PulseErrorState(error.message, { onAction(TransactionHistoryAction.RetryCategories) }) }
                        }
                        if (state.overview.loading && state.overview.value == null) {
                            item { PulseLoadingState(stringResource(R.string.pulse_horizon_loading_your_horizon)) }
                        }
                        categoryCards(
                            categories,
                            categorySearch,
                            categoryId,
                            testTagPrefix = "ledger-category",
                            showEmpty = state.overview.value != null,
                        ) { id ->
                            onAction(TransactionHistoryAction.Filter(search, state.historyType, id))
                            sheet = null
                        }
                    }
                } else {
                    listOf(null, "debit", "credit").forEach { type ->
                        LedgerSheetOption(ledgerTypeLabel(type), state.historyType == type) {
                            onAction(TransactionHistoryAction.Filter(search, type, categoryId))
                            sheet = null
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ledgerTypeLabel(type: String?): String =
    stringResource(
        when (type) {
            "debit" -> R.string.pulse_horizon_ledger_spending
            "credit" -> R.string.pulse_horizon_ledger_income
            else -> R.string.pulse_horizon_all_types
        },
    )

@Composable
private fun LedgerFilterPill(
    label: String,
    active: Boolean,
    modifier: Modifier,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val foreground = if (active) PulseTheme.colors.onPrimary else PulseTheme.colors.textPrimary
    PulseFilterChip(
        label = label,
        selected = active,
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        showSelectedIcon = false,
        trailingIcon = {
            Canvas(Modifier.size(16.dp)) {
                val tip = Offset(size.width * 0.5f, size.height * 0.65f)
                drawLine(foreground, Offset(size.width * 0.25f, size.height * 0.4f), tip, 1.5.dp.toPx(), StrokeCap.Round)
                drawLine(foreground, tip, Offset(size.width * 0.75f, size.height * 0.4f), 1.5.dp.toPx(), StrokeCap.Round)
            }
        },
    )
}

@Composable
private fun LedgerSheetOption(
    label: String,
    active: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    PulseRadioButton(active, onClick, label, modifier.fillMaxWidth())
}
