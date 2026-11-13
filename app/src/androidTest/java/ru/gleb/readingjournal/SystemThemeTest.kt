package ru.gleb.readingjournal

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Rule
import org.junit.Test
import ru.gleb.readingjournal.app.MainActivity
import java.io.FileInputStream

class SystemThemeTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()
    @Test fun systemThemeTracksAndroidNightModeAndManualModeOverridesIt() {
        try {
            night("yes")
            // Night mode can recreate the Activity. An absent root is transient here;
            // keep polling for exactly one expected theme node, with the same timeout.
            rule.waitUntil(15_000) { rule.onAllNodesWithTag("theme-true").fetchSemanticsNodes(atLeastOneRootRequired = false).size == 1 }
            rule.onNodeWithContentDescription("Настройки").performClick()
            rule.onNodeWithText("Светлая").performClick()
            rule.onNodeWithTag("theme-false").assertExists()
            rule.onNodeWithText("Системная").performClick()
            rule.onNodeWithTag("theme-true").assertExists()
            night("no")
            rule.waitUntil(15_000) { rule.onAllNodesWithTag("theme-false").fetchSemanticsNodes(atLeastOneRootRequired = false).size == 1 }
        } finally { night("auto") }
    }
    private fun night(mode: String) {
        val descriptor = InstrumentationRegistry.getInstrumentation().uiAutomation.executeShellCommand("cmd uimode night $mode")
        FileInputStream(descriptor.fileDescriptor).use { it.readBytes() }
        descriptor.close()
    }
}
