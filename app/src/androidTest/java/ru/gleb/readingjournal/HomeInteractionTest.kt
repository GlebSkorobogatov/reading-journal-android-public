package ru.gleb.readingjournal

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.compose.ui.geometry.Offset
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import ru.gleb.readingjournal.app.MainActivity
import ru.gleb.readingjournal.demo.HomeDemoViewModel
import ru.gleb.readingjournal.ui.home.*

/** Actual MainActivity. UI-01 retains all its assertions on its original shell fixture. */
class HomeInteractionTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()
    private fun model() = ViewModelProvider(rule.activity)[HomeDemoViewModel::class.java]
    private fun event(event: HomeEvent) { rule.runOnUiThread { model().dispatch(event) }; rule.waitForIdle() }
    private fun scenario(value: HomeScenario) = event(HomeEvent.Scenario(value))
    private fun scroll(tag: String): SemanticsNodeInteraction {
        rule.onNodeWithTag("home-feed").performScrollToNode(hasTestTag(tag))
        return rule.onNodeWithTag(tag)
    }
    private fun section(value: HomeSection) { scroll("section-${value.name}").performClick() }
    private fun close() { rule.onNodeWithText("Закрыть панель").performScrollTo().performClick() }

    @Test fun mainBlocksAndEveryActiveBookFollowTheApprovedOrder() {
        val tags = listOf("home-books-heading", "home-book-atlas", "home-book-garden", "home-book-coast",
            "home-xp", "home-metrics", "period", "section-DYNAMICS", "section-CALENDAR",
            "section-COMPARISON", "section-RECORDS", "home-footer")
        tags.forEachIndexed { index, tag ->
            rule.onNodeWithTag("home-feed").performScrollToIndex(index)
            rule.onNodeWithTag(tag).assertIsDisplayed()
        }
        rule.onNodeWithTag("section-SPEED").assertDoesNotExist()
    }
    @Test fun pinAndManualOrderAreInteractive() {
        scroll("pin-garden").performClick()
        scroll("pin-up-garden").performClick()
        rule.onNodeWithTag("home-feed").performScrollToIndex(1)
        rule.onNodeWithTag("home-book-garden").assertIsDisplayed()
        rule.runOnUiThread { assertEquals(listOf("garden", "atlas"), model().state.value.pinnedIds) }
        scroll("pin-garden").performClick()
        rule.onNodeWithTag("home-feed").performScrollToIndex(1)
        rule.onNodeWithTag("home-book-atlas").assertIsDisplayed()
    }
    @Test fun everyRecordButtonOpensItsBookWithoutSavingData() {
        val before = model().state.value
        val navigationBottom = ViewCompat.getRootWindowInsets(rule.activity.window.decorView)
            ?.getInsets(WindowInsetsCompat.Type.navigationBars())?.bottom ?: error("Missing system insets")
        listOf("atlas", "garden", "coast").forEach { id ->
            scroll("record-reading-$id").assertIsEnabled().performClick()
            rule.onNodeWithTag("home-sheet").assertTextEquals("Записать чтение · демонстрационный вход")
            rule.onNodeWithTag("home-sheet-body").assertTextContains("Книга: $id", substring = true)
            val close = rule.onNodeWithTag("home-sheet-close").performScrollTo().fetchSemanticsNode()
            val coordinates = close.layoutInfo.coordinates
            val bitmap = requireNotNull(InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot())
            try {
                val bottom = coordinates.localToScreen(Offset.Zero).y + coordinates.size.height
                assertTrue("Close action bottom=$bottom; safe bottom=${bitmap.height - navigationBottom}; inset=$navigationBottom",
                    bottom.isFinite() && bottom <= bitmap.height - navigationBottom + 1f)
            } finally { bitmap.recycle() }
            rule.runOnUiThread {
                assertEquals(before.books, model().state.value.books)
                assertEquals(before.xp, model().state.value.xp)
            }
            close()
        }
    }
    @Test fun emptyDiaryShowsRealZeroLevelOneAndNoBookOrTimer() {
        scenario(HomeScenario.EMPTY)
        scroll("home-empty").assertIsDisplayed()
        rule.onNodeWithTag("home-book-atlas").assertDoesNotExist()
        rule.onNodeWithTag("home-mini-timer").assertDoesNotExist()
        scroll("home-xp").assert(hasAnyDescendant(hasText("Уровень 1 · Искатель")))
        listOf("metric-pages", "metric-completions", "metric-streak").forEach { scroll(it).assert(hasText("0")) }
    }
    @Test fun sixPeriodsSwitchMetricsButKeepGlobalXpAndStreak() {
        val xp = model().state.value.xp
        val expected = listOf(21, 89, 320, 980, 1595, 1795)
        val completions = listOf(0, 1, 1, 4, 5, 7)
        HomePeriod.entries.forEachIndexed { index, period ->
            scroll("period-$index").performClick().assertIsSelected()
            scroll("metric-pages").assert(hasText(expected[index].toString()))
            scroll("metric-completions").assert(hasText(completions[index].toString()))
            rule.onNodeWithTag("metric-time").assertDoesNotExist()
            rule.runOnUiThread {
                assertEquals(period, model().state.value.period)
                assertEquals(xp, model().state.value.xp)
                assertEquals(3, model().state.value.streak)
            }
        }
        scroll("home-xp").assert(hasAnyDescendant(hasText("Уровень 8 · Ученик")))
        scroll("home-metrics")
        val metrics = listOf(Triple("metric-pages", "Страницы", "Прочитанные страницы"),
            Triple("metric-completions", "Прочтения", "Завершённые прочтения"),
            Triple("metric-streak", "Серия", "Текущая серия"))
        metrics.forEach { (tag, label, fullTitle) ->
            val layouts = mutableListOf<TextLayoutResult>()
            val labelNode = rule.onNodeWithTag("$tag-label", useUnmergedTree = true)
            labelNode.assertTextEquals(label)
                .performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(layouts) }
            rule.onNodeWithTag(tag).assert(hasContentDescription(fullTitle, substring = true))
            assertTrue("Metric label has measured text", layouts.isNotEmpty())
            layouts.forEach { layout ->
                assertEquals("The compact metric word stays on one line", 1, layout.lineCount)
                // String Text semantics reconstructs a paragraph at the parent maxWidth,
                // while the actual Text node wraps its single-line content. Test the
                // rendered line against that node, not the wider reconstructed paragraph.
                val size = labelNode.fetchSemanticsNode().layoutInfo.coordinates.size
                assertTrue("Metric line fits Text width: $label, line=${layout.getLineLeft(0)}..${layout.getLineRight(0)}, node=$size",
                    layout.getLineLeft(0) >= 0f && layout.getLineRight(0) <= size.width)
                assertTrue("Metric line fits Text height: $label, node=$size",
                    layout.getLineTop(0) >= 0f && layout.getLineBottom(0) <= size.height)
                assertFalse("Metric text has no ellipsis", layout.isLineEllipsized(0))
                for (line in 0 until layout.lineCount) {
                    val firstWord = layout.layoutInput.text.text.substring(layout.getLineStart(line), layout.getLineEnd(line))
                        .trim().substringBefore(' ')
                    assertTrue("Metric words must not wrap to a lone letter: $firstWord", firstWord.length > 1)
                }
            }
            rule.onNodeWithTag(tag).performClick()
            rule.onNodeWithTag("home-sheet").assertTextEquals(fullTitle)
            if (tag == "metric-completions") rule.onNodeWithTag("home-sheet-body")
                .assertTextContains("7 завершённых прочтений, включая повторные циклы", substring = true)
            close()
        }
    }
    @Test fun onlyOneOfFourSectionsCanRemainExpandedAndItCanCollapse() {
        HomeSection.entries.forEachIndexed { index, value ->
            section(value)
            scroll("expanded-${value.name}").assertIsDisplayed()
            if (index > 0) {
                val previous = HomeSection.entries[index - 1]
                scroll("section-${previous.name}")
                rule.onNodeWithTag("expanded-${previous.name}").assertDoesNotExist()
            }
            rule.runOnUiThread { assertEquals(value, model().state.value.expanded) }
        }
        section(HomeSection.RECORDS)
        rule.onNodeWithTag("expanded-RECORDS").assertDoesNotExist()
    }
    @Test fun recreationKeepsPeriodGroupingYearAndPinOrder() {
        event(HomeEvent.Period(HomePeriod.MONTH))
        event(HomeEvent.Grouping(ChartGrouping.WEEK))
        event(HomeEvent.Pin("garden"))
        event(HomeEvent.MovePin("garden", -1))
        event(HomeEvent.Year(-1))
        section(HomeSection.DYNAMICS)
        rule.activityRule.scenario.recreate()
        rule.runOnUiThread {
            val s = model().state.value
            assertEquals(HomeSection.DYNAMICS, s.expanded)
            assertEquals(HomePeriod.MONTH, s.period)
            assertEquals(ChartGrouping.WEEK, s.grouping)
            assertEquals(2025, s.calendarYear)
            assertEquals(listOf("garden", "atlas"), s.pinnedIds)
        }
        scroll("chart-grouping-1").assertIsSelected()
    }
    @Test fun settingsAndBackPreserveTheHomeContext() {
        event(HomeEvent.Period(HomePeriod.MONTH))
        section(HomeSection.RECORDS)
        rule.onNodeWithContentDescription("Настройки").performClick()
        rule.onNodeWithContentDescription("Назад").performClick()
        scroll("expanded-RECORDS").assertIsDisplayed()
        scroll("period-2").assertIsSelected()
    }
    @Test fun otherTabsRemainUi01PlaceholdersWithoutAnyMiniPanel() {
        listOf("Библиотека", "Достижения").forEach { tab ->
            rule.onNode(hasText(tab) and hasClickAction()).performClick()
            rule.onNodeWithText("$tab · UI-01").assertIsDisplayed()
            rule.onNodeWithTag("home-mini-timer").assertDoesNotExist()
        }
        rule.onNode(hasText("Главная") and hasClickAction()).performClick()
        rule.onNodeWithTag("home-books-heading").assertIsDisplayed()
        rule.onNodeWithTag("timer-atlas").assertDoesNotExist()
        rule.onNodeWithTag("start-atlas").assertDoesNotExist()
    }
    @Test fun bookmarkDoesNotBecomeTheReachedOrConfirmedPage() {
        scenario(HomeScenario.BOOKMARK)
        scroll("bookmark-atlas").assertTextEquals("Закладка 40 / 300 стр.")
        scroll("confirmed-atlas").assertTextEquals("Подтверждено в текущем цикле: 200 стр.")
        rule.onNode(hasText("Рубеж 200 / 300 стр.", substring = true)).assertExists()
        scroll("record-reading-atlas").performClick()
        close()
        scroll("bookmark-atlas").assertTextEquals("Закладка 40 / 300 стр.")
    }
    @Test fun historicalUnknownsDoNotBecomePagesOrRecords() {
        scenario(HomeScenario.HISTORICAL)
        scroll("metric-pages").assert(hasText("—")).performClick()
        rule.onNodeWithTag("home-sheet-body").assertTextContains("нет записанных страниц", substring = true)
        close()
        section(HomeSection.DYNAMICS)
        scroll("chart-empty").assertIsDisplayed()
        section(HomeSection.RECORDS)
        scroll("record-0").assert(hasAnyDescendant(hasText("За всё время · —")))
        scroll("records-coverage").assert(hasText("Охват записями: 0 из 2", substring = true))
    }
    @Test fun bookWithoutPagesStillHasAnEnabledRecordEntry() {
        scenario(HomeScenario.NO_PAGES)
        scroll("record-reading-coast").assertIsDisplayed().assertIsEnabled()
        scroll("metric-pages").assert(hasText("0"))
        section(HomeSection.COMPARISON)
        scroll("comparison-coast").assert(hasText("0 стр."))
    }
    @Test fun dynamicsOnlyShowsPagesAndAllThreeGroupings() {
        section(HomeSection.DYNAMICS)
        for (index in 0..2) {
            scroll("chart-grouping-$index").performClick().assertIsSelected()
            scroll("chart-pages-label").assertTextEquals("Новые подтверждённые страницы")
            rule.onNodeWithTag("chart-measure").assertDoesNotExist()
        }
        scroll("reading-chart")
        rule.onNodeWithTag("chart-bar-0").performScrollTo().assertContentDescriptionContains("89 новых подтверждённых страниц", substring = true)
    }
    @Test fun intervalNavigationUsesPreparedWindowsAndResetsWithGrouping() {
        event(HomeEvent.Period(HomePeriod.MONTH))
        section(HomeSection.DYNAMICS)
        scroll("chart-previous").assertIsEnabled().performClick().assertIsNotEnabled()
        scroll("chart-next").assertIsEnabled().performClick().assertIsNotEnabled()
        scroll("chart-grouping-2").performClick()
        scroll("chart-previous").assertIsNotEnabled()
    }
    @Test fun chartDetailIsExplicitlyAFutureDemonstrationEntry() {
        section(HomeSection.DYNAMICS)
        scroll("reading-chart")
        rule.onNodeWithTag("chart-bar-0").performScrollTo().performClick()
        rule.onNodeWithTag("home-sheet-body").assertTextContains("5 новых подтверждённых страниц", substring = true)
        rule.onNodeWithTag("home-sheet-body").assertTextContains("UI-09", substring = true)
    }
    @Test fun calendarContainsAllDaysIncludingALeapYear() {
        section(HomeSection.CALENDAR)
        scroll("heatmap-scroll")
        fun days(year: Int) = SemanticsMatcher("Every real calendar day") {
            it.config.getOrNull(SemanticsProperties.TestTag)?.startsWith("heat-$year-") == true
        }
        rule.onAllNodes(days(2026)).assertCountEquals(365)
        scroll("year-previous").performClick()
        scroll("year-previous").performClick().assertIsNotEnabled()
        scroll("heatmap-scroll")
        rule.onAllNodes(days(2024)).assertCountEquals(366)
    }
    @Test fun heatmapUsesEveryApprovedBoundaryAndOnePageQualifies() {
        section(HomeSection.CALENDAR)
        scroll("heatmap-scroll")
        listOf("2026-11-09" to (0 to 0), "2026-11-01" to (1 to 1), "2026-11-02" to (9 to 1),
            "2026-11-03" to (10 to 2), "2026-11-04" to (24 to 2), "2026-11-05" to (25 to 3),
            "2026-10-22" to (49 to 3), "2026-08-16" to (50 to 4)).forEach { (date, expected) ->
            rule.onNodeWithTag("heat-$date").performScrollTo()
                .assertContentDescriptionContains("${expected.first} новых страниц; уровень интенсивности ${expected.second}", substring = true)
        }
        rule.onNodeWithTag("heat-2026-11-01").performScrollTo().assertContentDescriptionContains("день серии", substring = true)
    }
    @Test fun dayEntryShowsPageRecordsGroupedByBook() {
        section(HomeSection.CALENDAR)
        scroll("heatmap-scroll")
        rule.onNodeWithTag("heat-2026-11-12").performScrollTo().performClick()
        rule.onNodeWithTag("home-sheet-body").assertTextContains("21 новых подтверждённых страниц", substring = true)
        rule.onNodeWithTag("home-sheet-body").assertTextContains("Атлас тихих открытий: 15", substring = true)
        rule.onNodeWithTag("home-sheet-body").assertTextContains("Сад между строк: 6", substring = true)
    }
    @Test fun comparisonIncludesEveryActiveBookAndTheZeroPageBook() {
        section(HomeSection.COMPARISON)
        listOf("atlas", "garden", "coast").forEach { scroll("comparison-$it").assertIsDisplayed() }
        scroll("comparison-coast").assert(hasText("0 стр.")).performClick()
        rule.onNodeWithTag("home-sheet-body").assertTextContains("Книга: coast", substring = true)
        close()
        rule.onNodeWithTag("home-feed").performScrollToNode(hasText("Северный свет", substring = true))
        rule.onNode(hasText("Северный свет · 2024 · страницы —", substring = true)).assertIsDisplayed()
    }
    @Test fun fourRecordsAndCoverageStayInTheirFixedOrder() {
        section(HomeSection.RECORDS)
        listOf("Лучший день по страницам", "Лучший месяц по страницам", "Самая длинная серия",
            "Книга с наибольшим количеством прочитанных страниц").forEachIndexed { i, title ->
            scroll("record-$i").assert(hasAnyDescendant(hasText(title)))
        }
        rule.onNodeWithTag("record-4").assertDoesNotExist()
        scroll("records-coverage").assert(hasText("Охват записями: 5 из 7", substring = true))
    }
    @Test fun debugCatalogExposesOnlyThePageBasedScenarios() {
        rule.onNodeWithContentDescription("Настройки").performClick()
        rule.onNodeWithText("Каталог компонентов · debug").performScrollTo().performClick()
        rule.onNodeWithText("Сценарии главной · debug").performScrollTo().performClick()
        HomeScenario.entries.forEach { value ->
            rule.onNode(hasText(value.label, substring = true) and hasClickAction()).performScrollTo().assertIsEnabled()
        }
        rule.onNode(hasText(HomeScenario.SINGLE.label, substring = true) and hasClickAction()).performScrollTo().performClick()
        scroll("record-reading-atlas").assertIsDisplayed()
        rule.runOnUiThread { assertEquals(1, model().state.value.books.size) }
    }
    @Test fun backgroundAndReturnKeepVisualStateWithoutRecordingHistory() {
        event(HomeEvent.Period(HomePeriod.MONTH))
        event(HomeEvent.Pin("garden"))
        section(HomeSection.COMPARISON)
        val before = model().state.value
        rule.activityRule.scenario.moveToState(Lifecycle.State.CREATED)
        rule.activityRule.scenario.moveToState(Lifecycle.State.RESUMED)
        rule.waitForIdle()
        rule.runOnUiThread { assertEquals(before, model().state.value) }
        scroll("expanded-COMPARISON").assertIsDisplayed()
    }
}
