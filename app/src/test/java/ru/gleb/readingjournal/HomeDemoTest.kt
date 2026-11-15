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
            assertEquals(200 + 50 * (xp.level - 1), xp.nextCost)
            assertEquals(25 * (xp.level - 1) * (xp.level + 6) + xp.inLevel, xp.total)
            assertTrue(xp.inLevel in 0 until xp.nextCost)
            assertEquals(if (xp.level == 1) "Искатель" else "Ученик", xp.title)
        }
        assertEquals(XpUi(8, "Ученик", 2520, 70, 550), HomeFixtures.create().xp)
        assertEquals(1795 + 5 * 100 + 75 + 75 + 75, HomeFixtures.create().xp.total)
    }
    @Test fun pinnedOrderIsIndependentOfLastReadingOrder() {
        var s = HomeFixtures.create()
        assertEquals(listOf("atlas", "garden", "coast"), s.orderedBooks.map { it.id })
        s = reduceHomeDemo(s, HomeEvent.Pin("garden"))
        s = reduceHomeDemo(s, HomeEvent.MovePin("garden", -1))
        assertEquals(listOf("garden", "atlas", "coast"), s.orderedBooks.map { it.id })
        s = reduceHomeDemo(s, HomeEvent.Pin("garden"))
        s = reduceHomeDemo(s, HomeEvent.Pin("atlas"))
        assertEquals(listOf("garden", "atlas", "coast"), s.orderedBooks.map { it.id })
        assertEquals(HomeFixtures.books, s.books)
    }
    @Test fun activeBookListHasNoArtificialLimit() {
        val base = HomeFixtures.create()
        val books = (1..40).map { base.books.first().copy(id = "synthetic-$it", lastReadOrder = it) }
        val s = base.copy(books = books, pinnedIds = listOf("synthetic-1", "synthetic-2"))
        assertEquals(40, s.orderedBooks.size)
        assertEquals(listOf("synthetic-1", "synthetic-2", "synthetic-40"), s.orderedBooks.take(3).map { it.id })
        assertEquals(40, s.orderedBooks.map { it.id }.distinct().size)
    }
    @Test fun recordEntryUsesTheSelectedBookAndDoesNotChangeData() {
        val initial = HomeFixtures.create()
        initial.books.forEach { book ->
            val next = reduceHomeDemo(initial, HomeEvent.Open(HomeDestination.RecordReading(book.id)))
            assertTrue(requireNotNull(next.sheet).body.contains("Книга: ${book.id}"))
            assertEquals(initial.books, next.books)
            assertEquals(initial.snapshots, next.snapshots)
            assertEquals(initial.xp, next.xp)
            assertEquals(initial.streak, next.streak)
            assertEquals(initial, reduceHomeDemo(next, HomeEvent.Dismiss))
        }
        assertEquals(initial, reduceHomeDemo(initial, HomeEvent.Open(HomeDestination.RecordReading("missing"))))
    }
    @Test fun allPeriodsUsePreparedPagesAndCompletedCyclesButKeepGlobals() {
        val initial = HomeFixtures.create()
        val expected = listOf(21 to 0, 89 to 1, 320 to 1, 980 to 4, 1595 to 5, 1795 to 7)
        HomePeriod.entries.forEachIndexed { i, period ->
            val s = reduceHomeDemo(initial, HomeEvent.Period(period))
            assertEquals(expected[i].first, s.snapshot.pages.value)
            assertEquals(expected[i].second, s.snapshot.completions)
            assertEquals(initial.xp, s.xp)
            assertEquals(3, s.streak)
        }
    }
    @Test fun accordionHasExactlyFourOrderedSectionsAndCanCollapse() {
        assertEquals(listOf("Динамика чтения", "Календарь активности", "Сравнение книг", "Итоги и рекорды"), HomeSection.entries.map { it.label })
        var s = HomeFixtures.create()
        HomeSection.entries.forEach { section ->
            s = reduceHomeDemo(s, HomeEvent.Section(section))
            assertEquals(section, s.expanded)
            s = reduceHomeDemo(s, HomeEvent.Period(HomePeriod.MONTH))
            assertEquals(section, s.expanded)
        }
        assertNull(reduceHomeDemo(s, HomeEvent.Section(HomeSection.RECORDS)).expanded)
    }
    @Test fun bookmarkReachedPageAndConfirmedPagesAreIndependent() {
        val s = HomeFixtures.create(HomeScenario.BOOKMARK)
        val atlas = s.books.first()
        assertEquals(40, atlas.bookmark)
        assertEquals(200, atlas.reachedPage)
        assertEquals(200, atlas.confirmedPages)
        val next = reduceHomeDemo(s, HomeEvent.Open(HomeDestination.RecordReading("atlas")))
        assertEquals(s.books, next.books)
        assertEquals(s.xp, next.xp)
    }
    @Test fun historicalUnknownIsNotZeroAndNeverCreatesPages() {
        val s = HomeFixtures.create(HomeScenario.HISTORICAL)
        assertEquals("—", s.snapshot.pages.text)
        assertEquals(2, s.snapshot.completions)
        assertEquals(0, s.xp.total)
        assertEquals(0, s.streak)
        assertTrue(HomeFixtures.year(s).isEmpty())
        assertTrue(HomeFixtures.chart(s).bars.isEmpty())
        assertEquals("0", HomeFixtures.create(HomeScenario.EMPTY).snapshot.pages.text)
        assertTrue(s.snapshot.records.all { it.allTime == "—" && it.selected == "—" })
    }
    @Test fun everyHeatmapBoundaryUsesTheApprovedFiveColors() {
        val values = listOf(0, 1, 9, 10, 24, 25, 49, 50)
        assertEquals(listOf(0, 1, 1, 2, 2, 3, 3, 4), values.map { HeatDayUi(it).intensity })
        val year = HomeFixtures.year(HomeFixtures.create())
        assertTrue(values.all { value -> value == 0 || year.values.any { it.pages == value } })
    }
    @Test fun preparedCalendarTotalsAgreeWithAllPeriodMetrics() {
        val s = HomeFixtures.create()
        val year = HomeFixtures.year(s)
        val end = LocalDate.of(2026, 11, 12)
        assertEquals(1595, year.values.sumOf { it.pages })
        assertEquals(200, HomeFixtures.year(s.copy(calendarYear = 2025)).values.sumOf { it.pages })
        listOf(0L to 21, 6L to 89, 29L to 320, 89L to 980).forEach { (days, amount) ->
            assertEquals(amount, year.filterKeys { !it.isBefore(end.minusDays(days)) && !it.isAfter(end) }.values.sumOf { it.pages })
        }
        assertEquals(95, year.values.maxOf { it.pages })
        assertEquals(500, year.entries.groupBy { it.key.monthValue }.values.maxOf { month -> month.sumOf { it.value.pages } })
        assertTrue((1..8).all { year.getValue(LocalDate.of(2026, 11, it)).pages > 0 })
        assertEquals(0, year.getValue(LocalDate.of(2026, 11, 9)).pages)
    }
    @Test fun preparedBookContributionsAgreeWithCalendarAndComparison() {
        val s = HomeFixtures.create()
        val allDays = HomeFixtures.year(s) + HomeFixtures.year(s.copy(calendarYear = 2025))
        allDays.values.forEach { day -> assertEquals(day.pages, day.books.sumOf { it.pages }) }
        val bookTotals = allDays.values.flatMap { it.books }.groupBy { it.bookId }.mapValues { (_, values) -> values.sumOf { it.pages } }
        assertEquals(mapOf("atlas" to 600, "garden" to 600, "letters" to 595), bookTotals)
        assertEquals(0, s.snapshots.getValue(HomePeriod.ALL).comparisonPages["coast"])
        val today = allDays.getValue(LocalDate.of(2026, 11, 12))
        assertEquals(listOf("atlas", "garden"), today.books.map { it.bookId })
        assertEquals(listOf(15, 6), today.books.map { it.pages })
        val long = HomeFixtures.create(HomeScenario.LONG_TEXT)
        val title = long.books.first().title
        assertEquals(title, HomeFixtures.year(long).getValue(LocalDate.of(2026, 11, 12)).books.first().title)
        assertTrue(HomeFixtures.completionSummary(long).first().startsWith(title))
        assertTrue(long.snapshot.records.last().allTime.startsWith(title))
    }
    @Test fun preparedChartWindowsNeverInventAnUnknownOrApproximatePageDate() {
        val initial = HomeFixtures.create()
        assertEquals(89, HomeFixtures.chart(initial).bars.sumOf { it.pages })
        assertEquals(listOf(41, 48), HomeFixtures.chart(initial.copy(grouping = ChartGrouping.WEEK)).bars.map { it.pages })
        val month = initial.copy(period = HomePeriod.MONTH)
        assertEquals(320, HomeFixtures.chart(month.copy(grouping = ChartGrouping.MONTH)).bars.sumOf { it.pages })
        val weeks = month.copy(grouping = ChartGrouping.WEEK)
        assertEquals(320, HomeFixtures.chart(weeks).bars.sumOf { it.pages } + HomeFixtures.chart(weeks.copy(interval = -1)).bars.sumOf { it.pages })
        val all = initial.copy(period = HomePeriod.ALL, grouping = ChartGrouping.MONTH)
        assertEquals(1795, HomeFixtures.chart(all).bars.sumOf { it.pages } + HomeFixtures.chart(all.copy(interval = -1)).bars.sumOf { it.pages })
    }
    @Test fun intervalNavigationRespectsPreparedWindowsAndResetsOnPeriod() {
        val initial = HomeFixtures.create()
        assertEquals(initial, reduceHomeDemo(initial, HomeEvent.Interval(-1)))
        var s = reduceHomeDemo(initial, HomeEvent.Period(HomePeriod.MONTH))
        s = reduceHomeDemo(s, HomeEvent.Interval(-1))
        assertEquals(-1, s.interval)
        assertEquals(69, HomeFixtures.chart(s).bars.sumOf { it.pages })
        assertEquals(s, reduceHomeDemo(s, HomeEvent.Interval(-1)))
        assertEquals(0, reduceHomeDemo(s, HomeEvent.Interval(1)).interval)
        assertEquals(0, reduceHomeDemo(s, HomeEvent.Grouping(ChartGrouping.MONTH)).interval)
        assertEquals(0, reduceHomeDemo(s, HomeEvent.Period(HomePeriod.TODAY)).interval)
    }
    @Test fun savedVisualSelectionsAndPinOrderSurviveNewStateHolder() {
        val handle = SavedStateHandle()
        val vm = HomeDemoViewModel(handle)
        vm.dispatch(HomeEvent.Period(HomePeriod.MONTH))
        vm.dispatch(HomeEvent.Section(HomeSection.DYNAMICS))
        vm.dispatch(HomeEvent.Grouping(ChartGrouping.WEEK))
        vm.dispatch(HomeEvent.Interval(-1))
        vm.dispatch(HomeEvent.Year(-1))
        vm.dispatch(HomeEvent.Pin("garden"))
        vm.dispatch(HomeEvent.MovePin("garden", -1))
        val restored = HomeDemoViewModel(SavedStateHandle(handle.keys().associateWith { handle.get<Any?>(it) })).state.value
        assertEquals(vm.state.value, restored)
        assertEquals(listOf("garden", "atlas"), restored.pinnedIds)
        assertTrue(handle.keys().none { it.contains("books") || it.contains("records") })
    }
    @Test fun invalidSavedSelectionsAndTransientSheetsCannotBecomeHistory() {
        val vm = HomeDemoViewModel(SavedStateHandle(mapOf("home.scenario" to "obsolete", "home.period" to "invalid",
            "home.year" to 9999, "home.pins" to arrayListOf("missing", "atlas", "atlas"))))
        assertEquals(HomeScenario.MULTIPLE, vm.state.value.scenario)
        assertEquals(HomePeriod.WEEK, vm.state.value.period)
        assertEquals(listOf("atlas"), vm.state.value.pinnedIds)
        assertEquals(2026, vm.state.value.calendarYear)
        vm.dispatch(HomeEvent.Open(HomeDestination.RecordReading("atlas")))
        val clean = HomeDemoViewModel(SavedStateHandle())
        assertNull(clean.state.value.sheet)
        assertEquals(HomeFixtures.create().books, clean.state.value.books)
    }
    @Test fun emptySingleAndNoPagesHaveHonestDistinctFixtures() {
        val empty = HomeFixtures.create(HomeScenario.EMPTY)
        assertEquals(XpUi(1, "Искатель", 0, 0, 200), empty.xp)
        assertTrue(empty.books.isEmpty())
        val single = HomeFixtures.create(HomeScenario.SINGLE)
        assertEquals(1, single.books.size)
        assertEquals(12, single.snapshot.pages.value)
        assertEquals(12, single.xp.total)
        val zero = HomeFixtures.create(HomeScenario.NO_PAGES)
        assertEquals(listOf("coast"), zero.books.map { it.id })
        assertEquals(0, zero.snapshot.pages.value)
        assertEquals(0, zero.streak)
        assertTrue(zero.snapshot.records.all { it.selected == "—" })
    }
    @Test fun fourRecordsKeepTheirApprovedOrderForEveryPeriod() {
        val s = HomeFixtures.create()
        s.snapshots.values.forEach { snapshot -> assertEquals(listOf("Лучший день по страницам", "Лучший месяц по страницам",
            "Самая длинная серия", "Книга с наибольшим количеством прочитанных страниц"), snapshot.records.map { it.title }) }
        assertTrue(s.snapshot.records[0].selected.contains("28 стр."))
        assertTrue(s.snapshot.records[2].allTime.contains("8 дней"))
    }
    @Test fun cycleCompletionsDoNotMeanDistinctBookTitles() {
        val s = HomeFixtures.create().copy(period = HomePeriod.ALL)
        assertEquals(7, s.snapshot.completions)
        assertEquals(5, HomeFixtures.completionSummary(s).size)
        assertTrue(HomeFixtures.completionSummary(s).count { it.contains("2 завершённых цикла") } == 2)
        val historic = HomeFixtures.create(HomeScenario.HISTORICAL)
        assertTrue(HomeFixtures.completionSummary(historic).all { it.contains("страницы —") })
        assertTrue(HomeFixtures.year(historic).isEmpty())
    }
    @Test fun detailsAreTypedDemonstrationDestinationsNotNewScreens() {
        val initial = HomeFixtures.create()
        val day = reduceHomeDemo(initial, HomeEvent.Open(HomeDestination.DayDetail(LocalDate.of(2026, 11, 12))))
        assertTrue(requireNotNull(day.sheet).body.contains("Атлас тихих открытий: 15"))
        assertTrue(requireNotNull(day.sheet).body.contains("Сад между строк: 6"))
        val book = reduceHomeDemo(initial, HomeEvent.Open(HomeDestination.BookStatistics("coast")))
        assertTrue(requireNotNull(book.sheet).body.contains("Книга: coast"))
        assertTrue(requireNotNull(book.sheet).body.contains("0 новых страниц"))
        assertEquals(initial.xp, day.xp)
        assertEquals(initial.books, book.books)
    }
}
