package ru.gleb.readingjournal

import androidx.compose.runtime.MonotonicFrameClock
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.ui.MotionDurationScale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.yield
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import ru.gleb.readingjournal.ui.designsystem.ChromeDriver
import ru.gleb.readingjournal.ui.designsystem.ChromeSample
import ru.gleb.readingjournal.ui.designsystem.ChromeTrack
import ru.gleb.readingjournal.ui.designsystem.classifyScroll
import ru.gleb.readingjournal.ui.designsystem.navigationIslandWidth
import ru.gleb.readingjournal.ui.designsystem.navigationSectorWidth
import ru.gleb.readingjournal.ui.designsystem.settleDurationMillis
import androidx.compose.ui.unit.dp

class ChromeMotionTest {
    @Test fun islandsStartVisible() {
        assertEquals(0f, ChromeTrack(100f).progress, 0.001f)
    }

    @Test fun consumedScrollMovesProgressAndDoesNotJumpToTheEnd() {
        val track = ChromeTrack(100f)
        assertEquals(0.25f, track.move(25f), 0.001f)
        assertEquals(0.25f, track.progress, 0.001f)
        assertEquals(0.40f, track.move(15f), 0.001f)
        assertTrue(track.progress < 1f)
        assertEquals(1f, track.move(1_000f), 0.001f)
        assertEquals(1f, track.move(50f), 0.001f)
    }

    @Test fun reverseStartsImmediatelyFromTheCurrentProgress() {
        val track = ChromeTrack(80f)
        track.move(60f)
        assertEquals(0.75f, track.progress, 0.001f)
        assertEquals(0.50f, track.move(-20f), 0.001f)
        assertEquals(0f, track.move(-1_000f), 0.001f)
    }

    @Test fun noiseDoesNotMoveAndTheSettleTargetIsTheNearestRest() {
        val track = ChromeTrack(100f)
        assertEquals(0f, track.move(0.2f), 0.001f)
        track.move(49f)
        assertEquals(0f, track.settleTarget(), 0.001f)
        track.move(2f)
        assertEquals(1f, track.settleTarget(), 0.001f)
        assertEquals(1, settleDurationMillis(0.2f, 0.2f))
        assertEquals(350, settleDurationMillis(0f, 1f))
        assertEquals(175, settleDurationMillis(0.5f, 0f))
    }

    @Test fun reachingTheStartShowsAndTheEndDoesNotHideByItself() {
        val hidden = ChromeTrack(100f)
        hidden.move(80f)
        assertEquals(0f, hidden.showCompletely(), 0.001f)
        val visible = ChromeTrack(100f)
        assertEquals(0f, visible.showCompletely(), 0.001f)
    }

    @Test fun scrollSamplesUseThePointerSignAndIgnoreHorizontalMotion() {
        assertEquals(ChromeSample.Moved(20f), classifyScroll(0f, -20f, 0f, 0f))
        assertEquals(ChromeSample.Moved(-12f), classifyScroll(0f, 12f, 0f, 0f))
        assertEquals(ChromeSample.OverscrollAtStart, classifyScroll(0f, 0f, 0f, 8f))
        assertEquals(ChromeSample.OverscrollAtStart, classifyScroll(0f, 12f, 0f, 6f))
        assertEquals(ChromeSample.OverscrollAtEnd, classifyScroll(0f, 0f, 0f, -8f))
        assertEquals(ChromeSample.Ignored, classifyScroll(30f, -4f, 0f, 0f))
        assertEquals(ChromeSample.Ignored, classifyScroll(0f, 0f, 20f, 4f))
        assertEquals(ChromeSample.Ignored, classifyScroll(0f, 0.2f, 0f, 0f))
        val track = ChromeTrack(100f)
        val towardEnd = (classifyScroll(0f, -40f, 0f, 0f) as ChromeSample.Moved).towardEnd
        assertEquals(0.4f, track.move(towardEnd), 0.001f)
        assertTrue(classifyScroll(40f, 2f, 0f, 0f) is ChromeSample.Ignored)
    }

    @Test fun fingerDrivesTheIslandsAndAHeldFingerDoesNotKeepAnimating() = runBlocking {
        val clock = ManualFrameClock()
        val scope = CoroutineScope(coroutineContext + Job(coroutineContext[Job]) + clock + RealMotion())
        try {
            val progress = mutableFloatStateOf(0f)
            val driver = ChromeDriver(ChromeTrack(100f), progress, scope)
            driver.pointerDown = true
            driver.onPointerDown()
            driver.onScroll(NestedScrollSource.UserInput, Offset(0f, -40f), Offset.Zero)
            assertEquals(0.4f, progress.floatValue, 0.001f)
            repeat(8) {
                clock.advance(16)
                yield()
            }
            assertEquals(0.4f, progress.floatValue, 0.001f)
            driver.onScroll(NestedScrollSource.UserInput, Offset(0f, 15f), Offset.Zero)
            assertEquals(0.25f, progress.floatValue, 0.001f)
        } finally {
            scope.cancel()
        }
    }

    @Test fun programmaticAndHorizontalSamplesDoNotHide() = runBlocking {
        val progress = mutableFloatStateOf(0f)
        val driver = ChromeDriver(ChromeTrack(100f), progress, this)
        driver.onScroll(NestedScrollSource.UserInput, Offset(0f, -80f), Offset.Zero)
        driver.onScroll(NestedScrollSource.SideEffect, Offset(0f, -80f), Offset.Zero)
        driver.pointerDown = true
        driver.onPointerDown()
        driver.onScroll(NestedScrollSource.UserInput, Offset(40f, -4f), Offset.Zero)
        assertEquals(0f, progress.floatValue, 0.001f)
    }

    @Test fun releaseSettlesToTheNearerEndAndANewFingerStopsIt() = runBlocking {
        val clock = ManualFrameClock()
        val scope = CoroutineScope(coroutineContext + Job(coroutineContext[Job]) + clock + RealMotion())
        try {
            val progress = mutableFloatStateOf(0f)
            val driver = ChromeDriver(ChromeTrack(100f), progress, scope)
            driver.pointerDown = true
            driver.onPointerDown()
            driver.onScroll(NestedScrollSource.UserInput, Offset(0f, -70f), Offset.Zero)
            assertEquals(0.7f, progress.floatValue, 0.001f)
            driver.onPreFling()
            driver.pointerDown = false
            driver.onPointerUp()
            driver.onPostFling()
            repeat(6) {
                clock.advance(16)
                yield()
            }
            val moving = progress.floatValue
            assertTrue("settle should have left 0.7 toward 1, was $moving", moving > 0.7f && moving < 1f)
            driver.pointerDown = true
            driver.onPointerDown()
            val stopped = progress.floatValue
            repeat(4) {
                clock.advance(16)
                yield()
            }
            assertEquals(stopped, progress.floatValue, 0.02f)
            driver.onScroll(NestedScrollSource.UserInput, Offset(0f, 20f), Offset.Zero)
            assertTrue(progress.floatValue < stopped)
        } finally {
            scope.cancel()
        }
    }

    @Test fun flingContinuesTheTravelAndOverscrollAtTheStartOpensTheIslands() = runBlocking {
        val progress = mutableFloatStateOf(0f)
        val driver = ChromeDriver(ChromeTrack(100f), progress, this)
        driver.pointerDown = true
        driver.onPointerDown()
        driver.onScroll(NestedScrollSource.UserInput, Offset(0f, -30f), Offset.Zero)
        driver.onPreFling()
        driver.pointerDown = false
        driver.onScroll(NestedScrollSource.SideEffect, Offset(0f, -30f), Offset.Zero)
        assertEquals(0.6f, progress.floatValue, 0.001f)
        driver.onScroll(NestedScrollSource.SideEffect, Offset(0f, 10f), Offset(0f, 8f))
        assertEquals(0f, progress.floatValue, 0.001f)
    }

    @Test fun disabledDurationScaleSettlesOnTheNextFrame() = runBlocking {
        val clock = ManualFrameClock()
        val scope = CoroutineScope(coroutineContext + Job(coroutineContext[Job]) + clock + ZeroMotion())
        try {
            val progress = mutableFloatStateOf(0f)
            val driver = ChromeDriver(ChromeTrack(100f), progress, scope)
            driver.pointerDown = true
            driver.onPointerDown()
            driver.onScroll(NestedScrollSource.UserInput, Offset(0f, -60f), Offset.Zero)
            driver.onPreFling()
            driver.pointerDown = false
            driver.onPostFling()
            repeat(3) {
                clock.advance(16)
                yield()
            }
            assertEquals(1f, progress.floatValue, 0.001f)
        } finally {
            scope.cancel()
        }
    }

    @Test fun navigationIslandPrefersSeventyTwoAndShrinksOnANarrowWindow() {
        assertEquals(72.dp, navigationSectorWidth(360.dp))
        assertEquals(72.dp, navigationSectorWidth(800.dp))
        assertEquals(240.dp, navigationIslandWidth(72.dp))
        val narrow = navigationSectorWidth(200.dp)
        assertEquals(48.dp, narrow)
        assertTrue(navigationSectorWidth(160.dp) < 48.dp)
    }

    private class ManualFrameClock : MonotonicFrameClock {
        private val frames = Channel<Long>(Channel.UNLIMITED)
        private var now = 0L
        suspend fun advance(millis: Long) {
            now += millis * 1_000_000L
            frames.send(now)
        }
        override suspend fun <R> withFrameNanos(onFrame: (frameTimeNanos: Long) -> R): R = onFrame(frames.receive())
    }

    private class ZeroMotion : MotionDurationScale {
        override val scaleFactor: Float = 0f
    }

    private class RealMotion : MotionDurationScale {
        override val scaleFactor: Float = 1f
    }
}
