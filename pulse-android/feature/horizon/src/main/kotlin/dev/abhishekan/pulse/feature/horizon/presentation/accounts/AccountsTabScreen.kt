package dev.abhishekan.pulse.feature.horizon.presentation.accounts

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
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
import dev.abhishekan.pulse.core.ui.LocalPulseContentPadding
import dev.abhishekan.pulse.core.ui.PulseHideNavigation
import dev.abhishekan.pulse.core.ui.PulseScreenTransition
import dev.abhishekan.pulse.core.ui.PulseSpacing
import dev.abhishekan.pulse.core.ui.PulseTheme
import dev.abhishekan.pulse.core.ui.PulseTokens
import dev.abhishekan.pulse.core.ui.PulseWindowSize
import dev.abhishekan.pulse.core.ui.components.PulseCard
import dev.abhishekan.pulse.core.ui.components.PulseCenteredLoadingState
import dev.abhishekan.pulse.core.ui.components.PulseErrorState
import dev.abhishekan.pulse.core.ui.components.PulseFloatingActionButton
import dev.abhishekan.pulse.core.ui.components.PulseLoadingState
import dev.abhishekan.pulse.core.ui.components.PulseText
import dev.abhishekan.pulse.core.ui.components.PulseTextStyle
import dev.abhishekan.pulse.core.ui.message
import dev.abhishekan.pulse.core.ui.pagePadding
import dev.abhishekan.pulse.feature.horizon.R
import dev.abhishekan.pulse.feature.horizon.presentation.components.HorizonTabScroll
import dev.abhishekan.pulse.feature.horizon.presentation.editor.HorizonEditorScaffold
import org.koin.androidx.compose.koinViewModel

@Composable
internal fun AccountsRoute(
    currency: String,
    active: Boolean,
    enabled: Boolean,
    width: PulseWindowSize,
    header: @Composable () -> Unit,
    notice: @Composable () -> Unit,
    vm: AccountsViewModel = koinViewModel(),
) {
    val state by vm.uiState.collectAsStateWithLifecycle()
    var selectedType by rememberSaveable { mutableStateOf<AccountType?>(null) }
    val listScroll = rememberSaveableStateHolder()
    PulseHideNavigation(active && selectedType != null)
    BackHandler(active && selectedType != null) {
        if (state.draft != null) {
            vm.onAction(AccountsAction.Cancel)
        } else {
            selectedType = null
        }
    }

    PulseScreenTransition(
        state = selectedType to state,
        screenDepth = { (type, screenState) ->
            when {
                type == null -> 0
                screenState.draft == null -> 1
                else -> 2
            }
        },
    ) { (type, screenState), _ ->
        val draft = screenState.draft
        when {
            type == null ->
                Column(Modifier.fillMaxSize().background(PulseTheme.colors.background)) {
                    header()
                    HorizonTabScroll(width.pagePadding) {
                        notice()
                        AccountsTabContent(screenState, enabled) { selectedType = it }
                        AccountsListStatus(screenState, vm::onAction)
                    }
                }
            draft == null ->
                listScroll.SaveableStateProvider(type.name) {
                    AccountListScreen(type, screenState, vm::onAction)
                }
            else -> AccountEditorScreen(type, screenState, draft, vm::onAction, currency)
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
internal fun AccountListScreen(
    type: AccountType,
    state: AccountsState,
    onAction: (AccountsAction) -> Unit,
) {
    val accounts = state.accountsOfType(type)
    HorizonEditorScaffold(
        title = stringResource(type.listTitle),
        backLabel = stringResource(R.string.pulse_horizon_back_to_accounts),
        busy = false,
    ) { width ->
        val gutter = width.pagePadding
        val bottom = LocalPulseContentPadding.current.calculateBottomPadding()
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
            Box(Modifier.widthIn(max = PulseTokens.readingWidth + gutter * 2).fillMaxSize()) {
                HorizonTabScroll(gutter, bottomSpacing = 56.dp + gutter) {
                    if (!state.busy) AccountsListStatus(state, onAction)
                    when {
                        accounts == null -> Unit
                        accounts.isEmpty() ->
                            EmptyAccounts(
                                type = type,
                                enabled = !state.busy,
                                onAdd = { onAction(AccountsAction.Add(type)) },
                            )
                        else ->
                            PulseCard(
                                modifier = Modifier.fillMaxWidth(),
                                border = BorderStroke(1.dp, PulseTheme.colors.border),
                                shape = PulseTheme.shapes.extraLarge,
                                contentPadding = PulseSpacing.none,
                            ) {
                                Column(Modifier.padding(horizontal = PulseSpacing.compact)) {
                                    accounts.forEachIndexed { index, account ->
                                        if (index > 0) HorizontalDivider(color = PulseTheme.colors.divider)
                                        key(account.id ?: index) {
                                            FinancialAccountRow(type, account, !state.busy, onAction)
                                        }
                                    }
                                }
                            }
                    }
                }
                if (state.busy) {
                    PulseCenteredLoadingState(stringResource(R.string.pulse_horizon_loading_accounts))
                }
                if (!state.busy && accounts?.isEmpty() != true) {
                    PulseFloatingActionButton(
                        onClick = { onAction(AccountsAction.Add(type)) },
                        modifier = Modifier.align(Alignment.BottomEnd).padding(end = gutter, bottom = bottom + gutter),
                    ) {
                        Icon(painterResource(R.drawable.pulse_purchase_add), contentDescription = stringResource(type.addTitle))
                    }
                }
            }
        }
    }
}

@Composable
private fun AccountsListStatus(
    state: AccountsState,
    onAction: (AccountsAction) -> Unit,
) {
    when (val request = state.request) {
        AccountsState.Request.Idle -> Unit
        AccountsState.Request.Loading -> PulseLoadingState(stringResource(R.string.pulse_horizon_loading_accounts))
        is AccountsState.Request.Failed -> PulseErrorState(request.failure.message, { onAction(AccountsAction.Refresh) })
    }
}
