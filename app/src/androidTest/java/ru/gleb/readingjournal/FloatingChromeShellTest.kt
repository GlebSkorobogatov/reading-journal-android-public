package ru.gleb.readingjournal

import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.graphics.Bitmap
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import androidx.activity.BackEventCompat
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.ViewModelProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import ru.gleb.readingjournal.app.MainActivity
import ru.gleb.readingjournal.app.ShellViewModel
import ru.gleb.readingjournal.app.ThemeMode
import ru.gleb.readingjournal.demo.DemoAction
import ru.gleb.readingjournal.demo.DemoScenario
import ru.gleb.readingjournal.demo.ReadingDemoViewModel

class FloatingChromeShellTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()
    private val tab = SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Tab)

    @Test fun realListsHideOnlyForAUserScrollAndTheNextTabRestoresThem() {
        rule.runOnIdle {
            ViewModelProvider(rule.activity)[ReadingDemoViewModel::class.java]
                .onAction(DemoAction.SelectScenario(DemoScenario.MANY))
        }
        rule.onNodeWithTag("reading-feed").performScrollToNode(hasTestTag("record-extra-12"))
        rule.onNodeWithContentDescription("Настройки").assertIsDisplayed()
        rule.onNodeWithTag("reading-feed").performScrollToIndex(0)
        rule.onNodeWithTag("reading-feed").performTouchInput { swipeUp() }
        rule.waitForIdle()
        rule.onNodeWithContentDescription("Настройки").assertDoesNotExist()
        rule.onNodeWithTag("reading-feed").performScrollToNode(hasTestTag("open-atlas"))
        rule.onNodeWithTag("open-atlas").performClick()
        rule.onNodeWithContentDescription("Назад").assertIsDisplayed()
        rule.onAllNodes(tab).assertCountEquals(0)
        rule.onNodeWithContentDescription("Назад").performClick()
        rule.onNodeWithContentDescription("Настройки").assertIsDisplayed()
        rule.onNode(tab and hasContentDescription("Статистика")).performClick()
        rule.onNodeWithContentDescription("Настройки").assertIsDisplayed()
        rule.onNode(tab and hasContentDescription("Статистика")).assertIsSelected()
        rule.onAllNodes(tab).assertCountEquals(3)
    }

    @Test fun contentPassesBehindTheIslandsInBothThemes() {
        rule.runOnIdle {
            ViewModelProvider(rule.activity)[ReadingDemoViewModel::class.java]
                .onAction(DemoAction.SelectScenario(DemoScenario.MANY))
        }
        for ((mode, name) in listOf(ThemeMode.LIGHT to "light", ThemeMode.DARK to "dark")) {
            rule.runOnIdle {
                ViewModelProvider(rule.activity)[ShellViewModel::class.java].selectTheme(mode)
            }
            rule.onNodeWithTag("reading-subtitle").assertIsDisplayed()
            shot("overlay-$name-visible")
            rule.onNodeWithTag("reading-feed").performSemanticsAction(SemanticsActions.ScrollBy) { it(0f, 360f) }
            rule.waitForIdle()
            assertEquals(0f, chromeProgress(), 0.001f)
            shot("overlay-$name-behind")
            rule.onNodeWithTag("reading-feed").performTouchInput { swipeUp() }
            rule.waitForIdle()
            assertEquals(1f, chromeProgress(), 0.02f)
            shot("overlay-$name-hidden")
            rule.onNodeWithTag("reading-feed").performTouchInput { swipeDown() }
            rule.waitForIdle()
            assertEquals(0f, chromeProgress(), 0.02f)
            shot("overlay-$name-restored")
            rule.onNodeWithTag("reading-feed").performSemanticsAction(SemanticsActions.ScrollBy) { it(0f, -100_000f) }
            rule.waitForIdle()
        }
    }

    @Test fun realFeedExtendsUnderTheIslandsAndTheLastBookClearsThem() {
        rule.runOnIdle {
            ViewModelProvider(rule.activity)[ReadingDemoViewModel::class.java]
                .onAction(DemoAction.SelectScenario(DemoScenario.MANY))
        }
        val root = rule.onNodeWithTag("chrome-root").fetchSemanticsNode().boundsInRoot
        val feed = rule.onNodeWithTag("reading-feed").fetchSemanticsNode().boundsInRoot
        val tabs = rule.onNode(tab and hasContentDescription("Библиотека")).fetchSemanticsNode().boundsInRoot
        assertEquals(root.top, feed.top, 1f)
        assertEquals(root.bottom, feed.bottom, 1f)
        assertTrue(feed.bottom > tabs.top + 1f)
        rule.onNodeWithTag("reading-feed").performSemanticsAction(SemanticsActions.ScrollBy) { it(0f, 100_000f) }
        rule.waitForIdle()
        val book = rule.onNodeWithTag("book-extra-12").fetchSemanticsNode().boundsInRoot
        assertTrue("last book ${book.bottom} tabs ${tabs.top}", book.bottom <= tabs.top + 1f)
        val progress = rule.onNodeWithTag("chrome-progress", useUnmergedTree = true)
            .fetchSemanticsNode().config[SemanticsProperties.StateDescription].toFloat()
        assertEquals(0f, progress, 0.001f)
    }

    @Test fun emptyReadingAndHorizontalMotionKeepTheIslands() {
        rule.runOnIdle {
            ViewModelProvider(rule.activity)[ReadingDemoViewModel::class.java]
                .onAction(DemoAction.SelectScenario(DemoScenario.EMPTY))
        }
        rule.onNodeWithTag("reading-empty").assertIsDisplayed()
        rule.onNodeWithTag("reading-feed").performTouchInput { swipeUp() }
        rule.onNodeWithTag("reading-feed").performTouchInput { swipeLeft() }
        rule.waitForIdle()
        rule.onNodeWithContentDescription("Настройки").assertIsDisplayed()
        rule.onAllNodes(tab).assertCountEquals(3)
    }

    @Test fun childScreensUseBackAndReturnWithTheIslandsVisible() {
        rule.onNodeWithTag("reading-feed").performScrollToNode(hasTestTag("open-atlas"))
        rule.onNodeWithTag("open-atlas").performClick()
        rule.onNodeWithContentDescription("Назад").assertIsDisplayed()
        rule.onAllNodes(tab).assertCountEquals(0)
        rule.onNodeWithText("Атлас тихих открытий").assertIsDisplayed()
        rule.onNodeWithContentDescription("Назад").performClick()
        rule.onNodeWithContentDescription("Настройки").assertIsDisplayed()
        rule.onAllNodes(tab).assertCountEquals(3)
        rule.onNode(tab and hasContentDescription("Читаю сейчас")).assertIsSelected()
    }

    @Test fun predictiveSettingsBackDoesNotScale() {
        rule.onNode(hasContentDescription("Библиотека") and hasClickAction()).performClick()
        rule.onNodeWithContentDescription("Настройки").performClick()
        rule.onNodeWithText("Тема").assertIsDisplayed()
        val initial = settingsBounds()
        rule.runOnUiThread {
            val dispatcher = rule.activity.onBackPressedDispatcher
            dispatcher.dispatchOnBackStarted(BackEventCompat(0f, 100f, 0f, BackEventCompat.EDGE_LEFT))
            dispatcher.dispatchOnBackProgressed(BackEventCompat(120f, 100f, 0.6f, BackEventCompat.EDGE_LEFT))
        }
        rule.waitForIdle()
        val during = settingsBounds()
        assertEquals(initial.width, during.width, 1f)
        assertEquals(initial.height, during.height, 1f)
        assertTrue(during.left > initial.left + 1f)
        rule.runOnUiThread { rule.activity.onBackPressedDispatcher.onBackPressed() }
        rule.onNodeWithText("Библиотека · UI-03").assertIsDisplayed()
        rule.onNode(hasContentDescription("Библиотека") and hasClickAction()).assertIsSelected()
    }

    @Test fun themesAndBothOrientationsKeepTheThreeTabs() {
        rule.onNodeWithContentDescription("Настройки").performClick()
        rule.onNodeWithText("Тёмная").performClick()
        rule.onNodeWithContentDescription("Назад").performClick()
        rule.onNodeWithContentDescription("Настройки").assertIsDisplayed()
        for (landscape in listOf(true, false)) {
            rule.runOnUiThread {
                rule.activity.requestedOrientation = if (landscape) ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                else ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            }
            val expected = if (landscape) Configuration.ORIENTATION_LANDSCAPE else Configuration.ORIENTATION_PORTRAIT
            rule.waitUntil(30_000) { rule.activity.resources.configuration.orientation == expected }
            rule.onNodeWithContentDescription("Настройки").assertIsDisplayed()
            rule.onAllNodes(tab).assertCountEquals(3)
            rule.onNode(tab and hasContentDescription("Читаю сейчас")).assertIsSelected()
        }
    }

    private fun chromeProgress(): Float = rule.onNodeWithTag("chrome-progress", useUnmergedTree = true)
        .fetchSemanticsNode().config[SemanticsProperties.StateDescription].toFloat()

    private fun shot(name: String) {
        rule.waitForIdle()
        val automation = InstrumentationRegistry.getInstrumentation().uiAutomation
        automation.waitForIdle(1_000, 3_000)
        val bitmap = automation.takeScreenshot() ?: return
        val file = File(rule.activity.filesDir, "screenshots/$name.png")
        file.parentFile?.mkdirs()
        file.outputStream().use { check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)) }
    }

    private fun settingsBounds(): Rect {
        val coordinates = rule.onNodeWithTag("settings-content").fetchSemanticsNode().layoutInfo.coordinates
        var root = coordinates
        while (root.parentLayoutCoordinates != null) root = requireNotNull(root.parentLayoutCoordinates)
        return root.localBoundingBoxOf(coordinates, clipBounds = false)
    }
}
