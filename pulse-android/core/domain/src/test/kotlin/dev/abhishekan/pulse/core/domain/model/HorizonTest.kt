package dev.abhishekan.pulse.core.domain.model

import java.math.BigDecimal
import org.junit.Assert.*
import org.junit.Test

class HorizonTest {
    @Test fun commitmentUsesExactBackendAmountIncludingZeroBoundary() {
        fun summary(remaining: String) = Horizon("INR", BigDecimal.ZERO, remaining.toBigDecimal(), BigDecimal.ZERO)
        assertTrue(summary("-0.000000000000000000001").isOvercommitted)
        assertFalse(summary("0.00").isOvercommitted)
        assertFalse(summary("0.000000000000000000001").isOvercommitted)
    }
}
