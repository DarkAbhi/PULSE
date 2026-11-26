package dev.abhishekan.pulse.feature.garage.shared

import androidx.compose.runtime.Composable
import dev.abhishekan.pulse.core.ui.message
import dev.abhishekan.pulse.core.ui.patterns.PulseErrorState

@Composable
internal fun <T> GarageSectionContent(
    section: GarageSection<T>,
    retry: () -> Unit,
    content: @Composable (T) -> Unit,
) {
    section.error?.let { PulseErrorState(it.message, retry) }
    section.value?.let { content(it) }
}
