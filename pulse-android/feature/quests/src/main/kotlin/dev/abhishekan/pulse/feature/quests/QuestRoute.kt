package dev.abhishekan.pulse.feature.quests

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.abhishekan.pulse.core.domain.model.QuestOverview
import dev.abhishekan.pulse.core.ui.LocalPulseContentPadding
import dev.abhishekan.pulse.core.ui.PulseAdaptiveLayout
import dev.abhishekan.pulse.core.ui.PulseTheme
import dev.abhishekan.pulse.core.ui.PulseTokens
import dev.abhishekan.pulse.core.ui.PulseWindowSize
import dev.abhishekan.pulse.core.ui.components.PulseButton
import dev.abhishekan.pulse.core.ui.components.PulseButtonVariant
import dev.abhishekan.pulse.core.ui.components.PulseText
import dev.abhishekan.pulse.core.ui.components.PulseTextStyle
import dev.abhishekan.pulse.core.ui.message
import dev.abhishekan.pulse.core.ui.pagePadding
import dev.abhishekan.pulse.core.ui.patterns.PulseCenteredLoadingState
import dev.abhishekan.pulse.core.ui.patterns.PulseErrorState
import dev.abhishekan.pulse.core.ui.patterns.PulseScreenScaffold
import org.koin.androidx.compose.koinViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@Composable
fun QuestRoute(
    onBack: () -> Unit,
    onWorkoutDays: () -> Unit,
) {
    val viewModel: QuestViewModel = koinViewModel()
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(viewModel) { viewModel.refresh() }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.refresh() }

    var showGoals by rememberSaveable { mutableStateOf(false) }
    var observedSave by remember { mutableIntStateOf(state.goalSaveVersion) }
    LaunchedEffect(state.goalSaveVersion) {
        if (state.goalSaveVersion > observedSave) {
            observedSave = state.goalSaveVersion
            showGoals = false
        }
    }
    val openGoals = {
        viewModel.clearSaveFailure()
        showGoals = true
    }

    PulseScreenScaffold(
        title = "",
        backLabel = stringResource(R.string.pulse_quests_back),
        onBack = onBack,
    ) {
        PulseAdaptiveLayout(Modifier.fillMaxSize().padding(LocalPulseContentPadding.current)) { size ->
            val data = state.overview
            if (data == null && state.loading) {
                PulseCenteredLoadingState(stringResource(R.string.pulse_quests_loading))
            } else {
                val fontScale = LocalDensity.current.fontScale
                Column(
                    Modifier
                        .widthIn(max = PulseTokens.contentWidth)
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(size.pagePadding),
                    verticalArrangement = Arrangement.spacedBy(PulseTokens.medium),
                ) {
                    state.failure?.let { PulseErrorState(it.message, viewModel::refresh) }
                    data?.let {
                        QuestContent(
                            it,
                            size.takeIf { fontScale < 1.35f } ?: PulseWindowSize.Compact,
                            state.loading || state.saving,
                            openGoals,
                            onWorkoutDays,
                            viewModel::selectWeek,
                        )
                    }
                }
            }
        }
    }
    val settings = state.overview?.settings
    if (showGoals && settings != null) {
        QuestGoalsDialog(settings.settings, state, viewModel::save) { showGoals = false }
    }
}

@Composable
private fun QuestContent(
    data: QuestOverview,
    size: PulseWindowSize,
    loading: Boolean,
    onGoals: () -> Unit,
    onWorkoutDays: () -> Unit,
    onWeek: (LocalDate) -> Unit,
) {
    FlowRow(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(PulseTokens.medium),
        verticalArrangement = Arrangement.spacedBy(PulseTokens.small),
        itemVerticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(PulseTokens.small),
        ) {
            Box(
                Modifier
                    .size(48.dp)
                    .background(
                        Brush.linearGradient(listOf(Color(0xFF3A2CD9), Color(0xFFB22BD8))),
                        PulseTheme.shapes.medium,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(painterResource(R.drawable.quest_gamepad), null, tint = Color.White)
            }
            PulseText(
                stringResource(R.string.pulse_quests_title),
                Modifier.semantics { heading() },
                style = PulseTextStyle.Heading1,
            )
        }
        QuestRewardPill(stringResource(R.string.pulse_quests_xp, data.progress.lifetimeXp))
        QuestRewardPill(stringResource(R.string.pulse_quests_hearts, data.progress.hearts))
        if (data.settings.configured) {
            PulseButton(
                stringResource(R.string.pulse_quests_edit_goals),
                onGoals,
                enabled = !loading,
                variant = PulseButtonVariant.Secondary,
            )
        }
    }

    data.settings.effective?.takeIf { it > data.today }?.let {
        val format = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(LocalLocale.current.platformLocale)
        PulseText(stringResource(R.string.pulse_quests_settings_effective, it.format(format)))
    }

    val gap = Arrangement.spacedBy(PulseTokens.medium)
    when (size) {
        PulseWindowSize.Compact -> {
            QuestStreak(data.progress.streak, Modifier.fillMaxWidth())
            QuestWeek(data, loading, onWeek, Modifier.fillMaxWidth())
            QuestTodayXp(data.progress, Modifier.fillMaxWidth())
        }
        PulseWindowSize.Medium -> {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = gap) {
                QuestStreak(data.progress.streak, Modifier.weight(1f))
                QuestTodayXp(data.progress, Modifier.weight(1f))
            }
            QuestWeek(data, loading, onWeek, Modifier.fillMaxWidth())
        }
        PulseWindowSize.Expanded -> {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = gap) {
                QuestStreak(data.progress.streak, Modifier.weight(1f))
                QuestWeek(data, loading, onWeek, Modifier.weight(1.25f))
                QuestTodayXp(data.progress, Modifier.weight(1f))
            }
        }
    }
    if (size == PulseWindowSize.Expanded) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = gap) {
            QuestDaily(data, onGoals, onWorkoutDays, Modifier.weight(1.65f))
            Column(Modifier.weight(1f), verticalArrangement = gap) {
                QuestMedals(data.progress.bestStreak)
                QuestWeekProgress(data.progress)
            }
        }
    } else {
        QuestDaily(data, onGoals, onWorkoutDays, Modifier.fillMaxWidth())
        QuestMedals(data.progress.bestStreak)
        QuestWeekProgress(data.progress)
    }
}

@Composable
private fun QuestRewardPill(label: String) {
    PulseText(
        label,
        Modifier
            .background(PulseTheme.colors.surface, PulseTheme.shapes.pill)
            .padding(horizontal = PulseTokens.medium, vertical = PulseTokens.small),
        style = PulseTextStyle.Label,
    )
}
