package ru.gleb.readingjournal.ui.home

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.dp
import ru.gleb.readingjournal.ui.designsystem.*
import java.time.LocalDate

@Composable
fun HomeScreen(state: HomeUiState, chart: ChartWindowUi, activity: Map<LocalDate, HeatDayUi>,
    onEvent: (HomeEvent) -> Unit) {
    // Capture the Activity inset before opening the separate modal window (API 26).
    val density = LocalDensity.current
    val platformBottom = ViewCompat.getRootWindowInsets(LocalView.current.rootView)
        ?.getInsets(WindowInsetsCompat.Type.navigationBars())?.bottom ?: 0
    val navigationBottom = with(density) { maxOf(platformBottom, WindowInsets.navigationBars.getBottom(this)).toDp() }
    val scroll = rememberLazyListState()
    var previousScenario by rememberSaveable { mutableStateOf(state.scenario.name) }
    LaunchedEffect(state.scenario) {
        if (previousScenario != state.scenario.name) {
            scroll.scrollToItem(0)
            previousScenario = state.scenario.name
        }
    }
    LazyColumn(Modifier.fillMaxSize().testTag("home-feed"), state = scroll,
        contentPadding = PaddingValues(Space.lg), verticalArrangement = Arrangement.spacedBy(Space.md)) {
        item("heading") {
            Column(verticalArrangement = Arrangement.spacedBy(Space.sm)) {
                Text("Демонстрационные данные · 12 ноября 2026", style = JournalType.caption,
                    color = LocalJournalColors.current.textSecondary)
                Text("Сейчас читаю", Modifier.semantics { heading() }.testTag("home-books-heading"), style = JournalType.screen)
                Text("Читайте в своём темпе", style = JournalType.secondary, color = LocalJournalColors.current.textSecondary)
            }
        }
        if (state.books.isEmpty()) item("empty") {
            JournalEmpty(if (state.scenario == HomeScenario.HISTORICAL) "История прочтений сохранена" else "Здесь начнётся ваша история",
                if (state.scenario == HomeScenario.HISTORICAL) "Активных книг нет. В примере есть два исторических завершения без записанных страниц."
                else "Сейчас нет книг со статусом «Читаю». Это пустой демонстрационный дневник.", Modifier.testTag("home-empty"))
        }
        items(state.orderedBooks, key = { "book-${it.id}" }) { book -> HomeBookCard(book, state, onEvent) }
        item("xp") { HomeXpCard(state.xp) }
        item("metrics") { HomeMetrics(state, onEvent) }
        item("period") {
            Column(verticalArrangement = Arrangement.spacedBy(Space.sm)) {
                Text("Период статистики", Modifier.semantics { heading() }, style = JournalType.section)
                HomeChoices(HomePeriod.entries, state.period, { it.label }, "period") { onEvent(HomeEvent.Period(it)) }
            }
        }
        HomeSection.entries.forEach { section -> item("section-${section.name}") {
            HomeAccordion(section, state, chart, activity, onEvent)
        } }
        item("footer") {
            Text("Только визуальный прототип. Страницы и XP не записываются.",
                Modifier.testTag("home-footer").padding(vertical = Space.md),
                style = JournalType.caption, color = LocalJournalColors.current.textSecondary)
        }
    }
    state.sheet?.let { sheet -> HomeDemoSheet(navigationBottom, { onEvent(HomeEvent.Dismiss) }) {
        Text(sheet.title, Modifier.semantics { heading() }.testTag("home-sheet"), style = JournalType.section)
        Text(sheet.body, Modifier.testTag("home-sheet-body"))
        sheet.actionLabel?.let { label -> JournalButton(label, { onEvent(HomeEvent.Explain(
            "Демонстрационный переход", "Подробный экран «$label» появится на следующем этапе. В UI-02 доступен только визуальный пример.")) }) }
    } }
}

/** The approved sheet styling, scoped to Home; its final action stays above system navigation. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeDemoSheet(navigationBottom: androidx.compose.ui.unit.Dp, onDismiss: () -> Unit,
    content: @Composable ColumnScope.() -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss, shape = JournalShape.sheet,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = LocalJournalColors.current.surface, tonalElevation = 0.dp,
        contentWindowInsets = { WindowInsets.safeDrawing.only(WindowInsetsSides.Top) }) {
        Column(Modifier.fillMaxWidth().padding(bottom = navigationBottom).imePadding()
            .verticalScroll(rememberScrollState()).padding(Space.lg),
            verticalArrangement = Arrangement.spacedBy(Space.md)) {
            content()
            JournalButton("Закрыть панель", onDismiss, Modifier.testTag("home-sheet-close"), kind = ButtonKind.TEXT)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun <T> HomeChoices(options: List<T>, selected: T, label: (T) -> String, tag: String,
    onSelect: (T) -> Unit) {
    FlowRow(Modifier.fillMaxWidth().testTag(tag), horizontalArrangement = Arrangement.spacedBy(Space.sm),
        verticalArrangement = Arrangement.spacedBy(Space.sm)) {
        options.forEachIndexed { index, option -> JournalChip(label(option), selected == option,
            { onSelect(option) }, Modifier.testTag("$tag-$index")) }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun HomeBookCard(book: HomeBookUi, state: HomeUiState, onEvent: (HomeEvent) -> Unit) {
    val c = LocalJournalColors.current
    val pin = state.pinnedIds.indexOf(book.id)
    Surface(Modifier.fillMaxWidth().testTag("home-book-${book.id}"), shape = JournalShape.card,
        color = c.surface, border = BorderStroke(Metric.border, c.outline)) {
        Column(Modifier.padding(Space.lg), verticalArrangement = Arrangement.spacedBy(Space.md)) {
            if (pin >= 0) Text("Закреплена · ${pin + 1}", color = c.accent, style = JournalType.caption)
            Row(horizontalArrangement = Arrangement.spacedBy(Space.md)) {
                JournalCover(book.title, book.author, book.id, Modifier.width(72.dp))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Space.xs)) {
                    Text(book.title, Modifier.semantics { heading() }, style = JournalType.card)
                    Text(book.author, color = c.textSecondary, style = JournalType.secondary)
                    Text("Закладка ${book.bookmark} / ${book.totalPages} стр.", Modifier.testTag("bookmark-${book.id}"), style = JournalType.caption)
                }
            }
            JournalProgress(book.reachedPage.toFloat() / book.totalPages, "Рубеж ${book.reachedPage} / ${book.totalPages} стр.")
            Text("Подтверждено в текущем цикле: ${book.confirmedPages} стр.",
                Modifier.testTag("confirmed-${book.id}"), style = JournalType.caption, color = c.textSecondary)
            JournalButton("Записать чтение", { onEvent(HomeEvent.Open(HomeDestination.RecordReading(book.id))) },
                Modifier.testTag("record-reading-${book.id}"))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(Space.sm), verticalArrangement = Arrangement.spacedBy(Space.xs)) {
                JournalButton(if (pin >= 0) "Открепить" else "Закрепить", { onEvent(HomeEvent.Pin(book.id)) },
                    Modifier.testTag("pin-${book.id}"), kind = ButtonKind.TEXT)
                if (pin >= 0 && state.pinnedIds.size > 1) {
                    JournalButton("Выше", { onEvent(HomeEvent.MovePin(book.id, -1)) },
                        Modifier.testTag("pin-up-${book.id}"), enabled = pin > 0, kind = ButtonKind.TEXT)
                    JournalButton("Ниже", { onEvent(HomeEvent.MovePin(book.id, 1)) },
                        Modifier.testTag("pin-down-${book.id}"), enabled = pin < state.pinnedIds.lastIndex, kind = ButtonKind.TEXT)
                }
            }
        }
    }
}

@Composable
private fun HomeMetrics(state: HomeUiState, onEvent: (HomeEvent) -> Unit) {
    val snapshot = state.snapshot
    val dayUnit = russianForm(state.streak, "день", "дня", "дней")
    val cards: @Composable () -> Unit = {
        MetricTile("Прочитанные страницы", "Страницы", snapshot.pages.text, "стр.", snapshot.pages.accessible, "metric-pages", onEvent)
        MetricTile("Завершённые прочтения", "Прочтения", snapshot.completions.toString(), "прочтений",
            "${snapshot.completions} завершённых прочтений, включая повторные циклы", "metric-completions", onEvent)
        MetricTile("Текущая серия", "Серия", state.streak.toString(), dayUnit,
            "Текущая серия ${state.streak} $dayUnit. Не зависит от периода статистики", "metric-streak", onEvent)
    }
    BoxWithConstraints(Modifier.fillMaxWidth().testTag("home-metrics")) {
        val stacked = LocalDensity.current.fontScale > 1.1f || maxWidth < 350.dp
        Column(verticalArrangement = Arrangement.spacedBy(Space.sm)) {
            if (stacked) Column(verticalArrangement = Arrangement.spacedBy(Space.md)) { cards() }
            else Row(Modifier.height(IntrinsicSize.Max), horizontalArrangement = Arrangement.spacedBy(Space.sm)) {
                Box(Modifier.weight(1f).fillMaxHeight()) { MetricTile("Прочитанные страницы", "Страницы", snapshot.pages.text, "стр.", snapshot.pages.accessible,
                    "metric-pages", onEvent, Modifier.fillMaxHeight()) }
                Box(Modifier.weight(1f).fillMaxHeight()) { MetricTile("Завершённые прочтения", "Прочтения", snapshot.completions.toString(), "прочтений",
                    "${snapshot.completions} завершённых прочтений, включая повторные циклы", "metric-completions", onEvent, Modifier.fillMaxHeight()) }
                Box(Modifier.weight(1f).fillMaxHeight()) { MetricTile("Текущая серия", "Серия", state.streak.toString(), dayUnit,
                    "Текущая серия ${state.streak} $dayUnit. Не зависит от периода статистики", "metric-streak", onEvent, Modifier.fillMaxHeight()) }
            }
            Text("День серии — хотя бы одна новая подтверждённая страница", style = JournalType.caption, color = LocalJournalColors.current.textSecondary)
        }
    }
}

@Composable
private fun MetricTile(title: String, label: String, number: String, unit: String, description: String, tag: String,
    onEvent: (HomeEvent) -> Unit, modifier: Modifier = Modifier) {
    JournalCard(modifier.fillMaxWidth().testTag(tag)
        .semantics { contentDescription = "$title. $description" }
        .clickable(role = Role.Button) { onEvent(HomeEvent.Explain(title, description)) }) {
        Text(number, style = JournalType.number)
        Text(unit, style = JournalType.caption, color = LocalJournalColors.current.textSecondary)
        Text(label, Modifier.testTag("$tag-label"), style = JournalType.secondary)
    }
}

@Composable
private fun HomeAccordion(section: HomeSection, state: HomeUiState, chart: ChartWindowUi,
    activity: Map<LocalDate, HeatDayUi>, onEvent: (HomeEvent) -> Unit) {
    val expanded = section == state.expanded
    val c = LocalJournalColors.current
    val summary = when (section) {
        HomeSection.DYNAMICS -> "${state.snapshot.pages.text} новых страниц за период"
        HomeSection.CALENDAR -> "${state.calendarYear} · текущая серия ${state.streak} ${russianForm(state.streak, "день", "дня", "дней")}"
        HomeSection.COMPARISON -> "${state.books.size} ${russianForm(state.books.size, "книга", "книги", "книг")} · по новым страницам"
        HomeSection.RECORDS -> "Четыре личных ориентира"
    }
    Surface(Modifier.fillMaxWidth().animateContentSize(tween(Motion.normal)), shape = JournalShape.card,
        color = c.surface, border = BorderStroke(Metric.border, c.outline)) {
        Column {
            Row(Modifier.fillMaxWidth().testTag("section-${section.name}").heightIn(min = Metric.touch)
                .clickable(role = Role.Button) { onEvent(HomeEvent.Section(section)) }
                .semantics(mergeDescendants = true) { heading(); stateDescription = if (expanded) "Развёрнуто" else "Свёрнуто" }
                .padding(Space.lg), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Space.xs)) {
                    Text(section.label, style = JournalType.section)
                    Text(summary, style = JournalType.secondary, color = c.textSecondary)
                }
                Text(if (expanded) "−" else "+", Modifier.padding(start = Space.sm).clearAndSetSemantics {}, style = JournalType.section)
            }
            if (expanded) Column(Modifier.padding(start = Space.lg, end = Space.lg, bottom = Space.lg)
                .testTag("expanded-${section.name}"), verticalArrangement = Arrangement.spacedBy(Space.md)) {
                when (section) {
                    HomeSection.DYNAMICS -> HomeDynamics(state, chart, onEvent)
                    HomeSection.CALENDAR -> HomeCalendar(state, activity, onEvent)
                    HomeSection.COMPARISON -> HomeComparison(state, onEvent)
                    HomeSection.RECORDS -> HomeRecords(state)
                }
            }
        }
    }
}

private fun russianForm(count: Int, one: String, few: String, many: String): String = when {
    count % 100 in 11..14 -> many
    count % 10 == 1 -> one
    count % 10 in 2..4 -> few
    else -> many
}
