package dev.abhishekan.pulse.core.ui.gallery
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import dev.abhishekan.pulse.core.domain.model.ThemePreference
import dev.abhishekan.pulse.core.ui.PulseAdaptiveLayout
import dev.abhishekan.pulse.core.ui.PulseTheme
import dev.abhishekan.pulse.core.ui.PulseTokens
import dev.abhishekan.pulse.core.ui.PulseWindowSize
import dev.abhishekan.pulse.core.ui.R
import dev.abhishekan.pulse.core.ui.UiText
import dev.abhishekan.pulse.core.ui.components.PulseBottomNavigationBar
import dev.abhishekan.pulse.core.ui.components.PulseButton
import dev.abhishekan.pulse.core.ui.components.PulseCalendarDay
import dev.abhishekan.pulse.core.ui.components.PulseCard
import dev.abhishekan.pulse.core.ui.components.PulseCheckbox
import dev.abhishekan.pulse.core.ui.components.PulseDatePickerDialog
import dev.abhishekan.pulse.core.ui.components.PulseDialog
import dev.abhishekan.pulse.core.ui.components.PulseEmptyState
import dev.abhishekan.pulse.core.ui.components.PulseErrorState
import dev.abhishekan.pulse.core.ui.components.PulseFilterChip
import dev.abhishekan.pulse.core.ui.components.PulseIconButton
import dev.abhishekan.pulse.core.ui.components.PulseLoadingState
import dev.abhishekan.pulse.core.ui.components.PulseNavigationItem
import dev.abhishekan.pulse.core.ui.components.PulsePasswordField
import dev.abhishekan.pulse.core.ui.components.PulseRadioButton
import dev.abhishekan.pulse.core.ui.components.PulseSelectionField
import dev.abhishekan.pulse.core.ui.components.PulseSnackbar
import dev.abhishekan.pulse.core.ui.components.PulseSwitch
import dev.abhishekan.pulse.core.ui.components.PulseText
import dev.abhishekan.pulse.core.ui.components.PulseTextButton
import dev.abhishekan.pulse.core.ui.components.PulseTextField
import dev.abhishekan.pulse.core.ui.components.PulseTextStyle
import dev.abhishekan.pulse.core.ui.components.PulseTimePickerDialog
import dev.abhishekan.pulse.core.ui.components.PulseTopAppBar
import dev.abhishekan.pulse.core.ui.components.PulseTransferProgress
import dev.abhishekan.pulse.core.ui.label
import dev.abhishekan.pulse.core.ui.uiText
import kotlinx.coroutines.launch

/** Debug fixtures only; this screen never requests product data. */
@Composable
fun PulseComponentGallery(
    theme: String = "System",
    onTheme: (String) -> Unit = {},
    error: UiText? = null,
) {
    val actionCompleted = stringResource(R.string.pulse_ui_gallery_sample_action_completed)
    val sampleConfirmed = stringResource(R.string.pulse_ui_gallery_sample_confirmed)
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var showDialog by rememberSaveable { mutableStateOf(false) }
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    Scaffold(
        topBar = { PulseTopAppBar(stringResource(R.string.pulse_ui_gallery_pulse_studio)) },
        snackbarHost = { SnackbarHost(snackbar) { PulseSnackbar(it) } },
        bottomBar = {
            PulseBottomNavigationBar(
                listOf(
                    PulseNavigationItem(stringResource(R.string.pulse_ui_gallery_controls)) {
                        PulseText("◉")
                    },
                    PulseNavigationItem(stringResource(R.string.pulse_ui_gallery_states)) {
                        PulseText("◌")
                    },
                ),
                selectedTab,
                { selectedTab = it },
            )
        },
    ) { insets ->
        PulseAdaptiveLayout(Modifier.fillMaxSize().padding(insets).imePadding()) { size ->
            val margin =
                when (size) {
                    PulseWindowSize.Compact -> PulseTokens.medium
                    PulseWindowSize.Medium -> PulseTokens.cardPadding
                    PulseWindowSize.Expanded -> PulseTokens.large
                }
            Column(
                Modifier
                    .widthIn(max = PulseTokens.contentWidth)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(margin),
                verticalArrangement = Arrangement.spacedBy(PulseTokens.cardPadding),
            ) {
                PulseText(
                    stringResource(R.string.pulse_ui_gallery_a_little_more_space_for_everyday_life),
                    Modifier.semantics {
                        heading()
                    },
                    PulseTextStyle.Display,
                )
                PulseText(
                    stringResource(R.string.pulse_ui_gallery_design_system_debug_preview),
                    style = PulseTextStyle.Label,
                    color = MaterialTheme.colorScheme.primary,
                )
                PulseText(
                    stringResource(
                        R.string.pulse_ui_gallery_sample_content_only_no_account_or_live_activity_is,
                    ),
                )
                PulseCard(Modifier.fillMaxWidth(), highlighted = true) {
                    PulseText(
                        stringResource(R.string.pulse_ui_gallery_a_calmer_daily_rhythm),
                        style = PulseTextStyle.Title,
                    )
                    PulseText("04", style = PulseTextStyle.Metric)
                    PulseText(
                        stringResource(
                            R.string.pulse_ui_gallery_one_home_for_fitness_finances_meals_and_vehicle_care,
                        ),
                    )
                }
                if (selectedTab == 0) {
                    if (size == PulseWindowSize.Compact) {
                        Controls { scope.launch { snackbar.showSnackbar(actionCompleted) } }
                        Appearance(theme, onTheme, error) { showDialog = true }
                    } else {
                        Row(horizontalArrangement = Arrangement.spacedBy(PulseTokens.cardPadding)) {
                            Box(
                                Modifier.weight(
                                    if (size ==
                                        PulseWindowSize.Expanded
                                    ) {
                                        1.2f
                                    } else {
                                        1f
                                    },
                                ),
                            ) {
                                Controls { scope.launch { snackbar.showSnackbar(actionCompleted) } }
                            }
                            Box(Modifier.weight(1f)) {
                                Appearance(theme, onTheme, error) {
                                    showDialog =
                                        true
                                }
                            }
                        }
                    }
                } else {
                    States(size)
                }
                PulseText(
                    stringResource(R.string.pulse_ui_gallery_built_for_your_pace),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
    if (showDialog) {
        PulseDialog(
            stringResource(R.string.pulse_ui_gallery_a_moment_to_confirm),
            stringResource(R.string.pulse_ui_gallery_this_is_a_sample_dialog_it_won_t_change),
            stringResource(R.string.pulse_ui_gallery_done),
            onConfirm = {
                showDialog = false
                scope.launch { snackbar.showSnackbar(sampleConfirmed) }
            },
            onDismiss = {
                showDialog =
                    false
            },
        )
    }
}

@Composable
private fun Controls(onAction: () -> Unit) {
    var username by rememberSaveable { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var checked by rememberSaveable { mutableStateOf(true) }
    var switched by rememberSaveable { mutableStateOf(false) }
    var choice by rememberSaveable { mutableIntStateOf(0) }
    var showTime by rememberSaveable { mutableStateOf(false) }
    var showDate by rememberSaveable { mutableStateOf(false) }
    var selectedDate by rememberSaveable { mutableStateOf("2026-10-17") }
    PulseCard(Modifier.fillMaxWidth()) {
        PulseText(
            stringResource(R.string.pulse_ui_gallery_calendar_controls),
            style = PulseTextStyle.Title,
        )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(PulseTokens.small)) {
            PulseCalendarDay(
                "17",
                stringResource(
                    R.string.pulse_ui_gallery_17_october_today_selected_workout_recorded,
                ),
                true,
                true,
                true,
                {
                },
            )
            PulseCalendarDay(
                "18",
                stringResource(
                    R.string.pulse_ui_gallery_18_october_workout_recorded,
                ),
                false,
                false,
                true,
                {
                },
            )
            PulseCalendarDay(
                "19",
                stringResource(
                    R.string.pulse_ui_gallery_19_october_no_workouts,
                ),
                false,
                false,
                false,
                {
                },
            )
            PulseCalendarDay(
                "20",
                stringResource(
                    R.string.pulse_ui_gallery_20_october_disabled,
                ),
                false,
                false,
                false,
                {
                },
                enabled = false,
            )
        }
        PulseTextButton(
            stringResource(
                R.string.pulse_ui_gallery_choose_sample_date_1_s,
                selectedDate,
            ),
            {
                showDate =
                    true
            },
        )
        PulseSelectionField(
            stringResource(
                R.string.pulse_ui_gallery_sample_selection,
            ),
            listOf(
                stringResource(R.string.pulse_ui_gallery_one),
                stringResource(R.string.pulse_ui_gallery_two),
            )[choice],
            listOf(
                stringResource(R.string.pulse_ui_gallery_one),
                stringResource(R.string.pulse_ui_gallery_two),
            ),
            {
                choice =
                    it
            },
            Modifier.fillMaxWidth(),
        )
        PulseSelectionField(
            stringResource(
                R.string.pulse_ui_gallery_disabled_selection,
            ),
            stringResource(
                R.string.pulse_ui_gallery_one,
            ),
            listOf(stringResource(R.string.pulse_ui_gallery_one)),
            {
            },
            enabled = false,
        )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(PulseTokens.small)) {
            PulseFilterChip(stringResource(R.string.pulse_ui_gallery_selected), true, {})
            PulseFilterChip(stringResource(R.string.pulse_ui_gallery_unselected), false, {})
            PulseFilterChip(stringResource(R.string.pulse_ui_gallery_disabled), false, {
            }, enabled = false)
        }
        PulseTextButton(stringResource(R.string.pulse_ui_gallery_choose_sample_time), {
            showTime =
                true
        })
        PulseText(
            stringResource(R.string.pulse_ui_gallery_the_essentials),
            style = PulseTextStyle.Title,
        )
        PulseText(
            stringResource(R.string.pulse_ui_gallery_forms_and_actions),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        PulseTextField(username, {
            username = it
        }, stringResource(R.string.pulse_ui_gallery_sample_username), Modifier.fillMaxWidth())
        PulsePasswordField(password, {
            password = it
        }, stringResource(R.string.pulse_ui_gallery_sample_password), Modifier.fillMaxWidth())
        PulseTextField(
            "",
            {
            },
            stringResource(
                R.string.pulse_ui_gallery_validation_example,
            ),
            Modifier.fillMaxWidth(),
            error =
                uiText(
                    R.string.pulse_ui_gallery_enter_a_username_to_continue,
                ),
        )
        PulseTextField(
            stringResource(R.string.pulse_ui_gallery_unavailable),
            {
            },
            stringResource(
                R.string.pulse_ui_gallery_disabled_field,
            ),
            Modifier.fillMaxWidth(),
            enabled = false,
        )
        PulseButton(
            stringResource(R.string.pulse_ui_gallery_continue_sample),
            onAction,
            Modifier.fillMaxWidth(),
        )
        PulseButton(stringResource(R.string.pulse_ui_gallery_please_wait), {
        }, Modifier.fillMaxWidth(), loading = true)
        PulseButton(stringResource(R.string.pulse_ui_gallery_unavailable), {
        }, Modifier.fillMaxWidth(), enabled = false)
        PulseTextButton(stringResource(R.string.pulse_ui_gallery_a_quieter_action), onAction)
        PulseIconButton(stringResource(R.string.pulse_ui_gallery_sample_information), onAction) {
            PulseText("i", style = PulseTextStyle.Title)
        }
        PulseCheckbox(checked, {
            checked = it
        }, stringResource(R.string.pulse_ui_gallery_sample_selection))
        PulseCheckbox(false, {
        }, stringResource(R.string.pulse_ui_gallery_disabled_selection), enabled = false)
        PulseSwitch(switched, {
            switched = it
        }, stringResource(R.string.pulse_ui_gallery_sample_switch))
        PulseSwitch(false, {
        }, stringResource(R.string.pulse_ui_gallery_disabled_switch), enabled = false)
    }
    if (showTime) {
        PulseTimePickerDialog(java.time.LocalTime.of(9, 30), { showTime = false }, {
            showTime =
                false
        })
    }
    if (showDate) {
        PulseDatePickerDialog(java.time.LocalDate.parse(selectedDate), {
            selectedDate =
                it.toString()
            showDate = false
        }, { showDate = false })
    }
}

@Composable
private fun Appearance(
    theme: String,
    onTheme: (String) -> Unit,
    error: UiText?,
    onDialog: () -> Unit,
) {
    PulseCard(Modifier.fillMaxWidth()) {
        PulseText(
            stringResource(R.string.pulse_ui_gallery_make_it_yours),
            style = PulseTextStyle.Title,
        )
        PulseText(stringResource(R.string.pulse_ui_gallery_appearance_is_saved_on_this_device))
        Column(Modifier.selectableGroup()) {
            listOf("System", "Light", "Dark").forEach { value ->
                PulseRadioButton(theme == value, {
                    onTheme(value)
                }, ThemePreference.valueOf(value).label, Modifier.fillMaxWidth())
            }
            PulseRadioButton(false, {
            }, stringResource(R.string.pulse_ui_gallery_disabled_option), enabled = false)
        }
        if (error != null) PulseText(error, color = MaterialTheme.colorScheme.error)
        PulseText("Aa", style = PulseTextStyle.Display)
        PulseText(
            stringResource(R.string.pulse_ui_gallery_everyday_considered),
            style = PulseTextStyle.Title,
        )
        PulseText(stringResource(R.string.pulse_ui_gallery_comfortable_reading_and_clear_hierarchy))
        PulseText(
            stringResource(R.string.pulse_ui_gallery_small_details_clear_purpose),
            style = PulseTextStyle.Label,
        )
        PulseText("12,480.50", style = PulseTextStyle.Metric)
        PulseButton(stringResource(R.string.pulse_ui_gallery_preview_dialog), onDialog)
    }
}

@Composable
private fun States(size: PulseWindowSize) {
    Column(verticalArrangement = Arrangement.spacedBy(PulseTokens.cardPadding)) {
        PulseCard(Modifier.fillMaxWidth()) {
            PulseLoadingState(stringResource(R.string.pulse_ui_gallery_loading_sample_section))
            PulseTransferProgress(.45f)
        }
        if (size == PulseWindowSize.Compact) {
            EmptyCard()
            ErrorCard()
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(PulseTokens.cardPadding)) {
                Box(Modifier.weight(1f)) { EmptyCard() }
                Box(Modifier.weight(1f)) { ErrorCard() }
            }
        }
    }
}

@Composable private fun EmptyCard() {
    PulseCard(Modifier.fillMaxWidth()) {
        PulseEmptyState(
            stringResource(R.string.pulse_ui_gallery_nothing_here_yet),
            stringResource(R.string.pulse_ui_gallery_your_updates_will_appear_here_when_available),
        )
    }
}

@Composable private fun ErrorCard() {
    var retried by rememberSaveable { mutableStateOf(false) }
    PulseCard(Modifier.fillMaxWidth()) {
        if (retried) {
            PulseEmptyState(
                stringResource(R.string.pulse_ui_gallery_sample_retry_complete),
                stringResource(R.string.pulse_ui_gallery_this_demo_made_no_network_request),
            )
        } else {
            PulseErrorState(
                stringResource(
                    R.string.pulse_ui_gallery_a_sample_connection_problem_other_sections_remain_available,
                ),
                {
                    retried =
                        true
                },
            )
        }
    }
}

@Preview(name = "Phone / Light", widthDp = 360, heightDp = 800, showBackground = true)
@Preview(name = "Tablet / Light", widthDp = 1024, heightDp = 900, showBackground = true)
@Preview(name = "Large type", widthDp = 360, heightDp = 800, fontScale = 2f, showBackground = true)
@Composable
private fun LightGalleryPreview() {
    PulseTheme(false) { PulseComponentGallery("Light") }
}

@Preview(name = "Phone / Dark", widthDp = 360, heightDp = 800, showBackground = true)
@Preview(name = "Tablet / Dark", widthDp = 1024, heightDp = 900, showBackground = true)
@Composable
private fun DarkGalleryPreview() {
    PulseTheme(true) { PulseComponentGallery("Dark") }
}

@Preview(name = "States / Light", widthDp = 360, heightDp = 900, showBackground = true)
@Composable
private fun LightStatesPreview() {
    PulseTheme(false) {
        Surface { Column(Modifier.padding(PulseTokens.medium)) { States(PulseWindowSize.Compact) } }
    }
}

@Preview(
    name = "States / Dark tablet",
    widthDp = 1024,
    heightDp = 900,
    fontScale = 1.3f,
    showBackground = true,
)
@Composable
private fun DarkStatesPreview() {
    PulseTheme(true) {
        Surface { Column(Modifier.padding(PulseTokens.large)) { States(PulseWindowSize.Expanded) } }
    }
}
