package ru.gleb.readingjournal

import androidx.compose.animation.core.Animatable
import androidx.compose.runtime.MonotonicFrameClock
import androidx.compose.ui.MotionDurationScale
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.yield
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import ru.gleb.readingjournal.ui.designsystem.Motion
import ru.gleb.readingjournal.ui.designsystem.animateChrome
import ru.gleb.readingjournal.ui.designsystem.islandAlpha

class ChromeAnimationTest {
    @Test fun disabledDurationScaleFinishesOnTheNextFrame() = runBlocking {
        val clock = ManualFrameClock()
        val anim = Animatable(0.25f)
        val job = launch(clock + ZeroMotion()) { anim.animateChrome(1f, Motion.island) }
        clock.advance(16)
        yield()
        withTimeout(2_000) { job.join() }
        assertEquals(1f, anim.value, 0.001f)
    }

    @Test fun transitionKeepsMovingUntilItsDurationAndReversesFromTheCurrentValue() = runBlocking {
        val clock = ManualFrameClock()
        val anim = Animatable(0f)
        val hide = launch(clock + RealMotion()) { anim.animateChrome(1f, Motion.island) }
        repeat(2) {
            clock.advance(16)
            yield()
        }
        val early = anim.value
        assertTrue("soft start, progress=$early", early > 0f && early < 0.08f)
        repeat(9) {
            clock.advance(16)
            yield()
        }
        val mid = anim.value
        assertTrue("halfway, progress=$mid", mid > 0.35f && mid < 0.65f)
        assertTrue("still moving", mid < 1f)
        assertEquals(1f, islandAlpha(mid), 0.001f)
        hide.cancel()
        yield()
        assertEquals(mid, anim.value, 0.001f)
        val show = launch(clock + RealMotion()) { anim.animateChrome(0f, Motion.island) }
        yield()
        assertEquals(mid, anim.value, 0.001f)
        repeat(24) {
            clock.advance(16)
            yield()
        }
        withTimeout(2_000) { show.join() }
        assertEquals(0f, anim.value, 0.001f)
    }

    @Test fun hideIsMonotonicAndDoesNotOvershoot() = runBlocking {
        val clock = ManualFrameClock()
        val anim = Animatable(0f)
        val job = launch(clock + RealMotion()) { anim.animateChrome(1f, Motion.island) }
        var previous = 0f
        repeat(24) {
            clock.advance(16)
            yield()
            assertTrue("progress went backwards or past the end: ${anim.value}", anim.value + 0.001f >= previous && anim.value <= 1f)
            previous = anim.value
        }
        withTimeout(2_000) { job.join() }
        assertEquals(1f, anim.value, 0.001f)
    }

    @Test fun alphaFollowsTheMoveAndDoesNotVanishEarly() {
        assertEquals(1f, islandAlpha(0f), 0.001f)
        assertEquals(1f, islandAlpha(0.7f), 0.001f)
        assertEquals(1f, islandAlpha(0.92f), 0.001f)
        val fading = islandAlpha(0.96f)
        assertTrue("alpha=$fading", fading > 0.15f && fading < 0.85f)
        assertEquals(0f, islandAlpha(1f), 0.001f)
        var previous = 1f
        var progress = 0f
        while (progress <= 1f) {
            val alpha = islandAlpha(progress)
            assertTrue(alpha <= previous + 0.0001f)
            previous = alpha
            progress += 0.05f
        }
    }

    @Test fun islandTimingDoesNotReplaceTheSharedTransition() {
        assertEquals(350, Motion.island)
        assertEquals(250, Motion.transition)
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
