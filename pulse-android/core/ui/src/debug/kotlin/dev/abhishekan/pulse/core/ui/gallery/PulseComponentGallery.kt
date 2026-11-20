package dev.abhishekan.pulse.core.ui.gallery

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import dev.abhishekan.pulse.core.ui.PulseAdaptiveLayout
import dev.abhishekan.pulse.core.ui.PulseTheme
import dev.abhishekan.pulse.core.ui.PulseTokens
import dev.abhishekan.pulse.core.ui.PulseWindowSize
import dev.abhishekan.pulse.core.ui.R
import dev.abhishekan.pulse.core.ui.UiText
import dev.abhishekan.pulse.core.ui.components.PulseButton
import dev.abhishekan.pulse.core.ui.components.PulseButtonStyle
import dev.abhishekan.pulse.core.ui.components.PulseCard
import dev.abhishekan.pulse.core.ui.components.PulseEmptyState
import dev.abhishekan.pulse.core.ui.components.PulseErrorState
import dev.abhishekan.pulse.core.ui.components.PulseLoadingState
import dev.abhishekan.pulse.core.ui.components.PulseSnackbar
import dev.abhishekan.pulse.core.ui.components.PulseText
import dev.abhishekan.pulse.core.ui.components.PulseTextStyle
import dev.abhishekan.pulse.core.ui.components.PulseTopAppBar
import dev.abhishekan.pulse.core.ui.components.PulseTransferProgress
import kotlinx.coroutines.launch

internal enum class GalleryCategory(
    val label: String,
    val description: String,
) {
    Inputs("Inputs", "Text fields, passwords and dropdown selections."),
    Buttons("Buttons", "Every action style, emphasis and state."),
    Typography("Typography", "All text styles, hierarchy and text treatments."),
    Cards("Cards", "Standard and highlighted surfaces."),
    Chips("Chips & pills", "Filters, selections and icon treatments."),
    Selection("Selection controls", "Checkboxes, radio buttons and switches."),
    Navigation("Navigation", "App bars, bottom navigation and navigation rails."),
    Feedback("Feedback", "Snackbars, status colors and progress indicators."),
    Dialogs("Dialogs", "Alerts, custom content, date and time pickers."),
    Menus("Menus", "Dropdown menus and menu item states."),
    Calendar("Calendar", "Day selections, today and activity markers."),
    States("States", "Loading, empty and recoverable error states."),
    Icons("Icons", "The complete shared vector icon set."),
    Images("Images", "Shared image assets at different sizes."),
    Foundations("Foundations", "Color, spacing, shape and elevation tokens."),
    Appearance("Appearance", "Preview components in system, light or dark mode."),
}

/** Debug fixtures only; this screen never requests product data. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PulseComponentGallery(
    theme: String = "System",
    onTheme: (String) -> Unit = {},
    error: UiText? = null,
    onBack: () -> Unit = {},
) {
    var selectedName by rememberSaveable { mutableStateOf(GalleryCategory.Inputs.name) }
    val category = GalleryCategory.valueOf(selectedName)
    val stateHolder = rememberSaveableStateHolder()
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val actionCompleted = stringResource(R.string.pulse_ui_gallery_sample_action_completed)
    val onAction: () -> Unit = {
        scope.launch { snackbar.showSnackbar(actionCompleted) }
    }
    Scaffold(
        containerColor = PulseTheme.colors.background,
        contentColor = PulseTheme.colors.textPrimary,
        topBar = {
            Column {
                PulseTopAppBar(
                    "Component library",
                    navigationIcon = {
                        PulseButton("Back", onBack, style = PulseButtonStyle.Icon) {
                            Icon(painterResource(R.drawable.pulse_back), contentDescription = null)
                        }
                    },
                )
                PrimaryScrollableTabRow(
                    selectedTabIndex = category.ordinal,
                    containerColor = PulseTheme.colors.background,
                    contentColor = PulseTheme.colors.action,
                    edgePadding = PulseTokens.medium,
                ) {
                    GalleryCategory.entries.forEach { tab ->
                        Tab(
                            selected = category == tab,
                            onClick = { selectedName = tab.name },
                            selectedContentColor = PulseTheme.colors.action,
                            unselectedContentColor = PulseTheme.colors.textSecondary,
                            text = { PulseText(tab.label, style = PulseTextStyle.Label) },
                        )
                    }
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbar) { PulseSnackbar(it) } },
    ) { insets ->
        PulseAdaptiveLayout(Modifier.fillMaxSize().padding(insets).imePadding()) { size ->
            val margin = if (size == PulseWindowSize.Compact) PulseTokens.medium else PulseTokens.large
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
                stateHolder.SaveableStateProvider(category.name) {
                    Column(
                        Modifier
                            .widthIn(max = PulseTokens.readingWidth)
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                            .padding(margin),
                        verticalArrangement = Arrangement.spacedBy(PulseTokens.cardPadding),
                    ) {
                        PulseText(
                            stringResource(R.string.pulse_ui_gallery_design_system_debug_preview),
                            style = PulseTextStyle.Overline,
                            color = PulseTheme.colors.textSecondary,
                        )
                        PulseText(category.label, Modifier.semantics { heading() }, PulseTextStyle.Heading1)
                        PulseText(category.description, color = PulseTheme.colors.textSecondary)
                        when (category) {
                            GalleryCategory.Inputs -> GalleryInputs()
                            GalleryCategory.Buttons -> GalleryButtons(onAction)
                            GalleryCategory.Typography -> GalleryTypography()
                            GalleryCategory.Cards -> GalleryCards()
                            GalleryCategory.Chips -> GalleryChips()
                            GalleryCategory.Selection -> GallerySelection()
                            GalleryCategory.Navigation -> GalleryNavigation()
                            GalleryCategory.Feedback -> GalleryFeedback(snackbar)
                            GalleryCategory.Dialogs -> GalleryDialogs()
                            GalleryCategory.Menus -> GalleryMenus()
                            GalleryCategory.Calendar -> GalleryCalendar()
                            GalleryCategory.States -> States(size)
                            GalleryCategory.Icons -> GalleryIcons()
                            GalleryCategory.Images -> GalleryImages()
                            GalleryCategory.Foundations -> GalleryFoundations()
                            GalleryCategory.Appearance -> GalleryAppearance(theme, onTheme, error)
                        }
                    }
                }
            }
        }
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
