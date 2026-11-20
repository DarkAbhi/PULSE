package dev.abhishekan.pulse.feature.horizon.presentation.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.abhishekan.pulse.core.domain.model.HorizonCategory
import dev.abhishekan.pulse.core.ui.PulseTheme
import dev.abhishekan.pulse.core.ui.components.PulseCard
import dev.abhishekan.pulse.core.ui.components.PulseRadioButton
import dev.abhishekan.pulse.core.ui.components.PulseText
import dev.abhishekan.pulse.core.ui.components.PulseTextStyle
import dev.abhishekan.pulse.feature.horizon.R

internal fun LazyListScope.categoryCards(
    categories: List<HorizonCategory>,
    search: String,
    selectedId: Long?,
    testTagPrefix: String = "horizon-category",
    showEmpty: Boolean = true,
    onSelect: (Long) -> Unit,
) {
    val query = search.trim()
    val categoriesByParent = categories.groupBy(HorizonCategory::parentId)
    val (parents, standalone) = categoriesByParent[null].orEmpty().partition { it.id in categoriesByParent }
    val groups =
        (parents + standalone).mapNotNull { parent ->
            val children = categoriesByParent[parent.id].orEmpty()
            val parentMatches = parent.name.contains(query, ignoreCase = true)
            val matches = if (parentMatches) children else children.filter { it.displayName.contains(query, ignoreCase = true) }
            (parent to matches).takeIf { parentMatches || matches.isNotEmpty() }
        }
    items(groups, key = { (parent, _) -> parent.id }, contentType = { "category-card" }) { (parent, children) ->
        CategoryCard(parent, children, selectedId, testTagPrefix, onSelect)
    }
    if (groups.isEmpty() && showEmpty) {
        item {
            PulseText(
                stringResource(R.string.pulse_horizon_filter_no_categories),
                Modifier.padding(vertical = PulseTheme.spacing.medium),
                color = PulseTheme.colors.textSecondary,
            )
        }
    }
}

@Composable
private fun CategoryCard(
    parent: HorizonCategory,
    children: List<HorizonCategory>,
    selectedId: Long?,
    testTagPrefix: String,
    onSelect: (Long) -> Unit,
) {
    val spacing = PulseTheme.spacing
    val colors = PulseTheme.colors
    val selectedChildIndex = children.indexOfFirst { it.id == selectedId }
    val selected = selectedId == parent.id || selectedChildIndex >= 0
    PulseCard(
        Modifier.fillMaxWidth(),
        border = BorderStroke(1.dp, if (selected) colors.action else colors.border),
        contentPadding = spacing.small,
    ) {
        PulseRadioButton(
            selectedId == parent.id,
            { onSelect(parent.id) },
            parent.name,
            Modifier.fillMaxWidth().testTag("$testTagPrefix-option-${parent.id}"),
        )
        if (children.isNotEmpty()) {
            key(children) {
                LazyRow(
                    modifier = Modifier.fillMaxWidth().testTag("$testTagPrefix-subcategories-${parent.id}").selectableGroup(),
                    state = rememberLazyListState(initialFirstVisibleItemIndex = selectedChildIndex.coerceAtLeast(0)),
                    horizontalArrangement = Arrangement.spacedBy(spacing.tiny),
                ) {
                    items(children, key = { it.id }, contentType = { "subcategory" }) { child ->
                        SubcategoryTile(
                            child.name,
                            child.id == selectedId,
                            Modifier.testTag("$testTagPrefix-option-${child.id}"),
                        ) { onSelect(child.id) }
                    }
                }
            }
        }
    }
}

@Composable
private fun SubcategoryTile(
    name: String,
    selected: Boolean,
    modifier: Modifier,
    onClick: () -> Unit,
) {
    val spacing = PulseTheme.spacing
    val colors = PulseTheme.colors
    val foreground = if (selected) colors.onPrimarySoft else colors.textPrimary
    Column(
        modifier
            .width(spacing.spacious + spacing.medium)
            .defaultMinSize(minHeight = spacing.spacious)
            .clip(PulseTheme.shapes.medium)
            .background(if (selected) colors.primarySoft else colors.surface)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .padding(horizontal = spacing.tiny, vertical = spacing.small),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(spacing.tiny),
    ) {
        Icon(
            painterResource(R.drawable.pulse_obligation_tag),
            contentDescription = null,
            modifier = Modifier.size(spacing.cardPadding),
            tint = if (selected) foreground else colors.textSecondary,
        )
        PulseText(
            name,
            style = PulseTextStyle.Body2,
            color = foreground,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
