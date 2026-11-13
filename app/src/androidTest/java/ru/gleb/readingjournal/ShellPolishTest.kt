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
            for (tab in listOf("Главная", "Библиотека", "Достижения")) {
                rule.onNode(hasText(tab) and hasClickAction()).performClick()
                rule.onNodeWithText("$tab · UI-01").assertIsDisplayed()
                val viewport = rule.onNodeWithTag("placeholder-viewport").fetchSemanticsNode().boundsInRoot
                val content = rule.onNodeWithTag("placeholder-content").fetchSemanticsNode().boundsInRoot
                assertEquals("Empty state vertical centre: $tab/$theme", viewport.center.y, content.center.y, 1f)
                assertEquals("Empty state horizontal centre: $tab/$theme", viewport.center.x, content.center.x, 1f)
                rule.onNodeWithContentDescription("Настройки").assertIsDisplayed()
                rule.onNode(hasText(tab) and hasClickAction()).assertIsSelected()
                screenshot("polish-$theme-$tab")
            }
        }
        // Android resource render with the emulator's adaptive mask, not a Samsung screenshot.
        val icon = rule.activity.packageManager.getApplicationIcon(rule.activity.applicationInfo)
        val bitmap = Bitmap.createBitmap(216, 216, Bitmap.Config.ARGB_8888)
        icon.setBounds(0, 0, bitmap.width, bitmap.height)
        icon.draw(Canvas(bitmap))
        save(bitmap, "polish-launcher-android-mask")
    }

    @Test fun predictiveSettingsBackDoesNotScaleAndSupportsCancelAndCommit() {
        rule.onNode(hasText("Библиотека") and hasClickAction()).performClick()
        for (edge in listOf(BackEventCompat.EDGE_LEFT, BackEventCompat.EDGE_RIGHT)) {
            rule.onNodeWithContentDescription("Настройки").performClick()
            rule.onNodeWithText("Тема").assertIsDisplayed()
            val initial = settingsBounds()
            rule.onNodeWithText("Библиотека · UI-01").assertDoesNotExist()
            startBack(0.6f, edge)
            rule.waitForIdle()
            // Prove the predictive preview is active, rather than testing an idle page.
            rule.onNodeWithText("Библиотека · UI-01").assertExists()
            val during = settingsBounds()
            assertEquals("Back must not shrink Settings width", initial.width, during.width, 1f)
            assertEquals("Back must not shrink Settings height", initial.height, during.height, 1f)
            assertTrue("Settings must follow the swipe edge", if (edge == BackEventCompat.EDGE_LEFT)
                during.left > initial.left + 1f else during.left < initial.left - 1f)
            screenshot("polish-settings-back-progress-$edge")
            rule.runOnUiThread { rule.activity.onBackPressedDispatcher.dispatchOnBackCancelled() }
            rule.onNodeWithText("Тема").assertIsDisplayed()
            assertEquals("Cancelled Back restores position", initial.left, settingsBounds().left, 1f)
            rule.onNodeWithText("Библиотека · UI-01").assertDoesNotExist()
            screenshot("polish-settings-back-cancelled-$edge")
            startBack(0.75f, edge)
            rule.runOnUiThread { rule.activity.onBackPressedDispatcher.onBackPressed() }
            rule.onNodeWithText("Библиотека · UI-01").assertIsDisplayed()
            rule.onNode(hasText("Библиотека") and hasClickAction()).assertIsSelected()
            rule.onNodeWithTag("settings-content").assertDoesNotExist()
            screenshot("polish-settings-back-complete-$edge")
        }
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
