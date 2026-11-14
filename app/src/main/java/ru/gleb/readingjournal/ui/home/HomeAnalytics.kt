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
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.dp
import ru.gleb.readingjournal.demo.HomeFixtures
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
    HomeChoices(ChartGrouping.entries, state.grouping, { it.label }, "chart-grouping") { onEvent(HomeEvent.Grouping(it)) }
    Text("${state.period.label} · ${window.label}", style = JournalType.secondary)
    FlowRow(horizontalArrangement = Arrangement.spacedBy(Space.sm), verticalArrangement = Arrangement.spacedBy(Space.sm)) {
        JournalButton("Назад по интервалам", { onEvent(HomeEvent.Interval(-1)) },
            Modifier.testTag("chart-previous"), enabled = state.interval == 0 && window.canPrevious, kind = ButtonKind.TEXT)
        JournalButton("Вперёд по интервалам", { onEvent(HomeEvent.Interval(1)) },
            Modifier.testTag("chart-next"), enabled = state.interval < 0, kind = ButtonKind.TEXT)
    }
    if (window.bars.isEmpty()) {
        Text("Пока нет записей страниц для диаграммы", Modifier.testTag("chart-empty"), style = JournalType.secondary)
        return
    }
    Text("Новые подтверждённые страницы", Modifier.testTag("chart-pages-label"), style = JournalType.caption)
    val maximum = window.bars.maxOf { it.pages }.coerceAtLeast(1)
    Text("$maximum стр.", style = JournalType.caption, color = c.textSecondary)
    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).testTag("reading-chart"),
        horizontalArrangement = Arrangement.spacedBy(Space.md), verticalAlignment = Alignment.Top) {
        window.bars.forEachIndexed { index, bar ->
            Column(Modifier.width(80.dp).heightIn(min = Metric.touch).testTag("chart-bar-$index")
                .clickable(role = Role.Button) { onEvent(HomeEvent.Open(HomeDestination.ChartDetail(state.grouping, bar.label, bar.pages))) }
                .semantics(mergeDescendants = true) { contentDescription = "${bar.label}: ${bar.pages} новых подтверждённых страниц" },
                horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(Space.sm)) {
                Text(bar.pages.toString(), style = JournalType.card)
                Box(Modifier.fillMaxWidth().height(128.dp).testTag("plot-area-$index"), contentAlignment = Alignment.BottomCenter) {
                    if (bar.pages > 0) Box(Modifier.width(36.dp).height((bar.pages.toFloat() / maximum * 120).dp)
                        .clip(JournalShape.control).background(c.accentSoft).border(Metric.border, c.accent, JournalShape.control))
                    Box(Modifier.fillMaxWidth().height(Metric.border).background(c.outlineStrong))
                }
                Text(bar.label, style = JournalType.caption)
            }
        }
    }
    Text("0 · ${state.grouping.label.lowercase(Locale.forLanguageTag("ru"))}", Modifier.testTag("dynamics-end"), style = JournalType.caption)
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HomeCalendar(state: HomeUiState, activity: Map<LocalDate, HeatDayUi>, onEvent: (HomeEvent) -> Unit) {
    val c = LocalJournalColors.current
    val colors = if (LocalJournalDark.current) DarkHeatmap else LightHeatmap
    Text("Годовая карта · ${state.calendarYear}", Modifier.semantics { heading() }, style = JournalType.card)
    FlowRow(horizontalArrangement = Arrangement.spacedBy(Space.sm), verticalArrangement = Arrangement.spacedBy(Space.sm)) {
        JournalButton("Предыдущий год", { onEvent(HomeEvent.Year(-1)) }, Modifier.testTag("year-previous"), enabled = state.calendarYear > 2024, kind = ButtonKind.TEXT)
        JournalButton("Следующий год", { onEvent(HomeEvent.Year(1)) }, Modifier.testTag("year-next"), enabled = state.calendarYear < 2026, kind = ButtonKind.TEXT)
    }
    Text("Листайте год по горизонтали. Каждая ячейка доступна для нажатия.", style = JournalType.caption, color = c.textSecondary)
    Text("Строки сверху вниз: пн–вс. Над сеткой — номер месяца.", style = JournalType.caption, color = c.textSecondary)
    val scroll = rememberScrollState()
    val density = LocalDensity.current
    LaunchedEffect(state.calendarYear) { scroll.scrollTo(with(density) { (42 * 48).dp.roundToPx() }) }
    val first = LocalDate.of(state.calendarYear, 1, 1)
    val start = first.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    val weekCount = (first.lengthOfYear() + first.dayOfWeek.value - 1 + 6) / 7
    val formatter = remember { DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.forLanguageTag("ru")) }
    Row(Modifier.fillMaxWidth().horizontalScroll(scroll).testTag("heatmap-scroll")) {
        repeat(weekCount) { week ->
            Column {
                val monday = start.plusDays((week * 7).toLong())
                Text(if (monday.dayOfMonth <= 7) monday.monthValue.toString() else "", Modifier.width(48.dp), style = JournalType.caption)
                repeat(7) { day ->
                    val date = monday.plusDays(day.toLong())
                    if (date.year != state.calendarYear) Spacer(Modifier.size(Metric.touch))
                    else {
                        val data = activity[date] ?: HeatDayUi(0)
                        val description = "${date.format(formatter)}: ${data.pages} новых страниц; уровень интенсивности ${data.intensity}; " +
                            if (data.pages > 0) "день серии" else "нет новых подтверждённых страниц"
                        Box(Modifier.size(Metric.touch).testTag("heat-$date")
                            .clickable(role = Role.Button) { onEvent(HomeEvent.Open(HomeDestination.DayDetail(date))) }
                            .semantics { contentDescription = description }, contentAlignment = Alignment.Center) {
                            Canvas(Modifier.size(28.dp).clip(JournalShape.control).background(colors[data.intensity])
                                .then(if (data.pages > 0) Modifier.border(Metric.border, c.accent, JournalShape.control) else Modifier)
                                .clearAndSetSemantics {}) {
                                if (data.pages > 0) {
                                    val mark = Path().apply { moveTo(size.width * .15f, size.height * .4f)
                                        lineTo(size.width * .3f, size.height * .55f); lineTo(size.width * .53f, size.height * .25f) }
                                    drawPath(mark, if (data.intensity >= 3) c.onAccent else c.textPrimary, style = Stroke(1.5.dp.toPx()))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    FlowRow(horizontalArrangement = Arrangement.spacedBy(Space.md), verticalArrangement = Arrangement.spacedBy(Space.sm)) {
        listOf("0", "1–9", "10–24", "25–49", "50+").forEachIndexed { index, label ->
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Space.xs)) {
                Box(Modifier.size(16.dp).background(colors[index], JournalShape.control))
                Text("$label стр.", style = JournalType.caption)
            }
        }
    }
    Text("✓ Хотя бы одна новая страница · день серии", Modifier.testTag("calendar-end"), style = JournalType.secondary)
}

@Composable
fun HomeComparison(state: HomeUiState, onEvent: (HomeEvent) -> Unit) {
    val c = LocalJournalColors.current
    val values = state.books.map { it to (state.snapshot.comparisonPages[it.id] ?: 0) }.sortedByDescending { it.second }
    if (values.isEmpty()) Text("Нет читаемых книг для сравнения", style = JournalType.secondary)
    val maximum = values.maxOfOrNull { it.second }?.coerceAtLeast(1) ?: 1
    values.forEach { (book, pages) ->
        Column(Modifier.fillMaxWidth().heightIn(min = Metric.touch).testTag("comparison-${book.id}")
            .clickable(role = Role.Button) { onEvent(HomeEvent.Open(HomeDestination.BookStatistics(book.id))) }
            .semantics(mergeDescendants = true) {}, verticalArrangement = Arrangement.spacedBy(Space.sm)) {
            Text(book.title, style = JournalType.card)
            Text("$pages стр.", style = JournalType.secondary)
            Box(Modifier.fillMaxWidth().height(Metric.progress).background(c.progressTrack, JournalShape.capsule)) {
                if (pages > 0) Box(Modifier.fillMaxWidth(pages.toFloat() / maximum).fillMaxHeight().background(c.accent, JournalShape.capsule))
            }
        }
    }
    Text("Завершённые прочтения · все циклы", Modifier.semantics { heading() }, style = JournalType.card)
    val completed = HomeFixtures.completionSummary(state)
    if (completed.isEmpty()) Text("Завершённых прочтений в этом примере нет", Modifier.testTag("comparison-end"), style = JournalType.secondary)
    completed.forEach { Text(it, style = JournalType.secondary) }
    if (completed.isNotEmpty()) Text("Историческое завершение не подтверждает страницы. Неизвестные показатели обозначены «—».", Modifier.testTag("comparison-end"), style = JournalType.caption)
}

@Composable
fun HomeRecords(state: HomeUiState) {
    state.snapshot.records.forEachIndexed { index, record ->
        JournalCard(Modifier.fillMaxWidth().testTag("record-$index")) {
            Text(record.title, Modifier.semantics { heading() }, style = JournalType.card)
            Text("За всё время · ${record.allTime}", style = JournalType.secondary)
            Text("${state.period.label} · ${record.selected}", style = JournalType.secondary)
        }
    }
    Text(when (state.scenario) {
        HomeScenario.EMPTY, HomeScenario.NO_PAGES, HomeScenario.SINGLE -> "Охват записями: 0 завершённых прочтений"
        HomeScenario.HISTORICAL -> "Охват записями: 0 из 2 исторических прочтений. Страницы неизвестны."
        else -> "Охват записями: 5 из 7 завершённых прочтений. Полнота незаписанного прошлого неизвестна."
    }, Modifier.testTag("records-coverage"), style = JournalType.caption)
}
