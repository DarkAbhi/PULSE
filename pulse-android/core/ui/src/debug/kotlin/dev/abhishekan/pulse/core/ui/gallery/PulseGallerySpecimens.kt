package dev.abhishekan.pulse.core.ui.gallery

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.Icon
import androidx.compose.material3.SnackbarDuration
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
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.abhishekan.pulse.core.domain.model.ThemePreference
import dev.abhishekan.pulse.core.ui.PulseTheme
import dev.abhishekan.pulse.core.ui.PulseTokens
import dev.abhishekan.pulse.core.ui.R
import dev.abhishekan.pulse.core.ui.UiText
import dev.abhishekan.pulse.core.ui.components.PulseBackNavigation
import dev.abhishekan.pulse.core.ui.components.PulseBottomNavigationBar
import dev.abhishekan.pulse.core.ui.components.PulseButton
import dev.abhishekan.pulse.core.ui.components.PulseButtonStyle
import dev.abhishekan.pulse.core.ui.components.PulseButtonVariant
import dev.abhishekan.pulse.core.ui.components.PulseCalendarDay
import dev.abhishekan.pulse.core.ui.components.PulseCard
import dev.abhishekan.pulse.core.ui.components.PulseCheckbox
import dev.abhishekan.pulse.core.ui.components.PulseDatePickerDialog
import dev.abhishekan.pulse.core.ui.components.PulseDialog
import dev.abhishekan.pulse.core.ui.components.PulseDropdownMenu
import dev.abhishekan.pulse.core.ui.components.PulseDropdownMenuItem
import dev.abhishekan.pulse.core.ui.components.PulseExtendedFloatingActionButton
import dev.abhishekan.pulse.core.ui.components.PulseFilterChip
import dev.abhishekan.pulse.core.ui.components.PulseFloatingActionButton
import dev.abhishekan.pulse.core.ui.components.PulseHorizontalDivider
import dev.abhishekan.pulse.core.ui.components.PulseLinearProgressIndicator
import dev.abhishekan.pulse.core.ui.components.PulseNavigationItem
import dev.abhishekan.pulse.core.ui.components.PulseNavigationRail
import dev.abhishekan.pulse.core.ui.components.PulsePasswordField
import dev.abhishekan.pulse.core.ui.components.PulseRadioButton
import dev.abhishekan.pulse.core.ui.components.PulseRefreshIndicator
import dev.abhishekan.pulse.core.ui.components.PulseSelectionField
import dev.abhishekan.pulse.core.ui.components.PulseSwitch
import dev.abhishekan.pulse.core.ui.components.PulseText
import dev.abhishekan.pulse.core.ui.components.PulseTextField
import dev.abhishekan.pulse.core.ui.components.PulseTextStyle
import dev.abhishekan.pulse.core.ui.components.PulseTimePickerDialog
import dev.abhishekan.pulse.core.ui.components.PulseTopAppBar
import dev.abhishekan.pulse.core.ui.components.PulseTransferProgress
import dev.abhishekan.pulse.core.ui.label
import dev.abhishekan.pulse.core.ui.uiText
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime

@Composable
private fun Specimen(
    title: String,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(PulseTokens.small)) {
        PulseText(title, Modifier.semantics { heading() }, PulseTextStyle.Title3)
        content()
    }
}

@Composable
private fun SampleIcon(
    resource: Int = R.drawable.pulse_check,
    description: String? = null,
) {
    Icon(painterResource(resource), description, Modifier.size(PulseTokens.cardPadding))
}

@Composable
internal fun GalleryInputs() {
    var text by rememberSaveable { mutableStateOf("") }
    var filled by rememberSaveable { mutableStateOf("Morning routine") }
    var password by remember { mutableStateOf("sample-password") }
    var choice by rememberSaveable { mutableIntStateOf(0) }
    val options = listOf("One", "Two", "Three")
    Specimen("Text fields · default, filled, supporting text") {
        PulseTextField(
            text,
            { text = it },
            "Sample username",
            Modifier.fillMaxWidth(),
            placeholder = { PulseText("Enter a username") },
            supportingText = { PulseText("A little guidance below the field.") },
        )
        PulseTextField(filled, { filled = it }, "Label above", Modifier.fillMaxWidth(), labelAbove = true)
        PulseTextField(
            text,
            { text = it },
            "With icons",
            Modifier.fillMaxWidth(),
            leadingIcon = { SampleIcon(R.drawable.pulse_person) },
            trailingIcon = { SampleIcon() },
        )
        PulseTextField("", {
        }, "Validation example", Modifier.fillMaxWidth(), error = uiText(R.string.pulse_ui_gallery_enter_a_username_to_continue))
        PulseTextField("Unavailable", {}, "Disabled field", Modifier.fillMaxWidth(), enabled = false)
    }
    Specimen("Passwords · reveal, validation, disabled") {
        PulsePasswordField(password, { password = it }, "Sample password", Modifier.fillMaxWidth())
        PulsePasswordField("", {
        }, "Password error", Modifier.fillMaxWidth(), error = uiText(R.string.pulse_ui_gallery_enter_a_username_to_continue))
        PulsePasswordField("sample", {}, "Disabled password", Modifier.fillMaxWidth(), enabled = false)
    }
    Specimen("Selection fields") {
        PulseSelectionField("Sample selection", options[choice], options, { choice = it })
        PulseSelectionField("Label above", options[choice], options, { choice = it }, labelAbove = true)
        PulseSelectionField("Disabled selection", "One", options, {}, enabled = false)
    }
}

@Composable
internal fun GalleryButtons(onAction: () -> Unit) {
    PulseButtonVariant.entries.forEach { variant ->
        Specimen(variant.name) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(PulseTokens.small)) {
                PulseButton(variant.name, onAction, variant = variant)
                PulseButton("Disabled", {}, enabled = false, variant = variant)
                PulseButton("Loading", {}, loading = true, variant = variant)
            }
        }
    }
    Specimen("Text actions") {
        PulseButton("Continue · sample", onAction, style = PulseButtonStyle.Text)
        PulseButton("Destructive text", onAction, variant = PulseButtonVariant.Destructive, style = PulseButtonStyle.Text)
        PulseButton("Disabled text", {}, enabled = false, style = PulseButtonStyle.Text)
        PulseButton("Loading text", {}, loading = true, style = PulseButtonStyle.Text)
    }
    Specimen("Icon actions") {
        FlowRow {
            PulseButton("Sample information", onAction, style = PulseButtonStyle.Icon, icon = { SampleIcon(R.drawable.pulse_bell) })
            PulseButton("Disabled icon", {}, enabled = false, style = PulseButtonStyle.Icon, icon = { SampleIcon() })
            PulseButton("Loading icon", {}, loading = true, style = PulseButtonStyle.Icon, icon = { SampleIcon() })
        }
        PulseButton("Leading icon", onAction, leadingIcon = { SampleIcon() })
        PulseButton("Trailing icon", onAction, trailingIcon = { SampleIcon() })
    }
    Specimen("Floating actions") {
        PulseFloatingActionButton(onAction) { SampleIcon(description = "Complete sample action") }
        PulseExtendedFloatingActionButton({ PulseText("Complete sample action") }, { SampleIcon() }, onAction)
    }
}

@Composable
internal fun GalleryTypography() {
    PulseTextStyle.entries.forEach { style ->
        Specimen(style.name + if (style == PulseTextStyle.Display) " · Heading1 alias" else "") {
            PulseText(if (style == PulseTextStyle.Metric) "12,480.50" else "Room for everyday life.", style = style)
        }
    }
    Specimen("Text colors") {
        val c = PulseTheme.colors
        listOf(
            "Primary" to c.textPrimary,
            "Secondary" to c.textSecondary,
            "Tertiary" to c.textTertiary,
            "Action" to c.action,
            "Success" to c.success,
            "Warning" to c.warning,
            "Error" to c.error,
            "Information" to c.info,
        ).forEach { (label, color) ->
            PulseText(label, color = color)
        }
    }
    Specimen("Alignment and overflow") {
        PulseText("Centered text", Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
        PulseText("End-aligned text", Modifier.fillMaxWidth(), textAlign = TextAlign.End)
        PulseText(
            "A longer sample of text that truncates with an ellipsis when it reaches the edge of its container.",
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        PulseText(uiText(R.string.pulse_ui_gallery_comfortable_reading_and_clear_hierarchy))
    }
}

@Composable
internal fun GalleryCards() {
    listOf(false, true).forEach { highlighted ->
        Specimen(if (highlighted) "Highlighted card" else "Standard card") {
            PulseCard(Modifier.fillMaxWidth(), highlighted = highlighted) {
                PulseText("A calmer daily rhythm", style = PulseTextStyle.Title)
                PulseText("04", style = PulseTextStyle.Metric)
                PulseText("One home for fitness, finances, meals and vehicle care.")
            }
        }
    }
}

@Composable
internal fun GalleryChips() {
    var selected by rememberSaveable { mutableStateOf(true) }
    Specimen("Interactive filter") { PulseFilterChip("Tap to toggle", selected, { selected = !selected }) }
    Specimen("Selection and disabled states") {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(PulseTokens.small)) {
            listOf(true, false).forEach { checked ->
                PulseFilterChip(if (checked) "Selected" else "Unselected", checked, {})
                PulseFilterChip(if (checked) "Disabled selected" else "Disabled unselected", checked, {}, enabled = false)
            }
        }
    }
    Specimen("Icon treatments") {
        PulseFilterChip("Without checkmark", true, {}, showSelectedIcon = false)
        PulseFilterChip("Leading icon", true, {}, leadingIcon = { SampleIcon(R.drawable.pulse_home) })
        PulseFilterChip("Trailing icon", false, {}, trailingIcon = { SampleIcon(R.drawable.pulse_back) })
    }
}

@Composable
internal fun GallerySelection() {
    var checked by rememberSaveable { mutableStateOf(true) }
    var switched by rememberSaveable { mutableStateOf(false) }
    var choice by rememberSaveable { mutableIntStateOf(0) }
    Specimen("Checkboxes") {
        PulseCheckbox(checked, { checked = it }, "Sample selection")
        PulseCheckbox(false, {}, "Unchecked")
        PulseCheckbox(true, {}, "Disabled checked", enabled = false)
        PulseCheckbox(false, {}, "Disabled unchecked", enabled = false)
    }
    Specimen("Switches") {
        PulseSwitch(switched, { switched = it }, "Sample switch")
        PulseSwitch(true, {}, "On")
        PulseSwitch(true, {}, "Disabled on", enabled = false)
        PulseSwitch(false, {}, "Disabled off", enabled = false)
    }
    Specimen("Radio buttons") {
        Column(Modifier.selectableGroup()) {
            listOf("First option", "Second option").forEachIndexed { index, label ->
                PulseRadioButton(choice == index, { choice = index }, label)
            }
        }
        PulseRadioButton(true, {}, "Disabled selected option", enabled = false)
        PulseRadioButton(false, {}, "Disabled option", enabled = false)
    }
}

@Composable
internal fun GalleryNavigation() {
    var selected by rememberSaveable { mutableIntStateOf(0) }
    val items =
        listOf(
            PulseNavigationItem("Home") { SampleIcon(R.drawable.pulse_home) },
            PulseNavigationItem("Fitness") { SampleIcon(R.drawable.pulse_fitness) },
            PulseNavigationItem("Garage") { SampleIcon(R.drawable.pulse_garage) },
        )
    Specimen("Top app bar") {
        PulseTopAppBar("Sample screen", navigationIcon = {
            PulseButton("Back", {}, style = PulseButtonStyle.Icon, icon = { SampleIcon(R.drawable.pulse_back) })
        }, actions = {
            PulseButton("Notifications", {}, style = PulseButtonStyle.Icon, icon = { SampleIcon(R.drawable.pulse_bell) })
        })
    }
    Specimen("Back navigation") {
        PulseBackNavigation("Back to sample", {})
        PulseBackNavigation("Disabled back", {}, enabled = false)
    }
    Specimen("Bottom navigation · tap to select") { PulseBottomNavigationBar(items, selected, { selected = it }) }
    Specimen("Navigation rail · tap to select") { PulseNavigationRail(items, selected, { selected = it }, Modifier.heightIn(max = 360.dp)) }
}

@Composable
internal fun GalleryFeedback(snackbar: SnackbarHostState) {
    val scope = rememberCoroutineScope()
    Specimen("Snackbar") {
        PulseButton("Show snackbar", { scope.launch { snackbar.showSnackbar("Sample action completed") } })
        PulseButton("Show snackbar with action", {
            scope.launch {
                snackbar.showSnackbar(
                    "Sample item removed",
                    actionLabel = "Undo",
                    withDismissAction = true,
                    duration = SnackbarDuration.Long,
                )
            }
        }, variant = PulseButtonVariant.Secondary)
    }
    Specimen("Status colors") {
        val c = PulseTheme.colors
        listOf(
            Triple("Success", c.success, c.successSoft),
            Triple("Warning", c.warning, c.warningSoft),
            Triple("Error", c.error, c.errorSoft),
            Triple("Information", c.info, c.infoSoft),
        ).forEach { (label, color, background) ->
            Box(Modifier.fillMaxWidth().background(background, PulseTheme.shapes.small).padding(PulseTokens.medium)) {
                PulseText(label, style = PulseTextStyle.Label, color = color)
            }
        }
        PulseText("Status color specimens. There are no shared badge or toast components yet.", style = PulseTextStyle.Caption)
    }
    Specimen("Determinate progress") {
        listOf(0f, .45f, 1f).forEach { progress ->
            PulseText("${(progress * 100).toInt()}%", style = PulseTextStyle.Label)
            PulseLinearProgressIndicator({ progress }, Modifier.fillMaxWidth())
        }
        PulseTransferProgress(.45f)
    }
    Specimen("Refresh · indeterminate") { PulseRefreshIndicator() }
    Specimen("Divider") { PulseHorizontalDivider() }
}

@Composable
internal fun GalleryDialogs() {
    var dialog by rememberSaveable { mutableStateOf<String?>(null) }
    var date by rememberSaveable { mutableStateOf("2026-10-17") }
    var time by rememberSaveable { mutableStateOf("09:30") }
    var note by rememberSaveable { mutableStateOf("") }
    Specimen("Alert dialog") { PulseButton("Preview alert dialog", { dialog = "alert" }) }
    Specimen("Custom content dialog") { PulseButton("Preview form dialog", { dialog = "form" }) }
    Specimen("Date picker") { PulseButton("Choose sample date · $date", { dialog = "date" }) }
    Specimen("Time picker") { PulseButton("Choose sample time · $time", { dialog = "time" }) }
    when (dialog) {
        "alert" ->
            PulseDialog("A moment to confirm", "This is a sample dialog. It won’t change your data.", "Done", { dialog = null }, {
                dialog =
                    null
            })
        "form" ->
            PulseDialog(
                onDismissRequest = { dialog = null },
                confirmButton = { PulseButton("Done", { dialog = null }, style = PulseButtonStyle.Text) },
                dismissButton = { PulseButton("Cancel", { dialog = null }, style = PulseButtonStyle.Text) },
                title = { PulseText("Sample note", style = PulseTextStyle.Title) },
                text = { PulseTextField(note, { note = it }, "Note", Modifier.fillMaxWidth()) },
            )
        "date" ->
            PulseDatePickerDialog(LocalDate.parse(date), {
                date = it.toString()
                dialog = null
            }, { dialog = null })
        "time" ->
            PulseTimePickerDialog(LocalTime.parse(time), {
                time = it.toString()
                dialog = null
            }, { dialog = null })
    }
}

@Composable
internal fun GalleryMenus() {
    var expanded by rememberSaveable { mutableStateOf(false) }
    var selection by rememberSaveable { mutableStateOf("No menu action selected") }
    Specimen("Dropdown menu") {
        Box {
            PulseButton("Open sample menu", { expanded = true })
            PulseDropdownMenu(expanded, { expanded = false }) {
                PulseDropdownMenuItem({ PulseText("Plain item") }, {
                    selection = "Plain item selected"
                    expanded = false
                })
                PulseDropdownMenuItem(
                    { PulseText("With icons") },
                    {
                        selection = "Icon item selected"
                        expanded = false
                    },
                    leadingIcon = { SampleIcon(R.drawable.pulse_home) },
                    trailingIcon = { SampleIcon() },
                )
                PulseDropdownMenuItem({ PulseText("Disabled item") }, {}, enabled = false)
            }
        }
        PulseText(selection)
    }
}

@Composable
internal fun GalleryCalendar() {
    var selected by rememberSaveable { mutableStateOf("17") }
    listOf(
        "17" to "Today with activity",
        "18" to "Activity recorded",
        "19" to "Regular day",
        "20" to "Disabled day",
        "30" to "Outside current month",
    ).forEach { (day, label) ->
        Specimen(label) {
            PulseCalendarDay(
                day,
                "$day October · $label",
                selected == day,
                day == "17",
                day in listOf("17", "18"),
                { selected = day },
                enabled = day != "20",
                inMonth = day != "30",
            )
        }
    }
}

private val galleryIcons =
    listOf(
        "Home" to R.drawable.pulse_home,
        "Garage" to R.drawable.pulse_garage,
        "Back" to R.drawable.pulse_back,
        "Check" to R.drawable.pulse_check,
        "Bell" to R.drawable.pulse_bell,
        "Fitness" to R.drawable.pulse_fitness,
        "Horizon" to R.drawable.pulse_horizon,
        "Meals" to R.drawable.pulse_meals,
        "Person" to R.drawable.pulse_person,
        "Spaces" to R.drawable.pulse_spaces,
    )

@Composable
internal fun GalleryIcons() {
    galleryIcons.forEach { (name, resource) ->
        Specimen(name) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(PulseTokens.medium)) {
                listOf(PulseTheme.colors.textPrimary, PulseTheme.colors.action, PulseTheme.colors.textSecondary).forEach { tint ->
                    Icon(painterResource(resource), name, Modifier.size(PulseTokens.cardPadding), tint = tint)
                }
            }
        }
    }
}

@Composable
internal fun GalleryImages() {
    PulseText("The shared image asset is the Pulse logo.")
    listOf(48, 96, 160).forEach { size ->
        Specimen("Pulse logo · ${size}dp") {
            Image(painterResource(R.drawable.pulse_logo), "Pulse logo", Modifier.size(size.dp))
        }
    }
}

@Composable
internal fun GalleryAppearance(
    theme: String,
    onTheme: (String) -> Unit,
    error: UiText?,
) {
    Specimen("Theme · applies to every gallery tab") {
        Column(Modifier.selectableGroup()) {
            listOf("System", "Light", "Dark").forEach { value ->
                PulseRadioButton(theme == value, { onTheme(value) }, ThemePreference.valueOf(value).label, Modifier.fillMaxWidth())
            }
        }
        if (error != null) PulseText(error, color = PulseTheme.colors.error)
    }
    PulseFoundationBoard()
}

@Composable
internal fun GalleryFoundations() {
    val c = PulseTheme.colors
    val spacing = PulseTheme.spacing
    Specimen("Color palette") {
        val palette =
            listOf(
                "background" to c.background,
                "surface" to c.surface,
                "surfaceMuted" to c.surfaceMuted,
                "surfaceStrong" to c.surfaceStrong,
                "textPrimary" to c.textPrimary,
                "textSecondary" to c.textSecondary,
                "textTertiary" to c.textTertiary,
                "primary" to c.primary,
                "primaryPressed" to c.primaryPressed,
                "onPrimary" to c.onPrimary,
                "primarySoft" to c.primarySoft,
                "onPrimarySoft" to c.onPrimarySoft,
                "action" to c.action,
                "border" to c.border,
                "outline" to c.outline,
                "divider" to c.divider,
                "success" to c.success,
                "successSoft" to c.successSoft,
                "warning" to c.warning,
                "warningSoft" to c.warningSoft,
                "error" to c.error,
                "errorSoft" to c.errorSoft,
                "info" to c.info,
                "infoSoft" to c.infoSoft,
                "coral" to c.coral,
                "sage" to c.sage,
                "sky" to c.sky,
                "lavender" to c.lavender,
                "inverseSurface" to c.inverseSurface,
                "inverseText" to c.inverseText,
                "inverseAction" to c.inverseAction,
            )
        palette.forEach { (name, color) ->
            Row(horizontalArrangement = Arrangement.spacedBy(spacing.small)) {
                Box(Modifier.size(spacing.target).background(color, PulseTheme.shapes.small))
                PulseText("$name · #%06X".format(color.toArgb() and 0xFFFFFF), style = PulseTextStyle.Body2)
            }
        }
    }
    Specimen("Spacing") {
        listOf(
            "None" to spacing.none,
            "Tiny" to spacing.tiny,
            "Small" to spacing.small,
            "Compact" to spacing.compact,
            "Medium" to spacing.medium,
            "Comfortable" to spacing.comfortable,
            "Card padding" to spacing.cardPadding,
            "Large" to spacing.large,
            "Extra large" to spacing.extraLarge,
            "Target" to spacing.target,
            "Spacious" to spacing.spacious,
        ).forEach { (name, value) ->
            PulseText("$name · $value", style = PulseTextStyle.Label)
            Box(Modifier.size(width = value, height = spacing.small).background(c.action))
        }
    }
    Specimen("Shapes") {
        val shapes = PulseTheme.shapes
        listOf(
            "None" to shapes.none,
            "Extra small" to shapes.extraSmall,
            "Small" to shapes.small,
            "Medium" to shapes.medium,
            "Large" to shapes.large,
            "Extra large" to shapes.extraLarge,
            "Pill" to shapes.pill,
        ).forEach { (name, shape) ->
            Surface(shape = shape, color = c.primarySoft, contentColor = c.onPrimarySoft) {
                PulseText(name, Modifier.padding(spacing.cardPadding))
            }
        }
    }
    Specimen("Elevation") {
        val elevation = PulseTheme.elevation
        listOf(
            "None" to elevation.none,
            "Low" to elevation.low,
            "Medium" to elevation.medium,
            "High" to elevation.high,
        ).forEach { (name, value) ->
            Surface(
                shape = PulseTheme.shapes.medium,
                color = c.surface,
                contentColor = c.textPrimary,
                shadowElevation = value,
            ) {
                PulseText("$name · $value", Modifier.padding(spacing.cardPadding))
            }
        }
    }
}
