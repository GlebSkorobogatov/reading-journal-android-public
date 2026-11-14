package ru.gleb.readingjournal.ui.home

/** Display contracts only. No clock, stored sessions, analytics or XP engine. */
enum class HomeScenario(val label: String) {
    EMPTY("Пустой дневник"), SINGLE("Одна книга"), MULTIPLE("Несколько книг"),
    RUNNING("Активный таймер"), PAUSED("Пауза"), PENDING("Ожидающие страницы"),
    INCOMPLETE("Неполные данные"), UNKNOWN("Неизвестные значения"),
    LONG_TEXT("Длинные названия"), MIXED_ACTIVITY("С временем и без таймера")
}
enum class HomePeriod(val label: String) {
    TODAY("Сегодня"), WEEK("7 дней"), MONTH("30 дней"), QUARTER("90 дней"), YEAR("Год"), ALL("Всё время")
}
enum class HomeSection(val label: String, val summary: String) {
    DYNAMICS("Динамика чтения", "Время и страницы по периодам"),
    CALENDAR("Календарь активности", "Год чтения · минуты и события без таймера"),
    COMPARISON("Сравнение книг", "Все читаемые книги, включая нулевые"),
    SPEED("Скорость чтения", "Средняя скорость и оставшиеся часы"),
    RECORDS("Итоги и рекорды", "Шесть личных ориентиров")
}
enum class ChartMeasure(val label: String) { TIME("Время"), PAGES("Страницы") }
enum class ChartGrouping(val label: String) { DAY("День"), WEEK("Неделя"), MONTH("Месяц") }
enum class PageState { KNOWN, PENDING, UNKNOWN }
enum class DemoTimerPhase(val label: String) { RUNNING("Идёт чтение"), PAUSED("На паузе"), FINISHING("Завершение") }

data class QuantityUi(val value: Int?, val minimum: Boolean = false, val explanation: String = "") {
    val text: String get() = when { value == null -> "—"; minimum -> "≥$value"; else -> value.toString() }
    val accessible: String get() = when {
        value == null -> "Неизвестно. $explanation"
        minimum -> "Не менее $value. $explanation"
        else -> "$value. $explanation"
    }
}
data class HomeBookUi(
    val id: String, val title: String, val author: String, val bookmark: Int,
    val totalPages: Int, val progress: Float, val lastReadOrder: Int,
    val pages: PageState = PageState.KNOWN
)
data class DemoTimerUi(val bookId: String, val phase: DemoTimerPhase, val elapsed: String = "18:40")
data class XpUi(val level: Int, val title: String, val total: Int, val inLevel: Int, val nextCost: Int)
data class PeriodSnapshotUi(
    val time: String, val pages: QuantityUi, val comparisonMinutes: List<Int>,
    val dayMinutes: List<Int>, val dayPages: List<Int>, val speed: String, val forecast: String,
    val bestDayTime: String, val bestDayPages: String, val longestSession: String, val bestMonth: String
)
data class ChartBarUi(
    val label: String, val amount: QuantityUi, val partialDate: Boolean = false,
    val detail: String = "", val exactDayPages: Int? = null, val monthOnlyPages: Int? = null
)
data class ChartWindowUi(val label: String, val bars: List<ChartBarUi>, val undated: List<String> = emptyList())
data class HeatDayUi(val minutes: Int, val pagesWithoutTimer: Int = 0)
data class HomeSheetUi(val title: String, val body: String, val finishingBookId: String? = null, val actionLabel: String? = null)
data class HomeUiState(
    val scenario: HomeScenario, val books: List<HomeBookUi>, val pinnedIds: List<String>,
    val snapshots: Map<HomePeriod, PeriodSnapshotUi>, val xp: XpUi,
    val period: HomePeriod = HomePeriod.WEEK, val expanded: HomeSection? = null,
    val timer: DemoTimerUi? = null, val measure: ChartMeasure = ChartMeasure.TIME,
    val grouping: ChartGrouping = ChartGrouping.DAY, val interval: Int = 0,
    val calendarYear: Int = 2026, val undatedExpanded: Boolean = false,
    val speedBookId: String? = null, val sheet: HomeSheetUi? = null,
    val focusBookId: String? = null
) {
    val orderedBooks: List<HomeBookUi> get() = books.sortedWith(
        compareBy<HomeBookUi> { pinnedIds.indexOf(it.id).let { rank -> if (rank < 0) Int.MAX_VALUE else rank } }
            .thenByDescending { it.lastReadOrder })
    val snapshot: PeriodSnapshotUi get() = snapshots.getValue(period)
    val streak: Int get() = when (scenario) { HomeScenario.EMPTY -> 0; HomeScenario.SINGLE -> 1; else -> 3 }
}

sealed interface HomeEvent {
    data class Scenario(val value: HomeScenario) : HomeEvent
    data class Period(val value: HomePeriod) : HomeEvent
    data class Section(val value: HomeSection) : HomeEvent
    data class Pin(val bookId: String) : HomeEvent
    data class MovePin(val bookId: String, val direction: Int) : HomeEvent
    data class Start(val bookId: String) : HomeEvent
    data object Pause : HomeEvent
    data object Resume : HomeEvent
    data object Finish : HomeEvent
    data class ResolvePages(val bookId: String, val value: PageState) : HomeEvent
    data class PagePrompt(val bookId: String) : HomeEvent
    data object OpenTimer : HomeEvent
    data object FocusConsumed : HomeEvent
    data class Measure(val value: ChartMeasure) : HomeEvent
    data class Grouping(val value: ChartGrouping) : HomeEvent
    data class Interval(val direction: Int) : HomeEvent
    data class Year(val direction: Int) : HomeEvent
    data object ToggleUndated : HomeEvent
    data class SpeedBook(val bookId: String?) : HomeEvent
    data class Explain(val title: String, val body: String, val actionLabel: String? = null) : HomeEvent
    data object Dismiss : HomeEvent
}
