package ru.gleb.readingjournal

import android.content.pm.ActivityInfo
import android.content.res.Configuration
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.Lifecycle
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Rule
import org.junit.Test
import ru.gleb.readingjournal.app.MainActivity
import java.io.FileInputStream

class LifecycleLayoutTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()
    @Test fun landscapeLargeTextAndBackgroundPreserveContext() {
        val originalScale = rule.activity.resources.configuration.fontScale
        val originalOrientation = rule.activity.resources.configuration.orientation
        try {
            // A real touch on the tab can also drag the pager and finish on the page it crossed.
            // The selection itself is the click action; the configuration change is what this test owns.
            rule.onNode(hasContentDescription("Библиотека") and hasClickAction())
                .performSemanticsAction(SemanticsActions.OnClick)
            rule.waitForIdle()
            rule.onNode(hasContentDescription("Библиотека") and hasClickAction()).assertIsSelected()
            rule.activityRule.scenario.moveToState(Lifecycle.State.CREATED)
            rule.activityRule.scenario.moveToState(Lifecycle.State.RESUMED)
            rule.onNodeWithText("Библиотека · UI-03").assertIsDisplayed()
            command("settings put system font_scale 2.0")
            rule.runOnUiThread { rule.activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE }
            rule.waitUntil(30_000) {
                val c = rule.activity.resources.configuration
                c.orientation == Configuration.ORIENTATION_LANDSCAPE && c.fontScale >= 1.99f
            }
            rule.onNodeWithContentDescription("Настройки").performClick()
            rule.onNodeWithText("Каталог компонентов · debug").performScrollTo().performClick()
            rule.onNodeWithText("Открыть диалог").performScrollTo().performClick()
            rule.onNodeWithText("Закрыть").assertIsDisplayed().performClick()
            rule.onNodeWithContentDescription("Назад").performClick()
            rule.onNodeWithContentDescription("Назад").performClick()
            rule.onNode(hasContentDescription("Библиотека") and hasClickAction()).assertIsSelected()
        } finally {
            command("settings put system font_scale $originalScale")
            rule.runOnUiThread { rule.activity.requestedOrientation = if (originalOrientation == Configuration.ORIENTATION_LANDSCAPE)
                ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE else ActivityInfo.SCREEN_ORIENTATION_PORTRAIT }
            // Shell settings/configuration changes are asynchronous on API 26. Do not
            // finish this test while its cleanup can still recreate the next host.
            rule.waitUntil(30_000) {
                val config = rule.activity.resources.configuration
                kotlin.math.abs(config.fontScale - originalScale) < 0.01f &&
                    config.orientation == originalOrientation
            }
        }
    }
    @Test fun orientationThenFontScaleThenTheReverseKeepsTheSelectedTab() {
        val originalScale = rule.activity.resources.configuration.fontScale
        val originalOrientation = rule.activity.resources.configuration.orientation
        try {
            selectLibrary()
            rotate(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE)
            assertLibrary()
            command("settings put system font_scale 1.5")
            waitFor(Configuration.ORIENTATION_LANDSCAPE, 1.45f)
            assertLibrary()
            rotate(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT)
            command("settings put system font_scale 2.0")
            waitFor(Configuration.ORIENTATION_PORTRAIT, 1.99f)
            assertLibrary()
            rule.onNodeWithContentDescription("Настройки").performClick()
            rule.onNodeWithText("Тема").assertIsDisplayed()
            rule.onNodeWithContentDescription("Назад").performClick()
            assertLibrary()
        } finally {
            command("settings put system font_scale $originalScale")
            rotate(if (originalOrientation == Configuration.ORIENTATION_LANDSCAPE)
                ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE else ActivityInfo.SCREEN_ORIENTATION_PORTRAIT)
            rule.waitUntil(30_000) {
                val config = rule.activity.resources.configuration
                kotlin.math.abs(config.fontScale - originalScale) < 0.01f &&
                    config.orientation == originalOrientation
            }
        }
    }

    @Test fun recreationKeepsTheLibraryTab() {
        selectLibrary()
        rule.activityRule.scenario.recreate()
        assertLibrary()
    }

    private fun selectLibrary() {
        rule.onNode(hasContentDescription("Библиотека") and hasClickAction())
            .performSemanticsAction(SemanticsActions.OnClick)
        rule.waitForIdle()
        assertLibrary()
    }

    private fun assertLibrary() {
        rule.onNode(hasContentDescription("Библиотека") and hasClickAction()).assertIsSelected()
        rule.onNodeWithText("Библиотека · UI-03").assertIsDisplayed()
    }

    private fun rotate(orientation: Int) {
        rule.runOnUiThread { rule.activity.requestedOrientation = orientation }
        val expected = if (orientation == ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE)
            Configuration.ORIENTATION_LANDSCAPE else Configuration.ORIENTATION_PORTRAIT
        rule.waitUntil(30_000) { rule.activity.resources.configuration.orientation == expected }
    }

    private fun waitFor(orientation: Int, minimumScale: Float) {
        rule.waitUntil(30_000) {
            val config = rule.activity.resources.configuration
            config.orientation == orientation && config.fontScale >= minimumScale
        }
    }

    private fun command(command: String) {
        val descriptor = InstrumentationRegistry.getInstrumentation().uiAutomation.executeShellCommand(command)
        FileInputStream(descriptor.fileDescriptor).use { it.readBytes() }
        descriptor.close()
    }
}
