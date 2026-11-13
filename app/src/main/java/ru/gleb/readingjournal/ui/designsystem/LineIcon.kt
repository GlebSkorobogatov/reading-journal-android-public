package ru.gleb.readingjournal.ui.designsystem

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.material3.LocalContentColor
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
    Canvas(Modifier.size(Metric.icon)) {
        val unit = size.width / 24f
        val outline = Stroke(1.5f, cap = StrokeCap.Round, join = StrokeJoin.Round)
        scale(unit, unit, pivot = Offset.Zero) {
            when (name) {
                "Главная" -> drawPath(Path().apply {
                    moveTo(3f, 11f); lineTo(12f, 3f); lineTo(21f, 11f); lineTo(21f, 21f)
                    lineTo(15f, 21f); lineTo(15f, 14f); lineTo(9f, 14f); lineTo(9f, 21f)
                    lineTo(3f, 21f); close()
                }, color, style = outline)
                "Библиотека" -> {
                    // Upright book spines and a leaning volume: a collection, not a single page.
                    listOf(4f, 8f, 12f).forEach { x ->
                        drawLine(color, Offset(x, if (x == 12f) 3f else 5f), Offset(x, 20f),
                            strokeWidth = 1.5f, cap = StrokeCap.Round)
                    }
                    drawLine(color, Offset(16f, 4f), Offset(20f, 20f),
                        strokeWidth = 1.5f, cap = StrokeCap.Round)
                }
                "Достижения" -> {
                    // A round medal with two ribbon tails; no cup, stem or pedestal.
                    drawPath(Path().apply {
                        moveTo(8.5f, 12.7f); lineTo(7f, 21f); lineTo(12f, 18.5f)
                        lineTo(17f, 21f); lineTo(15.5f, 12.7f)
                    }, color, style = outline)
                    drawCircle(color, radius = 5.5f, center = Offset(12f, 8.5f), style = outline)
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
