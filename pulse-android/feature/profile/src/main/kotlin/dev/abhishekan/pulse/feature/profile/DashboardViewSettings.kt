package dev.abhishekan.pulse.feature.profile

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.abhishekan.pulse.core.domain.model.Failure
import dev.abhishekan.pulse.core.domain.model.Outcome
import dev.abhishekan.pulse.core.domain.repository.DashboardPreferencesRepository
import dev.abhishekan.pulse.core.ui.PulseTheme
import dev.abhishekan.pulse.core.ui.components.PulseCard
import dev.abhishekan.pulse.core.ui.components.PulseRadioButton
import dev.abhishekan.pulse.core.ui.components.PulseText
import dev.abhishekan.pulse.core.ui.components.PulseTextStyle
import dev.abhishekan.pulse.core.ui.message
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

@Composable
internal fun DashboardViewSettings() {
    val preferences = koinInject<DashboardPreferencesRepository>()
    val compact by preferences.compact.collectAsStateWithLifecycle(false)
    var saving by remember { mutableStateOf(false) }
    var failure by remember { mutableStateOf<Failure?>(null) }
    val scope = rememberCoroutineScope()
    PulseCard(Modifier.fillMaxWidth()) {
        PulseText(stringResource(R.string.pulse_profile_dashboard_view), style = PulseTextStyle.Title)
        Column(Modifier.selectableGroup()) {
            listOf(false, true).forEach { option ->
                PulseRadioButton(
                    compact == option,
                    {
                        if (!saving && compact != option) {
                            saving = true
                            scope.launch {
                                val result = preferences.setCompact(option)
                                failure = (result as? Outcome.Error)?.failure
                                saving = false
                            }
                        }
                    },
                    stringResource(
                        if (option) R.string.pulse_profile_dashboard_compact else R.string.pulse_profile_dashboard_large,
                    ),
                    Modifier.fillMaxWidth(),
                    enabled = !saving,
                )
            }
        }
        failure?.let { PulseText(it.message, color = PulseTheme.colors.error) }
    }
}
