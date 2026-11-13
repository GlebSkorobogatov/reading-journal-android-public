package ru.gleb.readingjournal.ui.home

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import ru.gleb.readingjournal.ui.designsystem.*
import java.time.LocalDate

@Composable
fun HomeScreen(state: HomeUiState, chart: ChartWindowUi, activity: Map<LocalDate, HeatDayUi>,
    onEvent: (HomeEvent) -> Unit) {
    val scroll = rememberLazyListState()
    var previousScenario by rememberSaveable { mutableStateOf(state.scenario.name) }
    LaunchedEffect(state.scenario) {
        if (previousScenario != state.scenario.name) {
            scroll.scrollToItem(0)
            previousScenario = state.scenario.name
        }
    }
    LaunchedEffect(state.focusBookId) {
        state.focusBookId?.let { id ->
            val index = state.orderedBooks.indexOfFirst { it.id == id }
            if (index >= 0) scroll.scrollToItem(index + 1)
            onEvent(HomeEvent.FocusConsumed)
        }
    }
    LazyColumn(Modifier.fillMaxSize().testTag("home-feed"), state = scroll,
        contentPadding = PaddingValues(Space.lg), verticalArrangement = Arrangement.spacedBy(Space.md)) {
        item("heading") {
            Column(verticalArrangement = Arrangement.spacedBy(Space.sm)) {
                Text("Демонстрационные данные · 12 ноября 2026", style = JournalType.caption,
                    color = LocalJournalColors.current.textSecondary)
                Text("Сейчас читаю", Modifier.semantics { heading() }.testTag("home-books-heading"), style = JournalType.screen)
                Text("Время для новой главы", style = JournalType.secondary, color = LocalJournalColors.current.textSecondary)
            }
        }
        if (state.books.isEmpty()) item("empty") {
            JournalEmpty("Здесь начнётся ваша история", "Сейчас нет книг со статусом «Читаю». Это пустой демонстрационный дневник.",
                Modifier.testTag("home-empty"))
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
            Text("Только визуальный прототип. Сеансы и XP не записываются.",
                Modifier.testTag("home-footer").padding(vertical = Space.md),
                style = JournalType.caption, color = LocalJournalColors.current.textSecondary)
        }
    }
    state.sheet?.let { sheet -> JournalSheet({ onEvent(HomeEvent.Dismiss) }) {
        Text(sheet.title, Modifier.semantics { heading() }.testTag("home-sheet"), style = JournalType.section)
        Text(sheet.body)
        sheet.actionLabel?.let { label -> JournalButton(label, { onEvent(HomeEvent.Explain(
            "Демонстрационный переход", "Подробный экран «$label» появится на следующем этапе. В UI-02 доступен только визуальный пример.")) }) }
        sheet.finishingBookId?.let { id ->
            JournalButton("Показать заполненный пример", { onEvent(HomeEvent.ResolvePages(id, PageState.KNOWN)) })
            JournalButton("Позже · ожидают заполнения", { onEvent(HomeEvent.ResolvePages(id, PageState.PENDING)) }, kind = ButtonKind.SECONDARY)
            JournalButton("Не помню страницы", { onEvent(HomeEvent.ResolvePages(id, PageState.UNKNOWN)) }, kind = ButtonKind.TEXT)
        }
    } }
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
    val timer = state.timer?.takeIf { it.bookId == book.id }
    val pin = state.pinnedIds.indexOf(book.id)
    Surface(Modifier.fillMaxWidth().testTag("home-book-${book.id}"), shape = JournalShape.card,
        color = c.surface, border = BorderStroke(Metric.border, if (timer != null) c.accent else c.outline)) {
        Column(Modifier.padding(Space.lg), verticalArrangement = Arrangement.spacedBy(Space.md)) {
            if (pin >= 0) Text("Закреплена · ${pin + 1}", color = c.accent, style = JournalType.caption)
            Row(horizontalArrangement = Arrangement.spacedBy(Space.md)) {
                JournalCover(book.title, book.author, book.id, Modifier.width(72.dp))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Space.xs)) {
                    Text(book.title, Modifier.semantics { heading() }, style = JournalType.card)
                    Text(book.author, color = c.textSecondary, style = JournalType.secondary)
                    Text("${book.bookmark} / ${book.totalPages} стр.", style = JournalType.caption)
                }
            }
            JournalProgress(book.progress, "Достигнутый рубеж")
            when (book.pages) {
                PageState.PENDING -> {
                    Text("Ожидают заполнения · 1 сеанс", Modifier.testTag("pending-${book.id}"), color = c.warning, style = JournalType.secondary)
                    JournalButton("Завершить запись сеанса", { onEvent(HomeEvent.PagePrompt(book.id)) }, kind = ButtonKind.SECONDARY)
                }
                PageState.UNKNOWN -> Text("Не помню страницы · объём неизвестен, новый сеанс доступен",
                    Modifier.testTag("unknown-${book.id}"), style = JournalType.secondary, color = c.textSecondary)
                PageState.KNOWN -> Unit
            }
            if (timer != null) {
                Text("${timer.elapsed} · ${timer.phase.label} · демо", Modifier.testTag("timer-${book.id}")
                    .semantics { liveRegion = LiveRegionMode.Polite }, style = JournalType.number)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(Space.sm), verticalArrangement = Arrangement.spacedBy(Space.sm)) {
                    JournalButton(if (timer.phase == DemoTimerPhase.RUNNING) "Пауза" else "Продолжить", {
                        onEvent(if (timer.phase == DemoTimerPhase.RUNNING) HomeEvent.Pause else HomeEvent.Resume)
                    }, Modifier.testTag("timer-toggle"), enabled = timer.phase != DemoTimerPhase.FINISHING)
                    JournalButton("Завершить", { onEvent(HomeEvent.Finish) }, Modifier.testTag("timer-finish"),
                        enabled = timer.phase != DemoTimerPhase.FINISHING, kind = ButtonKind.SECONDARY)
                }
            } else {
                JournalButton("Начать чтение · демо", { onEvent(HomeEvent.Start(book.id)) },
                    Modifier.testTag("start-${book.id}"), enabled = state.timer == null && book.pages != PageState.PENDING)
                if (state.timer != null) Text("Уже открыт один демонстрационный таймер", style = JournalType.caption, color = c.textSecondary)
            }
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
    val timeNumber = snapshot.time.removeSuffix(" мин").removeSuffix(" ч").replace(" ч ", ":")
    val timeUnit = when { snapshot.time.contains("ч ") -> "ч : мин"; snapshot.time.endsWith("ч") -> "часов"; else -> "минут" }
    val dayUnit = russianForm(state.streak, "день", "дня", "дней")
    val cards: @Composable () -> Unit = {
        MetricTile("Время", timeNumber, timeUnit, snapshot.time, "metric-time", onEvent)
        MetricTile("Страницы", snapshot.pages.text, "стр.", snapshot.pages.accessible, "metric-pages", onEvent)
        MetricTile("Текущая серия", state.streak.toString(), dayUnit,
            "Текущая серия ${state.streak} $dayUnit. Не зависит от периода статистики", "metric-streak", onEvent)
    }
    BoxWithConstraints(Modifier.fillMaxWidth().testTag("home-metrics")) {
        val stacked = LocalDensity.current.fontScale > 1.1f || maxWidth < 350.dp
        Column(verticalArrangement = Arrangement.spacedBy(Space.sm)) {
            if (stacked) Column(verticalArrangement = Arrangement.spacedBy(Space.md)) { cards() }
            else Row(Modifier.height(IntrinsicSize.Max), horizontalArrangement = Arrangement.spacedBy(Space.sm)) {
                Box(Modifier.weight(1f).fillMaxHeight()) { MetricTile("Время", timeNumber, timeUnit, snapshot.time,
                    "metric-time", onEvent, Modifier.fillMaxHeight()) }
                Box(Modifier.weight(1f).fillMaxHeight()) { MetricTile("Страницы", snapshot.pages.text, "стр.", snapshot.pages.accessible,
                    "metric-pages", onEvent, Modifier.fillMaxHeight()) }
                Box(Modifier.weight(1f).fillMaxHeight()) { MetricTile("Текущая серия", state.streak.toString(), dayUnit,
                    "Текущая серия ${state.streak} $dayUnit. Не зависит от периода статистики", "metric-streak", onEvent, Modifier.fillMaxHeight()) }
            }
            Text("День серии — не менее 5 минут чтения", style = JournalType.caption, color = LocalJournalColors.current.textSecondary)
        }
    }
}

@Composable
private fun MetricTile(title: String, number: String, unit: String, description: String, tag: String,
    onEvent: (HomeEvent) -> Unit, modifier: Modifier = Modifier) {
    JournalCard(modifier.fillMaxWidth().testTag(tag).clickable(role = Role.Button) { onEvent(HomeEvent.Explain(title, description)) }) {
        Text(number, style = JournalType.number)
        Text(unit, style = JournalType.caption, color = LocalJournalColors.current.textSecondary)
        Text(title, style = JournalType.secondary)
    }
}

@Composable
private fun HomeAccordion(section: HomeSection, state: HomeUiState, chart: ChartWindowUi,
    activity: Map<LocalDate, HeatDayUi>, onEvent: (HomeEvent) -> Unit) {
    val expanded = section == state.expanded
    val c = LocalJournalColors.current
    val summary = when (section) {
        HomeSection.DYNAMICS -> "${state.snapshot.time} · ${state.snapshot.pages.text} стр."
        HomeSection.CALENDAR -> "${state.calendarYear} · текущая серия ${state.streak} ${russianForm(state.streak, "день", "дня", "дней")}"
        HomeSection.COMPARISON -> "${state.books.size} ${russianForm(state.books.size, "книга", "книги", "книг")} · по записанному времени"
        HomeSection.SPEED -> if (state.snapshot.speed == "—") "Недостаточно данных" else state.snapshot.speed
        HomeSection.RECORDS -> "Лучший день · ${state.snapshot.bestDayTime}"
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
                    HomeSection.SPEED -> HomeSpeed(state, onEvent)
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

@Composable
fun HomeMiniTimer(state: HomeUiState, onClick: () -> Unit) {
    val timer = state.timer ?: return
    val book = state.books.firstOrNull { it.id == timer.bookId } ?: return
    val c = LocalJournalColors.current
    val compact = with(LocalDensity.current) { LocalWindowInfo.current.containerSize.height.toDp() } < 480.dp
    Surface(Modifier.fillMaxWidth().testTag("home-mini-timer").heightIn(min = Metric.timer)
        .clickable(role = Role.Button, onClickLabel = "К управлению демонстрационным таймером", onClick = onClick),
        color = c.accentSoft, border = BorderStroke(Metric.border, c.accent)) {
        val phase = when (timer.phase) { DemoTimerPhase.RUNNING -> "Чтение"; DemoTimerPhase.PAUSED -> "Пауза"; DemoTimerPhase.FINISHING -> "Завершение" }
        if (compact) Row(Modifier.padding(horizontal = Space.lg, vertical = Space.sm),
            horizontalArrangement = Arrangement.spacedBy(Space.sm), verticalAlignment = Alignment.CenterVertically) {
            Text(book.title, Modifier.weight(1f), style = JournalType.caption, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text("${timer.elapsed} · $phase · демо", style = JournalType.caption, color = c.onAccentSoft)
        } else Column(Modifier.padding(horizontal = Space.lg, vertical = Space.sm), verticalArrangement = Arrangement.spacedBy(Space.xs)) {
            Text(book.title, style = JournalType.card, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text("${timer.elapsed} · $phase · демо", style = JournalType.secondary,
                color = c.onAccentSoft)
        }
    }
}
