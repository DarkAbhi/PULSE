package dev.abhishekan.pulse.feature.horizon

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.abhishekan.pulse.core.domain.model.HorizonCategory
import dev.abhishekan.pulse.core.ui.PulseTheme
import dev.abhishekan.pulse.core.ui.components.PulseButton
import dev.abhishekan.pulse.core.ui.components.PulseButtonVariant
import dev.abhishekan.pulse.core.ui.components.PulseCheckbox
import dev.abhishekan.pulse.core.ui.components.PulseFilterChip
import dev.abhishekan.pulse.core.ui.components.PulseRadioButton
import dev.abhishekan.pulse.core.ui.components.PulseText
import dev.abhishekan.pulse.core.ui.components.PulseTextField
import dev.abhishekan.pulse.core.ui.components.PulseTextStyle
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun LedgerFilters(
    state: HorizonUiState,
    vm: HorizonViewModel,
    categoryId: Long?,
    onCategory: (Long?) -> Unit,
    canMutate: Boolean,
    addCategory: () -> Unit,
    logTransaction: () -> Unit,
) {
    var search by rememberSaveable { mutableStateOf(state.search) }
    var sheet by rememberSaveable { mutableStateOf<String?>(null) }
    var categorySearch by rememberSaveable { mutableStateOf("") }
    val categories =
        state.overview.value
            ?.categories
            .orEmpty()
            .ifEmpty {
                state.transactions.value?.items.orEmpty().filter { it.categoryId != null }.distinctBy { it.categoryId }.map {
                    HorizonCategory(requireNotNull(it.categoryId), it.categoryName, "", "", false)
                }
            }.sortedBy { it.name.lowercase() }
    val allCategories = stringResource(R.string.pulse_horizon_ledger_all_categories)
    val categoryLabel = categories.find { it.id == categoryId }?.name ?: allCategories
    val typeLabel = ledgerTypeLabel(state.type)
    LaunchedEffect(search, state.type, state.busy) {
        if (search != state.search && !state.busy) {
            delay(350)
            vm.filter(search, state.type)
        }
    }
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val wide = maxWidth >= 720.dp * LocalDensity.current.fontScale.coerceAtLeast(1f)
        val actions: @Composable (Modifier) -> Unit = { actionModifier ->
            PulseButton(
                stringResource(R.string.pulse_horizon_category_type_action),
                addCategory,
                modifier = actionModifier,
                enabled = canMutate,
                variant = PulseButtonVariant.Secondary,
            )
            PulseButton(stringResource(R.string.pulse_horizon_log_transaction), logTransaction, modifier = actionModifier, enabled = canMutate)
        }
        if (wide) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                PulseTextField(search, { search = it }, stringResource(R.string.pulse_horizon_search_transactions), Modifier.weight(1f), enabled = !state.busy)
                actions(Modifier.offset(y = 4.dp))
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                PulseTextField(search, { search = it }, stringResource(R.string.pulse_horizon_search_transactions), Modifier.fillMaxWidth(), enabled = !state.busy)
                FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    actions(Modifier)
                }
            }
        }
    }
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val tablet = maxWidth >= 900.dp * LocalDensity.current.fontScale.coerceAtLeast(1f)
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (tablet) {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    listOf(null, "debit", "credit").forEach { type ->
                        LedgerFilterPill(
                            ledgerTypeLabel(type),
                            state.type == type,
                            enabled = !state.busy,
                            onClick = { vm.filter(search, type) },
                        )
                    }
                }
                Box(Modifier.padding(horizontal = 4.dp).size(1.dp, 24.dp).background(PulseTheme.colors.border))
                Row(Modifier.weight(1f).horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    LedgerFilterPill(allCategories, categoryId == null, enabled = !state.busy, onClick = { onCategory(null) })
                    categories.take(4).forEach { category ->
                        LedgerFilterPill(
                            category.name,
                            categoryId == category.id,
                            color = category.filterColor(),
                            enabled = !state.busy,
                            onClick = {
                                onCategory(category.id)
                            },
                        )
                    }
                }
                LedgerFilterPill(
                    if (categoryId != null &&
                        categories.take(4).none { it.id == categoryId }
                    ) {
                        categoryLabel
                    } else {
                        stringResource(R.string.pulse_horizon_filter_more)
                    },
                    categoryId != null && categories.take(4).none { it.id == categoryId },
                    Modifier.testTag("ledger-category-more"),
                    dropdown = true,
                    enabled = !state.busy,
                    onClick = {
                        categorySearch = ""
                        sheet = "category"
                    },
                )
            } else {
                LedgerFilterPill(
                    typeLabel,
                    true,
                    Modifier
                        .weight(
                            1f,
                        ).testTag("ledger-type-filter"),
                    dropdown = true,
                    enabled = !state.busy,
                    onClick = {
                        sheet =
                            "type"
                    },
                )
                LedgerFilterPill(
                    categoryLabel,
                    categoryId != null,
                    Modifier
                        .weight(
                            1.3f,
                        ).testTag("ledger-category-filter"),
                    dropdown = true,
                    enabled = !state.busy,
                    onClick = {
                        categorySearch =
                            ""

                        sheet = "category"
                    },
                )
            }
        }
    }
    if (sheet != null) {
        ModalBottomSheet(onDismissRequest = {
            sheet = null
        }, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true), containerColor = PulseTheme.colors.surface) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .testTag("ledger-category-sheet")
                    .heightIn(max = 640.dp)
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                PulseText(
                    stringResource(
                        if (sheet ==
                            "category"
                        ) {
                            R.string.pulse_horizon_filter_categories
                        } else {
                            R.string.pulse_horizon_filter_type
                        },
                    ),
                    style = PulseTextStyle.Title,
                )
                if (sheet == "category") {
                    PulseTextField(categorySearch, {
                        categorySearch = it
                    }, stringResource(R.string.pulse_horizon_filter_search_categories), Modifier.fillMaxWidth())
                    LazyColumn(
                        Modifier.weight(1f, fill = false),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        item {
                            PulseCheckbox(categoryId == null, {
                                onCategory(null)
                                sheet = null
                            }, allCategories, Modifier.fillMaxWidth())
                        }
                        val matches = categories.filter { it.name.contains(categorySearch.trim(), ignoreCase = true) }
                        items(matches, key = { it.id }) { category ->
                            PulseCheckbox(
                                categoryId == category.id,
                                {
                                    onCategory(category.id)
                                    sheet = null
                                },
                                category.name,
                                Modifier.fillMaxWidth().testTag("ledger-category-option-${category.id}"),
                            )
                        }
                        if (matches.isEmpty()) {
                            item {
                                PulseText(
                                    stringResource(R.string.pulse_horizon_filter_no_categories),
                                    Modifier.padding(vertical = 16.dp),
                                    color = PulseTheme.colors.textSecondary,
                                )
                            }
                        }
                    }
                } else {
                    listOf(null, "debit", "credit").forEach { type ->
                        LedgerSheetOption(ledgerTypeLabel(type), state.type == type) {
                            vm.filter(search, type)
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
            "debit" -> R.string.pulse_horizon_debit
            "credit" -> R.string.pulse_horizon_credit
            else -> R.string.pulse_horizon_all_types
        },
    )

@Composable
private fun HorizonCategory.filterColor(): Color =
    runCatching {
        Color(android.graphics.Color.parseColor(color))
    }.getOrDefault(PulseTheme.colors.action)

@Composable
private fun LedgerFilterPill(
    label: String,
    active: Boolean,
    modifier: Modifier = Modifier,
    dropdown: Boolean = false,
    color: Color? = null,
    enabled: Boolean = true,
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
        leadingIcon = color?.let { { Box(Modifier.size(8.dp).background(it, CircleShape)) } },
        trailingIcon =
            if (dropdown) {
                {
                    Canvas(Modifier.size(16.dp)) {
                        drawLine(
                            foreground,
                            Offset(size.width * 0.25f, size.height * 0.4f),
                            Offset(size.width * 0.5f, size.height * 0.65f),
                            1.5.dp.toPx(),
                            StrokeCap.Round,
                        )
                        drawLine(
                            foreground,
                            Offset(size.width * 0.5f, size.height * 0.65f),
                            Offset(size.width * 0.75f, size.height * 0.4f),
                            1.5.dp.toPx(),
                            StrokeCap.Round,
                        )
                    }
                }
            } else {
                null
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
