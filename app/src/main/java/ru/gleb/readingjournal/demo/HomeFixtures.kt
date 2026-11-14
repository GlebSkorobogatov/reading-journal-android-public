package ru.gleb.readingjournal.demo

import ru.gleb.readingjournal.ui.home.*
import java.time.LocalDate

/** Prepared synthetic display snapshots. No reading records or aggregation engine. */
object HomeFixtures {
    val books = listOf(
        HomeBookUi("atlas", "Атлас тихих открытий", "Анна Северная", 120, 200, 300, 200, 2),
        HomeBookUi("garden", "Сад между строк", "Михаил Лист", 72, 120, 240, 120, 3),
        HomeBookUi("coast", "Берег новых историй", "Елена Маяк", 0, 0, 180, 0, 1)
    )
    // 1795 pages + 5 valid completions ×100 + 75(streak)+75(pages)+75(cycles) =2520 XP.
    // Two historical completions add no pages, XP or achievement progress.
    private fun records(day: String, month: String, streak: String, book: String) = listOf(
        RecordUi("Лучший день по страницам", "15 марта 2026 · 95 стр.", day),
        RecordUi("Лучший месяц по страницам", "Сентябрь 2026 · 500 стр.", month),
        RecordUi("Самая длинная серия", "8 дней", streak),
        RecordUi("Книга с наибольшим количеством прочитанных страниц", "Атлас тихих открытий · 600 стр.", book)
    )
    private val snapshots = mapOf(
        HomePeriod.TODAY to PeriodSnapshotUi(QuantityUi(21), 0, mapOf("atlas" to 15, "garden" to 6, "coast" to 0),
            records("12 ноября · 21 стр.", "Ноябрь · 21 стр. в периоде", "1 день", "Атлас тихих открытий · 15 стр.")),
        HomePeriod.WEEK to PeriodSnapshotUi(QuantityUi(89), 1, mapOf("atlas" to 55, "garden" to 34, "coast" to 0),
            records("7 ноября · 28 стр.", "Ноябрь · 89 стр. в периоде", "3 дня", "Атлас тихих открытий · 55 стр.")),
        HomePeriod.MONTH to PeriodSnapshotUi(QuantityUi(320), 1, mapOf("atlas" to 200, "garden" to 120, "coast" to 0),
            records("14 октября · 63 стр.", "Октябрь · 162 стр. в части месяца", "8 дней", "Атлас тихих открытий · 200 стр.")),
        HomePeriod.QUARTER to PeriodSnapshotUi(QuantityUi(980), 4, mapOf("atlas" to 450, "garden" to 450, "coast" to 0),
            records("3 сентября · 90 стр.", "Сентябрь · 500 стр.", "8 дней", "Атлас тихих открытий · 450 стр.")),
        HomePeriod.YEAR to PeriodSnapshotUi(QuantityUi(1595), 5, mapOf("atlas" to 550, "garden" to 550, "coast" to 0),
            records("15 марта · 95 стр.", "Сентябрь · 500 стр.", "8 дней", "Атлас тихих открытий · 550 стр.")),
        HomePeriod.ALL to PeriodSnapshotUi(QuantityUi(1795), 7, mapOf("atlas" to 600, "garden" to 600, "coast" to 0),
            records("15 марта 2026 · 95 стр.", "Сентябрь 2026 · 500 стр.", "8 дней", "Атлас тихих открытий · 600 стр."))
    )

    fun create(scenario: HomeScenario = HomeScenario.MULTIPLE): HomeUiState {
        val emptyPages = scenario in listOf(HomeScenario.EMPTY, HomeScenario.NO_PAGES, HomeScenario.HISTORICAL)
        val selectedBooks = when (scenario) {
            HomeScenario.EMPTY, HomeScenario.HISTORICAL -> emptyList()
            HomeScenario.SINGLE -> books.take(1).map { it.copy(bookmark = 120, reachedPage = 120, confirmedPages = 12) }
            HomeScenario.NO_PAGES -> books.takeLast(1)
            HomeScenario.BOOKMARK -> books.map { if (it.id == "atlas") it.copy(bookmark = 40) else it }
            HomeScenario.LONG_TEXT -> books.map { if (it.id == "atlas") it.copy(
                title = "Долгое путешествие исследователя сквозь зимние библиотеки и незнакомые города у самого края северного моря",
                author = "Александра Константиновна Тихомирова-Северная и Михаил Александрович Листопадов") else it }
            else -> books
        }
        val states = snapshots.mapValues { (period, snapshot) -> when {
            emptyPages -> snapshot.copy(pages = if (scenario == HomeScenario.HISTORICAL && period == HomePeriod.ALL)
                QuantityUi(null, "У исторических прочтений нет записанных страниц; завершение не доказывает диапазоны") else QuantityUi(0),
                completions = if (scenario == HomeScenario.HISTORICAL) when (period) {
                    HomePeriod.ALL -> 2; HomePeriod.YEAR, HomePeriod.QUARTER -> 1; else -> 0
                } else 0,
                comparisonPages = selectedBooks.associate { it.id to 0 },
                records = snapshot.records.map { it.copy(allTime = "—", selected = "—") })
            scenario == HomeScenario.SINGLE -> snapshot.copy(pages = QuantityUi(12), completions = 0,
                comparisonPages = mapOf("atlas" to 12), records = listOf(
                    RecordUi("Лучший день по страницам", "12 ноября · 12 стр.", "12 ноября · 12 стр."),
                    RecordUi("Лучший месяц по страницам", "Ноябрь · 12 стр.", "Ноябрь · 12 стр."),
                    RecordUi("Самая длинная серия", "1 день", "1 день"),
                    RecordUi("Книга с наибольшим количеством прочитанных страниц", "Атлас тихих открытий · 12 стр.", "Атлас тихих открытий · 12 стр.")))
            else -> snapshot
        } }
        return HomeUiState(scenario, selectedBooks, when {
            scenario == HomeScenario.PINNED -> listOf("garden", "atlas")
            selectedBooks.size > 1 -> listOf("atlas")
            else -> emptyList()
        }, states, when { emptyPages -> XpUi(1, "Искатель", 0, 0, 200)
            scenario == HomeScenario.SINGLE -> XpUi(1, "Искатель", 12, 12, 200)
            else -> XpUi(8, "Ученик", 2520, 70, 550) },
            streak = when { emptyPages -> 0; scenario == HomeScenario.SINGLE -> 1; else -> 3 },
            period = if (scenario == HomeScenario.HISTORICAL) HomePeriod.ALL else HomePeriod.WEEK)
    }

    private fun bars(vararg values: Pair<String, Int>) = values.map { (label, pages) -> ChartBarUi(label, pages) }
    fun chart(state: HomeUiState): ChartWindowUi {
        if (state.scenario in listOf(HomeScenario.EMPTY, HomeScenario.NO_PAGES, HomeScenario.HISTORICAL))
            return ChartWindowUi("Нет записей страниц", emptyList())
        if (state.scenario == HomeScenario.SINGLE) return ChartWindowUi("12 ноября 2026", bars("12 нояб." to 12))
        if (state.period == HomePeriod.TODAY) return ChartWindowUi("12 ноября 2026", bars("12 нояб." to 21))
        val previousAllowed = when (state.grouping) {
            ChartGrouping.DAY, ChartGrouping.WEEK -> state.period != HomePeriod.WEEK
            ChartGrouping.MONTH -> state.period in listOf(HomePeriod.YEAR, HomePeriod.ALL)
        }
        val previous = state.interval < 0 && previousAllowed
        return when (state.grouping) {
            ChartGrouping.DAY -> if (previous) ChartWindowUi("30 октября–5 ноября · окно внутри периода",
                bars("30 окт." to 0, "31 окт." to 0, "1 нояб." to 1, "2 нояб." to 9, "3 нояб." to 10, "4 нояб." to 24, "5 нояб." to 25), true)
                else ChartWindowUi("6–12 ноября 2026 · окно внутри периода",
                    bars("6 нояб." to 5, "7 нояб." to 28, "8 нояб." to 8, "9 нояб." to 0, "10 нояб." to 12, "11 нояб." to 15, "12 нояб." to 21), previousAllowed)
            ChartGrouping.WEEK -> when {
                state.period == HomePeriod.WEEK -> ChartWindowUi("6–12 ноября · части календарных недель",
                    bars("6–8 нояб." to 41, "9–12 нояб." to 48))
                previous -> ChartWindowUi("14 октября–1 ноября · окно внутри периода",
                    bars("14–18 окт." to 63, "19–25 окт." to 99, "26 окт.–1 нояб." to 1), true)
                else -> ChartWindowUi("2–12 ноября · окно внутри периода",
                    bars("2–8 нояб." to 109, "9–12 нояб." to 48), previousAllowed)
            }
            ChartGrouping.MONTH -> when (state.period) {
                HomePeriod.WEEK -> ChartWindowUi("6–12 ноября · часть месяца", bars("Ноябрь" to 89))
                HomePeriod.MONTH -> ChartWindowUi("14 октября–12 ноября · части месяцев", bars("14–31 окт." to 162, "1–12 нояб." to 158))
                HomePeriod.QUARTER -> ChartWindowUi("15 августа–12 ноября", bars("15–31 авг." to 60, "Сентябрь" to 500, "Октябрь" to 262, "1–12 нояб." to 158))
                HomePeriod.YEAR -> if (previous) ChartWindowUi("Январь–март 2026", bars("Январь" to 150, "Февраль" to 200, "Март" to 265), true)
                    else ChartWindowUi("Август–ноябрь 2026 · окно внутри года", bars("Август" to 60, "Сентябрь" to 500, "Октябрь" to 262, "Ноябрь" to 158), true)
                HomePeriod.ALL -> if (previous) ChartWindowUi("2025 · подготовленное окно", bars("Декабрь" to 200), true)
                    else ChartWindowUi("2026 · подготовленное окно", bars("Январь" to 150, "Февраль" to 200, "Март" to 265, "Август" to 60, "Сентябрь" to 500, "Октябрь" to 262, "Ноябрь" to 158), true)
                HomePeriod.TODAY -> error("Today uses the single prepared day above")
            }
        }
    }

    // Fully specified daily and per-book numbers, not generated from user events.
    private val calendar: Map<LocalDate, HeatDayUi> = listOf(
        LocalDate.parse("2025-12-15") to HeatDayUi(20, listOf(DayBookUi("atlas", "Атлас тихих открытий", 10), DayBookUi("letters", "Письма с острова", 10))),
        LocalDate.parse("2025-12-16") to HeatDayUi(30, listOf(DayBookUi("atlas", "Атлас тихих открытий", 10), DayBookUi("garden", "Сад между строк", 10), DayBookUi("letters", "Письма с острова", 10))),
        LocalDate.parse("2025-12-17") to HeatDayUi(50, listOf(DayBookUi("atlas", "Атлас тихих открытий", 10), DayBookUi("garden", "Сад между строк", 10), DayBookUi("letters", "Письма с острова", 30))),
        LocalDate.parse("2025-12-18") to HeatDayUi(60, listOf(DayBookUi("atlas", "Атлас тихих открытий", 10), DayBookUi("garden", "Сад между строк", 20), DayBookUi("letters", "Письма с острова", 30))),
        LocalDate.parse("2025-12-19") to HeatDayUi(40, listOf(DayBookUi("atlas", "Атлас тихих открытий", 10), DayBookUi("garden", "Сад между строк", 10), DayBookUi("letters", "Письма с острова", 20))),
        LocalDate.parse("2026-01-01") to HeatDayUi(20, listOf(DayBookUi("atlas", "Атлас тихих открытий", 5), DayBookUi("garden", "Сад между строк", 5), DayBookUi("letters", "Письма с острова", 10))),
        LocalDate.parse("2026-01-02") to HeatDayUi(20, listOf(DayBookUi("atlas", "Атлас тихих открытий", 5), DayBookUi("garden", "Сад между строк", 5), DayBookUi("letters", "Письма с острова", 10))),
        LocalDate.parse("2026-01-03") to HeatDayUi(20, listOf(DayBookUi("atlas", "Атлас тихих открытий", 5), DayBookUi("garden", "Сад между строк", 5), DayBookUi("letters", "Письма с острова", 10))),
        LocalDate.parse("2026-01-04") to HeatDayUi(20, listOf(DayBookUi("atlas", "Атлас тихих открытий", 5), DayBookUi("garden", "Сад между строк", 5), DayBookUi("letters", "Письма с острова", 10))),
        LocalDate.parse("2026-01-05") to HeatDayUi(20, listOf(DayBookUi("atlas", "Атлас тихих открытий", 5), DayBookUi("garden", "Сад между строк", 10), DayBookUi("letters", "Письма с острова", 5))),
        LocalDate.parse("2026-01-06") to HeatDayUi(20, listOf(DayBookUi("atlas", "Атлас тихих открытий", 10), DayBookUi("garden", "Сад между строк", 10))),
        LocalDate.parse("2026-01-07") to HeatDayUi(30, listOf(DayBookUi("atlas", "Атлас тихих открытий", 15), DayBookUi("garden", "Сад между строк", 10), DayBookUi("letters", "Письма с острова", 5))),
        LocalDate.parse("2026-02-01") to HeatDayUi(50, listOf(DayBookUi("atlas", "Атлас тихих открытий", 5), DayBookUi("letters", "Письма с острова", 45))),
        LocalDate.parse("2026-02-10") to HeatDayUi(60, listOf(DayBookUi("atlas", "Атлас тихих открытий", 5), DayBookUi("garden", "Сад между строк", 5), DayBookUi("letters", "Письма с острова", 50))),
        LocalDate.parse("2026-02-20") to HeatDayUi(90, listOf(DayBookUi("garden", "Сад между строк", 5), DayBookUi("letters", "Письма с острова", 85))),
        LocalDate.parse("2026-03-01") to HeatDayUi(80, listOf(DayBookUi("atlas", "Атлас тихих открытий", 10), DayBookUi("garden", "Сад между строк", 10), DayBookUi("letters", "Письма с острова", 60))),
        LocalDate.parse("2026-03-15") to HeatDayUi(95, listOf(DayBookUi("atlas", "Атлас тихих открытий", 15), DayBookUi("garden", "Сад между строк", 15), DayBookUi("letters", "Письма с острова", 65))),
        LocalDate.parse("2026-03-30") to HeatDayUi(90, listOf(DayBookUi("atlas", "Атлас тихих открытий", 15), DayBookUi("garden", "Сад между строк", 15), DayBookUi("letters", "Письма с острова", 60))),
        LocalDate.parse("2026-08-15") to HeatDayUi(10, listOf(DayBookUi("atlas", "Атлас тихих открытий", 5), DayBookUi("garden", "Сад между строк", 5))),
        LocalDate.parse("2026-08-16") to HeatDayUi(50, listOf(DayBookUi("atlas", "Атлас тихих открытий", 15), DayBookUi("garden", "Сад между строк", 35))),
        LocalDate.parse("2026-09-01") to HeatDayUi(70, listOf(DayBookUi("atlas", "Атлас тихих открытий", 30), DayBookUi("garden", "Сад между строк", 30), DayBookUi("letters", "Письма с острова", 10))),
        LocalDate.parse("2026-09-02") to HeatDayUi(80, listOf(DayBookUi("atlas", "Атлас тихих открытий", 30), DayBookUi("garden", "Сад между строк", 40), DayBookUi("letters", "Письма с острова", 10))),
        LocalDate.parse("2026-09-03") to HeatDayUi(90, listOf(DayBookUi("atlas", "Атлас тихих открытий", 30), DayBookUi("garden", "Сад между строк", 50), DayBookUi("letters", "Письма с острова", 10))),
        LocalDate.parse("2026-09-04") to HeatDayUi(70, listOf(DayBookUi("atlas", "Атлас тихих открытий", 30), DayBookUi("garden", "Сад между строк", 30), DayBookUi("letters", "Письма с острова", 10))),
        LocalDate.parse("2026-09-05") to HeatDayUi(80, listOf(DayBookUi("atlas", "Атлас тихих открытий", 30), DayBookUi("garden", "Сад между строк", 40), DayBookUi("letters", "Письма с острова", 10))),
        LocalDate.parse("2026-09-06") to HeatDayUi(60, listOf(DayBookUi("atlas", "Атлас тихих открытий", 30), DayBookUi("garden", "Сад между строк", 20), DayBookUi("letters", "Письма с острова", 10))),
        LocalDate.parse("2026-09-07") to HeatDayUi(50, listOf(DayBookUi("atlas", "Атлас тихих открытий", 20), DayBookUi("garden", "Сад между строк", 10), DayBookUi("letters", "Письма с острова", 20))),
        LocalDate.parse("2026-10-01") to HeatDayUi(40, listOf(DayBookUi("atlas", "Атлас тихих открытий", 10), DayBookUi("garden", "Сад между строк", 30))),
        LocalDate.parse("2026-10-02") to HeatDayUi(60, listOf(DayBookUi("atlas", "Атлас тихих открытий", 20), DayBookUi("garden", "Сад между строк", 40))),
        LocalDate.parse("2026-10-14") to HeatDayUi(63, listOf(DayBookUi("atlas", "Атлас тихих открытий", 40), DayBookUi("garden", "Сад между строк", 23))),
        LocalDate.parse("2026-10-20") to HeatDayUi(50, listOf(DayBookUi("atlas", "Атлас тихих открытий", 30), DayBookUi("garden", "Сад между строк", 20))),
        LocalDate.parse("2026-10-22") to HeatDayUi(49, listOf(DayBookUi("atlas", "Атлас тихих открытий", 33), DayBookUi("garden", "Сад между строк", 16))),
        LocalDate.parse("2026-11-01") to HeatDayUi(1, listOf(DayBookUi("atlas", "Атлас тихих открытий", 1))),
        LocalDate.parse("2026-11-02") to HeatDayUi(9, listOf(DayBookUi("atlas", "Атлас тихих открытий", 5), DayBookUi("garden", "Сад между строк", 4))),
        LocalDate.parse("2026-11-03") to HeatDayUi(10, listOf(DayBookUi("atlas", "Атлас тихих открытий", 6), DayBookUi("garden", "Сад между строк", 4))),
        LocalDate.parse("2026-11-04") to HeatDayUi(24, listOf(DayBookUi("atlas", "Атлас тихих открытий", 15), DayBookUi("garden", "Сад между строк", 9))),
        LocalDate.parse("2026-11-05") to HeatDayUi(25, listOf(DayBookUi("atlas", "Атлас тихих открытий", 15), DayBookUi("garden", "Сад между строк", 10))),
        LocalDate.parse("2026-11-06") to HeatDayUi(5, listOf(DayBookUi("atlas", "Атлас тихих открытий", 3), DayBookUi("garden", "Сад между строк", 2))),
        LocalDate.parse("2026-11-07") to HeatDayUi(28, listOf(DayBookUi("atlas", "Атлас тихих открытий", 17), DayBookUi("garden", "Сад между строк", 11))),
        LocalDate.parse("2026-11-08") to HeatDayUi(8, listOf(DayBookUi("atlas", "Атлас тихих открытий", 5), DayBookUi("garden", "Сад между строк", 3))),
        LocalDate.parse("2026-11-09") to HeatDayUi(0, listOf()),
        LocalDate.parse("2026-11-10") to HeatDayUi(12, listOf(DayBookUi("atlas", "Атлас тихих открытий", 7), DayBookUi("garden", "Сад между строк", 5))),
        LocalDate.parse("2026-11-11") to HeatDayUi(15, listOf(DayBookUi("atlas", "Атлас тихих открытий", 8), DayBookUi("garden", "Сад между строк", 7))),
        LocalDate.parse("2026-11-12") to HeatDayUi(21, listOf(DayBookUi("atlas", "Атлас тихих открытий", 15), DayBookUi("garden", "Сад между строк", 6))),
    ).toMap()

    fun year(state: HomeUiState): Map<LocalDate, HeatDayUi> = when (state.scenario) {
        HomeScenario.EMPTY, HomeScenario.NO_PAGES, HomeScenario.HISTORICAL -> emptyMap()
        HomeScenario.SINGLE -> if (state.calendarYear == 2026) mapOf(LocalDate.of(2026, 11, 12) to
            HeatDayUi(12, listOf(DayBookUi("atlas", "Атлас тихих открытий", 12)))) else emptyMap()
        else -> calendar.filterKeys { it.year == state.calendarYear }
    }

    fun completionSummary(state: HomeUiState): List<String> = when (state.scenario) {
        HomeScenario.EMPTY, HomeScenario.NO_PAGES, HomeScenario.SINGLE -> emptyList()
        HomeScenario.HISTORICAL -> listOf("Северный свет · 2024 · страницы —", "Дом бумажных окон · сентябрь 2026 · страницы —")
        else -> listOf("Атлас тихих открытий · 2 завершённых цикла", "Сад между строк · 2 завершённых цикла",
            "Письма с острова · 595 подтверждённых страниц · завершено", "Северный свет · 2024 · страницы —",
            "Дом бумажных окон · сентябрь 2026 · страницы —")
    }
}
