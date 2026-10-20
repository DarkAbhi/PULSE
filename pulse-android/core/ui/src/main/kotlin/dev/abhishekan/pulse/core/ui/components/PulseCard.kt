package dev.abhishekan.pulse.core.ui.components
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import dev.abhishekan.pulse.core.ui.PulseTheme
import dev.abhishekan.pulse.core.ui.PulseTokens

@Composable
fun PulseCard(
    modifier: Modifier = Modifier,
    highlighted: Boolean = false,
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(
        modifier,
        shape = PulseTokens.cardShape,
        colors =
            CardDefaults.cardColors(
                containerColor = if (highlighted) PulseTheme.colors.primarySoft else PulseTheme.colors.surface,
                contentColor = if (highlighted) PulseTheme.colors.onPrimarySoft else PulseTheme.colors.textPrimary,
            ),
        elevation = CardDefaults.cardElevation(PulseTokens.elevation),
    ) {
        Column(
            Modifier.padding(PulseTokens.cardPadding),
            verticalArrangement = Arrangement.spacedBy(PulseTokens.small),
            content = content,
        )
    }
}
