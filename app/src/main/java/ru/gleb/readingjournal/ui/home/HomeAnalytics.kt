package ru.gleb.readingjournal.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.dp
import ru.gleb.readingjournal.ui.designsystem.*
import java.time.LocalDate
import java.time.DayOfWeek
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HomeDynamics(state: HomeUiState, window: ChartWindowUi, onEvent: (HomeEvent) -> Unit) {
    val c = LocalJournalColors.current
    HomeChoices(ChartMeasure.entries, state.measure, { it.label }, "chart-measure") { onEvent(HomeEvent.Measure(it)) }
    HomeChoices(ChartGrouping.entries, state.grouping, { it.label }, "chart-grouping") { onEvent(HomeEvent.Grouping(it)) }
    Text("${state.period.label} · ${window.label}", style = JournalType.secondary)
    FlowRow(horizontalArrangement = Arrangement.spacedBy(Space.sm), verticalArrangement = Arrangement.spacedBy(Space.sm)) {
        JournalButton("Назад по интервалам", { onEvent(HomeEvent.Interval(-1)) },
            Modifier.testTag("chart-previous"), enabled = state.interval == 0 && state.period != HomePeriod.TODAY, kind = ButtonKind.TEXT)
        JournalButton("Вперёд по интервалам", { onEvent(HomeEvent.Interval(1)) },
            Modifier.testTag("chart-next"), enabled = state.interval < 0, kind = ButtonKind.TEXT)
    }
    if (window.bars.isEmpty()) {
        Text("Пока нет записей для диаграммы", Modifier.testTag("chart-empty"), style = JournalType.secondary)
        return
    }
    val unit = if (state.measure == ChartMeasure.TIME) "мин" else "стр."
    Text(if (state.measure == ChartMeasure.TIME) "Минуты · записанное время" else "Страницы · подтверждённое количество", style = JournalType.caption)
    val maximum = window.bars.mapNotNull { it.amount.value }.maxOrNull()?.coerceAtLeast(1) ?: 1
    Text("${maximum} $unit", style = JournalType.caption, color = c.textSecondary)
    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).testTag("reading-chart"),
        horizontalArrangement = Arrangement.spacedBy(Space.md), verticalAlignment = Alignment.Bottom) {
        window.bars.forEachIndexed { index, bar ->
            val description = "${bar.label}: ${bar.amount.accessible} $unit" +
                if (bar.partialDate) ". Есть страницы без точного дня" else ""
            Column(Modifier.width(80.dp).heightIn(min = Metric.touch).testTag("chart-bar-$index")
                .clickable(role = Role.Button) {
                    val quantity = when { bar.amount.value == null -> "Страницы неизвестны"
                        bar.amount.minimum -> "Не менее ${bar.amount.value} страниц. Не заполнены страницы 1 сеанса."
                        else -> "${bar.amount.value} $unit" }
                    val split = if (bar.partialDate) "\nЕсть страницы без точного дня: ${bar.exactDayPages} с точными днями + ${bar.monthOnlyPages} с датой только до месяца." else ""
                    onEvent(HomeEvent.Explain(bar.label, "$quantity\n${bar.detail}$split",
                        if (bar.amount.minimum || bar.amount.value == null) "Уточнить страницы" else "Подробности чтения"))
                }.semantics(mergeDescendants = true) { contentDescription = description },
                horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(Space.sm)) {
                Text(bar.amount.text, style = JournalType.card)
                Box(Modifier.fillMaxWidth().height(128.dp), contentAlignment = Alignment.BottomCenter) {
                    if (bar.amount.value == null) {
                        Text("?", Modifier.align(Alignment.Center).testTag("unknown-bar-$index"), style = JournalType.number, color = c.textSecondary)
                    } else if (bar.amount.value > 0) Box(Modifier.width(36.dp)
                        .height((bar.amount.value.toFloat() / maximum * 120).dp)
                        .clip(JournalShape.control).background(c.accentSoft).border(Metric.border, c.accent, JournalShape.control))
                    Box(Modifier.fillMaxWidth().height(Metric.border).background(c.outlineStrong))
                }
                if (bar.amount.minimum) Text("≥ минимум", Modifier.testTag("quantity-marker-$index"), style = JournalType.caption, color = c.warning)
                if (bar.partialDate) Text("◇ Нет точного дня", Modifier.testTag("date-marker-$index"), style = JournalType.caption)
                Text(bar.label, style = JournalType.caption)
            }
        }
    }
    Text("0 · ${state.grouping.label.lowercase(Locale.forLanguageTag("ru"))}", style = JournalType.caption)
    if (window.undated.isNotEmpty()) {
        JournalButton("Без точной даты", { onEvent(HomeEvent.ToggleUndated) },
            Modifier.testTag("undated-toggle").semantics { stateDescription = if (state.undatedExpanded) "Развёрнуто" else "Свёрнуто" }, kind = ButtonKind.SECONDARY)
        if (state.undatedExpanded) {
            Text("Эти записи не распределены по показанным интервалам; их принадлежность этому окну не утверждается.", style = JournalType.secondary)
            window.undated.forEachIndexed { index, value ->
                JournalButton(value, { onEvent(HomeEvent.Explain("Без точной даты", "$value. Дата не уточнялась и не распределялась автоматически.", "Исходная запись")) },
                    Modifier.testTag("undated-$index"), kind = ButtonKind.TEXT)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HomeCalendar(state: HomeUiState, activity: Map<LocalDate, HeatDayUi>, onEvent: (HomeEvent) -> Unit) {
    val c = LocalJournalColors.current
    val colors = if (LocalJournalDark.current) DarkHeatmap else LightHeatmap
    Text("Годовая карта · ${state.calendarYear}", Modifier.semantics { heading() }, style = JournalType.card)
    FlowRow(horizontalArrangement = Arrangement.spacedBy(Space.sm), verticalArrangement = Arrangement.spacedBy(Space.sm)) {
        JournalButton("Предыдущий год", { onEvent(HomeEvent.Year(-1)) }, Modifier.testTag("year-previous"), enabled = state.calendarYear > 2025, kind = ButtonKind.TEXT)
        JournalButton("Следующий год", { onEvent(HomeEvent.Year(1)) }, Modifier.testTag("year-next"), enabled = state.calendarYear < 2026, kind = ButtonKind.TEXT)
    }
    Text("Листайте год по горизонтали. Каждая ячейка доступна для нажатия.", style = JournalType.caption, color = c.textSecondary)
    Text("Строки сверху вниз: пн–вс. Над сеткой — номер месяца.", style = JournalType.caption, color = c.textSecondary)
    val scroll = rememberScrollState()
    val density = LocalDensity.current
    LaunchedEffect(state.calendarYear) {
        scroll.scrollTo(with(density) { (42 * 48).dp.roundToPx() })
    }
    val first = LocalDate.of(state.calendarYear, 1, 1)
    val start = first.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    val formatter = remember { DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.forLanguageTag("ru")) }
    Row(Modifier.fillMaxWidth().horizontalScroll(scroll).testTag("heatmap-scroll")) {
        repeat(53) { week ->
            Column {
                val monday = start.plusDays((week * 7).toLong())
                Text(if (monday.dayOfMonth <= 7) monday.monthValue.toString() else "", Modifier.width(48.dp), style = JournalType.caption)
                repeat(7) { day ->
                    val date = monday.plusDays(day.toLong())
                    if (date.year != state.calendarYear) Spacer(Modifier.size(Metric.touch))
                    else {
                        val data = activity[date] ?: HeatDayUi(0)
                        val level = when (data.minutes) { 0 -> 0; in 1..14 -> 1; in 15..29 -> 2; in 30..59 -> 3; else -> 4 }
                        val description = "${date.format(formatter)}: ${data.minutes} минут" +
                            (if (data.minutes >= 5) "; порог серии достигнут" else "; порог серии не достигнут") +
                            (if (data.pagesWithoutTimer > 0) "; чтение без таймера: ${data.pagesWithoutTimer} страниц" else "")
                        Box(Modifier.size(Metric.touch).testTag("heat-$date")
                            .clickable(role = Role.Button) { onEvent(HomeEvent.Explain(date.format(formatter),
                                "Записанное время: ${data.minutes} мин.\nСтраницы без таймера: ${data.pagesWithoutTimer}.\n" +
                                    if (data.minutes >= 5) "День подходит для серии." else "Пятиминутный порог не достигнут.", "Подробности дня")) }
                            .semantics { contentDescription = description }, contentAlignment = Alignment.Center) {
                            Canvas(Modifier.size(28.dp).clip(JournalShape.control).background(colors[level])
                                .then(if (data.minutes >= 5) Modifier.border(Metric.border, c.accent, JournalShape.control) else Modifier)
                                .clearAndSetSemantics {}) {
                                val ink = if (level >= 3) c.onAccent else c.textPrimary
                                if (data.minutes >= 5) {
                                    val mark = Path().apply { moveTo(size.width * .15f, size.height * .4f)
                                        lineTo(size.width * .3f, size.height * .55f); lineTo(size.width * .53f, size.height * .25f) }
                                    drawPath(mark, ink, style = Stroke(1.5.dp.toPx()))
                                }
                                if (data.pagesWithoutTimer > 0) {
                                    val marker = Path().apply { moveTo(size.width * .72f, size.height * .53f)
                                        lineTo(size.width * .9f, size.height * .72f); lineTo(size.width * .72f, size.height * .9f)
                                        lineTo(size.width * .54f, size.height * .72f); close() }
                                    drawPath(marker, ink, style = Stroke(1.dp.toPx()))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    FlowRow(horizontalArrangement = Arrangement.spacedBy(Space.md), verticalArrangement = Arrangement.spacedBy(Space.sm)) {
        listOf("0", "1–14", "15–29", "30–59", "60+").forEachIndexed { index, label ->
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Space.xs)) {
                Box(Modifier.size(16.dp).background(colors[index], JournalShape.control))
                Text("$label мин", style = JournalType.caption)
            }
        }
    }
    Text("✓ ≥5 минут · день серии\n◇ Чтение без таймера · без добавления минут\n✓◇ Оба вида событий", style = JournalType.secondary)
}

@Composable
fun HomeComparison(state: HomeUiState, onEvent: (HomeEvent) -> Unit) {
    val c = LocalJournalColors.current
    val values = state.books.mapIndexed { index, book -> book to state.snapshot.comparisonMinutes.getOrElse(index) { 0 } }
        .sortedByDescending { it.second }
    if (values.isEmpty()) Text("Нет читаемых книг для сравнения", style = JournalType.secondary)
    val maximum = values.maxOfOrNull { it.second }?.coerceAtLeast(1) ?: 1
    values.forEach { (book, minutes) ->
        Column(Modifier.fillMaxWidth().heightIn(min = Metric.touch).testTag("comparison-${book.id}")
            .clickable(role = Role.Button) { onEvent(HomeEvent.Explain(book.title,
                "$minutes минут за период «${state.period.label}». Прогресс: ${book.bookmark} / ${book.totalPages} стр. Это демонстрационный итог.", "Статистика книги")) }
            .semantics(mergeDescendants = true) {}, verticalArrangement = Arrangement.spacedBy(Space.sm)) {
            Text(book.title, style = JournalType.card)
            Text("$minutes мин", style = JournalType.secondary)
            Box(Modifier.fillMaxWidth().height(Metric.progress).background(c.progressTrack, JournalShape.capsule)) {
                if (minutes > 0) Box(Modifier.fillMaxWidth(minutes.toFloat() / maximum).fillMaxHeight().background(c.accent, JournalShape.capsule))
            }
        }
    }
    Text("Завершённые прочтения", Modifier.semantics { heading() }, style = JournalType.card)
    if (state.scenario == HomeScenario.EMPTY) Text("Завершённых прочтений пока нет", style = JournalType.secondary)
    else if (state.scenario == HomeScenario.SINGLE) Text("Завершённых прочтений в этом примере нет", style = JournalType.secondary)
    else {
        val unknownPages = state.scenario in listOf(HomeScenario.UNKNOWN, HomeScenario.INCOMPLETE, HomeScenario.PENDING)
        Text("Письма с острова · 3 ч · ${if (unknownPages) "—" else "90"} стр. · 6 сеансов", style = JournalType.secondary)
        Text("Дом бумажных окон · 2 ч · ${if (unknownPages) "—" else "60"} стр. · 4 сеанса", style = JournalType.secondary)
        Text("Северный свет · время — · страницы — · сеансы —\nИсторическое прочтение без записанных показателей", style = JournalType.secondary)
    }
}

@Composable
fun HomeSpeed(state: HomeUiState, onEvent: (HomeEvent) -> Unit) {
    HomeChoices(listOf<String?>(null) + state.books.map { it.id }, state.speedBookId,
        { id -> state.books.firstOrNull { it.id == id }?.title ?: "Вся библиотека" }, "speed-book") { onEvent(HomeEvent.SpeedBook(it)) }
    val insufficient = state.snapshot.speed == "—" || state.speedBookId == "coast"
    val speed = when {
        insufficient -> "—"
        state.period == HomePeriod.TODAY && state.speedBookId == "atlas" -> "28,8 стр/ч"
        state.period == HomePeriod.TODAY && state.speedBookId == "garden" -> "36 стр/ч"
        else -> state.snapshot.speed
    }
    Text("Средняя скорость", style = JournalType.card)
    Text(speed, Modifier.testTag("speed-value"), style = JournalType.number)
    Text(if (insufficient) "Недостаточно сопоставимых записей с известными страницами и положительным временем."
        else "Только страницы и соответствующее им записанное время. Чтение без таймера сюда не входит.", style = JournalType.secondary)
    Text("Примерное оставшееся время", style = JournalType.card)
    Text(when { insufficient || state.speedBookId == null -> "—"
        state.period == HomePeriod.TODAY && state.speedBookId == "atlas" -> "≈ 6,3 ч"
        state.period == HomePeriod.TODAY && state.speedBookId == "garden" -> "≈ 4,7 ч"
        state.speedBookId == "garden" -> "≈ 5,6 ч"; else -> state.snapshot.forecast },
        Modifier.testTag("forecast-value"), style = JournalType.number)
    Text(if (state.speedBookId == null) "Выберите книгу для оценки оставшихся часов."
        else "Оценка относится только к текущему прочтению. Дата окончания не прогнозируется.", style = JournalType.secondary)
}

@Composable
fun HomeRecords(state: HomeUiState) {
    val s = state.snapshot
    val empty = state.scenario == HomeScenario.EMPTY
    val single = state.scenario == HomeScenario.SINGLE
    val incomplete = state.scenario in listOf(HomeScenario.INCOMPLETE, HomeScenario.PENDING, HomeScenario.UNKNOWN)
    val records = listOf(
        Triple("Лучший день по времени", if (empty) "—" else if (single) "25 мин" else "2 ч 15 мин", s.bestDayTime),
        Triple("Лучший день по страницам", if (empty || state.scenario == HomeScenario.UNKNOWN) "—" else if (single) "12 стр" else if (incomplete) "20 стр" else "52 стр", s.bestDayPages),
        Triple("Самый длинный сеанс", if (empty) "—" else if (single) "25 мин" else "1 ч 35 мин", s.longestSession),
        Triple("Лучший месяц по времени", if (empty) "—" else if (single) "25 мин" else "8 ч", s.bestMonth),
        Triple("Самая длинная серия", if (empty) "0 дней" else if (single) "1 день" else "12 дней", when {
            empty -> "0 дней"; single || state.period == HomePeriod.TODAY -> "1 день"
            state.period == HomePeriod.WEEK -> "3 дня"; state.period == HomePeriod.MONTH -> "6 дней"; else -> "12 дней"
        }),
        Triple("Книга с максимальным временем", if (empty) "—" else if (single) "Атлас тихих открытий · 25 мин" else "Атлас тихих открытий · 15 ч",
            if (empty) "—" else "${state.books.first().title} · ${s.comparisonMinutes.firstOrNull() ?: 0} мин")
    )
    records.forEachIndexed { index, (title, all, selected) ->
        JournalCard(Modifier.fillMaxWidth().testTag("record-$index")) {
            Text(title, Modifier.semantics { heading() }, style = JournalType.card)
            Text("За всё время · $all", style = JournalType.secondary)
            Text("${state.period.label} · $selected", style = JournalType.secondary)
            if (index == 1 && incomplete) Text("Среди записей с полными страницами. Неполные данные не подтверждают абсолютный рекорд.",
                style = JournalType.caption, color = LocalJournalColors.current.warning)
        }
    }
    Text(if (empty || single) "Охват записями: 0 завершённых прочтений" else "Охват записями: 2 из 3 завершённых прочтений. Полнота всего чтения не гарантируется.",
        Modifier.testTag("records-coverage"), style = JournalType.caption)
}
