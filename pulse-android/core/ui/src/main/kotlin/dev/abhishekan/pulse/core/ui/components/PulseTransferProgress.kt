package dev.abhishekan.pulse.core.ui.components
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import dev.abhishekan.pulse.core.ui.PulseTheme
import dev.abhishekan.pulse.core.ui.PulseTokens
import dev.abhishekan.pulse.core.ui.R

@Composable
fun PulseTransferProgress(
    progress: Float,
    modifier: Modifier = Modifier,
) {
    val fraction = progress.coerceIn(0f, 1f)
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(PulseTokens.small), verticalAlignment = Alignment.CenterVertically) {
        CircularProgressIndicator(
            progress = { fraction },
            modifier = Modifier.size(PulseTokens.large),
            color = PulseTheme.colors.action,
            trackColor = PulseTheme.colors.surfaceStrong,
        )
        PulseText(stringResource(R.string.pulse_ui_uploading_receipt_1_s, (fraction * 100).toInt()))
    }
}
