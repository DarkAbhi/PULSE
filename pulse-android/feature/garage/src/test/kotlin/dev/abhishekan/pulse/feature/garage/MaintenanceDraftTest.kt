package dev.abhishekan.pulse.feature.garage

import dev.abhishekan.pulse.core.domain.model.GarageChange
import dev.abhishekan.pulse.core.domain.model.Maintenance
import dev.abhishekan.pulse.core.domain.model.MaintenanceAction
import org.junit.Assert.assertEquals
import org.junit.Test
import java.math.BigDecimal
import java.time.Instant

class MaintenanceDraftTest {
    @Test fun workSurvivesEditingAndProcessRestoration() {
        val work = listOf(MaintenanceAction("engine_oil", "replaced", "15W50"), MaintenanceAction("clutch_cable", "adjusted"))
        val item =
            Maintenance(
                1,
                "service",
                "September service",
                BigDecimal.ZERO,
                Instant.parse("2026-09-07T09:34:00Z"),
                "4722".toBigDecimal(),
                "Workshop",
                null,
                emptyList(),
                work,
            )
        val draft = item.draft(7)
        val restored = GarageDraft.restore(draft.saved())
        val change = restored.change() as GarageChange.Care
        assertEquals(work, change.actions)
        assertEquals(item.date, change.date)
        assertEquals(BigDecimal.ZERO, change.amount)
        val updated = restored.withMaintenanceActions(work.drop(1))
        assertEquals(work.drop(1), (updated.change() as GarageChange.Care).actions)
        assertEquals(emptyList<MaintenanceAction>(), (updated.withMaintenanceActions(emptyList()).change() as GarageChange.Care).actions)
    }
}
