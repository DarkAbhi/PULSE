package dev.abhishekan.pulse.feature.horizon
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import dev.abhishekan.pulse.core.domain.model.Failure
import dev.abhishekan.pulse.core.domain.model.HorizonOverview
import dev.abhishekan.pulse.core.domain.model.HorizonProjection
import dev.abhishekan.pulse.core.ui.PulseAdaptiveLayout
import dev.abhishekan.pulse.core.ui.PulseTheme
import dev.abhishekan.pulse.core.ui.PulseTokens
import dev.abhishekan.pulse.core.ui.PulseWindowSize
import dev.abhishekan.pulse.core.ui.components.PulseButton
import dev.abhishekan.pulse.core.ui.components.PulseEmptyState
import dev.abhishekan.pulse.core.ui.components.PulseText
import dev.abhishekan.pulse.core.ui.components.PulseTextStyle

private val sample =
    HorizonOverview(
        "USD",
        "2400.00".toBigDecimal(),
        "875.50".toBigDecimal(),
        "1500.00".toBigDecimal(),
        "120.00".toBigDecimal(),
        "600.00".toBigDecimal(),
        "24.50".toBigDecimal(),
        "63.52".toBigDecimal(),
        emptyList(),
        emptyList(),
        emptyList(),
        emptyList(),
        listOf(HorizonProjection(3, "3 Months", "2626.50".toBigDecimal())),
    )

@Preview(name = "Finance phone light", widthDp = 360, heightDp = 800)
@Preview(name = "Finance tablet light", widthDp = 1024, heightDp = 900)
@Preview(name = "Finance large text", widthDp = 360, heightDp = 800, fontScale = 2f)
@Preview(name = "Finance landscape", widthDp = 800, heightDp = 360)
@Composable
private fun LightOverview() {
    FinancePreview(false)
}

@Preview(name = "Finance tablet dark", widthDp = 1024, heightDp = 900)
@Composable
private fun DarkOverview() {
    FinancePreview(true)
}

@Composable private fun FinancePreview(dark: Boolean) {
    PulseTheme(dark) {
        Surface {
            PulseAdaptiveLayout { width ->
                Column(
                    Modifier
                        .fillMaxSize()
                        .verticalScroll(
                            rememberScrollState(),
                        ).padding(PulseTokens.medium),
                    verticalArrangement = Arrangement.spacedBy(PulseTokens.cardPadding),
                ) {
                    PulseText("Preview fixtures", style = PulseTextStyle.Label)
                    if (width ==
                        PulseWindowSize.Compact
                    ) {
                        OverviewCard(sample, true, {})
                        CommitmentCard(sample)
                        ProjectionCard(sample)
                    } else {
                        Row(horizontalArrangement = Arrangement.spacedBy(PulseTokens.cardPadding)) {
                            Column(Modifier.weight(1.2f)) { OverviewCard(sample, true, {}) }
                            Column(
                                Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(PulseTokens.cardPadding),
                            ) {
                                CommitmentCard(sample)
                                ProjectionCard(sample)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Preview(name = "Finance editor dark", widthDp = 360, heightDp = 800)
@Preview(name = "Finance editor tablet", widthDp = 1024, heightDp = 900)
@Composable
private fun EditorPreview() {
    PulseTheme(true) {
        Surface {
            Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(
                        rememberScrollState(),
                    ).padding(PulseTokens.medium),
            ) {
                HorizonEditor(
                    HorizonDraft(
                        HorizonForm.Transaction,
                        name = "Lunch",
                        amount = "12.50",
                        date = "2026-10-17",
                        currency = "USD",
                    ),
                    sample,
                    false,
                    null,
                    false,
                    {
                    },
                    {},
                    {},
                )
            }
        }
    }
}

@Preview(name = "Finance states", widthDp = 360, heightDp = 800)
@Composable
private fun StatesPreview() {
    PulseTheme {
        Surface {
            Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(
                        rememberScrollState(),
                    ).padding(PulseTokens.medium),
                verticalArrangement = Arrangement.spacedBy(PulseTokens.medium),
            ) {
                SectionState(HorizonSection<HorizonOverview>(), {}) {}
                SectionState(
                    HorizonSection<HorizonOverview>(
                        loading = false,
                        error = Failure.Connection,
                    ),
                    {
                    },
                ) {}
                PulseEmptyState("No transactions", "Record your first transaction.")
                PulseButton("Saving transaction", {}, loading = true)
                PulseButton("Save transaction", {}, enabled = false)
            }
        }
    }
}
