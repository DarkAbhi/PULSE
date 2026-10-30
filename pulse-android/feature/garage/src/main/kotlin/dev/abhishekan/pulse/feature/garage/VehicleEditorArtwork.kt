package dev.abhishekan.pulse.feature.garage

import android.animation.ValueAnimator
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.delay

@Composable
internal fun VehicleEditorArtwork(vehicleType: String, modifier: Modifier = Modifier) {
    val artwork = listOf(R.drawable.garage_scooter, R.drawable.garage_motorcycle, R.drawable.garage_car)
    val selectedArtwork = when (vehicleType) {
        "two_wheeler" -> R.drawable.garage_motorcycle
        "four_wheeler" -> R.drawable.garage_car
        else -> null
    }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val index by produceState(0, lifecycle, selectedArtwork) {
        if (selectedArtwork != null) return@produceState
        lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            if (ValueAnimator.areAnimatorsEnabled()) {
                while (true) {
                    delay(8_000)
                    value = (value + 1) % artwork.size
                }
            }
        }
    }
    Crossfade(
        targetState = selectedArtwork ?: artwork[index],
        modifier = modifier.height(280.dp).testTag("vehicle-editor-artwork"),
        animationSpec = tween(2_400, easing = LinearEasing),
        label = "Vehicle artwork",
    ) { resource ->
        Image(
            painterResource(resource),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Fit,
        )
    }
}
