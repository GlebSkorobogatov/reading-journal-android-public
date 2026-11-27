package ru.gleb.readingjournal.ui.designsystem

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Icon
import androidx.compose.ui.res.painterResource
import ru.gleb.readingjournal.R
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/** Original line drawings on one 24-unit grid; labels belong to the parent control. */
@Composable
fun LineIcon(name: String) {
    val color = LocalContentColor.current
    if (name == "Читаю сейчас") {
        Icon(painterResource(R.drawable.ic_closed_book), contentDescription = null,
            modifier = Modifier.size(Metric.icon), tint = color)
        return
    }
    Canvas(Modifier.size(Metric.icon)) {
        val unit = size.width / 24f
        val outline = Stroke(1.5f, cap = StrokeCap.Round, join = StrokeJoin.Round)
        scale(unit, unit, pivot = Offset.Zero) {
            when (name) {
                "Статистика" -> drawPath(Path().apply {
                    moveTo(4f, 4f); lineTo(4f, 20f); lineTo(21f, 20f)
                    moveTo(8f, 16f); lineTo(8f, 12f)
                    moveTo(13f, 16f); lineTo(13f, 8f)
                    moveTo(18f, 16f); lineTo(18f, 5f)
                }, color, style = outline)
                "Библиотека" -> {
                    val pages = Stroke(1.15f, cap = StrokeCap.Round)
                    libraryBooks().forEach { book ->
                        // Open at the right: the fore-edge faces the viewer. The left curve is the binding.
                        // The shorter inner lines are the page block, inset so the cover sticks out past them.
                        drawPath(libraryCover(book), color, style = outline)
                        book.pageYs.forEach { y ->
                            drawLine(color, Offset(book.pageLeft, y), Offset(book.pageRight, y),
                                strokeWidth = pages.width, cap = StrokeCap.Round)
                        }
                    }
                }
                "Вперёд", "Предыдущий период", "Следующий период" -> {
                    val backward = name == "Предыдущий период"
                    drawPath(Path().apply {
                        if (name != "Вперёд") {
                            moveTo(4f, 12f); lineTo(20f, 12f)
                        }
                        val tip = if (backward) 4f else 20f
                        val tail = if (backward) 10f else 14f
                        moveTo(tail, 6f); lineTo(tip, 12f); lineTo(tail, 18f)
                    }, color, style = outline)
                }
                "Назад" -> drawPath(Path().apply {
                    moveTo(15f, 4f); lineTo(7f, 12f); lineTo(15f, 20f)
                }, color, style = outline)
                else -> {
                    // Six equally spaced teeth, rounded joins, and a concentric centre hole.
                    val gear = Path()
                    repeat(6) { tooth ->
                        listOf(-22f to 7f, -12f to 9f, 12f to 9f, 22f to 7f)
                            .forEachIndexed { index, (offset, radius) ->
                                val angle = (tooth * 60f - 90f + offset) * PI.toFloat() / 180f
                                val x = 12f + cos(angle) * radius
                                val y = 12f + sin(angle) * radius
                                if (tooth == 0 && index == 0) gear.moveTo(x, y) else gear.lineTo(x, y)
                            }
                    }
                    gear.close()
                    drawPath(gear, color, style = outline)
                    drawCircle(color, radius = 3f, center = Offset(12f, 12f), style = outline)
                }
            }
        }
    }
}

/**
 * One book lying with its page edges toward the viewer.
 * [left] is the binding shoulder. [spine] is how far the binding curve's control point
 * sits to the left of that shoulder. The cover is open on the right; [pageLeft]..[pageRight]
 * is the page block, inset so both cover boards stick out past it.
 */
internal data class LibraryBook(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float,
    val spine: Float,
    val pageLeft: Float,
    val pageRight: Float,
) {
    val width: Float get() = right - left
    val height: Float get() = bottom - top
    val pageYs: List<Float> get() = listOf(top + height * 0.36f, top + height * 0.66f)
}

internal fun libraryCover(book: LibraryBook): Path = Path().apply {
    val shoulder = book.left + 1.6f
    val mid = (book.top + book.bottom) / 2f
    moveTo(book.right, book.top)
    lineTo(shoulder, book.top)
    quadraticTo(book.left - book.spine, mid, shoulder, book.bottom)
    lineTo(book.right - 0.45f, book.bottom)
}

internal fun libraryBooks(): List<LibraryBook> = listOf(
    LibraryBook(7.2f, 0.9f, 22.4f, 6.7f, spine = 1.7f, pageLeft = 10.5f, pageRight = 20.0f),
    LibraryBook(1.6f, 9.2f, 17.0f, 15.0f, spine = 1.15f, pageLeft = 4.9f, pageRight = 14.5f),
    LibraryBook(4.4f, 17.5f, 21.6f, 22.85f, spine = 1.7f, pageLeft = 7.7f, pageRight = 19.1f),
)
