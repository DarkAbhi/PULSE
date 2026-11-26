package dev.abhishekan.pulse.core.ui.components
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.abhishekan.pulse.core.ui.PulseElevation
import dev.abhishekan.pulse.core.ui.PulseShapes
import dev.abhishekan.pulse.core.ui.PulseTheme
import dev.abhishekan.pulse.core.ui.PulseTokens
import dev.abhishekan.pulse.core.ui.PulseTone

/** Web `PulseCard` `elevation`: raised cards cast the soft card shadow; flat cards sit on another surface. */
enum class PulseCardElevation { Raised, Flat }

/** Web `PulseCard` `padding`: sm 16dp, md 24dp, lg 32dp. */
object PulseCardPadding {
    val none = 0.dp
    val small = 16.dp
    val medium = 24.dp
    val large = 32.dp
}

@Composable
fun PulseCard(
    modifier: Modifier = Modifier,
    highlighted: Boolean = false,
    border: BorderStroke? = null,
    shape: Shape = PulseTokens.cardShape,
    contentPadding: Dp = PulseTokens.cardPadding,
    elevation: PulseCardElevation = PulseCardElevation.Raised,
    containerColor: Color? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = PulseTheme.colors
    val raised = elevation == PulseCardElevation.Raised && !highlighted && containerColor == null
    Card(
        modifier.pulseCardShadow(raised, shape),
        shape = shape,
        border = border,
        colors =
            CardDefaults.cardColors(
                containerColor = containerColor ?: if (highlighted) colors.primarySoft else colors.surface,
                contentColor = if (highlighted) colors.onPrimarySoft else colors.textPrimary,
            ),
        elevation = CardDefaults.cardElevation(PulseElevation.none),
    ) {
        Column(
            Modifier.padding(contentPadding),
            verticalArrangement = Arrangement.spacedBy(PulseTokens.small),
            content = content,
        )
    }
}

/**
 * Web `PulseCard variant="tile"`: a gradient feature tile with the 32dp hero radius.
 * Colour comes from a [PulseTone], usually from `PulseTheme.featureTones`.
 */
@Composable
fun PulseTileCard(
    tone: PulseTone,
    modifier: Modifier = Modifier,
    contentPadding: Dp = PulseCardPadding.medium,
    content: @Composable ColumnScope.() -> Unit,
) {
    val shape = PulseShapes.hero
    Box(
        modifier
            .shadow(PulseElevation.card, shape, ambientColor = tone.start, spotColor = tone.start)
            .clip(shape)
            .background(Brush.linearGradient(listOf(tone.start, tone.end))),
    ) {
        CompositionLocalProvider(LocalContentColor provides tone.foreground) {
            Column(
                Modifier.padding(contentPadding),
                verticalArrangement = Arrangement.spacedBy(PulseTokens.small),
                content = content,
            )
        }
    }
}

@Composable
private fun Modifier.pulseCardShadow(
    raised: Boolean,
    shape: Shape,
): Modifier {
    if (!raised) return this
    val spot = if (PulseTheme.isDark) Color.Black.copy(alpha = 0.8f) else PulseTheme.colors.brand.copy(alpha = 0.4f)
    val ambient = if (PulseTheme.isDark) Color.Black.copy(alpha = 0.3f) else PulseTheme.colors.textPrimary.copy(alpha = 0.06f)
    return shadow(PulseTokens.elevation, shape, clip = false, ambientColor = ambient, spotColor = spot)
}
