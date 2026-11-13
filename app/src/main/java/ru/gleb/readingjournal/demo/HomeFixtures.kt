package ru.gleb.readingjournal.demo

import ru.gleb.readingjournal.ui.home.*
import java.time.LocalDate

/** Fixed synthetic examples, anchored at 12 November 2026. Never written as user records. */
object HomeFixtures {
    val books = listOf(
        HomeBookUi("atlas", "Атлас тихих открытий", "Анна Северная", 120, 300, .4f, 2),
        HomeBookUi("garden", "Сад между строк", "Михаил Лист", 72, 240, .3f, 3),
        HomeBookUi("coast", "Берег новых историй", "Елена Маяк", 0, 180, 0f, 1)
    )
    private val snapshots = mapOf(
        HomePeriod.TODAY to PeriodSnapshotUi("30 мин", QuantityUi(21), listOf(25, 5, 0),
            listOf(30), listOf(21), "30 стр/ч", "≈ 6 ч", "30 мин", "21 стр", "25 мин", "30 мин"),
        HomePeriod.WEEK to PeriodSnapshotUi("2 ч 30 мин", QuantityUi(89), listOf(110, 40, 0),
            listOf(10, 56, 0, 4, 20, 30, 30), listOf(5, 28, 8, 2, 10, 15, 21),
            "30 стр/ч", "≈ 6 ч", "56 мин", "28 стр", "40 мин", "2 ч 30 мин"),
        HomePeriod.MONTH to PeriodSnapshotUi("8 ч", QuantityUi(254), listOf(300, 180, 0),
            listOf(10, 56, 0, 4, 20, 30, 30), listOf(5, 28, 8, 2, 10, 15, 21),
            "30 стр/ч", "≈ 6 ч", "1 ч 35 мин", "52 стр", "1 ч 35 мин", "4 ч 29 мин"),
        HomePeriod.QUARTER to PeriodSnapshotUi("16 ч", QuantityUi(494), listOf(600, 360, 0),
            listOf(10, 56, 0, 4, 20, 30, 30), listOf(5, 28, 8, 2, 10, 15, 21),
            "30 стр/ч", "≈ 6 ч", "1 ч 35 мин", "52 стр", "1 ч 35 мин", "5 ч 30 мин"),
        HomePeriod.YEAR to PeriodSnapshotUi("25 ч", QuantityUi(764), listOf(900, 500, 0),
            listOf(10, 56, 0, 4, 20, 30, 30), listOf(5, 28, 8, 2, 10, 15, 21),
            "30 стр/ч", "≈ 6 ч", "2 ч 15 мин", "52 стр", "1 ч 35 мин", "8 ч"),
        HomePeriod.ALL to PeriodSnapshotUi("30 ч", QuantityUi(914), listOf(900, 600, 0),
            listOf(10, 56, 0, 4, 20, 30, 30), listOf(5, 28, 8, 2, 10, 15, 21),
            "30 стр/ч", "≈ 6 ч", "2 ч 15 мин", "52 стр", "1 ч 35 мин", "8 ч")
    )

    fun create(scenario: HomeScenario = HomeScenario.MULTIPLE): HomeUiState {
        val selectedBooks = when (scenario) {
            HomeScenario.EMPTY -> emptyList()
            HomeScenario.SINGLE -> books.take(1)
            HomeScenario.PENDING -> books.mapIndexed { i, b -> if (i == 0) b.copy(pages = PageState.PENDING) else b }
            HomeScenario.UNKNOWN -> books.map { it.copy(pages = PageState.UNKNOWN) }
            HomeScenario.LONG_TEXT -> books.mapIndexed { i, b -> if (i == 0) b.copy(
                title = "Долгое путешествие исследователя сквозь зимние библиотеки и незнакомые города у самого края северного моря",
                author = "Александра Константиновна Тихомирова-Северная и Михаил Александрович Листопадов") else b }
            else -> books
        }
        val states = snapshots.mapValues { (period, s) -> when (scenario) {
            HomeScenario.EMPTY -> s.copy(time = "0 мин", pages = QuantityUi(0), comparisonMinutes = emptyList(),
                dayMinutes = listOf(0), dayPages = listOf(0), speed = "—", forecast = "—",
                bestDayTime = "—", bestDayPages = "—", longestSession = "—", bestMonth = "—")
            HomeScenario.UNKNOWN -> s.copy(pages = QuantityUi(null, explanation = "Не помню страницы: объём не записан"),
                speed = "—", forecast = "—", bestDayPages = "—")
            HomeScenario.INCOMPLETE, HomeScenario.PENDING -> s.copy(pages = QuantityUi(when (period) {
                HomePeriod.TODAY -> 20; HomePeriod.WEEK -> 40; HomePeriod.MONTH -> 80
                HomePeriod.QUARTER -> 110; HomePeriod.YEAR -> 160; HomePeriod.ALL -> 175
            }, true, "Есть сеансы без страниц и записи без точного дня"), speed = "—", forecast = "—", bestDayPages = "20 стр")
            HomeScenario.SINGLE -> s.copy(time = "25 мин", pages = QuantityUi(12), comparisonMinutes = listOf(25),
                dayMinutes = listOf(25), dayPages = listOf(12), speed = "28,8 стр/ч", forecast = "≈ 6,3 ч",
                bestDayTime = "25 мин", bestDayPages = "12 стр", longestSession = "25 мин", bestMonth = "25 мин")
            else -> s
        } }
        return HomeUiState(scenario, selectedBooks, if (selectedBooks.size > 1) listOf("atlas") else emptyList(),
            states, if (scenario == HomeScenario.EMPTY) XpUi(1, "Искатель", 0, 0, 200)
            else if (scenario == HomeScenario.SINGLE) XpUi(1, "Искатель", 25, 25, 200)
            else XpUi(8, "Ученик", 2520, 70, 550),
            period = if (scenario == HomeScenario.INCOMPLETE || scenario == HomeScenario.UNKNOWN) HomePeriod.ALL else HomePeriod.WEEK,
            timer = when (scenario) {
                HomeScenario.RUNNING -> DemoTimerUi("atlas", DemoTimerPhase.RUNNING)
                HomeScenario.PAUSED -> DemoTimerUi("atlas", DemoTimerPhase.PAUSED)
                else -> null
            })
    }

    fun chart(state: HomeUiState): ChartWindowUi {
        if (state.scenario == HomeScenario.EMPTY) return ChartWindowUi("Нет записей", emptyList())
        val pages = state.measure == ChartMeasure.PAGES
        val incomplete = state.scenario in listOf(HomeScenario.INCOMPLETE, HomeScenario.PENDING)
        val unknown = state.scenario == HomeScenario.UNKNOWN
        if (state.period == HomePeriod.TODAY || state.scenario == HomeScenario.SINGLE) return ChartWindowUi(
            "12 ноября 2026", listOf(ChartBarUi("12 нояб.", if (pages) state.snapshot.pages else QuantityUi(if (state.scenario == HomeScenario.SINGLE) 25 else 30))))
        val bars = when (state.grouping) {
            ChartGrouping.DAY -> {
                val values = if (pages && incomplete) listOf(0, 0, 8, 0, 12, 0, 20)
                    else if (pages) state.snapshot.dayPages else state.snapshot.dayMinutes
                values.mapIndexed { i, amount -> ChartBarUi(
                    if (values.size == 1) "12 нояб." else "${i + 6} нояб.",
                    when { pages && unknown -> QuantityUi(null, explanation = "Страницы неизвестны")
                        pages && incomplete && i == 4 -> QuantityUi(12, true, "1 сеанс без страниц")
                        pages && incomplete && i == 5 -> QuantityUi(null, explanation = "Страницы неизвестны")
                        else -> QuantityUi(amount) }, detail = "Синтетические записи «Атласа тихих открытий» и «Сада между строк»") }
            }
            ChartGrouping.WEEK -> listOf(
                ChartBarUi("2–8 нояб.", if (pages && unknown) QuantityUi(null) else QuantityUi(if (pages) { if (incomplete) 8 else 41 } else 66)),
                ChartBarUi("9–15 нояб.", if (pages && unknown) QuantityUi(null) else QuantityUi(if (pages) { if (incomplete) 32 else 48 } else 84, pages && incomplete)))
            ChartGrouping.MONTH -> if (state.period == HomePeriod.WEEK) listOf(
                ChartBarUi("Ноябрь", if (pages) state.snapshot.pages else QuantityUi(150))) else listOf(
                ChartBarUi("Сентябрь", if (pages && unknown) QuantityUi(null) else QuantityUi(if (pages) { if (incomplete) 30 else 120 } else 240)),
                ChartBarUi("Октябрь", if (pages && unknown) QuantityUi(null) else QuantityUi(if (pages) { if (incomplete) 40 else 165 } else 330)),
                ChartBarUi("Ноябрь", if (pages && unknown) QuantityUi(null) else QuantityUi(if (pages) { if (incomplete) 70 else 149 } else 269, pages && incomplete),
                    partialDate = pages && incomplete && state.period == HomePeriod.ALL,
                    detail = "Ноябрь 2026", exactDayPages = 40, monthOnlyPages = 30)).let { values ->
                        if (state.period == HomePeriod.MONTH) listOf(
                            ChartBarUi("Октябрь · с 14-го", QuantityUi(if (pages) { if (incomplete) 40 else 105 } else 211)),
                            ChartBarUi("Ноябрь · до 12-го", if (pages) QuantityUi(if (incomplete) 40 else 149, incomplete) else QuantityUi(269)))
                    else values
                    }
        }
        // Only prepared windows are navigable; no generated reading events or date allocation.
        val previous = when (state.grouping) {
            ChartGrouping.DAY -> {
                val labels = listOf("30 окт.", "31 окт.", "1 нояб.", "2 нояб.", "3 нояб.", "4 нояб.", "5 нояб.")
                val amounts = if (pages) listOf(0, 0, 1, 7, 8, 30, 14) else listOf(0, 0, 1, 14, 15, 60, 29)
                labels.mapIndexed { i, label -> ChartBarUi(label, QuantityUi(amounts[i])) }
            }
            ChartGrouping.WEEK -> listOf(ChartBarUi("19–25 окт.", QuantityUi(0)), ChartBarUi("26 окт.–1 нояб.", QuantityUi(1)))
            ChartGrouping.MONTH -> listOf(ChartBarUi("Декабрь 2025", QuantityUi(if (pages) 150 else 300)))
        }.map { bar -> if (pages && (unknown || incomplete && bar.amount.value != 0)) bar.copy(amount = QuantityUi(null)) else bar }
        val visible = if (state.interval < 0) previous else bars
        val undated = if (pages && incomplete) when (state.grouping) {
            ChartGrouping.DAY, ChartGrouping.WEEK -> listOf("Ноябрь 2026 · 30 страниц · день неизвестен")
            ChartGrouping.MONTH -> listOf("2026 · 20 страниц · месяц неизвестен", "Дата неизвестна · 15 страниц · только за всё время")
        } else emptyList()
        return ChartWindowUi(when (state.grouping) {
            ChartGrouping.DAY -> if (state.interval < 0) "30 октября–5 ноября 2026" else "6–12 ноября 2026"
            ChartGrouping.WEEK -> if (state.interval < 0) "19 октября–1 ноября 2026" else "2–15 ноября 2026"
            ChartGrouping.MONTH -> if (state.interval < 0) "2025 · месяцы" else "2026 · месяцы (окно внутри выбранного периода)"
        } + if (state.interval < 0) " · предыдущее окно" else " · текущее окно", visible, undated)
    }

    fun heatDay(year: Int, month: Int, day: Int, empty: Boolean): HeatDayUi {
        if (empty) return HeatDayUi(0)
        if (year == 2025 && month == 12 && day in 15..19) return HeatDayUi(60)
        if (year != 2026) return HeatDayUi(0)
        if (month == 1) return HeatDayUi(when (day) { 1 -> 135; 2, 3, 4 -> 95; 5 -> 60; else -> 0 })
        if (month == 2 && day == 1) return HeatDayUi(60)
        if (month == 8) return HeatDayUi(when (day) { 15 -> 60; 16 -> 61; else -> 0 })
        if (month == 9 && day in 1..12) return HeatDayUi(20)
        if (month == 10) return HeatDayUi(when (day) { 1, 15 -> 60; 2 -> 59; 14 -> 95; 16 -> 56; else -> 0 })
        if (month != 11) return HeatDayUi(0)
        return when (day) {
            1 -> HeatDayUi(1); 2 -> HeatDayUi(14); 3 -> HeatDayUi(15); 4 -> HeatDayUi(60)
            5 -> HeatDayUi(29); 6 -> HeatDayUi(10); 7 -> HeatDayUi(56)
            8 -> HeatDayUi(0, 8); 9 -> HeatDayUi(4); 10 -> HeatDayUi(20)
            11 -> HeatDayUi(30); 12 -> HeatDayUi(30, 6); else -> HeatDayUi(0)
        }
    }

    fun year(state: HomeUiState): Map<LocalDate, HeatDayUi> {
        if (state.scenario == HomeScenario.EMPTY) return emptyMap()
        if (state.scenario == HomeScenario.SINGLE) return if (state.calendarYear == 2026)
            mapOf(LocalDate.of(2026, 11, 12) to HeatDayUi(25)) else emptyMap()
        val first = LocalDate.of(state.calendarYear, 1, 1)
        return (0 until first.lengthOfYear()).associate { offset ->
            val date = first.plusDays(offset.toLong())
            val data = heatDay(date.year, date.monthValue, date.dayOfMonth, false)
            date to if (state.scenario == HomeScenario.UNKNOWN) data.copy(pagesWithoutTimer = 0) else data
        }
    }
}
