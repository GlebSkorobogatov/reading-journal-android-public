package ru.gleb.readingjournal.demo

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import ru.gleb.readingjournal.ui.home.*

/** Only small visual selections are restored; books and history are never persisted. */
class HomeDemoViewModel(private val saved: SavedStateHandle) : ViewModel() {
    private val mutableState = MutableStateFlow(restore())
    val state = mutableState.asStateFlow()

    fun dispatch(event: HomeEvent) {
        val next = reduceHomeDemo(mutableState.value, event)
        mutableState.value = next
        saved["home.scenario"] = next.scenario.name
        saved["home.period"] = next.period.name
        saved["home.section"] = next.expanded?.name
        saved["home.grouping"] = next.grouping.name
        saved["home.interval"] = next.interval
        saved["home.year"] = next.calendarYear
        saved["home.pins"] = ArrayList(next.pinnedIds)
    }

    private fun restore(): HomeUiState {
        val initial = HomeFixtures.create(enumValue(saved["home.scenario"], HomeScenario.MULTIPLE))
        val restored = initial.copy(
            period = enumValue(saved["home.period"], initial.period),
            expanded = HomeSection.entries.firstOrNull { it.name == saved.get<String>("home.section") },
            grouping = enumValue(saved["home.grouping"], initial.grouping),
            interval = saved.get<Int>("home.interval")?.coerceIn(-1, 0) ?: 0,
            calendarYear = saved.get<Int>("home.year")?.coerceIn(2024, 2026) ?: 2026,
            pinnedIds = saved.get<ArrayList<String>>("home.pins")?.filter { id -> initial.books.any { it.id == id } }
                ?.distinct() ?: initial.pinnedIds)
        return if (restored.interval < 0 && !HomeFixtures.chart(restored.copy(interval = 0)).canPrevious)
            restored.copy(interval = 0) else restored
    }
}
private inline fun <reified T : Enum<T>> enumValue(value: String?, fallback: T): T =
    enumValues<T>().firstOrNull { it.name == value } ?: fallback

/** Navigation and fixture selection, never recording pages or awarding XP. */
fun reduceHomeDemo(state: HomeUiState, event: HomeEvent): HomeUiState = when (event) {
    is HomeEvent.Scenario -> HomeFixtures.create(event.value)
    is HomeEvent.Period -> state.copy(period = event.value, interval = 0)
    is HomeEvent.Section -> state.copy(expanded = event.value.takeUnless { it == state.expanded })
    is HomeEvent.Pin -> if (state.books.none { it.id == event.bookId }) state else state.copy(
        pinnedIds = if (event.bookId in state.pinnedIds) state.pinnedIds - event.bookId else state.pinnedIds + event.bookId)
    is HomeEvent.MovePin -> {
        val index = state.pinnedIds.indexOf(event.bookId)
        val target = index + event.direction
        if (index < 0 || target !in state.pinnedIds.indices || event.direction !in listOf(-1, 1)) state
        else state.copy(pinnedIds = state.pinnedIds.toMutableList().apply { add(target, removeAt(index)) }.toList())
    }
    is HomeEvent.Open -> when (val target = event.destination) {
        is HomeDestination.RecordReading -> state.books.firstOrNull { it.id == target.bookId }?.let { book ->
            state.copy(sheet = HomeSheetUi("Записать чтение · демонстрационный вход",
                "${book.title}\nКнига: ${book.id}\nОбщая форма UI-08 ещё не реализована: текущая страница, точная дата, явный переключатель обновления закладки и сохранение. Реальные страницы, прогресс и XP не изменяются."))
        } ?: state
        is HomeDestination.ChartDetail -> state.copy(sheet = HomeSheetUi("${target.label} · демонстрационный вход",
            "${target.pages} новых подтверждённых страниц. Группировка: ${target.grouping.label}. Полные подробности появятся в UI-09; это подготовленные данные, не расчёт истории."))
        is HomeDestination.DayDetail -> {
            val day = HomeFixtures.year(state)[target.date] ?: HeatDayUi(0)
            state.copy(sheet = HomeSheetUi("${target.date} · демонстрационный вход",
                "${day.pages} новых подтверждённых страниц.\n" +
                    day.books.joinToString("\n") { "${it.title}: ${it.pages} стр. · запись чтения" } +
                    if (day.pages == 0) "\nЗаписей страниц на этот день нет. Полные подробности — UI-09." else "\nЗаписи сгруппированы по книгам. Полные подробности — UI-09."))
        }
        is HomeDestination.BookStatistics -> state.books.firstOrNull { it.id == target.bookId }?.let { book ->
            state.copy(sheet = HomeSheetUi(book.title,
                "Книга: ${book.id}\n${state.snapshot.comparisonPages[book.id] ?: 0} новых страниц за период «${state.period.label}». Закладка ${book.bookmark}, рубеж ${book.reachedPage} из ${book.totalPages}. Это демонстрационный вход в будущую статистику книги, не готовый экран."))
        } ?: state
    }
    is HomeEvent.Grouping -> state.copy(grouping = event.value, interval = 0)
    is HomeEvent.Interval -> if (event.direction == 1 && state.interval < 0) state.copy(interval = 0)
        else if (event.direction == -1 && state.interval == 0 && HomeFixtures.chart(state).canPrevious) state.copy(interval = -1) else state
    is HomeEvent.Year -> state.copy(calendarYear = (state.calendarYear + event.direction).coerceIn(2024, 2026))
    is HomeEvent.Explain -> state.copy(sheet = HomeSheetUi(event.title, event.body, event.actionLabel))
    HomeEvent.Dismiss -> state.copy(sheet = null)
}
