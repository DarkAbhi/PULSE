package dev.abhishekan.pulse.feature.horizon.presentation.accounts

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.abhishekan.pulse.core.domain.model.FinancialAccount
import dev.abhishekan.pulse.core.ui.LocalPulseContentPadding
import dev.abhishekan.pulse.core.ui.PulseScreenTransition
import dev.abhishekan.pulse.core.ui.PulseSpacing
import dev.abhishekan.pulse.core.ui.PulseTheme
import dev.abhishekan.pulse.core.ui.PulseTokens
import dev.abhishekan.pulse.core.ui.components.PulseButton
import dev.abhishekan.pulse.core.ui.components.PulseButtonStyle
import dev.abhishekan.pulse.core.ui.components.PulseCard
import dev.abhishekan.pulse.core.ui.components.PulseDropdownMenu
import dev.abhishekan.pulse.core.ui.components.PulseDropdownMenuItem
import dev.abhishekan.pulse.core.ui.components.PulseFloatingActionButton
import dev.abhishekan.pulse.core.ui.components.PulseHorizontalDivider
import dev.abhishekan.pulse.core.ui.components.PulseText
import dev.abhishekan.pulse.core.ui.components.PulseTextStyle
import dev.abhishekan.pulse.core.ui.message
import dev.abhishekan.pulse.core.ui.pagePadding
import dev.abhishekan.pulse.core.ui.patterns.PulseCenteredLoadingState
import dev.abhishekan.pulse.core.ui.patterns.PulseErrorState
import dev.abhishekan.pulse.core.ui.patterns.PulseLoadingState
import dev.abhishekan.pulse.feature.horizon.R
import dev.abhishekan.pulse.feature.horizon.presentation.components.HorizonTabScroll
import dev.abhishekan.pulse.feature.horizon.presentation.editor.HorizonEditorScaffold
import org.koin.androidx.compose.koinViewModel

@Composable
internal fun CreditCardsScreen(
    currency: String,
    active: Boolean,
    vm: AccountsViewModel = koinViewModel(),
) {
    val state by vm.uiState.collectAsStateWithLifecycle()
    val listScroll = rememberSaveableStateHolder()
    BackHandler(active && state.draft != null) { vm.onAction(AccountsAction.Cancel) }
    PulseScreenTransition(state, screenDepth = { if (it.draft == null) 0 else 1 }) { screen, _ ->
        val draft = screen.draft
        if (draft == null) {
            listScroll.SaveableStateProvider("credit_cards") {
                CreditCardsScreenContent(screen, vm::onAction)
            }
        } else {
            AccountEditorScreen(AccountType.CreditCard, screen, draft, vm::onAction, currency)
        }
    }
}

@Composable
internal fun CreditCardsScreenContent(
    state: AccountsState,
    onAction: (AccountsAction) -> Unit,
) {
    HorizonEditorScaffold(
        title = stringResource(R.string.pulse_horizon_credit_cards),
        backLabel = stringResource(R.string.pulse_horizon_back_to_accounts),
        busy = false,
    ) { width ->
        val accounts = remember(state.accounts) { state.accountsOfType(AccountType.CreditCard) }
        val gutter = width.pagePadding
        val bottom = LocalPulseContentPadding.current.calculateBottomPadding()
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
            Box(Modifier.widthIn(max = PulseTokens.readingWidth + gutter * 2).fillMaxSize()) {
                HorizonTabScroll(gutter, bottomSpacing = 56.dp + gutter) {
                    if (!state.busy) CreditCardsStatus(state, onAction)
                    when {
                        accounts == null -> Unit
                        accounts.isEmpty() ->
                            EmptyCreditCards(
                                enabled = !state.busy,
                                onAdd = { onAction(AccountsAction.Add(AccountType.CreditCard)) },
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
                                        if (index > 0) PulseHorizontalDivider(color = PulseTheme.colors.divider)
                                        key(account.id ?: index) {
                                            CreditCardRow(account, !state.busy, onAction)
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
                        onClick = { onAction(AccountsAction.Add(AccountType.CreditCard)) },
                        modifier = Modifier.align(Alignment.BottomEnd).padding(end = gutter, bottom = bottom + gutter),
                    ) {
                        Icon(
                            painterResource(R.drawable.pulse_purchase_add),
                            contentDescription = stringResource(R.string.pulse_horizon_add_credit_card),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CreditCardRow(
    account: FinancialAccount,
    enabled: Boolean,
    onAction: (AccountsAction) -> Unit,
) {
    val colors = PulseTheme.colors
    val hasBadges = account.archived
    val subtitle =
        listOfNotNull(
            account.institutionName,
            account.lastFour.takeIf { it.isNotBlank() }?.let { stringResource(R.string.pulse_horizon_account_masked_digits, it) },
            account.cardType.takeIf { it.isNotBlank() },
            cardNetworkLabels[account.cardNetwork]?.let { stringResource(it) },
        ).joinToString(" · ")

    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val inlineBadges = maxWidth >= 268.dp * LocalDensity.current.fontScale.coerceAtLeast(1f)
        val badges: @Composable () -> Unit = {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(PulseTokens.tiny),
                verticalArrangement = Arrangement.spacedBy(PulseTokens.tiny),
            ) {
                if (account.archived) {
                    CreditCardTag(stringResource(R.string.pulse_horizon_account_archived), colors.surfaceMuted, colors.textSecondary)
                }
            }
        }
        Row(
            Modifier.fillMaxWidth().padding(vertical = PulseTokens.small),
            horizontalArrangement = Arrangement.spacedBy(PulseTokens.small),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CreditCardInitialsBadge(account.displayName)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(PulseTokens.tiny)) {
                PulseText(account.displayName, style = PulseTextStyle.Label, maxLines = 2, overflow = TextOverflow.Ellipsis)
                PulseText(
                    subtitle,
                    style = PulseTextStyle.Body3,
                    color = colors.textSecondary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                if (!inlineBadges && hasBadges) badges()
            }
            if (inlineBadges && hasBadges) {
                Box(Modifier.widthIn(max = 96.dp)) { badges() }
            }
            CreditCardMenu(account, enabled, onAction)
        }
    }
}

@Composable
private fun CreditCardTag(
    label: String,
    background: Color,
    foreground: Color,
) {
    Surface(shape = PulseTheme.shapes.pill, color = background) {
        PulseText(
            label,
            modifier = Modifier.padding(horizontal = PulseTokens.small, vertical = PulseTokens.tiny),
            style = PulseTextStyle.Caption,
            color = foreground,
        )
    }
}

@Composable
private fun CreditCardMenu(
    account: FinancialAccount,
    enabled: Boolean,
    onAction: (AccountsAction) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        PulseButton(
            stringResource(R.string.pulse_horizon_account_actions, account.displayName),
            { expanded = true },
            enabled = enabled,
            style = PulseButtonStyle.Icon,
        ) {
            Icon(painterResource(R.drawable.pulse_horizon_more), null, tint = PulseTheme.colors.textSecondary)
        }
        PulseDropdownMenu(expanded && enabled, { expanded = false }) {
            PulseDropdownMenuItem(
                text = { PulseText(stringResource(R.string.pulse_horizon_edit_account)) },
                onClick = {
                    expanded = false
                    onAction(AccountsAction.Edit(account))
                },
            )
            account.id?.let { id ->
                val label = if (account.archived) R.string.pulse_horizon_restore_account else R.string.pulse_horizon_archive_account
                PulseDropdownMenuItem(
                    text = { PulseText(stringResource(label)) },
                    onClick = {
                        expanded = false
                        onAction(AccountsAction.SetArchived(id, !account.archived))
                    },
                )
            }
        }
    }
}

@Composable
private fun CreditCardInitialsBadge(name: String) {
    val initials =
        name
            .asSequence()
            .filter(Char::isLetterOrDigit)
            .take(2)
            .joinToString("")
            .uppercase()
    Box(
        Modifier
            .size(PulseSpacing.extraLarge)
            .background(PulseTheme.colors.primarySoft, PulseTheme.shapes.medium)
            .clearAndSetSemantics {},
        contentAlignment = Alignment.Center,
    ) {
        if (initials.isEmpty()) {
            Icon(
                painterResource(R.drawable.pulse_obligation_card),
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = PulseTheme.colors.onPrimarySoft,
            )
        } else {
            PulseText(
                initials,
                modifier = Modifier.padding(horizontal = PulseTokens.tiny),
                style = PulseTextStyle.Label,
                color = PulseTheme.colors.onPrimarySoft,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun EmptyCreditCards(
    enabled: Boolean,
    onAdd: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val accent = PulseTheme.colors.onPrimarySoft
    val soft = PulseTheme.colors.primarySoft
    Column(
        modifier.fillMaxWidth().padding(vertical = PulseSpacing.cardPadding),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier.size(PulseSpacing.spacious * 2, PulseSpacing.extraLarge * 2 + PulseSpacing.small),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                Modifier
                    .align(Alignment.BottomStart)
                    .rotate(-12f)
                    .size(PulseSpacing.spacious)
                    .background(soft.copy(alpha = 0.6f), PulseTheme.shapes.extraLarge),
            )
            Box(
                Modifier
                    .align(Alignment.BottomEnd)
                    .rotate(12f)
                    .size(PulseSpacing.spacious)
                    .background(soft.copy(alpha = 0.6f), PulseTheme.shapes.extraLarge),
            )
            Box(
                Modifier
                    .align(Alignment.TopCenter)
                    .size(PulseSpacing.extraLarge * 2)
                    .background(soft, PulseTheme.shapes.extraLarge),
                contentAlignment = Alignment.Center,
            ) {
                Canvas(Modifier.matchParentSize()) {
                    val stroke = 1.dp.toPx()
                    val inset = stroke / 2
                    drawRoundRect(
                        color = accent,
                        topLeft = Offset(inset, inset),
                        size = Size(size.width - stroke, size.height - stroke),
                        cornerRadius = CornerRadius(PulseSpacing.comfortable.toPx()),
                        style = Stroke(stroke, pathEffect = PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 3.dp.toPx()))),
                    )
                }
                Icon(
                    painterResource(R.drawable.pulse_obligation_card),
                    contentDescription = null,
                    modifier = Modifier.size(PulseSpacing.cardPadding),
                    tint = accent,
                )
            }
        }
        PulseText(
            stringResource(R.string.pulse_horizon_no_credit_cards),
            Modifier.padding(top = PulseSpacing.extraLarge).semantics { heading() },
            style = PulseTextStyle.Heading3,
            textAlign = TextAlign.Center,
        )
        PulseText(
            stringResource(R.string.pulse_horizon_no_credit_cards_help),
            Modifier.widthIn(max = PulseTokens.readingWidth / 2).padding(top = PulseSpacing.small, bottom = PulseSpacing.cardPadding),
            style = PulseTextStyle.Body2,
            color = PulseTheme.colors.textSecondary,
            textAlign = TextAlign.Center,
        )
        PulseButton(
            text = stringResource(R.string.pulse_horizon_add_credit_card),
            onClick = onAdd,
            enabled = enabled,
            leadingIcon = {
                Icon(
                    painterResource(R.drawable.pulse_purchase_add),
                    contentDescription = null,
                    modifier = Modifier.size(PulseSpacing.medium),
                )
            },
        )
    }
}

@Composable
private fun CreditCardsStatus(
    state: AccountsState,
    onAction: (AccountsAction) -> Unit,
) {
    when (val request = state.request) {
        AccountsState.Request.Idle -> Unit
        AccountsState.Request.Loading -> PulseLoadingState(stringResource(R.string.pulse_horizon_loading_accounts))
        is AccountsState.Request.Failed -> PulseErrorState(request.failure.message, { onAction(AccountsAction.Refresh) })
    }
}
