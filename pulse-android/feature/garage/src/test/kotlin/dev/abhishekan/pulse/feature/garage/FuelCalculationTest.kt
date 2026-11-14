package dev.abhishekan.pulse.feature.garage

import org.junit.Assert.assertEquals
import org.junit.Test

class FuelCalculationTest {
    @Test fun totalUpdatesForEachTankAndClearsWithAnIncompleteInput() {
        val draft =
            GarageDraft(GarageForm.Fuel)
                .setFuelValue(0, "quantity", "2.5")
                .setFuelValue(0, "price", "100.1")
                .setFuelValue(1, "price", "20")
                .setFuelValue(1, "quantity", "3")
        assertEquals("250.25", draft["total0"])
        assertEquals("60", draft["total1"])
        assertEquals("300.3", draft.setFuelValue(0, "quantity", "3")["total0"])
        assertEquals("", draft.setFuelValue(0, "price", "")["total0"])
        assertEquals("60", draft.setFuelValue(0, "price", "")["total1"])
    }

    @Test fun manualTotalIsKeptWhenThereIsNoCompletePair() {
        val draft = GarageDraft(GarageForm.Fuel).set("total0", "100")
        assertEquals("100", draft.setFuelValue(0, "quantity", "2")["total0"])
    }
}
