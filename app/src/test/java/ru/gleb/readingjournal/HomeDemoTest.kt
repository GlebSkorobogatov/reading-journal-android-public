package ru.gleb.readingjournal

import androidx.lifecycle.SavedStateHandle
import org.junit.Assert.*
import org.junit.Test
import ru.gleb.readingjournal.demo.*
import ru.gleb.readingjournal.ui.home.*
import java.time.LocalDate

class HomeDemoTest {
    @Test fun xpFixturesUseApprovedLevelCostAndTitle() {
        HomeScenario.entries.forEach { scenario ->
            val xp = HomeFixtures.create(scenario).xp
            val floor = 25 * (xp.level - 1) * (xp.level + 6)
            assertEquals(200 + 50 * (xp.level - 1), xp.nextCost)
            assertEquals(floor + xp.inLevel, xp.total)
            assertTrue(xp.inLevel in 0 until xp.nextCost)
            assertEquals(if (xp.level == 1) "Искатель" else "Ученик", xp.title)
        }
    }
    @Test fun pinnedOrderIsIndependentOfLastReadingOrder() {
        var s = HomeFixtures.create()
        assertEquals(listOf("atlas", "garden", "coast"), s.orderedBooks.map { it.id })
        s = reduceHomeDemo(s, HomeEvent.Pin("garden"))
        s = reduceHomeDemo(s, HomeEvent.MovePin("garden", -1))
        assertEquals(listOf("garden", "atlas", "coast"), s.orderedBooks.map { it.id })
        s = reduceHomeDemo(s, HomeEvent.Pin("atlas"))
        assertEquals(listOf("garden", "atlas", "coast"), s.orderedBooks.map { it.id })
        s = reduceHomeDemo(s, HomeEvent.Pin("garden"))
        assertEquals(listOf("garden", "atlas", "coast"), s.orderedBooks.map { it.id })
    }
    @Test fun activeBookListIsNotLimitedToAVisualPageSize() {
        val base = HomeFixtures.create()
        val books = (1..40).map { base.books.first().copy(id = "synthetic-$it", lastReadOrder = it) }
        val s = base.copy(books = books, pinnedIds = listOf("synthetic-1", "synthetic-2"))
        assertEquals(40, s.orderedBooks.size)
        assertEquals(listOf("synthetic-1", "synthetic-2", "synthetic-40"), s.orderedBooks.take(3).map { it.id })
        assertEquals(40, s.orderedBooks.map { it.id }.distinct().size)
    }
    @Test fun runningAndPausedTimerBothExcludeASecondTimer() {
        var s = reduceHomeDemo(HomeFixtures.create(), HomeEvent.Start("atlas"))
        s = reduceHomeDemo(s, HomeEvent.Start("garden"))
        assertEquals("atlas", s.timer?.bookId)
        assertEquals(DemoTimerPhase.RUNNING, s.timer?.phase)
        s = reduceHomeDemo(s, HomeEvent.Pause)
        s = reduceHomeDemo(s, HomeEvent.Start("garden"))
        assertEquals("atlas", s.timer?.bookId)
        assertEquals(DemoTimerPhase.PAUSED, s.timer?.phase)
        s = reduceHomeDemo(s, HomeEvent.Resume)
        assertEquals("18:40", s.timer?.elapsed)
    }
    @Test fun pendingBlocksOnlyItsBookWhileUnknownAllowsReading() {
        val pending = HomeFixtures.create(HomeScenario.PENDING)
        assertNull(reduceHomeDemo(pending, HomeEvent.Start("atlas")).timer)
        assertEquals("garden", reduceHomeDemo(pending, HomeEvent.Start("garden")).timer?.bookId)
        val unknown = reduceHomeDemo(pending, HomeEvent.ResolvePages("atlas", PageState.UNKNOWN))
        assertEquals("atlas", reduceHomeDemo(unknown, HomeEvent.Start("atlas")).timer?.bookId)
        assertEquals(PageState.UNKNOWN, unknown.books.first().pages)
    }
    @Test fun finishingDoesNotWriteStatisticsOrXp() {
        val initial = HomeFixtures.create(HomeScenario.RUNNING)
        val finish = reduceHomeDemo(initial, HomeEvent.Finish)
        assertEquals(DemoTimerPhase.FINISHING, finish.timer?.phase)
        assertEquals("atlas", reduceHomeDemo(finish, HomeEvent.Start("garden")).timer?.bookId)
        val later = reduceHomeDemo(finish, HomeEvent.ResolvePages("atlas", PageState.PENDING))
        assertNull(later.timer)
        assertEquals(PageState.PENDING, later.books.first().pages)
        assertEquals(initial.xp, later.xp)
        assertEquals(initial.snapshots, later.snapshots)
    }
    @Test fun dismissedFinishingReturnsToPauseWithoutInventingTime() {
        val s = reduceHomeDemo(reduceHomeDemo(HomeFixtures.create(HomeScenario.RUNNING), HomeEvent.Finish), HomeEvent.Dismiss)
        assertNull(s.sheet)
        assertEquals(DemoTimerPhase.PAUSED, s.timer?.phase)
        assertEquals("18:40", s.timer?.elapsed)
    }
    @Test fun allPeriodsKeepGlobalXpAndCurrentStreak() {
        val initial = HomeFixtures.create()
        assertEquals(6, HomePeriod.entries.size)
        HomePeriod.entries.forEach { period ->
            val s = reduceHomeDemo(initial, HomeEvent.Period(period))
            assertEquals(period, s.period)
            assertEquals(initial.xp, s.xp)
            assertEquals(3, s.streak)
            assertNotNull(s.snapshot)
        }
    }
    @Test fun accordionHasOneRememberedSectionAndCanCollapse() {
        var s = HomeFixtures.create()
        HomeSection.entries.forEach { section ->
            s = reduceHomeDemo(s, HomeEvent.Section(section))
            assertEquals(section, s.expanded)
            s = reduceHomeDemo(s, HomeEvent.Period(HomePeriod.MONTH))
            assertEquals(section, s.expanded)
        }
        s = reduceHomeDemo(s, HomeEvent.Section(HomeSection.RECORDS))
        assertNull(s.expanded)
    }
    @Test fun unknownZeroAndMinimumHaveDistinctDisplayContracts() {
        assertEquals("0", HomeFixtures.create(HomeScenario.EMPTY).snapshot.pages.text)
        assertEquals("—", HomeFixtures.create(HomeScenario.UNKNOWN).snapshot.pages.text)
        val minimum = HomeFixtures.create(HomeScenario.INCOMPLETE).snapshot.pages
        assertEquals("≥175", minimum.text)
        assertTrue(minimum.accessible.contains("Не менее"))
        assertNotEquals(QuantityUi(0), QuantityUi(null))
    }
    @Test fun monthlyQuantityAndDateMarkersAreIndependent() {
        val initial = HomeFixtures.create(HomeScenario.INCOMPLETE).copy(measure = ChartMeasure.PAGES)
        val monthly = HomeFixtures.chart(initial.copy(grouping = ChartGrouping.MONTH)).bars.last()
        assertEquals(70, monthly.amount.value)
        assertTrue(monthly.amount.minimum)
        assertTrue(monthly.partialDate)
        assertEquals(40, monthly.exactDayPages)
        assertEquals(30, monthly.monthOnlyPages)
        val daily = HomeFixtures.chart(initial.copy(grouping = ChartGrouping.DAY))
        assertTrue(daily.bars.none { it.partialDate })
        assertTrue(daily.bars.any { it.amount.value == null })
        assertTrue(daily.bars.any { it.amount.minimum })
    }
    @Test fun impreciseDatesStayOutsideFinerBuckets() {
        val s = HomeFixtures.create(HomeScenario.INCOMPLETE).copy(measure = ChartMeasure.PAGES)
        val day = HomeFixtures.chart(s.copy(grouping = ChartGrouping.DAY))
        assertTrue(day.undated.any { it.contains("30 страниц") && it.contains("день неизвестен") })
        val month = HomeFixtures.chart(s.copy(grouping = ChartGrouping.MONTH))
        assertTrue(month.undated.any { it.contains("месяц неизвестен") })
        assertTrue(month.undated.any { it.contains("Дата неизвестна") && it.contains("только за всё время") })
        assertTrue(day.bars.none { it.label.contains("Дата неизвестна") })
    }
    @Test fun pageOnlyHeatmapDoesNotCreateTimeOrAStreakDay() {
        val without = HomeFixtures.heatDay(2026, 11, 8, false)
        assertEquals(0, without.minutes)
        assertEquals(8, without.pagesWithoutTimer)
        val both = HomeFixtures.heatDay(2026, 11, 12, false)
        assertEquals(30, both.minutes)
        assertEquals(6, both.pagesWithoutTimer)
        assertTrue(HomeFixtures.heatDay(2026, 11, 9, false).minutes < 5)
        assertEquals(3, HomeFixtures.create().streak)
    }
    @Test fun savedVisualSelectionsSurviveNewStateHolder() {
        val handle = SavedStateHandle()
        val vm = HomeDemoViewModel(handle)
        vm.dispatch(HomeEvent.Period(HomePeriod.MONTH))
        vm.dispatch(HomeEvent.Section(HomeSection.SPEED))
        vm.dispatch(HomeEvent.SpeedBook("garden"))
        vm.dispatch(HomeEvent.Pin("garden"))
        vm.dispatch(HomeEvent.MovePin("garden", -1))
        vm.dispatch(HomeEvent.Start("garden"))
        vm.dispatch(HomeEvent.Pause)
        val restored = HomeDemoViewModel(SavedStateHandle(handle.keys().associateWith { handle.get<Any?>(it) })).state.value
        assertEquals(HomePeriod.MONTH, restored.period)
        assertEquals(HomeSection.SPEED, restored.expanded)
        assertEquals("garden", restored.speedBookId)
        assertEquals(listOf("garden", "atlas"), restored.pinnedIds)
        assertEquals(DemoTimerPhase.PAUSED, restored.timer?.phase)
        assertEquals("garden", restored.timer?.bookId)
    }
    @Test fun resolvedTimerDoesNotReappearWhenScenarioInitiallyHadOne() {
        val handle = SavedStateHandle()
        val vm = HomeDemoViewModel(handle)
        vm.dispatch(HomeEvent.Scenario(HomeScenario.RUNNING))
        vm.dispatch(HomeEvent.Finish)
        vm.dispatch(HomeEvent.ResolvePages("atlas", PageState.UNKNOWN))
        val restored = HomeDemoViewModel(SavedStateHandle(handle.keys().associateWith { handle.get<Any?>(it) })).state.value
        assertNull(restored.timer)
        assertEquals(PageState.UNKNOWN, restored.books.first().pages)
        assertEquals("atlas", reduceHomeDemo(restored, HomeEvent.Start("atlas")).timer?.bookId)
    }
    @Test fun preparedCalendarTotalsAndRecordsMatchThePeriodFixtures() {
        val initial = HomeFixtures.create()
        val year = HomeFixtures.year(initial)
        assertEquals(1500, year.values.sumOf { it.minutes })
        assertEquals(300, HomeFixtures.year(initial.copy(calendarYear = 2025)).values.sumOf { it.minutes })
        val end = LocalDate.of(2026, 11, 12)
        assertEquals(30, year.getValue(end).minutes)
        assertEquals(150, year.filterKeys { !it.isBefore(end.minusDays(6)) && !it.isAfter(end) }.values.sumOf { it.minutes })
        assertEquals(480, year.filterKeys { !it.isBefore(end.minusDays(29)) && !it.isAfter(end) }.values.sumOf { it.minutes })
        assertEquals(960, year.filterKeys { !it.isBefore(end.minusDays(89)) && !it.isAfter(end) }.values.sumOf { it.minutes })
        assertEquals(135, year.values.maxOf { it.minutes })
        assertEquals(480, year.entries.groupBy { it.key.monthValue }.values.maxOf { month -> month.sumOf { it.value.minutes } })
        assertTrue((1..12).all { year.getValue(LocalDate.of(2026, 9, it)).minutes >= 5 })
        assertEquals(0, year.getValue(LocalDate.of(2026, 9, 13)).minutes)
        val month = HomeFixtures.chart(initial.copy(grouping = ChartGrouping.MONTH))
        assertEquals(150, month.bars.single().amount.value)
        val monthWindow = HomeFixtures.chart(initial.copy(period = HomePeriod.MONTH, grouping = ChartGrouping.MONTH))
        assertEquals(480, monthWindow.bars.sumOf { requireNotNull(it.amount.value) })
    }
}
