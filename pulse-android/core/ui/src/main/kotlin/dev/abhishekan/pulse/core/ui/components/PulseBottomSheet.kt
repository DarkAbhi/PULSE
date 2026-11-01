package dev.abhishekan.pulse.core.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import dev.abhishekan.pulse.core.ui.PulseTheme
import dev.abhishekan.pulse.core.ui.PulseTokens
import dev.abhishekan.pulse.core.ui.R

/** Opens halfway unless [fullScreen] is needed; content owns its scrolling. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PulseBottomSheet(
    title: String,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    fullScreen: Boolean = false,
    content: @Composable ColumnScope.() -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        modifier = modifier,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = fullScreen),
        sheetMaxWidth = PulseTokens.bottomSheetMaxWidth,
        containerColor = PulseTheme.colors.surface,
        contentColor = PulseTheme.colors.textPrimary,
        dragHandle = { BottomSheetDefaults.DragHandle(color = PulseTheme.colors.textSecondary) },
    ) {
        // A full-height surface keeps the half-height anchor available even for short content.
        Column(Modifier.fillMaxWidth().fillMaxHeight()) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = PulseTheme.spacing.medium),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                PulseText(
                    title,
                    Modifier.weight(1f).semantics { heading() },
                    PulseTextStyle.Title3,
                )
                PulseButton(
                    stringResource(R.string.pulse_ui_close_sheet),
                    onDismissRequest,
                    style = PulseButtonStyle.Icon,
                ) {
                    Icon(painterResource(R.drawable.pulse_close), null, Modifier.size(PulseTheme.spacing.cardPadding))
                }
            }
            Column(Modifier.weight(1f).fillMaxWidth(), content = content)
        }
    }
}
