package dev.abhishekan.pulse.core.domain
import dev.abhishekan.pulse.core.domain.model.NotificationTarget
import dev.abhishekan.pulse.core.domain.usecase.AccountValidation
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AccountValidationTest {
    @Test fun unicodeNamesMatchGoRuneCountsAndWhitespace() {
        assertTrue(AccountValidation.validName("😀".repeat(120)))
        assertFalse(AccountValidation.validName("😀".repeat(121)))
        assertEquals("Asha", AccountValidation.trimmedName("\u0085\u00a0 Asha \u2009"))
        assertFalse(AccountValidation.validName("\u3000\n"))
        assertEquals("\u001cAsha", AccountValidation.trimmedName("\u001cAsha"))
    }

    @Test fun passwordValidationMatchesUtf8AndBcryptWithoutTrimming() {
        assertTrue(AccountValidation.validPassword(" ", "😀é", "😀é"))
        assertTrue(AccountValidation.validPassword("old", " ".repeat(6), " ".repeat(6)))
        assertFalse(AccountValidation.validPassword("", "password", "password"))
        assertFalse(AccountValidation.validPassword("old", "😀".repeat(19), "😀".repeat(19)))
        assertTrue(AccountValidation.validPassword("old", "😀".repeat(18), "😀".repeat(18)))
        assertFalse(AccountValidation.validPassword("old", " secret ", "secret"))
    }

    @Test fun allowlistAcceptsOnlyImplementedLocalRoutesAndPositiveIds() {
        assertEquals(NotificationTarget.Garage, NotificationTarget.parse("/garage"))
        assertEquals(NotificationTarget.Vehicle(42), NotificationTarget.parse("/garage/42"))
        assertEquals(NotificationTarget.Workout(Long.MAX_VALUE), NotificationTarget.parse("/fitness/${Long.MAX_VALUE}"))
        assertEquals(NotificationTarget.Horizon, NotificationTarget.parse("/financial-horizon"))
        assertEquals(NotificationTarget.MealPlan, NotificationTarget.parse("/meal-plan"))
        listOf(
            "https://evil.test/garage",
            "//evil.test",
            "/garage/0",
            "/garage/-1",
            "/garage/01",
            "/garage/9223372036854775808",
            "/garage/1?x=2",
            "/garage%2F1",
            "/garage/1#x",
            "/garage/../profile",
            "/profile",
        ).forEach {
            assertNull(it, NotificationTarget.parse(it))
        }
    }
}
