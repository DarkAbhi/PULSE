package dev.abhishekan.pulse.core.domain.model
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal

class HorizonTest {
    @Test fun financeOverviewAndBudgetUseExactZeroBoundaries() {
        val zero = BigDecimal.ZERO
        val overview =
            HorizonOverview(
                "USD",
                zero,
                BigDecimal("-0.00000000000000000001"),
                zero,
                zero,
                zero,
                zero,
                zero,
                emptyList(),
                emptyList(),
                emptyList(),
                emptyList(),
                emptyList(),
            )
        assertTrue(overview.isOvercommitted)
        assertFalse(overview.copy(remaining = zero).isOvercommitted)
        val budget = HorizonBudget(1, "Budget", zero, zero, BigDecimal("-0.01"), zero)
        assertTrue(budget.isOverBudget)
        assertFalse(budget.copy(available = zero).isOverBudget)
    }

    @Test fun commitmentUsesExactBackendAmountIncludingZeroBoundary() {
        fun summary(remaining: String) = Horizon("INR", BigDecimal.ZERO, remaining.toBigDecimal(), BigDecimal.ZERO)
        assertTrue(summary("-0.000000000000000000001").isOvercommitted)
        assertFalse(summary("0.00").isOvercommitted)
        assertFalse(summary("0.000000000000000000001").isOvercommitted)
    }
}
