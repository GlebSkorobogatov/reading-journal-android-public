package ru.gleb.readingjournal.ui.overview

import androidx.annotation.DrawableRes
import ru.gleb.readingjournal.ui.designsystem.ReadingStatus

@JvmInline value class BookId(val value: String)
@JvmInline value class CycleId(val value: String)

data class ReadingBook(
    val id: BookId,
    val cycleId: CycleId,
    val title: String,
    val author: String,
    val bookmark: Int,
    val reachedPage: Int,
    val totalPages: Int,
    val confirmedPages: Int,
    val activityOrder: Int,
    @param:DrawableRes val cover: Int? = null,
    val status: ReadingStatus = ReadingStatus.READING,
)

data class CurrentlyReadingUi(val books: List<ReadingBook>, val pins: List<BookId>)
data class QuantityUi(val value: Int?, val explanation: String? = null) {
    init { require(value != null || !explanation.isNullOrBlank()) }
}

enum class Period(val label: String) {
    TODAY("Сегодня"), WEEK("7 дней"), MONTH("30 дней"), QUARTER("90 дней"), YEAR("Год"), ALL("Всё время");

    fun step(direction: Direction): Period? = entries.getOrNull(ordinal + direction.offset)
}

enum class Direction(val offset: Int, val label: String) {
    PREVIOUS(-1, "Предыдущий период"), NEXT(1, "Следующий период")
}

enum class StatisticsDestination(val route: String, val title: String) {
    ACHIEVEMENTS("Achievements", "Достижения"),
    DYNAMICS("ReadingDynamics", "Динамика чтения"),
    CALENDAR("ActivityCalendar", "Календарь активности"),
    RECORDS("ReadingRecords", "Рекорды")
}

data class XpUi(val level: Int, val title: String, val total: Int, val inLevel: Int, val nextCost: Int)
data class PeriodSnapshot(val pages: QuantityUi, val completedCycles: QuantityUi)
data class StatisticsUi(val xp: XpUi, val streak: Int, val period: Period, val snapshot: PeriodSnapshot)
