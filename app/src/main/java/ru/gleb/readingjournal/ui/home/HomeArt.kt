package ru.gleb.readingjournal.ui.home

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import ru.gleb.readingjournal.ui.designsystem.*

/** Original local drawing: an open book, a winding trail and a distant star. */
@Composable
fun HomeXpCard(xp: XpUi) {
    val c = LocalJournalColors.current
    JournalCard(Modifier.fillMaxWidth().testTag("home-xp"), large = true) {
        Text("Путь исследователя знаний", style = JournalType.caption, color = c.textSecondary)
        Canvas(Modifier.fillMaxWidth().height(132.dp).clearAndSetSemantics {}) {
            val w = size.width; val h = size.height
            drawCircle(c.accentSoft, h * .45f, Offset(w * .67f, h * .46f))
            val trail = Path().apply {
                moveTo(w * .7f, h * .8f); cubicTo(w * .97f, h * .52f, w * .41f, h * .53f, w * .7f, h * .17f)
            }
            drawPath(trail, c.outlineStrong, style = Stroke(1.5.dp.toPx()))
            val left = Path().apply {
                moveTo(w * .12f, h * .32f); quadraticTo(w * .29f, h * .25f, w * .46f, h * .4f)
                lineTo(w * .46f, h * .88f); quadraticTo(w * .29f, h * .73f, w * .12f, h * .8f); close()
            }
            val right = Path().apply {
                moveTo(w * .46f, h * .4f); quadraticTo(w * .57f, h * .24f, w * .72f, h * .3f)
                lineTo(w * .72f, h * .76f); quadraticTo(w * .57f, h * .71f, w * .46f, h * .88f); close()
            }
            drawPath(left, c.surfaceMuted); drawPath(right, c.surface)
            drawPath(left, c.accent, style = Stroke(1.5.dp.toPx())); drawPath(right, c.accent, style = Stroke(1.5.dp.toPx()))
            for (line in 0..3) {
                val y = h * (.45f + line * .075f)
                drawLine(c.outlineStrong, Offset(w * .18f, y), Offset(w * .37f, y + h * .015f), 1.dp.toPx())
                drawLine(c.outline, Offset(w * .53f, y + h * .02f), Offset(w * .65f, y - h * .01f), 1.dp.toPx())
            }
            drawLine(c.accent, Offset(w * .79f, h * .08f), Offset(w * .79f, h * .24f), 1.5.dp.toPx())
            drawLine(c.accent, Offset(w * .75f, h * .16f), Offset(w * .83f, h * .16f), 1.5.dp.toPx())
            drawCircle(c.accent, 2.dp.toPx(), Offset(w * .87f, h * .36f))
        }
        Text("Уровень ${xp.level} · ${xp.title}", Modifier.semantics { heading() }, style = JournalType.section)
        Text("${xp.total} XP за всё время", style = JournalType.secondary, color = c.textSecondary)
        JournalProgress(xp.inLevel.toFloat() / xp.nextCost, "До уровня ${xp.level + 1}")
        Text("${xp.inLevel} / ${xp.nextCost} XP · ещё ${xp.nextCost - xp.inLevel} XP", style = JournalType.card)
    }
}
