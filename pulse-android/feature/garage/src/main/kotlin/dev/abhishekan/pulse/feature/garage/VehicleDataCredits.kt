package dev.abhishekan.pulse.feature.garage

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withLink
import dev.abhishekan.pulse.core.ui.PulseTheme
import dev.abhishekan.pulse.core.ui.components.PulseDialog
import dev.abhishekan.pulse.core.ui.components.PulseText

@Composable
internal fun VehicleDataCredits(
    modifier: Modifier = Modifier,
    textAlign: TextAlign = TextAlign.Start,
) {
    var showCredits by remember { mutableStateOf(false) }
    val color = PulseTheme.colors.textSecondary
    val linkStyles = TextLinkStyles(style = SpanStyle(color = color))
    val providerLabel = stringResource(R.string.pulse_garage_vehicle_data_credit)
    val sourcesLabel = stringResource(R.string.pulse_garage_data_sources)
    val attribution =
        buildAnnotatedString {
            withLink(LinkAnnotation.Url("https://vehiclesdb.com", linkStyles)) {
                append(providerLabel)
            }
            append(" · ")
            withLink(LinkAnnotation.Clickable("vehicle-data-credits", linkStyles) { showCredits = true }) {
                append(sourcesLabel)
            }
        }
    PulseText(attribution, modifier, color = color, textAlign = textAlign)
    if (showCredits) {
        val resources = LocalContext.current.resources
        val notices =
            remember(resources) {
                resources
                    .openRawResource(R.raw.vehiclesdb_attribution)
                    .bufferedReader()
                    .use {
                        it.readText()
                    }.replace(Regex("<!--[\\s\\S]*?-->"), "")
                    .trim()
            }
        PulseDialog(
            stringResource(R.string.pulse_garage_data_sources),
            stringResource(R.string.pulse_garage_data_version) + "\n\n" + notices,
            stringResource(R.string.pulse_garage_close_credits),
            {
                showCredits =
                    false
            },
            { showCredits = false },
        )
    }
}
