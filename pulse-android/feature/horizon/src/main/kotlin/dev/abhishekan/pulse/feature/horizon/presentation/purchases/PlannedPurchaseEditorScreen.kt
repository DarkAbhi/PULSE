package dev.abhishekan.pulse.feature.horizon.presentation.purchases

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import dev.abhishekan.pulse.core.ui.LocalPulseContentPadding
import dev.abhishekan.pulse.core.ui.pagePadding
import dev.abhishekan.pulse.feature.horizon.presentation.editor.HorizonEditorAction
import dev.abhishekan.pulse.feature.horizon.presentation.editor.HorizonEditorScaffold
import dev.abhishekan.pulse.feature.horizon.presentation.editor.HorizonEditorState
import dev.abhishekan.pulse.feature.horizon.presentation.editor.topBarTitle

@Composable
internal fun PlannedPurchaseEditorScreen(
    state: HorizonEditorState,
    backLabel: String,
    onAction: (HorizonEditorAction) -> Unit,
    notice: @Composable () -> Unit,
) {
    HorizonEditorScaffold(
        title = stringResource(state.draft.topBarTitle()),
        backLabel = backLabel,
        busy = state.busy,
    ) { width ->
        val bottom = LocalPulseContentPadding.current.calculateBottomPadding()
        Box(Modifier.fillMaxSize().padding(bottom = bottom)) {
            PlannedPurchaseEditor(
                state.draft,
                state.data,
                state.busy,
                state.error,
                state.reviewRequired,
                { onAction(HorizonEditorAction.Change(it)) },
                { onAction(HorizonEditorAction.Save) },
                { onAction(HorizonEditorAction.Cancel) },
                width.pagePadding,
                notice,
            )
        }
    }
}
