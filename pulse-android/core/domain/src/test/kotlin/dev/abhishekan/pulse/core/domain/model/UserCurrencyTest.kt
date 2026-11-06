package dev.abhishekan.pulse.core.domain.model
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal
import java.util.Locale

class UserCurrencyTest {
    @Test fun selectionFormattingAndReset() {
        val previous = Locale.getDefault()
        try {
            Locale.setDefault(Locale.US)
            assertEquals("£", UserCurrency.symbol("GBP"))
            assertEquals("₹", UserCurrency.symbol("INR"))
            assertEquals("€", UserCurrency.symbol("EUR"))
            UserCurrency.select("INR")
            assertEquals("₹1,234.50", UserCurrency.format(BigDecimal("1234.5")))
            UserCurrency.select("JPY")
            assertEquals("¥123", UserCurrency.format(BigDecimal("123")))
            UserCurrency.select("KWD")
            assertEquals("KWD1.2345", UserCurrency.format(BigDecimal("1.2345")))
            for (code in UserCurrency.available) assertTrue(UserCurrency.label(code).contains(code))
            assertFalse(UserCurrency.valid("ZZZ"))
            assertFalse(UserCurrency.valid("£"))
        } finally {
            Locale.setDefault(previous)
            UserCurrency.reset()
        }
        assertEquals("GBP", UserCurrency.code)
    }
}
