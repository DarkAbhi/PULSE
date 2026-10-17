package dev.abhishekan.pulse.core.ui.components

import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarData
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun PulseSnackbar(
    data: SnackbarData,
    modifier: Modifier = Modifier,
) {
    Snackbar(data, modifier)
}
