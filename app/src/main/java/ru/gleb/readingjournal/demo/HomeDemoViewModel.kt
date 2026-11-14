package ru.gleb.readingjournal.demo

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import ru.gleb.readingjournal.ui.home.*

/** A single in-memory demo session. SavedState contains only small visual selections. */
class HomeDemoViewModel(private val saved: SavedStateHandle) : ViewModel() {
    private val mutableState = MutableStateFlow(restore())
    val state = mutableState.asStateFlow()

    fun dispatch(event: HomeEvent) {
        val next = reduceHomeDemo(mutableState.value, event)
        mutableState.value = next
        saved["home.scenario"] = next.scenario.name
        saved["home.period"] = next.period.name
        saved["home.section"] = next.expanded?.name
        saved["home.measure"] = next.measure.name
        saved["home.grouping"] = next.grouping.name
        saved["home.interval"] = next.interval
        saved["home.year"] = next.calendarYear
        saved["home.undated"] = next.undatedExpanded
        saved["home.speedBook"] = next.speedBookId
        saved["home.pins"] = ArrayList(next.pinnedIds)
        saved["home.pages"] = ArrayList(next.books.map { "${it.id}:${it.pages.name}" })
        saved["home.timerBook"] = next.timer?.bookId
        saved["home.timerPhase"] = next.timer?.phase?.name
    }

    private fun restore(): HomeUiState {
        val initial = HomeFixtures.create(enumValue(saved["home.scenario"], HomeScenario.MULTIPLE))
        val pages = saved.get<ArrayList<String>>("home.pages").orEmpty().associate { entry ->
            entry.substringBefore(':') to enumValue(entry.substringAfter(':'), PageState.KNOWN)
        }
        val timerBook = saved.get<String>("home.timerBook")
        return initial.copy(
            period = enumValue(saved["home.period"], initial.period),
            expanded = HomeSection.entries.firstOrNull { it.name == saved.get<String>("home.section") },
            measure = enumValue(saved["home.measure"], initial.measure),
            grouping = enumValue(saved["home.grouping"], initial.grouping),
            interval = saved.get<Int>("home.interval")?.coerceIn(-1, 0) ?: 0,
            calendarYear = saved.get<Int>("home.year")?.coerceIn(2025, 2026) ?: 2026,
            undatedExpanded = saved["home.undated"] ?: false,
            speedBookId = saved["home.speedBook"],
            pinnedIds = saved.get<ArrayList<String>>("home.pins")?.filter { id -> initial.books.any { it.id == id } } ?: initial.pinnedIds,
            books = initial.books.map { it.copy(pages = pages[it.id] ?: it.pages) },
            timer = if (!saved.contains("home.timerBook")) initial.timer else timerBook?.let {
                val phase = enumValue(saved["home.timerPhase"], DemoTimerPhase.PAUSED)
                DemoTimerUi(it, if (phase == DemoTimerPhase.FINISHING) DemoTimerPhase.PAUSED else phase) })
    }
}

private inline fun <reified T : Enum<T>> enumValue(value: String?, fallback: T): T =
    enumValues<T>().firstOrNull { it.name == value } ?: fallback

/** Changes only the demonstration, never records reading or calculates statistics. */
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
        else state.copy(pinnedIds = state.pinnedIds.toMutableList().apply {
            val id = removeAt(index); add(target, id)
        }.toList())
    }
    is HomeEvent.Start -> if (state.timer != null || state.books.none {
        it.id == event.bookId && it.pages != PageState.PENDING }) state
        else state.copy(timer = DemoTimerUi(event.bookId, DemoTimerPhase.RUNNING))
    HomeEvent.Pause -> state.copy(timer = state.timer?.let {
        if (it.phase == DemoTimerPhase.RUNNING) it.copy(phase = DemoTimerPhase.PAUSED) else it })
    HomeEvent.Resume -> state.copy(timer = state.timer?.let {
        if (it.phase == DemoTimerPhase.PAUSED) it.copy(phase = DemoTimerPhase.RUNNING) else it })
    HomeEvent.Finish -> state.timer?.let { timer -> state.copy(
        timer = timer.copy(phase = DemoTimerPhase.FINISHING),
        sheet = HomeSheetUi("Завершение · демонстрация",
            "Выберите пример состояния страниц. Реальный сеанс, минуты и XP не сохраняются.", timer.bookId)) } ?: state
    is HomeEvent.ResolvePages -> if (state.books.none { it.id == event.bookId }) state else state.copy(
        books = state.books.map { if (it.id == event.bookId) it.copy(pages = event.value) else it },
        timer = state.timer?.takeUnless { it.bookId == event.bookId && it.phase == DemoTimerPhase.FINISHING },
        sheet = null)
    is HomeEvent.PagePrompt -> if (state.books.none { it.id == event.bookId }) state else state.copy(
        sheet = HomeSheetUi("Страницы · демонстрация", "Выберите визуальное состояние. Это пример без записи сеанса.", event.bookId))
    HomeEvent.OpenTimer -> state.copy(focusBookId = state.timer?.bookId)
    HomeEvent.FocusConsumed -> state.copy(focusBookId = null)
    is HomeEvent.Measure -> state.copy(measure = event.value)
    is HomeEvent.Grouping -> state.copy(grouping = event.value, interval = 0)
    is HomeEvent.Interval -> state.copy(interval = (state.interval + event.direction).coerceIn(-1, 0))
    is HomeEvent.Year -> state.copy(calendarYear = (state.calendarYear + event.direction).coerceIn(2025, 2026))
    HomeEvent.ToggleUndated -> state.copy(undatedExpanded = !state.undatedExpanded)
    is HomeEvent.SpeedBook -> state.copy(speedBookId = event.bookId)
    is HomeEvent.Explain -> state.copy(sheet = HomeSheetUi(event.title, event.body, actionLabel = event.actionLabel))
    HomeEvent.Dismiss -> state.copy(sheet = null,
        timer = state.timer?.let { if (it.phase == DemoTimerPhase.FINISHING) it.copy(phase = DemoTimerPhase.PAUSED) else it })
}
