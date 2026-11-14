package ru.gleb.readingjournal

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.ViewModelProvider
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import ru.gleb.readingjournal.app.MainActivity
import ru.gleb.readingjournal.demo.HomeDemoViewModel
import ru.gleb.readingjournal.ui.home.*

/** Actual MainActivity integration. The ten UI-01 tests remain a separate regression suite. */
class HomeInteractionTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()
    private fun event(event: HomeEvent) { rule.runOnUiThread { model().dispatch(event) } }
    private fun model() = ViewModelProvider(rule.activity)[HomeDemoViewModel::class.java]
    private fun scenario(value: HomeScenario) = event(HomeEvent.Scenario(value))
    private fun scroll(tag: String): SemanticsNodeInteraction {
        rule.onNodeWithTag("home-feed").performScrollToNode(hasTestTag(tag))
        return rule.onNodeWithTag(tag)
    }
    private fun section(value: HomeSection) { scroll("section-${value.name}").performClick() }

    @Test fun mainBlocksAndEveryActiveBookFollowTheApprovedOrder() {
        val tags = listOf("home-books-heading", "home-book-atlas", "home-book-garden", "home-book-coast",
            "home-xp", "home-metrics", "period", "section-DYNAMICS", "section-CALENDAR",
            "section-COMPARISON", "section-SPEED", "section-RECORDS", "home-footer")
        tags.forEachIndexed { index, tag ->
            rule.onNodeWithTag("home-feed").performScrollToIndex(index)
            rule.onNodeWithTag(tag).assertIsDisplayed()
        }
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
    @Test fun emptyDiaryShowsRealZeroAndNoBookOrTimer() {
        scenario(HomeScenario.EMPTY)
        scroll("home-empty").assertIsDisplayed()
        rule.onNodeWithTag("home-book-atlas").assertDoesNotExist()
        rule.onNodeWithTag("home-mini-timer").assertDoesNotExist()
        scroll("metric-pages").assert(hasText("0"))
        scroll("metric-streak").assert(hasText("0"))
    }
    @Test fun sixPeriodsSwitchWithoutChangingGlobalLevelOrStreak() {
        val xp = model().state.value.xp
        HomePeriod.entries.forEachIndexed { index, period ->
            scroll("period-$index").performClick().assertIsSelected()
            rule.runOnUiThread {
                assertEquals(period, model().state.value.period)
                assertEquals(xp, model().state.value.xp)
                assertEquals(3, model().state.value.streak)
            }
        }
        scroll("home-xp").assert(hasAnyDescendant(hasText("Уровень 8 · Ученик")))
    }
    @Test fun onlyOneAnalyticsSectionStaysExpanded() {
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
    }
    @Test fun recreationKeepsSectionPeriodAndSpeedSelection() {
        event(HomeEvent.Period(HomePeriod.MONTH))
        section(HomeSection.SPEED)
        scroll("speed-book-2").performClick()
        rule.activityRule.scenario.recreate()
        rule.runOnUiThread {
            assertEquals(HomeSection.SPEED, model().state.value.expanded)
            assertEquals(HomePeriod.MONTH, model().state.value.period)
            assertEquals("garden", model().state.value.speedBookId)
        }
        scroll("speed-value").assertTextEquals("30 стр/ч")
        scroll("period-2").assertIsSelected()
    }
    @Test fun startPauseResumeAndFinishAreVisualAndDeterministic() {
        scroll("start-atlas").performClick()
        rule.onNodeWithTag("home-mini-timer").assertIsDisplayed()
        scroll("timer-atlas").assertTextContains("18:40", substring = true)
        scroll("timer-toggle").performClick()
        scroll("timer-atlas").assertTextContains("На паузе", substring = true)
        scroll("timer-toggle").performClick()
        scroll("timer-atlas").assertTextContains("Идёт чтение", substring = true)
        scroll("timer-finish").performClick()
        rule.onNodeWithTag("home-sheet").assertTextEquals("Завершение · демонстрация")
        rule.onNodeWithText("Показать заполненный пример").performScrollTo().performClick()
        rule.onNodeWithTag("home-mini-timer").assertDoesNotExist()
        scroll("start-atlas").assertIsEnabled()
    }
    @Test fun pausedTimerPreventsStartingAnotherBook() {
        scenario(HomeScenario.PAUSED)
        scroll("start-garden").assertIsNotEnabled()
        rule.runOnUiThread { assertEquals("atlas", model().state.value.timer?.bookId) }
        scroll("timer-atlas").assertTextContains("На паузе", substring = true)
    }
    @Test fun miniTimerOnAnotherTabReturnsToItsBook() {
        scenario(HomeScenario.RUNNING)
        rule.onNode(hasText("Библиотека") and hasClickAction()).performClick()
        rule.onNodeWithText("Библиотека · UI-01").assertIsDisplayed()
        rule.onNodeWithTag("home-mini-timer").assertIsDisplayed().performClick()
        rule.onNode(hasText("Главная") and hasClickAction()).assertIsSelected()
        rule.onNodeWithTag("timer-atlas").assertIsDisplayed()
    }
    @Test fun pendingAndUnknownPagesHaveDifferentActions() {
        scenario(HomeScenario.PENDING)
        scroll("pending-atlas").assertIsDisplayed()
        scroll("start-atlas").assertIsNotEnabled()
        scroll("start-garden").assertIsEnabled()
        rule.onNodeWithTag("home-feed").performScrollToNode(hasText("Завершить запись сеанса"))
        rule.onNodeWithText("Завершить запись сеанса").performClick()
        rule.onNodeWithText("Не помню страницы").performScrollTo().performClick()
        scroll("unknown-atlas").assertIsDisplayed()
        rule.onNodeWithTag("pending-atlas").assertDoesNotExist()
        scroll("start-atlas").assertIsEnabled()
    }
    @Test fun delayingPagesFinishesTheDemoButBlocksOnlyItsBook() {
        scenario(HomeScenario.RUNNING)
        scroll("timer-finish").performClick()
        rule.onNodeWithText("Позже · ожидают заполнения").performScrollTo().performClick()
        rule.onNodeWithTag("home-mini-timer").assertDoesNotExist()
        scroll("start-atlas").assertIsNotEnabled()
        scroll("start-garden").assertIsEnabled()
    }
    @Test fun unknownZeroAndMinimumAreVisibleAsDifferentValues() {
        scenario(HomeScenario.UNKNOWN)
        scroll("metric-pages").assert(hasText("—")).performClick()
        rule.onNodeWithText("Неизвестно. Не помню страницы: объём не записан").assertIsDisplayed()
        rule.onNodeWithText("Закрыть панель").performScrollTo().performClick()
        scenario(HomeScenario.EMPTY)
        scroll("metric-pages").assert(hasText("0"))
        scenario(HomeScenario.INCOMPLETE)
        scroll("metric-pages").assert(hasText("≥175"))
    }
    @Test fun monthQuantityAndDateMarkersRemainIndependent() {
        scenario(HomeScenario.INCOMPLETE)
        event(HomeEvent.Measure(ChartMeasure.PAGES))
        event(HomeEvent.Grouping(ChartGrouping.MONTH))
        section(HomeSection.DYNAMICS)
        scroll("reading-chart")
        rule.onNodeWithTag("chart-bar-2").performScrollTo().assertIsDisplayed()
            .assertContentDescriptionContains("Есть страницы без точного дня", substring = true)
        rule.onNodeWithTag("quantity-marker-2", useUnmergedTree = true).assertIsDisplayed()
        rule.onNodeWithTag("date-marker-2", useUnmergedTree = true).assertIsDisplayed()
        val baseline = rule.onNodeWithTag("plot-area-0", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot.bottom
        for (index in 1..2) assertEquals("Bars share a baseline despite independent markers", baseline,
            rule.onNodeWithTag("plot-area-$index", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot.bottom, 1f)
        rule.onNodeWithTag("chart-bar-2").performScrollTo().performClick()
        rule.onNode(hasText("Есть страницы без точного дня: 40 с точными днями + 30", substring = true)).assertIsDisplayed()
        rule.onNodeWithText("Уточнить страницы").assertIsEnabled()
    }
    @Test fun undatedGroupAndIntervalNavigationDoNotInventDates() {
        scenario(HomeScenario.INCOMPLETE)
        event(HomeEvent.Measure(ChartMeasure.PAGES))
        section(HomeSection.DYNAMICS)
        scroll("undated-toggle").performClick()
        scroll("undated-0").assert(hasText("Ноябрь 2026 · 30 страниц · день неизвестен"))
        scroll("chart-previous").performClick()
        scroll("chart-next").assertIsEnabled().performClick().assertIsNotEnabled()
        scroll("chart-grouping-2").performClick()
        scroll("undated-0").assert(hasText("2026 · 20 страниц · месяц неизвестен"))
        scroll("undated-1").assert(hasText("Дата неизвестна · 15 страниц · только за всё время"))
    }
    @Test fun heatmapSeparatesTimePagesOnlyAndTheStreakThreshold() {
        scenario(HomeScenario.MIXED_ACTIVITY)
        section(HomeSection.CALENDAR)
        scroll("heatmap-scroll")
        rule.onNodeWithTag("heat-2026-11-08").performScrollTo()
            .assertContentDescriptionContains("0 минут; порог серии не достигнут; чтение без таймера: 8 страниц", substring = true)
            .performClick()
        rule.onNode(hasText("Записанное время: 0 мин.", substring = true)).assertIsDisplayed()
        rule.onNodeWithText("Закрыть панель").performScrollTo().performClick()
        rule.onNodeWithTag("heat-2026-11-12").performScrollTo()
            .assertContentDescriptionContains("30 минут; порог серии достигнут; чтение без таймера: 6 страниц", substring = true)
        scroll("year-previous").performClick().assertIsNotEnabled()
        scroll("year-next").performClick().assertIsNotEnabled()
    }
    @Test fun comparisonIncludesTheBookWithZeroTimeAndHonestHistoricalUnknowns() {
        section(HomeSection.COMPARISON)
        listOf("atlas", "garden", "coast").forEach { scroll("comparison-$it").assertIsDisplayed() }
        rule.onNodeWithTag("comparison-coast").assert(hasText("0 мин"))
        rule.onNodeWithTag("home-feed").performScrollToNode(hasText("Северный свет", substring = true))
        rule.onNode(hasText("Северный свет · время — · страницы — · сеансы —", substring = true)).assertIsDisplayed()
    }
    @Test fun speedShowsLibraryBookForecastAndInsufficientData() {
        section(HomeSection.SPEED)
        scroll("speed-value").assertTextEquals("30 стр/ч")
        scroll("forecast-value").assertTextEquals("—")
        scroll("speed-book-1").performClick()
        scroll("forecast-value").assertTextEquals("≈ 6 ч")
        scroll("speed-book-3").performClick()
        scroll("speed-value").assertTextEquals("—")
        scroll("forecast-value").assertTextEquals("—")
    }
    @Test fun sixRecordsAndCoverageStayInTheirFixedOrder() {
        section(HomeSection.RECORDS)
        val titles = listOf("Лучший день по времени", "Лучший день по страницам", "Самый длинный сеанс",
            "Лучший месяц по времени", "Самая длинная серия", "Книга с максимальным временем")
        titles.forEachIndexed { index, title -> scroll("record-$index").assert(hasAnyDescendant(hasText(title))) }
        rule.onNodeWithTag("record-6").assertDoesNotExist()
        scroll("records-coverage").assert(hasText("Охват записями: 2 из 3", substring = true))
    }
    @Test fun miniTimerReservesSpaceInsteadOfCoveringFeedOrNavigation() {
        scenario(HomeScenario.RUNNING)
        val feed = rule.onNodeWithTag("home-feed").fetchSemanticsNode().boundsInRoot
        val mini = rule.onNodeWithTag("home-mini-timer").fetchSemanticsNode().boundsInRoot
        val nav = rule.onNodeWithTag("shell-navigation").fetchSemanticsNode().boundsInRoot
        assertTrue(feed.bottom <= mini.top + 1f)
        assertTrue(mini.bottom <= nav.top + 1f)
        assertTrue(feed.height > 0f)
    }
    @Test fun debugCatalogSwitchesAllTenScenariosAndSettingsReturnKeepsContext() {
        rule.onNodeWithContentDescription("Настройки").performClick()
        rule.onNodeWithText("Каталог компонентов · debug").performScrollTo().performClick()
        rule.onNodeWithText("Сценарии главной · debug").performScrollTo().performClick()
        HomeScenario.entries.forEach { scenario -> rule.onNode(hasText(scenario.label, substring = true) and hasClickAction()).performScrollTo().assertIsEnabled() }
        rule.onNode(hasText(HomeScenario.SINGLE.label, substring = true) and hasClickAction()).performScrollTo().performClick()
        scroll("home-book-atlas").assertIsDisplayed()
        rule.runOnUiThread { assertEquals(1, model().state.value.books.size) }
        section(HomeSection.SPEED)
        rule.onNodeWithContentDescription("Настройки").performClick()
        rule.onNodeWithContentDescription("Назад").performClick()
        scroll("expanded-SPEED").assertIsDisplayed()
    }
}
