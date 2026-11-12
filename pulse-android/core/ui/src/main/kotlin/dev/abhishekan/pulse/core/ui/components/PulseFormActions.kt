package dev.abhishekan.pulse.core.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.abhishekan.pulse.core.ui.PulseTokens

class PulseFormAction(
    val label: String,
    val onClick: () -> Unit,
    val enabled: Boolean = true,
    val loading: Boolean = false,
    val modifier: Modifier = Modifier,
)

@Composable
fun PulseFormActions(
    primary: PulseFormAction,
    secondary: PulseFormAction? = null,
    modifier: Modifier = Modifier,
    additional: PulseFormAction? = null,
) {
    Column(
        modifier.fillMaxWidth().padding(vertical = PulseTokens.small),
        verticalArrangement = Arrangement.spacedBy(PulseTokens.small),
    ) {
        Row(
            Modifier.fillMaxWidth().height(IntrinsicSize.Min),
            horizontalArrangement = Arrangement.spacedBy(PulseTokens.small),
        ) {
            secondary?.let { FormButton(it, Modifier.weight(1f), PulseButtonVariant.Secondary) }
            FormButton(primary, Modifier.weight(1f), PulseButtonVariant.Primary)
        }
        additional?.let {
            PulseButton(
                it.label,
                it.onClick,
                Modifier.fillMaxWidth().then(it.modifier),
                enabled = it.enabled,
                loading = it.loading,
                style = PulseButtonStyle.Text,
            )
        }
    }
}

@Composable
private fun FormButton(action: PulseFormAction, modifier: Modifier, variant: PulseButtonVariant) {
    PulseButton(
        action.label,
        action.onClick,
        modifier.fillMaxHeight().heightIn(min = 56.dp).then(action.modifier),
        enabled = action.enabled,
        loading = action.loading,
        variant = variant,
        contentPadding = PaddingValues(PulseTokens.small),
    )
}

@Composable
fun PulseFormLayout(
    modifier: Modifier = Modifier,
    scrollContentModifier: Modifier = Modifier,
    footer: @Composable ColumnScope.() -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
        Column(
            Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).then(scrollContentModifier),
            verticalArrangement = Arrangement.spacedBy(PulseTokens.cardPadding),
            content = content,
        )
        footer()
    }
}
