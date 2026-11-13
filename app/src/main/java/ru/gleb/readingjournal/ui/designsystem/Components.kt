package ru.gleb.readingjournal.ui.designsystem

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/** Stateless components; no repositories, activities or persistent data. */
enum class ButtonKind { PRIMARY, SECONDARY, DANGER, TEXT }

@Composable
fun JournalButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier,
    enabled: Boolean = true, kind: ButtonKind = ButtonKind.PRIMARY) {
    val c = LocalJournalColors.current
    var focused by remember { mutableStateOf(false) }
    val m = modifier.defaultMinSize(minWidth = Metric.touch, minHeight = Metric.touch)
        .onFocusChanged { focused = it.isFocused }
        .then(if (focused) Modifier.border(Metric.focusBorder, c.accent, JournalShape.control) else Modifier)
    val foreground = when (kind) {
        ButtonKind.PRIMARY -> c.onAccent
        ButtonKind.DANGER -> c.error
        else -> c.accent
    }
    Button(onClick, m, enabled, shape = JournalShape.control,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (kind == ButtonKind.PRIMARY) c.accent else if (kind == ButtonKind.TEXT) Color.Transparent else c.surface,
            contentColor = foreground, disabledContainerColor = c.surfaceMuted,
            disabledContentColor = c.disabledText),
        border = if (kind == ButtonKind.SECONDARY || kind == ButtonKind.DANGER)
            BorderStroke(Metric.border, if (kind == ButtonKind.DANGER) c.error else c.outlineStrong) else null,
        elevation = ButtonDefaults.buttonElevation(0.dp, 0.dp, 0.dp, 0.dp, 0.dp),
        contentPadding = PaddingValues(Space.lg)) {
        Text(text, style = JournalType.button)
    }
}

@Composable
fun JournalIconButton(label: String, onClick: () -> Unit, modifier: Modifier = Modifier,
    enabled: Boolean = true, icon: @Composable () -> Unit) {
    IconButton(onClick, modifier.sizeIn(minWidth = Metric.touch, minHeight = Metric.touch)
        .semantics { contentDescription = label }, enabled = enabled,
        colors = IconButtonDefaults.iconButtonColors(contentColor = LocalJournalColors.current.textPrimary,
            disabledContentColor = LocalJournalColors.current.disabledText), content = icon)
}

@Composable
fun JournalCard(modifier: Modifier = Modifier, large: Boolean = false,
    content: @Composable ColumnScope.() -> Unit) {
    val c = LocalJournalColors.current
    Surface(modifier, shape = if (large) JournalShape.largeCard else JournalShape.card,
        color = c.surface, border = BorderStroke(Metric.border, c.outline), shadowElevation = Metric.shadow) {
        Column(Modifier.padding(Space.lg), verticalArrangement = Arrangement.spacedBy(Space.md), content = content)
    }
}

enum class ReadingStatus(val label: String) {
    READING("Читаю"), WANTED("Хочу прочитать"), COMPLETE("Прочитано"), DEFERRED("Отложено")
}
@Composable
fun JournalStatus(status: ReadingStatus) {
    val c = LocalJournalColors.current
    val fg = when (status) {
        ReadingStatus.READING -> c.onAccentSoft; ReadingStatus.WANTED -> c.textSecondary
        ReadingStatus.COMPLETE -> c.success; ReadingStatus.DEFERRED -> c.warning
    }
    Surface(shape = JournalShape.capsule,
        color = if (status == ReadingStatus.READING) c.accentSoft else c.surfaceMuted) {
        Text(status.label, Modifier.padding(horizontal = Space.md, vertical = Space.xs),
            color = fg, style = JournalType.caption)
    }
}
@Composable
fun JournalChip(label: String, selected: Boolean, onClick: () -> Unit,
    modifier: Modifier = Modifier, enabled: Boolean = true) {
    val c = LocalJournalColors.current
    Surface(modifier.defaultMinSize(minHeight = Metric.touch, minWidth = Metric.touch)
        .selectable(selected, enabled = enabled, role = Role.Checkbox, onClick = onClick),
        shape = JournalShape.capsule, color = if (selected) c.accentSoft else c.surfaceMuted,
        border = if (selected) BorderStroke(Metric.border, c.accent) else null) {
        Box(Modifier.padding(Space.md), contentAlignment = Alignment.Center) {
            Text((if (selected) "✓ " else "") + label, style = JournalType.button,
                color = if (!enabled) c.disabledText else if (selected) c.onAccentSoft else c.textSecondary)
        }
    }
}
@Composable
fun JournalField(value: String, onValueChange: (String) -> Unit, label: String,
    modifier: Modifier = Modifier, errorMessage: String? = null, enabled: Boolean = true) {
    val c = LocalJournalColors.current
    OutlinedTextField(value, onValueChange, modifier.fillMaxWidth()
        .semantics { if (errorMessage != null) error(errorMessage) },
        enabled = enabled, label = { Text(label) }, textStyle = JournalType.body,
        isError = errorMessage != null, shape = JournalShape.control,
        supportingText = errorMessage?.let { { Text(it, style = JournalType.secondary) } },
        colors = OutlinedTextFieldDefaults.colors(unfocusedBorderColor = c.outlineStrong,
            focusedBorderColor = c.accent, disabledTextColor = c.disabledText,
            disabledBorderColor = c.outline, disabledLabelColor = c.disabledText,
            disabledPlaceholderColor = c.disabledText, disabledSupportingTextColor = c.disabledText,
            errorBorderColor = c.error))
}

@Composable
fun JournalProgress(value: Float?, description: String, modifier: Modifier = Modifier) {
    require(value == null || value in 0f..1f)
    val c = LocalJournalColors.current
    Column(modifier, verticalArrangement = Arrangement.spacedBy(Space.sm)) {
        Text(if (value == null) "— · $description: неизвестно" else "$description: ${(value * 100).toInt()}%",
            style = JournalType.secondary)
        // Unknown is a static track, never an indefinite loading animation or a false zero.
        Box(Modifier.fillMaxWidth().height(Metric.progress).clip(JournalShape.capsule)
            .background(c.progressTrack).semantics {
                if (value == null) stateDescription = "$description: значение неизвестно"
                else progressBarRangeInfo = ProgressBarRangeInfo(value, 0f..1f)
            }) {
            if (value != null) Box(Modifier.fillMaxWidth(value).fillMaxHeight().background(c.accent))
        }
    }
}
@Composable
fun JournalCover(title: String, author: String, stableKey: String, modifier: Modifier = Modifier,
    painter: Painter? = null) {
    val index = (stableKey.hashCode().toLong() and 0x7fffffff) % CoverColors.size
    Box(modifier.aspectRatio(Metric.coverRatio).clip(JournalShape.control)
        .background(CoverColors[index.toInt()]).semantics(mergeDescendants = true) {
            contentDescription = "Обложка: $title, $author" }, contentAlignment = Alignment.Center) {
        if (painter != null) Image(painter, null, Modifier.fillMaxSize(), contentScale = ContentScale.Fit)
        else Column(Modifier.verticalScroll(rememberScrollState()).padding(Space.md), verticalArrangement = Arrangement.spacedBy(Space.sm)) {
            Text(title, color = CoverText, style = JournalType.literary)
            Text(author, color = CoverText, style = JournalType.caption)
        }
    }
}
@Composable
fun JournalEmpty(title: String, explanation: String, modifier: Modifier = Modifier,
    centered: Boolean = false) {
    val textAlign = if (centered) TextAlign.Center else TextAlign.Start
    Column(modifier.padding(Space.lg), verticalArrangement = Arrangement.spacedBy(Space.md),
        horizontalAlignment = if (centered) Alignment.CenterHorizontally else Alignment.Start) {
        Text(title, Modifier.semantics { heading() }, style = JournalType.section, textAlign = textAlign)
        Text(explanation, style = JournalType.body, textAlign = textAlign)
    }
}
@Composable
fun JournalTopBar(title: String, action: @Composable RowScope.() -> Unit) {
    Row(Modifier.fillMaxWidth().statusBarsPadding().heightIn(min = Metric.topBar)
        .padding(horizontal = Space.lg), verticalAlignment = Alignment.CenterVertically) {
        Text(title, Modifier.weight(1f).semantics { heading() }, style = JournalType.section)
        action()
    }
}
@Composable
fun JournalBottomBar(labels: List<String>, selected: String, onSelect: (String) -> Unit,
    icon: @Composable (String) -> Unit) {
    val c = LocalJournalColors.current
    Row(Modifier.fillMaxWidth().navigationBarsPadding().selectableGroup()) {
        labels.forEach { label ->
            Column(Modifier.weight(1f).heightIn(min = Metric.navigation)
                .selectable(selected == label, role = Role.Tab, onClick = { onSelect(label) })
                .padding(horizontal = Space.xs, vertical = Space.sm),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(Space.xs)) {
                Box(Modifier.size(Metric.indicatorWidth, Metric.indicatorHeight)
                    .background(if (selected == label) c.accent else Color.Transparent))
                CompositionLocalProvider(LocalContentColor provides if (selected == label) c.accent else c.textSecondary) {
                    icon(label)
                    Text(label, style = JournalType.navigation)
                }
            }
        }
    }
}
@Composable
fun JournalDialog(title: String, text: String, onDismiss: () -> Unit) {
    AlertDialog(onDismissRequest = onDismiss, title = { Text(title, style = JournalType.section) },
        text = { Column(Modifier.verticalScroll(rememberScrollState())) { Text(text) } },
        confirmButton = { JournalButton("Закрыть", onDismiss, kind = ButtonKind.TEXT) },
        shape = JournalShape.largeCard, containerColor = LocalJournalColors.current.surface,
        tonalElevation = 0.dp)
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JournalSheet(onDismiss: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss, shape = JournalShape.sheet,
        containerColor = LocalJournalColors.current.surface, tonalElevation = 0.dp) {
        Column(Modifier.fillMaxWidth().imePadding().verticalScroll(rememberScrollState())
            .padding(Space.lg), verticalArrangement = Arrangement.spacedBy(Space.md)) {
            content()
            JournalButton("Закрыть панель", onDismiss, kind = ButtonKind.TEXT)
        }
    }
}
