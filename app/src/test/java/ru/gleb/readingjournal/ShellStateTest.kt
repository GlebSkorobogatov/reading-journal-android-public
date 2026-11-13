package ru.gleb.readingjournal

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
        val first = ShellViewModel()
        first.selectTheme(ThemeMode.DARK)
        assertEquals(ThemeMode.DARK, first.state.value.theme)
        assertEquals(ThemeMode.SYSTEM, ShellViewModel().state.value.theme)
    }
}
