package dev.abhishekan.pulse.core.ui.components
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.Composable
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
    Column(modifier, verticalArrangement = Arrangement.spacedBy(PulseTokens.small)) {
        PulseText(stringResource(R.string.pulse_ui_uploading_receipt_1_s, (progress * 100).toInt()))
        LinearProgressIndicator(progress = {
            progress
        }, modifier = Modifier.fillMaxWidth(), color = PulseTheme.colors.action, trackColor = PulseTheme.colors.surfaceStrong)
    }
}
