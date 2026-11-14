package dev.abhishekan.pulse.feature.horizon

import org.junit.Assert.assertEquals
import org.junit.Test

class EditorLabelsTest {
    @Test fun everyFormUsesItsAddOrEditTitle() {
        HorizonForm.entries.forEach { kind ->
            assertEquals(kind.editorLabel(EditorAction.Add), HorizonDraft(kind = kind).topBarTitle())
            assertEquals(kind.editorLabel(EditorAction.Edit), HorizonDraft(kind = kind, id = 1).topBarTitle())
        }
        assertEquals(R.string.pulse_horizon_add_transaction, HorizonDraft(kind = HorizonForm.Transaction).topBarTitle())
        assertEquals(R.string.pulse_horizon_edit_category, HorizonDraft(kind = HorizonForm.Category, id = 1).topBarTitle())
    }
}
