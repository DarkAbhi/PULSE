package dev.abhishekan.pulse.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.abhishekan.pulse.core.domain.model.Outcome
import dev.abhishekan.pulse.core.domain.model.UserTimeZone
import dev.abhishekan.pulse.core.domain.repository.DashboardFeedRepository
import dev.abhishekan.pulse.core.domain.repository.DashboardRepository
import dev.abhishekan.pulse.core.domain.repository.FitnessRepository
import dev.abhishekan.pulse.core.domain.repository.HorizonRepository
import dev.abhishekan.pulse.core.domain.repository.NotificationsRepository
import dev.abhishekan.pulse.core.domain.repository.QuestRepository
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Clock

class HomeViewModel(
    private val dashboard: DashboardRepository,
    private val fitness: FitnessRepository,
    private val horizon: HorizonRepository,
    private val notifications: NotificationsRepository,
    private val feed: DashboardFeedRepository,
    private val quests: QuestRepository,
    private val clock: Clock = Clock.systemUTC(),
) : ViewModel() {
    // Align the header to each minute; stop ticking when the route has no collectors.
    val now =
        flow {
            while (true) {
                val instant = clock.instant()
                emit(instant)
                delay(60_000 - instant.toEpochMilli() % 60_000)
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), clock.instant())

    private val state = MutableStateFlow(HomeUiState())
    val uiState = state.asStateFlow()
    private val sectionJobs = mutableMapOf<HomeSection, Job>()
    private var notificationJob: Job? = null
    private var requestedTimeZone = UserTimeZone.zone

    init {
        observeChanges(dashboard.changes) { refresh(HomeSection.Profile, replaceInFlight = true) }
        observeChanges(horizon.changes) { refresh(HomeSection.Horizon, replaceInFlight = true) }
        observeChanges(fitness.changes) {
            refresh(HomeSection.Workout, replaceInFlight = true)
            refresh(HomeSection.FitnessToday, replaceInFlight = true)
            refresh(HomeSection.RecentActivity, replaceInFlight = true)
        }
        observeChanges(quests.changes) {
            refresh(HomeSection.Quests, replaceInFlight = true)
            refresh(HomeSection.QuestSettings, replaceInFlight = true)
        }
        HomeSection.entries.forEach { refresh(it) }
        refreshNotificationCount()
    }

    private fun observeChanges(
        revisions: Flow<*>,
        onChange: () -> Unit,
    ) {
        // Capture the initial revision before any requests can invalidate it.
        viewModelScope.launch(start = CoroutineStart.UNDISPATCHED) {
            revisions.drop(1).collect { onChange() }
        }
    }

    fun onAction(action: HomeAction) {
        when (action) {
            is HomeAction.Retry -> refresh(action.section)
            is HomeAction.TimeZoneChanged -> {
                if (requestedTimeZone == action.zone) return
                requestedTimeZone = action.zone
                HomeSection.entries.forEach { refresh(it, replaceInFlight = true) }
            }
            HomeAction.Resumed -> {
                // Synced activity, quest progress and the feed can change while the app is away.
                refresh(HomeSection.Workout)
                refresh(HomeSection.FitnessToday)
                refresh(HomeSection.Quests)
                refresh(HomeSection.QuestSettings)
                refresh(HomeSection.RecentActivity)
                refreshNotificationCount()
            }
        }
    }

    private fun refreshNotificationCount() {
        if (notificationJob?.isActive == true) return
        notificationJob =
            viewModelScope.launch {
                when (val result = notifications.list()) {
                    is Outcome.Success -> state.update { it.copy(notificationCount = result.value.size) }
                    is Outcome.Error -> Unit // Keep the last known count when a refresh fails.
                }
            }
    }

    private fun refresh(
        section: HomeSection,
        replaceInFlight: Boolean = false,
    ) {
        when (section) {
            HomeSection.Profile ->
                load(
                    section,
                    replaceInFlight,
                    dashboard::profile,
                    { it.name == null && it.hasProfile },
                    HomeUiState::profile,
                ) { state, value -> state.copy(profile = value) }
            HomeSection.Workout ->
                load(section, replaceInFlight, dashboard::workout, { !it.visited }, HomeUiState::workout) { state, value ->
                    state.copy(workout = value)
                }
            HomeSection.Horizon ->
                load(section, replaceInFlight, dashboard::horizon, { false }, HomeUiState::horizon) { state, value ->
                    state.copy(horizon = value)
                }
            HomeSection.FitnessToday ->
                load(section, replaceInFlight, feed::fitnessToday, { false }, HomeUiState::fitnessToday) { state, value ->
                    state.copy(fitnessToday = value)
                }
            HomeSection.Quests ->
                load(section, replaceInFlight, quests::activity, { false }, HomeUiState::quests) { state, value ->
                    state.copy(quests = value)
                }
            HomeSection.QuestSettings ->
                load(section, replaceInFlight, quests::settings, { false }, HomeUiState::questSettings) { state, value ->
                    state.copy(questSettings = value)
                }
            HomeSection.RecentActivity ->
                load(section, replaceInFlight, feed::recentActivity, { it.isEmpty() }, HomeUiState::recentActivity) { state, value ->
                    state.copy(recentActivity = value)
                }
        }
    }

    private fun <T> load(
        section: HomeSection,
        replaceInFlight: Boolean,
        fetch: suspend () -> Outcome<T>,
        empty: (T) -> Boolean,
        current: (HomeUiState) -> Section<T>,
        update: (HomeUiState, Section<T>) -> HomeUiState,
    ) {
        val running = sectionJobs[section]
        if (running?.isActive == true) {
            if (!replaceInFlight) return
            running.cancel()
        }
        val retained = current(state.value).displayed as? Section.Available
        state.update { update(it, if (retained == null) Section.Loading else Section.Refreshing(retained)) }
        val job =
            viewModelScope.launch(start = CoroutineStart.LAZY) {
                val result = fetch()
                ensureActive()
                val next =
                    when (result) {
                        is Outcome.Error ->
                            if (retained == null) Section.Error(result.failure) else Section.RefreshFailed(retained, result.failure)
                        is Outcome.Success -> if (empty(result.value)) Section.Empty else Section.Content(result.value)
                    }
                state.update { update(it, next) }
            }
        sectionJobs[section] = job
        job.start()
    }
}
