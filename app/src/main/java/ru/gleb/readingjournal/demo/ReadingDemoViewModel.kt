package ru.gleb.readingjournal.demo

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import ru.gleb.readingjournal.ui.designsystem.ReadingStatus
import ru.gleb.readingjournal.ui.overview.*

data class ReadingDemoUi(
    val scenario: DemoScenario,
    val reading: CurrentlyReadingUi,
    val statistics: StatisticsUi
)

sealed interface DemoAction {
    data class SelectScenario(val scenario: DemoScenario) : DemoAction
    data class TogglePin(val book: BookId) : DemoAction
    data class MovePin(val book: BookId, val direction: Direction) : DemoAction
    data class StepPeriod(val direction: Direction) : DemoAction
}

class ReadingDemoViewModel(private val saved: SavedStateHandle) : ViewModel() {
    private var scenario = DemoScenario.entries.find { it.name == saved.get<String>("scenario") }
        ?: DemoScenario.MULTIPLE
    private var fixture = ReadingDemoFixtures.create(scenario)
    private var pins = saved.get<ArrayList<String>>("pins")?.map(::BookId) ?: fixture.pins
    private var period = Period.entries.find { it.name == saved.get<String>("period") } ?: Period.WEEK
    private val mutableState = MutableStateFlow(project())
    val state = mutableState.asStateFlow()

    fun onAction(action: DemoAction) {
        when (action) {
            is DemoAction.SelectScenario -> {
                if (scenario == action.scenario) return
                scenario = action.scenario
                fixture = ReadingDemoFixtures.create(scenario)
                pins = fixture.pins
                saved["scenario"] = scenario.name
            }
            is DemoAction.TogglePin -> {
                if (mutableState.value.reading.books.none { it.id == action.book }) return
                pins = if (action.book in pins) pins - action.book else pins + action.book
            }
            is DemoAction.MovePin -> {
                val from = pins.indexOf(action.book)
                val to = from + action.direction.offset
                if (from < 0 || to !in pins.indices) return
                pins = pins.toMutableList().apply { add(to, removeAt(from)) }.toList()
            }
            is DemoAction.StepPeriod -> period = period.step(action.direction) ?: return
        }
        saved["pins"] = ArrayList(pins.map { it.value })
        saved["period"] = period.name
        mutableState.value = project()
    }

    private fun project(): ReadingDemoUi = ReadingDemoUi(
        scenario, CurrentlyReadingUi(orderActiveBooks(fixture.books, pins), pins),
        StatisticsUi(fixture.xp, fixture.streak, period, fixture.snapshots.getValue(period))
    )
}

internal fun orderActiveBooks(books: List<ReadingBook>, pins: List<BookId>): List<ReadingBook> {
    val active = books.filter { it.status == ReadingStatus.READING }
    val byId = active.associateBy { it.id }
    return pins.mapNotNull(byId::get) + active.filter { it.id !in pins }
        .sortedWith(compareByDescending<ReadingBook> { it.activityOrder }.thenBy { it.id.value })
}
