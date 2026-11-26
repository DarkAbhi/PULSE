package dev.abhishekan.pulse.feature.horizon.presentation.subscriptions

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import dev.abhishekan.pulse.core.ui.PulseTokens
import dev.abhishekan.pulse.feature.horizon.presentation.editor.HorizonEditorAction
import dev.abhishekan.pulse.feature.horizon.presentation.editor.HorizonEditorForm
import dev.abhishekan.pulse.feature.horizon.presentation.editor.HorizonEditorScaffold
import dev.abhishekan.pulse.feature.horizon.presentation.editor.HorizonEditorState
import dev.abhishekan.pulse.feature.horizon.presentation.editor.topBarTitle

@Composable
internal fun SubscriptionEditorScreen(
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
        HorizonEditorForm(
            width,
            state.busy,
            state.canSave,
            PulseTokens.contentWidth,
            { onAction(HorizonEditorAction.Save) },
            { onAction(HorizonEditorAction.Cancel) },
        ) {
            notice()
            SubscriptionEditor(state.draft, state.data, state.busy, state.error) { onAction(HorizonEditorAction.Change(it)) }
        }
    }
}
