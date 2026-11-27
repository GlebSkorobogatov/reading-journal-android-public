package ru.gleb.readingjournal.ui.designsystem

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Icon
import androidx.compose.ui.res.painterResource
import ru.gleb.readingjournal.R
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
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
                "Библиотека" -> libraryBooks().forEach { book ->
                    // A book lying flat. The outline is the cover; the line near the bottom
                    // is the page block facing the viewer. There is no spine.
                    drawRoundRect(color, Offset(book.left, book.top), Size(book.width, book.height),
                        CornerRadius(1.15f, 1.15f), style = outline)
                    drawLine(color, Offset(book.left + 2.1f, book.pageY), Offset(book.right - 2.1f, book.pageY),
                        strokeWidth = 1.5f, cap = StrokeCap.Round)
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

/** Three books stacked one above another, each wider than it is tall, with different horizontal offsets. */
internal data class LibraryBook(val left: Float, val top: Float, val right: Float, val bottom: Float) {
    val width: Float get() = right - left
    val height: Float get() = bottom - top
    /** Page block, in the lower part of the cover, not a spine. */
    val pageY: Float get() = bottom - 2.15f
}

internal fun libraryBooks(): List<LibraryBook> = listOf(
    LibraryBook(6.4f, 0.9f, 20.6f, 6.5f),
    LibraryBook(3.2f, 9.1f, 17.6f, 14.7f),
    LibraryBook(4.8f, 17.3f, 19.6f, 22.9f),
)
