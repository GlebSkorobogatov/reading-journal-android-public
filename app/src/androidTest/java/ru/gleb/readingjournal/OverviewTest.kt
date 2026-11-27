package ru.gleb.readingjournal

import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.ViewModelProvider
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import ru.gleb.readingjournal.app.MainActivity
import ru.gleb.readingjournal.demo.DemoAction
import ru.gleb.readingjournal.demo.DemoScenario
import ru.gleb.readingjournal.demo.ReadingDemoViewModel
import ru.gleb.readingjournal.ui.overview.ReadingSubtitle

class OverviewTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()
    private val tab = SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Tab)

    @Test fun readingRootOnlyShowsActiveBooksAndEachRecordOpensItsOwnCycle() {
        rule.onNodeWithText(ReadingSubtitle).assertIsDisplayed()
        rule.onAllNodes(tab).assertCountEquals(3)
        val tabs = rule.onAllNodes(tab).fetchSemanticsNodes().sortedBy { it.boundsInRoot.left }
        assertEquals(listOf("Читаю сейчас", "Статистика", "Библиотека"),
            tabs.map { it.config[SemanticsProperties.ContentDescription].single() })
        rule.onNode(tab and hasContentDescription("Читаю сейчас")).assertIsSelected()
        rule.onNodeWithTag("xp-card").assertDoesNotExist()
        rule.onNodeWithTag("overview-metrics").assertDoesNotExist()
        rule.onNodeWithTag("record-atlas").showBookPart("record-atlas").performClick()
        rule.onNodeWithText("Атлас тихих открытий").assertIsDisplayed()
        rule.onNodeWithText("Записать чтение").assertIsDisplayed()
        rule.onNodeWithTag("record-destination-atlas-atlas-2").assertExists()
        rule.onAllNodes(tab).assertCountEquals(0)
        back()
        rule.onNodeWithTag("record-atlas").assertIsDisplayed()
        rule.onNodeWithTag("record-garden").showBookPart("record-garden").performClick()
        rule.onNodeWithText("Сад между строк").assertIsDisplayed()
        back()
        rule.onNodeWithTag("open-coast").showBookPart("open-coast").performClick()
        rule.onNodeWithText("Берег новых историй").assertIsDisplayed()
        back()
        rule.onNodeWithTag("cover-coast").showBookPart("cover-coast").assert(
            SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "Заглушка обложки: Б"))
    }

    @Test fun pinControlsReorderAndLastBookRemainsReachable() {
        demo(DemoScenario.MANY)
        rule.onNodeWithTag("pin-down-garden").showBookPart("pin-down-garden").assertIsEnabled().performClick()
        assertEquals(listOf("atlas", "garden"),
            model().state.value.reading.pins.map { it.value })
        rule.onNodeWithTag("pin-up-atlas").showBookPart("pin-up-atlas").assertIsNotEnabled()
        rule.onNodeWithTag("record-extra-12").showBookPart("record-extra-12").performClick()
        rule.onNodeWithText("Письма с севера · 12").assertIsDisplayed()
        back()
        rule.onNodeWithTag("record-extra-12").assertIsDisplayed()
    }

    @Test fun statisticsUsesOneCardSequentialPeriodsAndSeparateOutlets() {
        select("Статистика")
        rule.onNodeWithTag("overview-metrics").assertExists()
        rule.onNodeWithContentDescription("Прочитанные страницы").assertExists()
        rule.onNodeWithContentDescription("Завершённые прочтения").assertExists()
        rule.onNodeWithContentDescription("Текущая серия").assertExists()
        val xp = rule.onNodeWithTag("xp-total", useUnmergedTree = true).fetchSemanticsNode()
            .config[SemanticsProperties.Text].single().text
        val streak = rule.onNodeWithTag("overview-streak").fetchSemanticsNode().config[SemanticsProperties.StateDescription]
        step("PREVIOUS")
        rule.onNodeWithTag("period-PREVIOUS").assertIsNotEnabled()
        for ((period, pages, cycles) in listOf(
            Triple("Сегодня", "21", "0"), Triple("7 дней", "89", "1"), Triple("30 дней", "320", "1"),
            Triple("90 дней", "980", "4"), Triple("Год", "1,5K", "5"), Triple("Всё время", "1,7K", "7")
        )) {
            rule.onNodeWithTag("selected-period").assertTextEquals(period)
            rule.onNodeWithTag("overview-pages-value", useUnmergedTree = true).assertTextEquals(pages)
            rule.onNodeWithTag("overview-cycles-value", useUnmergedTree = true).assertTextEquals(cycles)
            if (period != "Всё время") step("NEXT")
        }
        rule.onNodeWithTag("period-NEXT").assertIsNotEnabled()
        rule.onNodeWithTag("xp-total", useUnmergedTree = true).assertTextEquals(xp)
        rule.onNodeWithTag("overview-streak").assert(
            SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, streak))
        rule.onNodeWithTag("xp-card").performScrollTo().performClick()
        rule.onNodeWithText("Достижения").assertIsDisplayed()
        rule.onAllNodes(tab).assertCountEquals(0)
        back()
        for ((route, label) in listOf("ReadingDynamics" to "Динамика чтения",
            "ActivityCalendar" to "Календарь активности", "ReadingRecords" to "Рекорды")) {
            rule.onNodeWithTag("overview-$route").performScrollTo().performClick()
            rule.onNodeWithText(label).assertIsDisplayed()
            rule.onAllNodes(tab).assertCountEquals(0)
            back()
            rule.onNodeWithTag("overview-$route").assertIsDisplayed()
            rule.onNodeWithTag("selected-period").assertTextEquals("Всё время")
        }
        back()
        rule.onNode(tab and hasContentDescription("Читаю сейчас")).assertIsSelected()
        select("Статистика")
        rule.onNodeWithTag("selected-period").assertTextEquals("Всё время")
        for (period in listOf("Год", "90 дней", "30 дней", "7 дней", "Сегодня")) {
            step("PREVIOUS")
            rule.onNodeWithTag("selected-period").assertTextEquals(period)
        }
        rule.onNodeWithTag("period-PREVIOUS").assertIsNotEnabled()
    }

    @Test fun stateSurvivesRecreationBackgroundAndTabSwitches() {
        demo(DemoScenario.MANY)
        rule.onNodeWithTag("record-extra-12").showBookPart("record-extra-12")
        select("Статистика")
        step("NEXT")
        rule.onNodeWithTag("overview-ReadingRecords").performScrollTo()
        rule.activityRule.scenario.recreate()
        rule.onNodeWithTag("selected-period").assertTextEquals("30 дней")
        rule.onNodeWithTag("overview-ReadingRecords").assertIsDisplayed()
        rule.activityRule.scenario.moveToState(androidx.lifecycle.Lifecycle.State.CREATED)
        rule.activityRule.scenario.moveToState(androidx.lifecycle.Lifecycle.State.RESUMED)
        rule.onNodeWithTag("overview-ReadingRecords").assertIsDisplayed()
        select("Библиотека")
        rule.onNodeWithText("Библиотека · UI-03").assertIsDisplayed()
        back()
        rule.onNodeWithTag("record-extra-12").assertIsDisplayed()
    }

    @Test fun emptyAndHistoricalFixturesDistinguishMissingData() {
        demo(DemoScenario.EMPTY)
        rule.onNodeWithTag("reading-empty").assertIsDisplayed()
        rule.onNodeWithTag("book-atlas").assertDoesNotExist()
        demo(DemoScenario.HISTORICAL)
        select("Статистика")
        repeat(4) { step("NEXT") }
        rule.onNodeWithTag("overview-pages-value", useUnmergedTree = true).assertTextEquals("—")
        rule.onNodeWithText("У завершённых циклов нет сведений о страницах").assertExists()
        rule.onNodeWithTag("overview-cycles-value", useUnmergedTree = true).assertTextEquals("2")
    }

    private fun SemanticsNodeInteraction.showBookPart(tag: String): SemanticsNodeInteraction {
        rule.onNodeWithTag("reading-feed").performScrollToNode(hasTestTag(tag))
        return this
    }

    private fun model() = ViewModelProvider(rule.activity)[ReadingDemoViewModel::class.java]
    private fun demo(scenario: DemoScenario) {
        rule.runOnIdle { model().onAction(DemoAction.SelectScenario(scenario)) }
    }
    private fun select(label: String) = rule.onNode(tab and hasContentDescription(label)).performClick()
    private fun step(direction: String) = rule.onNodeWithTag("period-$direction").performScrollTo().performClick()
    private fun back() = rule.runOnUiThread { rule.activity.onBackPressedDispatcher.onBackPressed() }
}
