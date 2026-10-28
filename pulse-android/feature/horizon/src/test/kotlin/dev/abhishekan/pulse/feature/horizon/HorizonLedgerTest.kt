package dev.abhishekan.pulse.feature.horizon

import dev.abhishekan.pulse.core.domain.model.HorizonTransaction
import dev.abhishekan.pulse.core.domain.model.UserTimeZone
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test
import java.math.BigDecimal
import java.time.Instant
import java.time.YearMonth

class HorizonLedgerTest {
    @After fun resetZone() = UserTimeZone.reset()

    @Test fun salaryAndRefundsDoNotReduceOrIncreaseSpending() {
        val items = listOf(tx(1, "debit", "299.00"), tx(2, "credit", "120000.00"), tx(3, "credit", "100.00"), tx(4, "debit", "412.50"))
        assertEquals(BigDecimal("711.50"), ledgerSpending(items))
        assertEquals(BigDecimal.ZERO, ledgerSpending(items.filter { it.type == "credit" }))
        assertEquals(BigDecimal.ZERO, ledgerSpending(emptyList()))
    }

    @Test fun monthBoundaryUsesProfileZoneAndSortsNewestFirst() {
        UserTimeZone.select("Asia/Kolkata")
        val older = tx(1, "debit", "1.00").copy(date = Instant.parse("2026-10-31T18:00:00Z"))
        val newer = tx(2, "debit", "2.00").copy(date = Instant.parse("2026-10-31T19:00:00Z"))
        val months = ledgerMonths(listOf(older, newer))
        assertEquals(listOf(YearMonth.of(2026, 11), YearMonth.of(2026, 10)), months.keys.toList())
        assertEquals(listOf(newer), months[YearMonth.of(2026, 11)])
    }

    private fun tx(
        id: Long,
        type: String,
        amount: String,
    ) = HorizonTransaction(
        id,
        "Merchant",
        amount.toBigDecimal(),
        type,
        Instant.parse("2026-10-29T00:00:00Z"),
        null,
        "General",
        null,
        null,
        null,
        null,
        null,
    )
}
