package ru.gleb.readingjournal.ui.designsystem

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import kotlin.math.abs

internal enum class ChromeTarget { VISIBLE, HIDDEN }

internal sealed class ChromeSample {
    data class Moved(val towardEnd: Float) : ChromeSample()
    data object OverscrollAtStart : ChromeSample()
    data object OverscrollAtEnd : ChromeSample()
    data object Ignored : ChromeSample()
}

/**
 * Classifies one nested-scroll sample.
 *
 * Compose reports the pointer delta here: a finger moving up (toward the end of a vertical list)
 * has a negative y. The list's own scroll offset uses the opposite sign. Overscroll keeps that
 * pointer sign, so a leftover negative y is the end of the list and a leftover positive y is the start.
 */
internal fun classifyScroll(consumedX: Float, consumedY: Float, availableX: Float, availableY: Float): ChromeSample {
    if (abs(consumedX) > abs(consumedY) && abs(consumedX) >= SAMPLE_EPSILON) return ChromeSample.Ignored
    if (abs(consumedY) < SAMPLE_EPSILON && abs(consumedX) < SAMPLE_EPSILON) {
        val vertical = abs(availableY) >= abs(availableX)
        return when {
            vertical && availableY > SAMPLE_EPSILON -> ChromeSample.OverscrollAtStart
            vertical && availableY < -SAMPLE_EPSILON -> ChromeSample.OverscrollAtEnd
            else -> ChromeSample.Ignored
        }
    }
    if (abs(consumedY) < SAMPLE_EPSILON) return ChromeSample.Ignored
    return ChromeSample.Moved(towardEnd = -consumedY)
}

/**
 * One visibility decision for every island on the current screen.
 * Progress itself lives in an Animatable so a reversal continues from the current value.
 * [slop] is the touch slop in pixels. Movement below it, and any opposite twitch, does not switch the target.
 */
internal class ChromeModel(val slop: Float) {
    var target: ChromeTarget = ChromeTarget.VISIBLE
        private set
    private var pending = 0f
    private var pendingTowardEnd = true

    fun accept(towardEnd: Float): ChromeTarget? {
        if (abs(towardEnd) < SAMPLE_EPSILON) return null
        val towardEndNow = towardEnd > 0f
        if (towardEndNow != pendingTowardEnd) {
            pending = 0f
            pendingTowardEnd = towardEndNow
        }
        val desired = if (towardEndNow) ChromeTarget.HIDDEN else ChromeTarget.VISIBLE
        if (desired == target) {
            pending = 0f
            return null
        }
        pending += abs(towardEnd)
        if (pending < slop) return null
        pending = 0f
        target = desired
        return desired
    }

    fun forceVisible(): ChromeTarget? {
        pending = 0f
        if (target == ChromeTarget.VISIBLE) return null
        target = ChromeTarget.VISIBLE
        return ChromeTarget.VISIBLE
    }

    fun reset() {
        pending = 0f
        pendingTowardEnd = true
        target = ChromeTarget.VISIBLE
    }
}

internal suspend fun Animatable<Float, AnimationVector1D>.animateChrome(target: Float, durationMillis: Int) {
    animateTo(target, tween(durationMillis = durationMillis, easing = FastOutSlowInEasing))
}

private const val SAMPLE_EPSILON = 0.5f
