package ru.gleb.readingjournal.demo

import ru.gleb.readingjournal.R
import ru.gleb.readingjournal.ui.designsystem.ReadingStatus
import ru.gleb.readingjournal.ui.overview.*

enum class DemoScenario(val title: String) {
    MULTIPLE("Несколько книг"),
    EMPTY("Нет активных книг"),
    SINGLE("Одна книга"),
    MANY("Много книг"),
    LONG_TEXT("Длинные названия и крупные числа"),
    HISTORICAL("История без числа страниц")
}

internal data class ReadingDemoFixture(
    val books: List<ReadingBook>,
    val pins: List<BookId>,
    val xp: XpUi,
    val streak: Int,
    val snapshots: Map<Period, PeriodSnapshot>
)

internal object ReadingDemoFixtures {
    private val atlas = ReadingBook(BookId("atlas"), CycleId("atlas-2"), "Атлас тихих открытий",
        "Анна Северная", 120, 200, 300, 168, 2, R.drawable.demo_cover_atlas)
    private val garden = ReadingBook(BookId("garden"), CycleId("garden-1"), "Сад между строк",
        "Михаил Лист", 72, 120, 240, 120, 3, R.drawable.demo_cover_garden)
    private val coast = ReadingBook(BookId("coast"), CycleId("coast-1"), "Берег новых историй",
        "Елена Маяк", 18, 18, 180, 0, 1)
    private val later = coast.copy(id = BookId("later"), cycleId = CycleId("later-1"),
        title = "Книга на потом", status = ReadingStatus.WANTED, activityOrder = 100)
    private val standard = listOf(atlas, garden, coast, later)
    private val everyday = byPeriod(21 to 0, 89 to 1, 320 to 1, 980 to 4, 1595 to 5, 1795 to 7)
    private val zero = Period.entries.associateWith { snapshot(0, 0) }
    private val starter = XpUi(1, "Искатель", 0, 0, 200)

    /** Presentation snapshots for a fixed example, with no history, XP engine or clock. */
    fun create(scenario: DemoScenario): ReadingDemoFixture = when (scenario) {
        DemoScenario.EMPTY -> ReadingDemoFixture(listOf(later), emptyList(), starter, 0, zero)
        DemoScenario.SINGLE -> ReadingDemoFixture(listOf(coast), emptyList(), starter, 0, zero)
        DemoScenario.HISTORICAL -> ReadingDemoFixture(
            emptyList(), emptyList(), starter, 0, zero + (Period.ALL to PeriodSnapshot(
                QuantityUi(null, "У завершённых циклов нет сведений о страницах"), QuantityUi(2)))
        )
        DemoScenario.MULTIPLE -> ReadingDemoFixture(standard, listOf(atlas.id), XpUi(8, "Ученик", 2520, 70, 550), 3, everyday)
        DemoScenario.LONG_TEXT -> ReadingDemoFixture(
            listOf(
                atlas.copy(
                    title = "Долгое путешествие исследователя сквозь зимние библиотеки и незнакомые города края северного моря",
                    author = "Александра Константиновна Тихомирова-Северная и Михаил Александрович Листопадов"
                ),
                garden, coast
            ),
            listOf(garden.id, atlas.id),
            XpUi(70, "Мудрец V", 133_260, 2_160, 3_650), 248,
            byPeriod(21 to 0, 280 to 3, 1200 to 10, 6500 to 24, 35_100 to 92, 128_540 to 142)
        )
        DemoScenario.MANY -> ReadingDemoFixture(
            standard + (1..12).map { number ->
                coast.copy(id = BookId("extra-$number"), cycleId = CycleId("extra-cycle-$number"),
                    title = if (number == 1) atlas.title else "Письма с севера · $number",
                    author = "Лидия Ветер", activityOrder = -number)
            },
            listOf(garden.id, atlas.id), XpUi(8, "Ученик", 2520, 70, 550), 3, everyday
        )
    }

    private fun snapshot(pages: Int, cycles: Int) = PeriodSnapshot(QuantityUi(pages), QuantityUi(cycles))

    private fun byPeriod(vararg counts: Pair<Int, Int>) =
        Period.entries.zip(counts.map { (pages, cycles) -> snapshot(pages, cycles) }).toMap()
}
