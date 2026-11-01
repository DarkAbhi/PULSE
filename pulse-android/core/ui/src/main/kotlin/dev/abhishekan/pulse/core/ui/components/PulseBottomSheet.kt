package dev.abhishekan.pulse.core.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import dev.abhishekan.pulse.core.ui.PulseTheme
import dev.abhishekan.pulse.core.ui.PulseTokens

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
    ) {
        // A full-height surface keeps the half-height anchor available even for short content.
        Column(Modifier.fillMaxWidth().fillMaxHeight()) {
            PulseText(
                title,
                Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = PulseTheme.spacing.medium,
                        vertical = PulseTheme.spacing.compact,
                    ).semantics { heading() },
                PulseTextStyle.Title3,
                textAlign = TextAlign.Center,
            )
            Column(Modifier.weight(1f).fillMaxWidth(), content = content)
        }
    }
}
