package ru.gleb.readingjournal

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.MotionDurationScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import ru.gleb.readingjournal.ui.designsystem.JournalChrome
import ru.gleb.readingjournal.ui.designsystem.JournalTheme
import ru.gleb.readingjournal.ui.designsystem.chromeScroll

class FloatingChromeScaleTest {
    @get:Rule val rule = createComposeRule(effectContext = ZeroMotion())

    @Test fun disabledSystemAnimationsFinishOnTheNextFrame() {
        rule.mainClock.autoAdvance = false
        rule.setContent {
            JournalTheme {
                JournalChrome("Читаю сейчас", "Настройки", {}, listOf("Читаю сейчас", "Статистика", "Библиотека"),
                    "Читаю сейчас") {
                    LazyColumn(Modifier.chromeScroll().fillMaxSize().testTag("feed")) {
                        items(30) { index ->
                            Text("Строка $index", Modifier.height(56.dp).testTag("scale-$index"))
                        }
                    }
                }
            }
        }
        rule.mainClock.advanceTimeByFrame()
        // A slow drag past the hide distance, without a fling. A fling under a zero
        // duration scale can keep scheduling frames, and this test would never go idle.
        val distance = with(rule.density) { 140.dp.toPx() }
        rule.onNodeWithTag("feed").performTouchInput {
            down(center)
            moveBy(Offset(0f, -distance), delayMillis = 16)
            moveBy(Offset(0f, -1f), delayMillis = 400)
            up()
        }
        rule.mainClock.advanceTimeByFrame()
        val value = rule.onNodeWithTag("chrome-progress", useUnmergedTree = true)
            .fetchSemanticsNode().config[SemanticsProperties.StateDescription].toFloat()
        assertEquals(1f, value, 0.02f)
        rule.mainClock.autoAdvance = true
    }

    private class ZeroMotion : MotionDurationScale {
        override val scaleFactor: Float = 0f
    }
}
