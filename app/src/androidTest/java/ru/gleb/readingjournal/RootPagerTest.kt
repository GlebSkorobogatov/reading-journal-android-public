package ru.gleb.readingjournal

import android.graphics.Bitmap
import androidx.compose.ui.geometry.Offset
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.test.swipeRight
import androidx.compose.ui.test.swipeUp
import androidx.lifecycle.ViewModelProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import ru.gleb.readingjournal.app.MainActivity
import ru.gleb.readingjournal.demo.DemoAction
import ru.gleb.readingjournal.demo.DemoScenario
import ru.gleb.readingjournal.demo.ReadingDemoViewModel

class RootPagerTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()
    private val tab = SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Tab)

    @Test fun swipesMoveOnePageAndAShortSwipeCancels() {
        rule.onNodeWithText("Не количество страниц, а глубина понимания.").assertIsDisplayed()
        rule.onAllNodesWithTag("chrome-tabs").assertCountEquals(1)
        rule.onAllNodesWithText("Библиотека").assertCountEquals(0)
        rule.onNode(tab and hasContentDescription("Библиотека")).assertIsDisplayed()
        swipeReading { swipeLeft() }
        rule.onNodeWithTag("xp-card").assertIsDisplayed()
        rule.onNode(tab and hasContentDescription("Статистика")).assertIsSelected()
        assertEquals(0f, progress(), 0.02f)
        rule.onNodeWithTag("statistics-feed").performTouchInput { swipeLeft() }
        rule.waitForIdle()
        rule.onNodeWithText("Библиотека · UI-03").assertIsDisplayed()
        rule.onNodeWithTag("placeholder-viewport").performTouchInput { swipeLeft() }
        rule.onNodeWithText("Библиотека · UI-03").assertIsDisplayed()
        rule.onNode(tab and hasContentDescription("Библиотека")).assertIsSelected()
        rule.onNodeWithTag("placeholder-viewport").performTouchInput { swipeRight() }
        rule.onNodeWithTag("xp-card").assertIsDisplayed()
        rule.onNodeWithTag("statistics-feed").performTouchInput { swipeRight() }
        rule.waitForIdle()
        rule.onNodeWithText("Не количество страниц, а глубина понимания.").assertIsDisplayed()
        swipeReading { swipeRight() }
        rule.onNodeWithText("Не количество страниц, а глубина понимания.").assertIsDisplayed()
        // 20% of the width, held long enough to stay under the pager's 400 dp/s fling floor.
        // The same distance in 180 ms is about 450 dp/s here, so the stock pager treats it as a flick.
        swipeReading {
            swipeLeft(startX = right * 0.75f, endX = right * 0.55f, durationMillis = 800)
        }
        rule.onNodeWithText("Не количество страниц, а глубина понимания.").assertIsDisplayed()
        rule.onNodeWithTag("xp-card").assertIsNotDisplayed()
        swipeReading {
            swipeLeft(startX = right * 0.80f, endX = right * 0.55f, durationMillis = 50)
        }
        rule.onNodeWithTag("xp-card").assertIsDisplayed()
        rule.onNode(tab and hasContentDescription("Статистика")).assertIsSelected()
    }

    @Test fun aSecondTapFinishesOnTheLatestPage() {
        rule.mainClock.autoAdvance = false
        rule.onNode(tab and hasContentDescription("Статистика")).performClick()
        rule.mainClock.advanceTimeByFrame()
        rule.mainClock.advanceTimeBy(140)
        rule.onNode(tab and hasContentDescription("Библиотека")).performClick()
        rule.mainClock.advanceTimeBy(500)
        rule.mainClock.autoAdvance = true
        rule.waitForIdle()
        rule.onNodeWithText("Библиотека · UI-03").assertIsDisplayed()
        rule.onNode(tab and hasContentDescription("Библиотека")).assertIsSelected()
        rule.onAllNodesWithTag("chrome-tabs").assertCountEquals(1)
    }

    @Test fun iconTapsSlideAndBackReturnsToTheFirstPage() {
        rule.onNode(tab and hasContentDescription("Библиотека")).performClick()
        rule.onNodeWithText("Библиотека · UI-03").assertIsDisplayed()
        rule.onAllNodesWithTag("chrome-tabs").assertCountEquals(1)
        rule.onNode(tab and hasContentDescription("Статистика")).performClick()
        rule.onNodeWithTag("xp-card").assertIsDisplayed()
        rule.runOnUiThread { rule.activity.onBackPressedDispatcher.onBackPressed() }
        rule.onNodeWithText("Не количество страниц, а глубина понимания.").assertIsDisplayed()
        rule.onNode(tab and hasContentDescription("Читаю сейчас")).assertIsSelected()
    }

    @Test fun scrollPeriodAndChildReturnSurviveThePager() {
        rule.runOnIdle {
            ViewModelProvider(rule.activity)[ReadingDemoViewModel::class.java]
                .onAction(DemoAction.SelectScenario(DemoScenario.MANY))
        }
        rule.onNodeWithTag("reading-feed").performScrollToNode(hasTestTag("record-extra-12"))
        rule.onNodeWithTag("record-extra-12").assertIsDisplayed()
        rule.onNode(tab and hasContentDescription("Статистика")).performClick()
        val before = rule.onNodeWithTag("selected-period").fetchSemanticsNode()
            .config[SemanticsProperties.Text].single().text
        rule.onNodeWithTag("period-NEXT").performClick()
        val changed = rule.onNodeWithTag("selected-period").fetchSemanticsNode()
            .config[SemanticsProperties.Text].single().text
        assertTrue(changed != before)
        rule.onNode(tab and hasContentDescription("Читаю сейчас")).performClick()
        rule.onNodeWithTag("record-extra-12").assertIsDisplayed()
        rule.onNode(tab and hasContentDescription("Статистика")).performClick()
        rule.onNodeWithTag("selected-period").assertTextEquals(changed)
        rule.onNodeWithContentDescription("Настройки").performClick()
        rule.onNodeWithText("Тема").assertIsDisplayed()
        rule.onAllNodesWithTag("chrome-tabs").assertCountEquals(0)
        rule.onNodeWithContentDescription("Назад").performClick()
        rule.onNodeWithTag("selected-period").assertTextEquals(changed)
        rule.activityRule.scenario.recreate()
        rule.onNodeWithTag("selected-period").assertTextEquals(changed)
        rule.onNode(tab and hasContentDescription("Статистика")).assertIsSelected()
    }

    @Test fun aVerticalGestureDoesNotTurnThePageAndAHorizontalOneDoesNotHideTheIslands() {
        rule.onNodeWithTag("reading-feed").performTouchInput {
            down(center)
            moveBy(Offset(-24f, -520f))
            up()
        }
        rule.waitForIdle()
        // A vertical drag past the hide distance takes the islands off screen and drops their
        // semantics, so the selected tab is proven by the page that is still showing.
        assertEquals(1f, progress(), 0.02f)
        rule.onNodeWithContentDescription("Настройки").assertDoesNotExist()
        rule.onNodeWithTag("xp-card").assertIsNotDisplayed()
        rule.onNodeWithText("Библиотека · UI-03").assertDoesNotExist()
        swipeReading { swipeLeft(durationMillis = 260) }
        rule.onNodeWithTag("xp-card").assertIsDisplayed()
        assertEquals(0f, progress(), 0.02f)
    }

    @Test fun horizontalAndSettingsTransitionsLeaveOpaqueFrames() {
        rule.mainClock.autoAdvance = false
        rule.onNode(tab and hasContentDescription("Статистика")).performClick()
        repeat(4) { step ->
            rule.mainClock.advanceTimeByFrame()
            rule.mainClock.advanceTimeBy(48)
            shot("pager-to-statistics-$step")
        }
        rule.mainClock.autoAdvance = true
        rule.waitForIdle()
        rule.onNodeWithTag("xp-card").assertIsDisplayed()
        rule.mainClock.autoAdvance = false
        rule.onNodeWithContentDescription("Настройки").performClick()
        repeat(4) { step ->
            rule.mainClock.advanceTimeByFrame()
            rule.mainClock.advanceTimeBy(48)
            shot("settings-open-$step")
        }
        rule.mainClock.autoAdvance = true
        rule.waitForIdle()
        rule.onNodeWithText("Тема").assertIsDisplayed()
        rule.onAllNodesWithTag("chrome-tabs").assertCountEquals(0)
        rule.mainClock.autoAdvance = false
        rule.onNodeWithContentDescription("Назад").performClick()
        repeat(4) { step ->
            rule.mainClock.advanceTimeByFrame()
            rule.mainClock.advanceTimeBy(48)
            shot("settings-return-$step")
        }
        rule.mainClock.autoAdvance = true
        rule.waitForIdle()
        rule.onNodeWithTag("xp-card").assertIsDisplayed()
        rule.onNode(tab and hasContentDescription("Статистика")).assertIsSelected()
        rule.onNodeWithTag("settings-content").assertDoesNotExist()
    }

    @Test fun aHorizontalSwipeStillChangesPageAfterTheIslandsHide() {
        // Three default cards can sit inside the API 26 viewport. A swipe that cannot move the
        // list settles back to a visible island, so the page change is never reached.
        rule.runOnIdle {
            ViewModelProvider(rule.activity)[ReadingDemoViewModel::class.java]
                .onAction(DemoAction.SelectScenario(DemoScenario.MANY))
        }
        rule.onNodeWithTag("reading-subtitle").assertIsDisplayed()
        // swipeUp() from the feed edge also turns the pager on API 26, and the page change
        // shows the islands again. A vertical drag hides without leaving the page.
        rule.onNodeWithTag("reading-feed").performTouchInput {
            down(center)
            moveBy(Offset(0f, -520f))
            up()
        }
        rule.waitForIdle()
        assertEquals(1f, progress(), 0.02f)
        rule.onNodeWithContentDescription("Настройки").assertDoesNotExist()
        swipeReading { swipeLeft() }
        rule.onNodeWithTag("xp-card").assertIsDisplayed()
        rule.onNode(tab and hasContentDescription("Статистика")).assertIsSelected()
        assertEquals(0f, progress(), 0.02f)
        rule.onNodeWithContentDescription("Настройки").assertIsDisplayed()
    }

    private fun swipeReading(gesture: androidx.compose.ui.test.TouchInjectionScope.() -> Unit) {
        rule.onNodeWithTag("reading-feed").performTouchInput(gesture)
        rule.waitForIdle()
    }

    private fun progress(): Float = rule.onNodeWithTag("chrome-progress", useUnmergedTree = true)
        .fetchSemanticsNode().config[SemanticsProperties.StateDescription].toFloat()

    private fun shot(name: String) {
        val bitmap = InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot() ?: return
        val file = File(rule.activity.filesDir, "screenshots/$name.png")
        file.parentFile?.mkdirs()
        file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }
}
