package ru.gleb.readingjournal.ui.overview

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import ru.gleb.readingjournal.ui.designsystem.*

const val ReadingSubtitle = "Не количество страниц, а глубина понимания."

@Composable
fun CurrentlyReading(
    state: CurrentlyReadingUi,
    scroll: LazyListState,
    onOpenBook: (BookId) -> Unit,
    onRecord: (BookId, CycleId) -> Unit,
    onTogglePin: (BookId) -> Unit,
    onMovePin: (BookId, Direction) -> Unit,
) {
    val chrome = LocalChromeObstruction.current
    LazyColumn(Modifier.fillMaxSize().testTag("reading-feed"), state = scroll,
        contentPadding = PaddingValues(start = Space.lg, end = Space.lg, top = chrome.top, bottom = chrome.bottom),
        verticalArrangement = Arrangement.spacedBy(Space.md)) {
        item(key = "subtitle") {
            Text(ReadingSubtitle, Modifier.testTag("reading-subtitle"), style = JournalType.secondary,
                color = LocalJournalColors.current.textSecondary)
        }
        if (state.books.isEmpty()) {
            item(key = "empty") {
                JournalEmpty("Нет активных книг", "Здесь появятся книги со статусом «Читаю».",
                    Modifier.fillMaxWidth().testTag("reading-empty"))
            }
        }
        items(state.books, key = { it.id.value }) { book ->
            ReadingCard(book, state.pins.indexOf(book.id), state.pins.lastIndex,
                onOpenBook = { onOpenBook(book.id) },
                onRecord = { onRecord(book.id, book.cycleId) },
                onTogglePin = { onTogglePin(book.id) },
                onMovePin = { direction -> onMovePin(book.id, direction) })
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ReadingCard(
    book: ReadingBook,
    pinIndex: Int,
    lastPin: Int,
    onOpenBook: () -> Unit,
    onRecord: () -> Unit,
    onTogglePin: () -> Unit,
    onMovePin: (Direction) -> Unit,
) {
    val colors = LocalJournalColors.current
    val id = book.id.value
    val pinned = pinIndex >= 0
    JournalCard(Modifier.fillMaxWidth().testTag("book-$id")) {
        if (pinned) Text("Закреплена · ${pinIndex + 1}", Modifier.testTag("pin-position-$id"),
            style = JournalType.caption, color = colors.accent)
        Row(horizontalArrangement = Arrangement.spacedBy(Space.md)) {
            JournalCover(book.title, book.author, id, Modifier.width(72.dp).testTag("cover-$id"),
                painter = book.cover?.let { painterResource(it) })
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Space.xs)) {
                Text(book.title, Modifier.semantics { heading() }, style = JournalType.card)
                Text(book.author, style = JournalType.secondary, color = colors.textSecondary)
                JournalStatus(book.status)
                Text("Закладка: ${book.bookmark} / ${book.totalPages} стр.",
                    Modifier.testTag("bookmark-$id"), style = JournalType.caption, color = colors.textSecondary)
            }
        }
        JournalProgress(book.reachedPage.toFloat() / book.totalPages, "Рубеж ${book.reachedPage} / ${book.totalPages} стр.",
            Modifier.testTag("reached-$id"))
        Text("Подтверждено в текущем цикле: ${book.confirmedPages} стр.",
            Modifier.testTag("confirmed-$id"), style = JournalType.caption, color = colors.textSecondary)
        JournalButton("Записать чтение", onRecord, Modifier.fillMaxWidth().testTag("record-$id"))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(Space.sm)) {
            JournalButton("Открыть книгу", onOpenBook, Modifier.testTag("open-$id"), kind = ButtonKind.TEXT)
            JournalButton(if (pinned) "Открепить" else "Закрепить", onTogglePin,
                Modifier.testTag("pin-$id"), kind = ButtonKind.TEXT)
            if (pinned && lastPin > 0) {
                JournalButton("Выше", { onMovePin(Direction.PREVIOUS) }, Modifier.testTag("pin-up-$id"),
                    enabled = pinIndex > 0, kind = ButtonKind.TEXT)
                JournalButton("Ниже", { onMovePin(Direction.NEXT) }, Modifier.testTag("pin-down-$id"),
                    enabled = pinIndex < lastPin, kind = ButtonKind.TEXT)
            }
        }
    }
}
