package dev.abhishekan.pulse.core.ui.components

import dev.abhishekan.pulse.core.ui.R

import dev.abhishekan.pulse.core.ui.uiText

import androidx.compose.foundation.layout.*
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import dev.abhishekan.pulse.core.ui.PulseTokens

@Composable
fun PulseTransferProgress(progress: Float, modifier: Modifier = Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(PulseTokens.small)) {
        PulseText(uiText(R.string.pulse_ui_uploading_receipt_1_s , (progress * 100).toInt()))
        LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth())
    }
}
