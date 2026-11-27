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
import ru.gleb.readingjournal.ui.designsystem.animateChrome

class ChromeAnimationTest {
    @Test fun disabledDurationScaleFinishesOnTheNextFrame() = runBlocking {
        val clock = ManualFrameClock()
        val anim = Animatable(0.25f)
        val job = launch(clock + ZeroMotion()) { anim.animateChrome(1f, 250) }
        clock.advance(16)
        yield()
        withTimeout(2_000) { job.join() }
        assertEquals(1f, anim.value, 0.001f)
    }

    @Test fun transitionKeepsMovingUntilItsDurationAndReversesFromTheCurrentValue() = runBlocking {
        val clock = ManualFrameClock()
        val anim = Animatable(0f)
        val hide = launch(clock + RealMotion()) { anim.animateChrome(1f, 250) }
        repeat(8) {
            clock.advance(16)
            yield()
        }
        val mid = anim.value
        assertTrue("progress=$mid", mid > 0.55f && mid < 1f)
        hide.cancel()
        yield()
        assertEquals(mid, anim.value, 0.001f)
        val show = launch(clock + RealMotion()) { anim.animateChrome(0f, 250) }
        yield()
        assertEquals(mid, anim.value, 0.001f)
        repeat(20) {
            clock.advance(16)
            yield()
        }
        withTimeout(2_000) { show.join() }
        assertEquals(0f, anim.value, 0.001f)
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
