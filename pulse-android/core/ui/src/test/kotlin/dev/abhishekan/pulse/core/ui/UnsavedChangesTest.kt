package dev.abhishekan.pulse.core.ui

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class UnsavedChangesTest {
    @Test fun clearingOneOwnerKeepsAnotherDraftFromTheSameFeature() =
        runTest {
            val changes = UnsavedChanges()
            val first = CoroutineScope(SupervisorJob() + StandardTestDispatcher(testScheduler))
            val second = CoroutineScope(SupervisorJob() + StandardTestDispatcher(testScheduler))
            val firstDraft = MutableStateFlow(true)
            val secondDraft = MutableStateFlow(true)
            var sources = emptySet<DraftSource>()
            backgroundScope.launch { changes.sources.collect { sources = it } }
            try {
                changes.track(DraftSource.Garage, firstDraft, first)
                changes.track(DraftSource.Garage, secondDraft, second)
                runCurrent()
                assertEquals(setOf(DraftSource.Garage), sources)
                first.cancel()
                runCurrent()
                assertEquals(setOf(DraftSource.Garage), sources)
                secondDraft.value = false
                runCurrent()
                assertEquals(emptySet<DraftSource>(), sources)
                secondDraft.value = true
                runCurrent()
                second.cancel()
                runCurrent()
                assertEquals(emptySet<DraftSource>(), sources)
            } finally {
                first.cancel()
                second.cancel()
            }
        }
}
