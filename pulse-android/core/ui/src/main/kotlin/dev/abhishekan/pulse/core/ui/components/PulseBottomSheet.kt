package dev.abhishekan.pulse.core.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetValue
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalWindowInfo
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
    expandOnOverflow: Boolean = false,
    content: @Composable ColumnScope.() -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = fullScreen)
    val windowHeight = LocalWindowInfo.current.containerSize.height
    var viewportHeight by remember { mutableIntStateOf(0) }
    var contentHeight by remember { mutableIntStateOf(0) }
    var checked by remember { mutableStateOf(false) }
    LaunchedEffect(expandOnOverflow, viewportHeight, contentHeight, sheetState.currentValue) {
        if (expandOnOverflow && !checked && viewportHeight > 0 && contentHeight > 0 && sheetState.currentValue != SheetValue.Hidden) {
            checked = true
            if (contentHeight > viewportHeight - windowHeight / 2) sheetState.expand()
        }
    }
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        modifier = modifier,
        sheetState = sheetState,
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
            if (expandOnOverflow) {
                Column(
                    Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .onSizeChanged { viewportHeight = it.height }
                        .verticalScroll(rememberScrollState()),
                ) {
                    Column(Modifier.fillMaxWidth().onSizeChanged { contentHeight = it.height }, content = content)
                }
            } else {
                Column(Modifier.weight(1f).fillMaxWidth(), content = content)
            }
        }
    }
}
