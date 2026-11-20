package dev.abhishekan.pulse.feature.horizon.presentation.editor

import dev.abhishekan.pulse.core.domain.model.HorizonChange
import dev.abhishekan.pulse.core.domain.model.HorizonTransaction
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant

class HorizonDraftMappingTest {
    @Test fun transactionNarrationSurvivesDraftRestorationAndNameChanges() {
        val transaction =
            HorizonTransaction(
                id = 1,
                name = "Zerodha payment",
                amount = "10.00".toBigDecimal(),
                type = "debit",
                date = Instant.parse("2026-11-01T12:00:00Z"),
                categoryId = 30L,
                categoryName = "Investment / others",
                budgetId = null,
                budgetName = null,
                subscriptionId = null,
                subscriptionName = null,
                notes = null,
                source = "statement",
                originalNarration = "UPI/DR/104332491964/ZEROD HA/HDFC/zerodha/reference",
            )
        val draft = transaction.draft("INR").copy(name = "Brokerage payment")
        val restored = HorizonDraft.restore(draft.saved())

        assertEquals(transaction.originalNarration, restored.originalNarration)
        assertEquals("Brokerage payment", (restored.change() as HorizonChange.Transaction).name)
        assertEquals("", HorizonDraft.restore(draft.saved().take(28).toCollection(ArrayList())).originalNarration)
    }
}
