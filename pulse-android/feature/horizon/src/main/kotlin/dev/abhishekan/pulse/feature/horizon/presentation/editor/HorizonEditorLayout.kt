package dev.abhishekan.pulse.feature.horizon.presentation.editor

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import dev.abhishekan.pulse.core.ui.LocalPulseContentPadding
import dev.abhishekan.pulse.core.ui.PulseAdaptiveLayout
import dev.abhishekan.pulse.core.ui.PulseTokens
import dev.abhishekan.pulse.core.ui.PulseWindowSize
import dev.abhishekan.pulse.core.ui.pagePadding
import dev.abhishekan.pulse.core.ui.patterns.PulseFormAction
import dev.abhishekan.pulse.core.ui.patterns.PulseFormActions
import dev.abhishekan.pulse.core.ui.patterns.PulseFormLayout
import dev.abhishekan.pulse.core.ui.patterns.PulseScreenScaffold
import dev.abhishekan.pulse.core.ui.R as CoreUiR

@Composable
internal fun HorizonEditorScaffold(
    title: String,
    backLabel: String,
    busy: Boolean,
    actions: @Composable RowScope.() -> Unit = {},
    content: @Composable (PulseWindowSize) -> Unit,
) {
    PulseScreenScaffold(title, backLabel = backLabel, backEnabled = !busy, actions = actions) {
        PulseAdaptiveLayout(Modifier.fillMaxSize()) { width ->
            Column(
                Modifier
                    .widthIn(max = PulseTokens.contentWidth)
                    .fillMaxWidth()
                    .padding(top = LocalPulseContentPadding.current.calculateTopPadding())
                    .imePadding(),
            ) {
                content(width)
            }
        }
    }
}

@Composable
internal fun HorizonEditorForm(
    width: PulseWindowSize,
    busy: Boolean,
    canSave: Boolean,
    footerWidth: Dp,
    onSave: () -> Unit,
    onCancel: () -> Unit,
    content: @Composable () -> Unit,
) {
    val pagePadding = width.pagePadding
    val bottomPadding = LocalPulseContentPadding.current.calculateBottomPadding()
    val keyboard = LocalSoftwareKeyboardController.current
    PulseFormLayout(
        modifier = Modifier.padding(bottom = bottomPadding),
        scrollContentModifier = Modifier.padding(pagePadding),
        footer = {
            PulseFormActions(
                primary =
                    PulseFormAction(
                        stringResource(CoreUiR.string.pulse_ui_save),
                        {
                            keyboard?.hide()
                            onSave()
                        },
                        enabled = canSave,
                        loading = busy,
                    ),
                secondary = PulseFormAction(stringResource(CoreUiR.string.pulse_ui_cancel), onCancel, enabled = !busy),
                modifier =
                    Modifier
                        .align(Alignment.Start)
                        .padding(horizontal = pagePadding)
                        .widthIn(max = footerWidth)
                        .testTag("horizon-editor-actions"),
            )
        },
    ) {
        content()
    }
}
