package dev.abhishekan.pulse.core.ui.components

import androidx.compose.ui.res.stringResource
import dev.abhishekan.pulse.core.ui.R

import androidx.compose.foundation.layout.*
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import dev.abhishekan.pulse.core.ui.PulseTokens

@Composable
fun PulseTransferProgress(progress: Float, modifier: Modifier = Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(PulseTokens.small)) {
        PulseText(stringResource(R.string.pulse_ui_uploading_receipt_1_s , (progress * 100).toInt()))
        LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth())
    }
}
