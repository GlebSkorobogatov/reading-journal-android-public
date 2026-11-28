package ru.gleb.readingjournal.ui.designsystem

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.tween
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Ease-in-out used only to finish a gesture at the nearer rest state.
 * Both control points sit on the 0 and 1 lines, so the curve never overshoots.
 * The islands' travel during the gesture is not this curve: it follows the scroll.
 */
internal val IslandEasing: Easing = CubicBezierEasing(0.42f, 0f, 0.58f, 1f)

/**
 * Hiding is the move off screen. Alpha stays at 1 until the island is almost gone,
 * then eases out so the last pixels of the shadow do not pop. It is not how the island disappears.
 */
internal fun islandAlpha(progress: Float): Float {
    val start = 0.92f
    if (progress <= start) return 1f
    val t = ((progress - start) / (1f - start)).coerceIn(0f, 1f)
    val smooth = t * t * (3f - 2f * t)
    return 1f - smooth
}

internal sealed class ChromeSample {
    data class Moved(val towardEnd: Float) : ChromeSample()
    data object OverscrollAtStart : ChromeSample()
    data object OverscrollAtEnd : ChromeSample()
    data object Ignored : ChromeSample()
}

/**
 * Classifies one nested-scroll sample.
 *
 * Compose reports the pointer delta: a finger moving up (toward the end of a vertical list)
 * has a negative y. Overscroll keeps that sign, so a leftover negative y is the end of the list
 * and a leftover positive y is the start. Horizontal motion is ignored so a pager swipe is not a hide.
 * Reaching the start while still pulling toward it restores the islands even if the accumulator
 * has not unwound, because the list cannot move any further.
 */
internal fun classifyScroll(consumedX: Float, consumedY: Float, availableX: Float, availableY: Float): ChromeSample {
    if (abs(consumedX) > abs(consumedY) && abs(consumedX) >= SAMPLE_EPSILON) return ChromeSample.Ignored
    val consumedVertical = abs(consumedY) >= SAMPLE_EPSILON
    if (!consumedVertical && abs(consumedX) < SAMPLE_EPSILON) {
        val vertical = abs(availableY) >= abs(availableX)
        return when {
            vertical && availableY > SAMPLE_EPSILON -> ChromeSample.OverscrollAtStart
            vertical && availableY < -SAMPLE_EPSILON -> ChromeSample.OverscrollAtEnd
            else -> ChromeSample.Ignored
        }
    }
    if (!consumedVertical) return ChromeSample.Ignored
    val hitStart = consumedY > 0f && availableY > SAMPLE_EPSILON && abs(availableY) >= abs(availableX)
    if (hitStart) return ChromeSample.OverscrollAtStart
    return ChromeSample.Moved(towardEnd = -consumedY)
}

/**
 * One continuous progress for every island on the screen.
 * 0 is fully visible, 1 is fully hidden. [hideDistancePx] of consumed vertical scroll covers that range.
 * The value follows the consumed delta directly: it stops when the finger stops and reverses with it.
 */
internal class ChromeTrack(val hideDistancePx: Float) {
    init {
        require(hideDistancePx > 0f)
    }

    var progress: Float = 0f
        private set

    fun move(towardEndPx: Float): Float {
        if (abs(towardEndPx) < SAMPLE_EPSILON) return progress
        progress = (progress + towardEndPx / hideDistancePx).coerceIn(0f, 1f)
        return progress
    }

    fun showCompletely(): Float {
        progress = 0f
        return progress
    }

    fun snap(value: Float): Float {
        progress = value.coerceIn(0f, 1f)
        return progress
    }

    /** Nearest rest position after the finger and its fling have finished. */
    fun settleTarget(): Float = if (progress >= 0.5f) 1f else 0f
}

/** Settle time shrinks with the distance still left, and never exceeds [fullMillis]. */
internal fun settleDurationMillis(from: Float, to: Float, fullMillis: Int = Motion.island): Int {
    val remaining = abs(to - from).coerceIn(0f, 1f)
    if (remaining <= 0.001f) return 1
    return (fullMillis * remaining).roundToInt().coerceIn(1, fullMillis)
}

internal suspend fun Animatable<Float, AnimationVector1D>.animateChrome(
    target: Float,
    durationMillis: Int,
    onFrame: (Float) -> Unit = {},
) {
    animateTo(target, tween(durationMillis = durationMillis, easing = IslandEasing)) {
        onFrame(value)
    }
}

internal const val SAMPLE_EPSILON = 0.5f
