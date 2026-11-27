package ru.gleb.readingjournal

import androidx.lifecycle.SavedStateHandle
import org.junit.Assert.*
import org.junit.Test
import ru.gleb.readingjournal.demo.*
import ru.gleb.readingjournal.ui.designsystem.ReadingStatus
import ru.gleb.readingjournal.ui.designsystem.coverInitial
import ru.gleb.readingjournal.ui.overview.*

class ReadingDemoTest {
    @Test fun onlyActiveBooksAppearAndTiesInTitleNeverMergeIdentities() {
        val model = ReadingDemoViewModel(SavedStateHandle())
        model.onAction(DemoAction.SelectScenario(DemoScenario.MANY))
        val books = model.state.value.reading.books
        assertEquals(15, books.size)
        assertEquals(listOf("garden", "atlas", "coast"), books.take(3).map { it.id.value })
        assertEquals(2, books.count { it.title == "Атлас тихих открытий" })
        assertEquals(books.size, books.map { it.id }.distinct().size)
        assertEquals(books.size, books.map { it.cycleId }.distinct().size)
        assertTrue(books.all { it.status == ReadingStatus.READING })
    }

    @Test fun pinReorderAndUnpinKeepReadingActivityOrder() {
        val saved = SavedStateHandle()
        val model = ReadingDemoViewModel(saved)
        val garden = BookId("garden")
        model.onAction(DemoAction.TogglePin(garden))
        model.onAction(DemoAction.MovePin(garden, Direction.PREVIOUS))
        model.onAction(DemoAction.MovePin(garden, Direction.PREVIOUS))
        assertEquals(listOf(garden, BookId("atlas")), model.state.value.reading.pins)
        assertEquals(model.state.value, ReadingDemoViewModel(saved).state.value)
        model.onAction(DemoAction.TogglePin(garden))
        model.onAction(DemoAction.TogglePin(BookId("later")))
        assertEquals(listOf(BookId("atlas")), model.state.value.reading.pins)
        assertEquals(listOf("atlas", "garden", "coast"), model.state.value.reading.books.map { it.id.value })
    }

    @Test fun periodStepsStopAtEdgesAndGlobalValuesAreStable() {
        val saved = SavedStateHandle()
        val model = ReadingDemoViewModel(saved)
        val initial = model.state.value.statistics
        val pages = mutableListOf<Int?>()
        model.onAction(DemoAction.StepPeriod(Direction.PREVIOUS))
        model.onAction(DemoAction.StepPeriod(Direction.PREVIOUS))
        for (period in Period.entries) {
            val now = model.state.value.statistics
            assertEquals(period, now.period)
            assertEquals(initial.xp, now.xp)
            assertEquals(initial.streak, now.streak)
            pages += now.snapshot.pages.value
            model.onAction(DemoAction.StepPeriod(Direction.NEXT))
        }
        assertEquals(listOf(21, 89, 320, 980, 1595, 1795), pages)
        assertEquals(Period.ALL, ReadingDemoViewModel(saved).state.value.statistics.period)
    }

    @Test fun missingHistoryDiffersFromConfirmedZeroAndBookmarkIsIndependent() {
        val model = ReadingDemoViewModel(SavedStateHandle())
        val atlas = model.state.value.reading.books.first()
        assertEquals(120, atlas.bookmark)
        assertEquals(200, atlas.reachedPage)
        assertEquals(168, atlas.confirmedPages)
        model.onAction(DemoAction.SelectScenario(DemoScenario.EMPTY))
        assertTrue(model.state.value.reading.books.isEmpty())
        assertEquals(0, model.state.value.statistics.snapshot.pages.value)
        model.onAction(DemoAction.SelectScenario(DemoScenario.HISTORICAL))
        repeat(4) { model.onAction(DemoAction.StepPeriod(Direction.NEXT)) }
        val historical = model.state.value.statistics.snapshot
        assertNull(historical.pages.value)
        assertTrue(!historical.pages.explanation.isNullOrBlank())
        assertEquals(2, historical.completedCycles.value)
    }

    @Test fun coverInitialUsesTheFirstUnicodeLetterAndSafeFallback() {
        assertEquals("Ё", coverInitial(" \u00a0«„ёлка”»"))
        assertEquals("A", coverInitial(" 1984: A story"))
        assertEquals("É", coverInitial("équinoxe"))
        assertEquals(String(Character.toChars(0x10400)), coverInitial("«" + String(Character.toChars(0x10428))))
        assertEquals("—", coverInitial("«123 📚»"))
    }
}
