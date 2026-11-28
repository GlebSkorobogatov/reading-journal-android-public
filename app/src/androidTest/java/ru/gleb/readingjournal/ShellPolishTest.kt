package ru.gleb.readingjournal

import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.activity.BackEventCompat
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import ru.gleb.readingjournal.app.MainActivity
import java.io.File

class ShellPolishTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()

    @Test fun placeholdersStayCenteredBetweenBarsInBothThemes() {
        for (theme in listOf("Светлая", "Тёмная")) {
            rule.onNodeWithContentDescription("Настройки").performClick()
            rule.onNodeWithText(theme).performClick()
            rule.onNodeWithContentDescription("Назад").performClick()
            rule.onNode(hasContentDescription("Библиотека") and hasClickAction()).performClick()
            rule.onNodeWithText("Библиотека · UI-03").assertIsDisplayed()
            assertPlaceholderCentered("Library/$theme")
            rule.onNodeWithContentDescription("Настройки").assertIsDisplayed()
            rule.onNode(hasContentDescription("Библиотека") and hasClickAction()).assertIsSelected()
            screenshot("polish-$theme-library")

            // The two live roots replaced their UI-01 placeholders. Their child stubs
            // still use the same centering contract, with a system inset at the bottom.
            rule.onNode(hasContentDescription("Читаю сейчас") and hasClickAction()).performClick()
            rule.onNodeWithTag("reading-feed").performScrollToNode(hasTestTag("open-atlas"))
            rule.onNodeWithTag("open-atlas").performClick()
            assertPlaceholderCentered("Book/$theme")
            rule.onNodeWithContentDescription("Назад").performClick()
            rule.onNode(hasContentDescription("Статистика") and hasClickAction()).performClick()
            rule.onNodeWithTag("xp-card").performClick()
            assertPlaceholderCentered("Achievements/$theme")
            rule.onNodeWithContentDescription("Назад").performClick()
        }
        // Android resource render with the emulator's adaptive mask, not a Samsung screenshot.
        val icon = rule.activity.packageManager.getApplicationIcon(rule.activity.applicationInfo)
        val bitmap = Bitmap.createBitmap(216, 216, Bitmap.Config.ARGB_8888)
        icon.setBounds(0, 0, bitmap.width, bitmap.height)
        icon.draw(Canvas(bitmap))
        save(bitmap, "polish-launcher-android-mask")
    }

    @Test fun predictiveSettingsBackDoesNotScaleAndSupportsCancelAndCommit() {
        rule.onNode(hasContentDescription("Библиотека") and hasClickAction()).performClick()
        for (edge in listOf(BackEventCompat.EDGE_LEFT, BackEventCompat.EDGE_RIGHT)) {
            rule.onNodeWithContentDescription("Настройки").performClick()
            rule.onNodeWithText("Тема").assertIsDisplayed()
            val initial = settingsBounds()
            rule.onNodeWithText("Библиотека · UI-03").assertDoesNotExist()
            startBack(0.6f, edge)
            rule.waitForIdle()
            // Prove the predictive preview is active, rather than testing an idle page.
            rule.onNodeWithText("Библиотека · UI-03").assertExists()
            val during = settingsBounds()
            assertEquals("Back must not shrink Settings width", initial.width, during.width, 1f)
            assertEquals("Back must not shrink Settings height", initial.height, during.height, 1f)
            assertTrue("Settings must follow the swipe edge", if (edge == BackEventCompat.EDGE_LEFT)
                during.left > initial.left + 1f else during.left < initial.left - 1f)
            screenshot("polish-settings-back-progress-$edge")
            rule.runOnUiThread { rule.activity.onBackPressedDispatcher.dispatchOnBackCancelled() }
            rule.onNodeWithText("Тема").assertIsDisplayed()
            assertEquals("Cancelled Back restores position", initial.left, settingsBounds().left, 1f)
            rule.onNodeWithText("Библиотека · UI-03").assertDoesNotExist()
            screenshot("polish-settings-back-cancelled-$edge")
            startBack(0.75f, edge)
            rule.runOnUiThread { rule.activity.onBackPressedDispatcher.onBackPressed() }
            rule.onNodeWithText("Библиотека · UI-03").assertIsDisplayed()
            rule.onNode(hasContentDescription("Библиотека") and hasClickAction()).assertIsSelected()
            rule.onNodeWithTag("settings-content").assertDoesNotExist()
            screenshot("polish-settings-back-complete-$edge")
        }
    }

    private fun assertPlaceholderCentered(case: String) {
        val viewport = rule.onNodeWithTag("placeholder-viewport").fetchSemanticsNode().boundsInRoot
        val content = rule.onNodeWithTag("placeholder-content").fetchSemanticsNode().boundsInRoot
        assertEquals("Empty state vertical centre: $case", viewport.center.y, content.center.y, 1f)
        assertEquals("Empty state horizontal centre: $case", viewport.center.x, content.center.x, 1f)
    }

    private fun settingsBounds(): Rect {
        val coordinates = rule.onNodeWithTag("settings-content").fetchSemanticsNode().layoutInfo.coordinates
        var root = coordinates
        while (root.parentLayoutCoordinates != null) root = requireNotNull(root.parentLayoutCoordinates)
        // Include transforms, but not viewport clipping: sliding is not scaling.
        return root.localBoundingBoxOf(coordinates, clipBounds = false)
    }

    private fun startBack(progress: Float, edge: Int) {
        rule.runOnUiThread {
            val dispatcher = rule.activity.onBackPressedDispatcher
            dispatcher.dispatchOnBackStarted(BackEventCompat(0f, 100f, 0f, edge))
            dispatcher.dispatchOnBackProgressed(BackEventCompat(100f, 100f, progress, edge))
        }
    }

    private fun screenshot(name: String) {
        rule.waitForIdle()
        save(requireNotNull(InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot()), name)
    }

    private fun save(bitmap: Bitmap, name: String) {
        val file = File(rule.activity.filesDir, "screenshots/$name.png")
        file.parentFile?.mkdirs()
        file.outputStream().use { check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)) }
    }
}
