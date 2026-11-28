package ru.gleb.readingjournal

import androidx.lifecycle.SavedStateHandle
import org.junit.Assert.*
import org.junit.Test
import ru.gleb.readingjournal.app.*

class ShellStateTest {
    @Test fun systemModeFollowsBothSystemThemes() {
        assertFalse(ThemeMode.SYSTEM.isDark(false))
        assertTrue(ThemeMode.SYSTEM.isDark(true))
    }
    @Test fun explicitModesOverrideTheSystem() {
        for (system in listOf(false, true)) {
            assertFalse(ThemeMode.LIGHT.isDark(system))
            assertTrue(ThemeMode.DARK.isDark(system))
        }
    }
    @Test fun newSessionResetsDemoTheme() {
        val first = ShellViewModel(SavedStateHandle())
        first.selectTheme(ThemeMode.DARK)
        assertEquals(ThemeMode.DARK, first.state.value.theme)
        assertEquals(ThemeMode.SYSTEM, ShellViewModel(SavedStateHandle()).state.value.theme)
    }

    @Test fun rootPageStaysInTheSameHandleAcrossANewViewModelWithThatHandle() {
        val saved = SavedStateHandle()
        val first = ShellViewModel(saved)
        assertEquals(0, first.rootPage)
        first.rootPage = 2
        assertEquals(2, ShellViewModel(saved).rootPage)
    }

    @Test fun aSettledGestureStoresItsPageUntilANewerChoiceExists() {
        assertEquals(1, rootPageCommittedByGesture(0, 0, settledPage = 1, chosenPage = 0))
        assertEquals(2, rootPageCommittedByGesture(0, 1, settledPage = 1, chosenPage = 2))
    }
}
