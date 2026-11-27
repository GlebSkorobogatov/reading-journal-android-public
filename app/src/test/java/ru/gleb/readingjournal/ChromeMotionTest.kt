package ru.gleb.readingjournal

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import ru.gleb.readingjournal.ui.designsystem.ChromeModel
import ru.gleb.readingjournal.ui.designsystem.ChromeSample
import ru.gleb.readingjournal.ui.designsystem.ChromeTarget
import ru.gleb.readingjournal.ui.designsystem.classifyScroll

class ChromeMotionTest {
    @Test fun islandsStartVisible() {
        assertEquals(ChromeTarget.VISIBLE, ChromeModel(24f).target)
    }

    @Test fun movementBelowTheSlopDoesNotHide() {
        val model = ChromeModel(24f)
        assertNull(model.accept(10f))
        assertNull(model.accept(10f))
        assertEquals(ChromeTarget.VISIBLE, model.target)
        assertEquals(ChromeTarget.HIDDEN, model.accept(10f))
    }

    @Test fun reverseScrollShowsAndASmallOppositeTwitchDoesNot() {
        val model = ChromeModel(24f)
        assertEquals(ChromeTarget.HIDDEN, model.accept(30f))
        assertNull(model.accept(-8f))
        assertEquals(ChromeTarget.HIDDEN, model.target)
        assertEquals(ChromeTarget.VISIBLE, model.accept(-24f))
        assertNull(model.accept(8f))
        assertEquals(ChromeTarget.VISIBLE, model.target)
    }

    @Test fun changingDirectionBeforeTheSlopStartsAgain() {
        val model = ChromeModel(24f)
        assertNull(model.accept(20f))
        assertNull(model.accept(-20f))
        assertNull(model.accept(20f))
        assertEquals(ChromeTarget.VISIBLE, model.target)
        assertEquals(ChromeTarget.HIDDEN, model.accept(24f))
    }

    @Test fun overscrollAtTheStartShowsAndOverscrollAtTheEndDoesNotHide() {
        val hidden = ChromeModel(24f)
        hidden.accept(30f)
        assertEquals(ChromeTarget.VISIBLE, hidden.forceVisible())
        assertNull(hidden.forceVisible())
        val visible = ChromeModel(24f)
        assertNull(visible.forceVisible())
        assertEquals(ChromeTarget.VISIBLE, visible.target)
    }

    @Test fun scrollSamplesUseThePointerSignAndIgnoreHorizontalMotion() {
        assertEquals(ChromeSample.Moved(20f), classifyScroll(0f, -20f, 0f, 0f))
        assertEquals(ChromeSample.Moved(-12f), classifyScroll(0f, 12f, 0f, 0f))
        assertEquals(ChromeSample.OverscrollAtStart, classifyScroll(0f, 0f, 0f, 8f))
        assertEquals(ChromeSample.OverscrollAtEnd, classifyScroll(0f, 0f, 0f, -8f))
        assertEquals(ChromeSample.Ignored, classifyScroll(30f, -4f, 0f, 0f))
        assertEquals(ChromeSample.Ignored, classifyScroll(0f, 0f, 20f, 4f))
        assertEquals(ChromeSample.Ignored, classifyScroll(0f, 0.2f, 0f, 0f))
        val model = ChromeModel(16f)
        val towardEnd = (classifyScroll(0f, -20f, 0f, 0f) as ChromeSample.Moved).towardEnd
        assertEquals(ChromeTarget.HIDDEN, model.accept(towardEnd))
        assertTrue(classifyScroll(40f, 2f, 0f, 0f) is ChromeSample.Ignored)
    }
}
