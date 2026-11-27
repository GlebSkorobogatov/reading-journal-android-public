package ru.gleb.readingjournal.ui.overview

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.style.TextAlign
import ru.gleb.readingjournal.ui.designsystem.*
import java.text.NumberFormat
import java.util.Locale

private val destinations = listOf(
    StatisticsDestination.DYNAMICS, StatisticsDestination.CALENDAR, StatisticsDestination.RECORDS
)

@Composable
fun StatisticsOverview(
    state: StatisticsUi,
    scroll: ScrollState,
    onStepPeriod: (Direction) -> Unit,
    onOpen: (StatisticsDestination) -> Unit,
) {
    Column(Modifier.fillMaxSize().testTag("statistics-feed").verticalScroll(scroll).padding(Space.lg),
        verticalArrangement = Arrangement.spacedBy(Space.md)) {
        CompactXp(state.xp) { onOpen(StatisticsDestination.ACHIEVEMENTS) }
        OverviewMetrics(state.snapshot, state.streak)
        PeriodArrows(state.period, onStepPeriod)
        destinations.forEach { destination ->
            OverviewLink(destination) { onOpen(destination) }
        }
    }
}

@Composable
private fun CompactXp(xp: XpUi, onClick: () -> Unit) {
    val colors = LocalJournalColors.current
    OverviewCard(onClick, "xp-card", "Открыть достижения") {
        FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Space.md),
            verticalArrangement = Arrangement.spacedBy(Space.xs)) {
            Text("Уровень ${xp.level} · ${xp.title}", Modifier.semantics { heading() }, style = JournalType.card)
            Text("${formatCount(xp.total)} XP", Modifier.testTag("xp-total"),
                style = JournalType.secondary, color = colors.textSecondary)
        }
        JournalProgress(xp.inLevel.toFloat() / xp.nextCost, "До уровня ${xp.level + 1}")
        Text("${formatCount(xp.inLevel)} / ${formatCount(xp.nextCost)} XP",
            style = JournalType.caption, color = colors.textSecondary)
    }
}

@Composable
private fun OverviewMetrics(snapshot: PeriodSnapshot, streak: Int) {
    val colors = LocalJournalColors.current
    Surface(Modifier.fillMaxWidth().testTag("overview-metrics"), shape = JournalShape.card,
        color = colors.surface, border = BorderStroke(Metric.border, colors.outline)) {
        Column {
            Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
                MetricCell("Страницы", "Прочитанные страницы", snapshot.pages,
                    Modifier.weight(1f), "pages")
                VerticalDivider(Modifier.fillMaxHeight().testTag("overview-divider-pages"), Metric.border, colors.outline)
                MetricCell("Про\u00adчте\u00adния", "Завершённые прочтения", snapshot.completedCycles,
                    Modifier.weight(1f), "cycles")
                VerticalDivider(Modifier.fillMaxHeight().testTag("overview-divider-cycles"), Metric.border, colors.outline)
                MetricCell("Серия", "Текущая серия", QuantityUi(streak), Modifier.weight(1f), "streak")
            }
            val explanations = listOfNotNull(snapshot.pages.explanation, snapshot.completedCycles.explanation).distinct()
            if (explanations.isNotEmpty()) {
                HorizontalDivider(thickness = Metric.border, color = colors.outline)
                explanations.forEach { explanation ->
                    Text(explanation, Modifier.padding(horizontal = Space.md, vertical = Space.sm),
                        style = JournalType.caption, color = colors.textSecondary)
                }
            }
        }
    }
}

@Composable
private fun MetricCell(label: String, accessibleLabel: String, quantity: QuantityUi,
    modifier: Modifier, key: String) {
    val colors = LocalJournalColors.current
    val value = quantity.value?.let { formatCount(it) } ?: "—"
    val spokenValue = quantity.value?.let { count ->
        when (key) {
            "pages" -> "$value ${plural(count, "страница", "страницы", "страниц")}"
            "cycles" -> "$value ${plural(count, "завершённый цикл", "завершённых цикла", "завершённых циклов")}, включая перечитывания"
            else -> "$value ${plural(count, "день", "дня", "дней")}"
        }
    }
    Column(modifier.testTag("overview-$key").clearAndSetSemantics {
        contentDescription = accessibleLabel
        stateDescription = spokenValue ?: "Неизвестно. ${quantity.explanation.orEmpty()}"
    }.padding(horizontal = Space.xs, vertical = Space.md),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(Space.xs)) {
        // Use the same width for drawing and Compose's semantic remeasurement.
        Text(value, Modifier.fillMaxWidth().testTag("overview-$key-value"), textAlign = TextAlign.Center,
            style = JournalType.number)
        Text(label, Modifier.fillMaxWidth().testTag("overview-$key-label"), textAlign = TextAlign.Center,
            style = JournalType.caption)
        if (key == "streak" && quantity.value != null) {
            Text(plural(quantity.value, "день", "дня", "дней"), Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center, style = JournalType.caption, color = colors.textSecondary)
        }
    }
}

private fun plural(count: Int, one: String, few: String, many: String) = when {
    count % 100 in 11..14 -> many
    count % 10 == 1 -> one
    count % 10 in 2..4 -> few
    else -> many
}

@Composable
private fun PeriodArrows(period: Period, onStep: (Direction) -> Unit) {
    Row(Modifier.fillMaxWidth().testTag("period-selector"), verticalAlignment = Alignment.CenterVertically) {
        PeriodArrow(period, Direction.PREVIOUS, onStep)
        Text(period.label, Modifier.weight(1f).testTag("selected-period").semantics { liveRegion = LiveRegionMode.Polite },
            textAlign = TextAlign.Center, style = JournalType.card)
        PeriodArrow(period, Direction.NEXT, onStep)
    }
}

@Composable
private fun PeriodArrow(period: Period, direction: Direction, onStep: (Direction) -> Unit) {
    val adjacent = period.step(direction)
    val boundary = if (direction == Direction.PREVIOUS) "Первый период" else "Последний период"
    JournalIconButton(direction.label, { onStep(direction) },
        Modifier.testTag("period-${direction.name}").semantics { stateDescription = adjacent?.label ?: boundary },
        enabled = adjacent != null) {
        LineIcon(direction.label)
    }
}

@Composable
private fun OverviewLink(destination: StatisticsDestination, onClick: () -> Unit) {
    OverviewCard(onClick, "overview-${destination.route}", "Открыть: ${destination.title}") {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Space.md)) {
            Text(destination.title, Modifier.weight(1f), style = JournalType.card)
            LineIcon("Вперёд")
        }
    }
}

@Composable
private fun OverviewCard(onClick: () -> Unit, tag: String, label: String,
    content: @Composable ColumnScope.() -> Unit) {
    val colors = LocalJournalColors.current
    Surface(onClick = onClick, modifier = Modifier.fillMaxWidth().heightIn(min = Metric.touch).testTag(tag)
        .semantics { role = Role.Button; contentDescription = label },
        shape = JournalShape.card, color = colors.surface, border = BorderStroke(Metric.border, colors.outline)) {
        Column(Modifier.padding(horizontal = Space.lg, vertical = Space.md),
            verticalArrangement = Arrangement.spacedBy(Space.sm), content = content)
    }
}

/** Keep display grouping breakable on narrow cells, including legacy Android font scaling. */
private fun formatCount(value: Int): String =
    NumberFormat.getIntegerInstance(Locale.forLanguageTag("ru-RU")).format(value)
        .map { if (it.isWhitespace() || Character.isSpaceChar(it)) ' ' else it }.joinToString("")
