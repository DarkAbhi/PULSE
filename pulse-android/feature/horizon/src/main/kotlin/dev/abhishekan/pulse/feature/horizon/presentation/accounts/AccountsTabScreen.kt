package dev.abhishekan.pulse.feature.horizon.presentation.accounts

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.abhishekan.pulse.core.ui.PulseSpacing
import dev.abhishekan.pulse.core.ui.PulseTheme
import dev.abhishekan.pulse.core.ui.PulseTokens
import dev.abhishekan.pulse.core.ui.PulseWindowSize
import dev.abhishekan.pulse.core.ui.components.PulseErrorState
import dev.abhishekan.pulse.core.ui.components.PulseLoadingState
import dev.abhishekan.pulse.core.ui.components.PulseText
import dev.abhishekan.pulse.core.ui.components.PulseTextStyle
import dev.abhishekan.pulse.core.ui.message
import dev.abhishekan.pulse.core.ui.pagePadding
import dev.abhishekan.pulse.feature.horizon.R
import dev.abhishekan.pulse.feature.horizon.presentation.components.HorizonTabScroll
import org.koin.androidx.compose.koinViewModel

@Composable
internal fun AccountsTabRoute(
    enabled: Boolean,
    width: PulseWindowSize,
    header: @Composable () -> Unit,
    notice: @Composable () -> Unit,
    onOpen: (AccountType) -> Unit,
    vm: AccountsViewModel = koinViewModel(),
) {
    val state by vm.uiState.collectAsStateWithLifecycle()
    AccountsTabScreen(state, enabled, width, header, notice, onOpen, vm::onAction)
}

@Composable
internal fun AccountsTabScreen(
    state: AccountsState,
    enabled: Boolean,
    width: PulseWindowSize,
    header: @Composable () -> Unit,
    notice: @Composable () -> Unit,
    onOpen: (AccountType) -> Unit,
    onAction: (AccountsAction) -> Unit,
) {
    Column(Modifier.fillMaxSize().background(PulseTheme.colors.background)) {
        header()
        HorizonTabScroll(width.pagePadding) {
            notice()
            AccountsTabContent(state, enabled, onOpen)
            AccountsListStatus(state, onAction)
        }
    }
}

@Composable
internal fun AccountsTabContent(
    state: AccountsState,
    enabled: Boolean,
    onOpen: (AccountType) -> Unit,
) {
    val colors = PulseTheme.colors
    Row(
        Modifier.widthIn(max = PulseTokens.readingWidth).fillMaxWidth().height(IntrinsicSize.Min),
        horizontalArrangement = Arrangement.spacedBy(PulseSpacing.compact),
    ) {
        AccountType.entries.forEach { type ->
            val accounts = state.accountsOfType(type)
            Surface(
                onClick = { onOpen(type) },
                modifier = Modifier.weight(1f).fillMaxHeight().semantics { role = Role.Button },
                enabled = enabled,
                shape = PulseTheme.shapes.extraLarge,
                color = type.softColor,
                contentColor = colors.textPrimary,
                border = BorderStroke(1.dp, colors.border),
            ) {
                Column(
                    Modifier.padding(PulseSpacing.compact),
                    verticalArrangement = Arrangement.spacedBy(PulseTokens.small),
                ) {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        AccountTypeBadge(type)
                        Icon(
                            painterResource(R.drawable.pulse_obligation_chevron),
                            contentDescription = null,
                            modifier = Modifier.rotate(if (LocalLayoutDirection.current == LayoutDirection.Ltr) -90f else 90f),
                            tint = colors.textPrimary,
                        )
                    }
                    Column {
                        accounts?.let {
                            PulseText(
                                stringResource(R.string.pulse_horizon_account_count, it.size),
                                style = PulseTextStyle.Heading1,
                            )
                        }
                        PulseText(stringResource(type.listTitle), style = PulseTextStyle.Body2)
                    }
                    if (!accounts.isNullOrEmpty()) {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(PulseTokens.tiny),
                            verticalArrangement = Arrangement.spacedBy(PulseTokens.tiny),
                        ) {
                            accounts.take(3).forEach { AccountInitialsBadge(type, it.displayName, compact = true) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun AccountsListStatus(
    state: AccountsState,
    onAction: (AccountsAction) -> Unit,
) {
    when (val request = state.request) {
        AccountsState.Request.Idle -> Unit
        AccountsState.Request.Loading -> PulseLoadingState(stringResource(R.string.pulse_horizon_loading_accounts))
        is AccountsState.Request.Failed -> PulseErrorState(request.failure.message, { onAction(AccountsAction.Refresh) })
    }
}
