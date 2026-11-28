package ru.gleb.readingjournal

import android.content.pm.ActivityInfo
import android.content.res.Configuration
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry
import androidx.test.runner.lifecycle.Stage
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import ru.gleb.readingjournal.app.MainActivity
import ru.gleb.readingjournal.app.ShellViewModel
import java.io.FileInputStream
import kotlin.math.abs

class LifecycleLayoutTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()
    @Test fun landscapeLargeTextAndBackgroundPreserveContext() {
        val originalScale = rule.activity.resources.configuration.fontScale
        val originalOrientation = rule.activity.resources.configuration.orientation
        try {
            selectLibrary()
            rule.activityRule.scenario.moveToState(Lifecycle.State.CREATED)
            rule.activityRule.scenario.moveToState(Lifecycle.State.RESUMED)
            assertLibrary()
            changeConfiguration(Configuration.ORIENTATION_LANDSCAPE, 2f) {
                command("settings put system font_scale 2.0")
                rotate(Configuration.ORIENTATION_LANDSCAPE)
            }
            assertLibrary()
            rule.onNodeWithContentDescription("Настройки").performClick()
            rule.onNodeWithText("Каталог компонентов · debug").performScrollTo().performClick()
            rule.onNodeWithText("Открыть диалог").performScrollTo().performClick()
            rule.onNodeWithText("Закрыть").assertIsDisplayed().performClick()
            rule.onNodeWithContentDescription("Назад").performClick()
            rule.onNodeWithContentDescription("Назад").performClick()
            rule.onNodeWithTag("settings-content").assertDoesNotExist()
            assertLibrary()
        } finally {
            restoreConfiguration(originalOrientation, originalScale)
        }
    }
    @Test fun orientationThenFontScaleThenTheReverseKeepsTheSelectedTab() {
        val originalScale = rule.activity.resources.configuration.fontScale
        val originalOrientation = rule.activity.resources.configuration.orientation
        try {
            selectLibrary()
            changeConfiguration(Configuration.ORIENTATION_LANDSCAPE, originalScale) {
                rotate(Configuration.ORIENTATION_LANDSCAPE)
            }
            assertLibrary()
            changeConfiguration(Configuration.ORIENTATION_LANDSCAPE, 1.5f) {
                command("settings put system font_scale 1.5")
            }
            assertLibrary()
            changeConfiguration(Configuration.ORIENTATION_PORTRAIT, 1.5f) {
                rotate(Configuration.ORIENTATION_PORTRAIT)
            }
            assertLibrary()
            changeConfiguration(Configuration.ORIENTATION_PORTRAIT, 2f) {
                command("settings put system font_scale 2.0")
            }
            assertLibrary()
            rule.onNodeWithContentDescription("Настройки").performClick()
            rule.onNodeWithText("Тема").assertIsDisplayed()
            rule.onNodeWithContentDescription("Назад").performClick()
            assertLibrary()
        } finally {
            restoreConfiguration(originalOrientation, originalScale)
        }
    }

    @Test fun recreationKeepsTheLibraryTab() {
        selectLibrary()
        rule.activityRule.scenario.recreate()
        assertLibrary()
    }

    private fun selectLibrary() {
        // Exercise the click itself. Touch and swipe arbitration is covered by RootPagerTest.
        rule.onNode(hasContentDescription("Библиотека") and hasClickAction())
            .performSemanticsAction(SemanticsActions.OnClick)
        rule.waitForIdle()
        assertLibrary()
    }

    private fun assertLibrary() {
        rule.runOnIdle {
            val activity = rule.activity
            val savedPage = ViewModelProvider(activity)[ShellViewModel::class.java].rootPage
            assertEquals("Saved Library page in $activity (${activity.resources.configuration})", 2, savedPage)
        }
        rule.onNode(hasContentDescription("Библиотека") and hasClickAction()).assertIsSelected()
        rule.onNodeWithText("Библиотека · UI-03").assertIsDisplayed()
    }

    private fun rotate(orientation: Int) {
        rule.runOnUiThread {
            rule.activity.requestedOrientation = if (orientation == Configuration.ORIENTATION_LANDSCAPE)
                ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE else ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }
    }

    private fun restoreConfiguration(orientation: Int, scale: Float) {
        changeConfiguration(orientation, scale) {
            command("settings put system font_scale $scale")
            rotate(orientation)
        }
    }

    private fun changeConfiguration(orientation: Int, scale: Float, request: () -> Unit) {
        val before = rule.activity
        val config = Configuration(before.resources.configuration)
        val recreates = config.orientation != orientation || abs(config.fontScale - scale) >= .01f
        request()
        // Resources can reflect the updated device configuration before Android replaces the
        // window. Wait for the resumed, laid-out host, independently of the expected tab.
        rule.waitUntil("resumed Activity laid out in orientation=$orientation fontScale=$scale", 30_000) {
            rule.runOnUiThread {
                val activity = ActivityLifecycleMonitorRegistry.getInstance()
                    .getActivitiesInStage(Stage.RESUMED).filterIsInstance<MainActivity>().singleOrNull()
                    ?: return@runOnUiThread false
                val current = activity.resources.configuration
                val decor = activity.window.decorView
                (!recreates || activity !== before && before.isDestroyed) &&
                    current.orientation == orientation && abs(current.fontScale - scale) < .01f &&
                    decor.isLaidOut && !decor.isLayoutRequested &&
                    (decor.width > decor.height) == (orientation == Configuration.ORIENTATION_LANDSCAPE)
            }
        }
        rule.waitForIdle()
    }

    private fun command(command: String) {
        val descriptor = InstrumentationRegistry.getInstrumentation().uiAutomation.executeShellCommand(command)
        FileInputStream(descriptor.fileDescriptor).use { it.readBytes() }
        descriptor.close()
    }
}
