package dev.abhishekan.pulse.feature.horizon.presentation.components

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import dev.abhishekan.pulse.core.domain.model.HorizonCategory
import dev.abhishekan.pulse.core.ui.PulseTheme
import dev.abhishekan.pulse.core.ui.components.PulseBottomSheet
import dev.abhishekan.pulse.core.ui.components.PulseRadioButton
import dev.abhishekan.pulse.core.ui.components.PulseTextField
import dev.abhishekan.pulse.feature.horizon.R
import dev.abhishekan.pulse.feature.horizon.presentation.editor.TransactionPickerField

@Composable
internal fun HorizonCategoryPicker(
    @StringRes label: Int,
    selectedId: Long?,
    categories: List<HorizonCategory>,
    enabled: Boolean,
    @StringRes emptyChoice: Int = R.string.pulse_horizon_none,
    onSelect: (Long?) -> Unit,
) {
    var open by rememberSaveable { mutableStateOf(false) }
    var search by rememberSaveable { mutableStateOf("") }
    val focus = LocalFocusManager.current
    val none = stringResource(emptyChoice)
    val selected =
        if (selectedId == null) {
            none
        } else {
            categories.find { it.id == selectedId }?.displayName ?: stringResource(R.string.pulse_horizon_unavailable_selection)
        }
    TransactionPickerField(label, selected, enabled) {
        focus.clearFocus()
        search = ""
        open = true
    }
    if (open && enabled) {
        val choose: (Long?) -> Unit = {
            open = false
            onSelect(it)
        }
        val spacing = PulseTheme.spacing
        PulseBottomSheet(stringResource(label), { open = false }, fullScreen = true) {
            Column(
                Modifier.fillMaxWidth().padding(horizontal = spacing.medium),
                verticalArrangement = Arrangement.spacedBy(spacing.compact),
            ) {
                PulseTextField(
                    search,
                    { search = it },
                    stringResource(R.string.pulse_horizon_filter_search_categories),
                    Modifier.fillMaxWidth(),
                )
                LazyColumn(
                    Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(spacing.small),
                ) {
                    item { PulseRadioButton(selectedId == null, { choose(null) }, none, Modifier.fillMaxWidth()) }
                    categoryCards(categories, search, selectedId, onSelect = choose)
                }
            }
        }
    }
}
