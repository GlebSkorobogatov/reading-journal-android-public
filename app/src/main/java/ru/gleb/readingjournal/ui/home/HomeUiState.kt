package ru.gleb.readingjournal.ui.home

import java.time.LocalDate

/** Display-only contracts. Prepared examples never become user reading records. */
enum class HomeScenario(val label: String) {
    EMPTY("Пустой дневник"), SINGLE("Одна книга"), MULTIPLE("Несколько книг"),
    PINNED("Закреплённые книги"), NO_PAGES("Книга без записанных страниц"),
    BOOKMARK("Закладка ниже рубежа"), HISTORICAL("Исторические прочтения"),
    LONG_TEXT("Длинные названия")
}
enum class HomePeriod(val label: String) {
    TODAY("Сегодня"), WEEK("7 дней"), MONTH("30 дней"), QUARTER("90 дней"), YEAR("Год"), ALL("Всё время")
}
enum class HomeSection(val label: String) {
    DYNAMICS("Динамика чтения"), CALENDAR("Календарь активности"),
    COMPARISON("Сравнение книг"), RECORDS("Итоги и рекорды")
}
enum class ChartGrouping(val label: String) { DAY("День"), WEEK("Неделя"), MONTH("Месяц") }
data class QuantityUi(val value: Int?, val explanation: String = "") {
    val text: String get() = value?.toString() ?: "—"
    val accessible: String get() = if (value == null) "Неизвестно. $explanation" else "$value. $explanation"
}
data class HomeBookUi(
    val id: String, val title: String, val author: String, val bookmark: Int,
    val reachedPage: Int, val totalPages: Int, val confirmedPages: Int, val lastReadOrder: Int
)
data class XpUi(val level: Int, val title: String, val total: Int, val inLevel: Int, val nextCost: Int)
data class RecordUi(val title: String, val allTime: String, val selected: String)
data class PeriodSnapshotUi(
    val pages: QuantityUi, val completions: Int, val comparisonPages: Map<String, Int>,
    val records: List<RecordUi>
)
data class ChartBarUi(val label: String, val pages: Int, val detail: String = "")
data class ChartWindowUi(val label: String, val bars: List<ChartBarUi>, val canPrevious: Boolean = false)
data class DayBookUi(val bookId: String, val title: String, val pages: Int)
data class HeatDayUi(val pages: Int, val books: List<DayBookUi> = emptyList()) {
    /** Presentation bucket only; this does not determine a real streak or aggregate records. */
    val intensity: Int get() = when (pages) { 0 -> 0; in 1..9 -> 1; in 10..24 -> 2; in 25..49 -> 3; else -> 4 }
}
data class HomeSheetUi(val title: String, val body: String, val actionLabel: String? = null)
data class HomeUiState(
    val scenario: HomeScenario, val books: List<HomeBookUi>, val pinnedIds: List<String>,
    val snapshots: Map<HomePeriod, PeriodSnapshotUi>, val xp: XpUi, val streak: Int,
    val period: HomePeriod = HomePeriod.WEEK, val expanded: HomeSection? = null,
    val grouping: ChartGrouping = ChartGrouping.DAY, val interval: Int = 0,
    val calendarYear: Int = 2026, val sheet: HomeSheetUi? = null
) {
    val orderedBooks: List<HomeBookUi> get() = books.sortedWith(
        compareBy<HomeBookUi> { pinnedIds.indexOf(it.id).let { rank -> if (rank < 0) Int.MAX_VALUE else rank } }
            .thenByDescending { it.lastReadOrder })
    val snapshot: PeriodSnapshotUi get() = snapshots.getValue(period)
}

/** Typed future destinations shared by the two planned reading entry points (READ-01). */
sealed interface HomeDestination {
    data class RecordReading(val bookId: String) : HomeDestination
    data class ChartDetail(val grouping: ChartGrouping, val label: String, val pages: Int) : HomeDestination
    data class DayDetail(val date: LocalDate) : HomeDestination
    data class BookStatistics(val bookId: String) : HomeDestination
}
sealed interface HomeEvent {
    data class Scenario(val value: HomeScenario) : HomeEvent
    data class Period(val value: HomePeriod) : HomeEvent
    data class Section(val value: HomeSection) : HomeEvent
    data class Pin(val bookId: String) : HomeEvent
    data class MovePin(val bookId: String, val direction: Int) : HomeEvent
    data class Open(val destination: HomeDestination) : HomeEvent
    data class Grouping(val value: ChartGrouping) : HomeEvent
    data class Interval(val direction: Int) : HomeEvent
    data class Year(val direction: Int) : HomeEvent
    data class Explain(val title: String, val body: String, val actionLabel: String? = null) : HomeEvent
    data object Dismiss : HomeEvent
}
