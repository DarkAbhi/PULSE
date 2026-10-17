package dev.abhishekan.pulse.core.ui.gallery

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import dev.abhishekan.pulse.core.ui.*
import dev.abhishekan.pulse.core.ui.components.*
import kotlinx.coroutines.launch

/** Debug fixtures only; this screen never requests product data. */
@Composable
fun PulseComponentGallery(theme: String = "System", onTheme: (String) -> Unit = {}, error: String? = null) {
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var showDialog by rememberSaveable { mutableStateOf(false) }
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    Scaffold(
        topBar = { PulseTopAppBar("Pulse / Studio") },
        snackbarHost = { SnackbarHost(snackbar) { PulseSnackbar(it) } },
        bottomBar = {
            PulseBottomNavigationBar(
                listOf(PulseNavigationItem("Controls") { PulseText("◉") }, PulseNavigationItem("States") { PulseText("◌") }),
                selectedTab, { selectedTab = it })
        },
    ) { insets ->
        PulseAdaptiveLayout(Modifier.fillMaxSize().padding(insets).imePadding()) { size ->
            val margin = when (size) {
                PulseWindowSize.Compact -> PulseTokens.medium
                PulseWindowSize.Medium -> PulseTokens.cardPadding
                PulseWindowSize.Expanded -> PulseTokens.large
            }
            Column(Modifier.widthIn(max = PulseTokens.contentWidth).fillMaxWidth()
                .verticalScroll(rememberScrollState()).padding(margin), verticalArrangement = Arrangement.spacedBy(PulseTokens.cardPadding)) {
                PulseText("A little more space\nfor everyday life.", Modifier.semantics { heading() }, PulseTextStyle.Display)
                PulseText("DESIGN SYSTEM · DEBUG PREVIEW", style = PulseTextStyle.Label, color = MaterialTheme.colorScheme.primary)
                PulseText("Sample content only. No account or live activity is shown.")
                PulseCard(Modifier.fillMaxWidth(), highlighted = true) {
                    PulseText("A calmer daily rhythm", style = PulseTextStyle.Title)
                    PulseText("04", style = PulseTextStyle.Metric)
                    PulseText("One home for fitness, finances, meals, and vehicle care.")
                }
                if (selectedTab == 0) {
                    if (size == PulseWindowSize.Compact) {
                        Controls { scope.launch { snackbar.showSnackbar("Sample action completed") } }
                        Appearance(theme, onTheme, error) { showDialog = true }
                    } else {
                        Row(horizontalArrangement = Arrangement.spacedBy(PulseTokens.cardPadding)) {
                            Box(Modifier.weight(if (size == PulseWindowSize.Expanded) 1.2f else 1f)) {
                                Controls { scope.launch { snackbar.showSnackbar("Sample action completed") } }
                            }
                            Box(Modifier.weight(1f)) { Appearance(theme, onTheme, error) { showDialog = true } }
                        }
                    }
                } else {
                    States(size)
                }
                PulseText("Built for your pace.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
    if (showDialog) PulseDialog("A moment to confirm", "This is a sample dialog. It won’t change account data.", "Done",
        onConfirm = { showDialog = false; scope.launch { snackbar.showSnackbar("Sample confirmed") } }, onDismiss = { showDialog = false })
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
        PulseText("Calendar controls", style = PulseTextStyle.Title)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(PulseTokens.small)) {
            PulseCalendarDay("17", "17 October, today, selected, workout recorded", true, true, true, {})
            PulseCalendarDay("18", "18 October, workout recorded", false, false, true, {})
            PulseCalendarDay("19", "19 October, no workouts", false, false, false, {})
            PulseCalendarDay("20", "20 October, disabled", false, false, false, {}, enabled = false)
        }
        PulseTextButton("Choose sample date: $selectedDate", { showDate = true })
        PulseSelectionField("Sample selection", listOf("One", "Two")[choice], listOf("One", "Two"), { choice = it }, Modifier.fillMaxWidth())
        PulseSelectionField("Disabled selection", "One", listOf("One"), {}, enabled = false)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(PulseTokens.small)) {
            PulseFilterChip("Selected", true, {}); PulseFilterChip("Unselected", false, {}); PulseFilterChip("Disabled", false, {}, enabled = false)
        }
        PulseTextButton("Choose sample time", { showTime = true })
        PulseText("The essentials", style = PulseTextStyle.Title)
        PulseText("Forms and actions", color = MaterialTheme.colorScheme.onSurfaceVariant)
        PulseTextField(username, { username = it }, "Sample username", Modifier.fillMaxWidth())
        PulsePasswordField(password, { password = it }, "Sample password", Modifier.fillMaxWidth())
        PulseTextField("", {}, "Validation example", Modifier.fillMaxWidth(), error = "Enter a username to continue.")
        PulseTextField("Unavailable", {}, "Disabled field", Modifier.fillMaxWidth(), enabled = false)
        PulseButton("Continue · sample", onAction, Modifier.fillMaxWidth())
        PulseButton("Please wait", {}, Modifier.fillMaxWidth(), loading = true)
        PulseButton("Unavailable", {}, Modifier.fillMaxWidth(), enabled = false)
        PulseTextButton("A quieter action", onAction)
        PulseIconButton("Sample information", onAction) { PulseText("i", style = PulseTextStyle.Title) }
        PulseCheckbox(checked, { checked = it }, "Sample selection")
        PulseCheckbox(false, {}, "Disabled selection", enabled = false)
        PulseSwitch(switched, { switched = it }, "Sample switch")
        PulseSwitch(false, {}, "Disabled switch", enabled = false)
    }
    if (showTime) PulseTimePickerDialog(java.time.LocalTime.of(9, 30), { showTime = false }, { showTime = false })
    if (showDate) PulseDatePickerDialog(java.time.LocalDate.parse(selectedDate), { selectedDate = it.toString(); showDate = false }, { showDate = false })
}

@Composable
private fun Appearance(theme: String, onTheme: (String) -> Unit, error: String?, onDialog: () -> Unit) {
    PulseCard(Modifier.fillMaxWidth()) {
        PulseText("Make it yours", style = PulseTextStyle.Title)
        PulseText("Appearance is saved on this device.")
        Column(Modifier.selectableGroup()) {
            listOf("System", "Light", "Dark").forEach { value ->
                PulseRadioButton(theme == value, { onTheme(value) }, value, Modifier.fillMaxWidth())
            }
            PulseRadioButton(false, {}, "Disabled option", enabled = false)
        }
        if (error != null) PulseText(error, color = MaterialTheme.colorScheme.error)
        PulseText("Aa", style = PulseTextStyle.Display)
        PulseText("Everyday, considered.", style = PulseTextStyle.Title)
        PulseText("Comfortable reading and clear hierarchy.")
        PulseText("SMALL DETAILS, CLEAR PURPOSE", style = PulseTextStyle.Label)
        PulseText("12,480.50", style = PulseTextStyle.Metric)
        PulseButton("Preview dialog", onDialog)
    }
}

@Composable
private fun States(size: PulseWindowSize) {
    Column(verticalArrangement = Arrangement.spacedBy(PulseTokens.cardPadding)) {
        PulseCard(Modifier.fillMaxWidth()) { PulseLoadingState("Loading sample section"); PulseTransferProgress(.45f) }
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
    PulseCard(Modifier.fillMaxWidth()) { PulseEmptyState("Nothing here yet", "Your updates will appear here when available.") }
}
@Composable private fun ErrorCard() {
    var retried by rememberSaveable { mutableStateOf(false) }
    PulseCard(Modifier.fillMaxWidth()) {
        if (retried) PulseEmptyState("Sample retry complete", "This demo made no network request.")
        else PulseErrorState("A sample connection problem. Other sections remain available.", { retried = true })
    }
}

@Preview(name = "Phone / Light", widthDp = 360, heightDp = 800, showBackground = true)
@Preview(name = "Tablet / Light", widthDp = 1024, heightDp = 900, showBackground = true)
@Preview(name = "Large type", widthDp = 360, heightDp = 800, fontScale = 2f, showBackground = true)
@Composable
private fun LightGalleryPreview() { PulseTheme(false) { PulseComponentGallery("Light") } }

@Preview(name = "Phone / Dark", widthDp = 360, heightDp = 800, showBackground = true)
@Preview(name = "Tablet / Dark", widthDp = 1024, heightDp = 900, showBackground = true)
@Composable
private fun DarkGalleryPreview() { PulseTheme(true) { PulseComponentGallery("Dark") } }

@Preview(name = "States / Light", widthDp = 360, heightDp = 900, showBackground = true)
@Composable
private fun LightStatesPreview() {
    PulseTheme(false) { Surface { Column(Modifier.padding(PulseTokens.medium)) { States(PulseWindowSize.Compact) } } }
}

@Preview(name = "States / Dark tablet", widthDp = 1024, heightDp = 900, fontScale = 1.3f, showBackground = true)
@Composable
private fun DarkStatesPreview() {
    PulseTheme(true) { Surface { Column(Modifier.padding(PulseTokens.large)) { States(PulseWindowSize.Expanded) } } }
}
