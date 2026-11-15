package dev.abhishekan.pulse.feature.horizon

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import dev.abhishekan.pulse.core.domain.model.HorizonOverview
import dev.abhishekan.pulse.core.ui.LocalPulseContentPadding
import dev.abhishekan.pulse.core.ui.UiText
import dev.abhishekan.pulse.core.ui.pagePadding

@Composable
internal fun PlannedPurchaseEditorScreen(
    draft: HorizonDraft,
    busy: Boolean,
    error: UiText?,
    backLabel: String,
    onChange: (HorizonDraft) -> Unit,
    onSave: () -> Unit,
    onCancel: () -> Unit,
    notice: @Composable () -> Unit,
    data: HorizonOverview?,
    reviewRequired: Boolean,
) {
    HorizonEditorScaffold(
        title = stringResource(draft.topBarTitle()),
        backLabel = backLabel,
        busy = busy,
    ) { width ->
        val bottom = LocalPulseContentPadding.current.calculateBottomPadding()
        Box(Modifier.fillMaxSize().padding(bottom = bottom)) {
            PlannedPurchaseEditor(draft, data, busy, error, reviewRequired, onChange, onSave, onCancel, width.pagePadding, notice)
        }
    }
}
