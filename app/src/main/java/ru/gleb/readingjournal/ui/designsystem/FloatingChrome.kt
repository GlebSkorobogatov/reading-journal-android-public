package ru.gleb.readingjournal.ui.designsystem

import android.content.Context
import android.view.accessibility.AccessibilityManager
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableFloatState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.hideFromAccessibility
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.yield

/**
 * Space the scrollable content keeps clear at its start and end.
 * It is content padding, not a viewport inset, so items travel behind the islands.
 * It does not follow the hide progress, so the list does not jump.
 */
internal data class ChromeObstruction(val top: Dp = 0.dp, val bottom: Dp = 0.dp)

internal val LocalChromeObstruction = staticCompositionLocalOf { ChromeObstruction() }

/** Vertical lists call [Modifier.chromeScroll] so the pager's horizontal gesture is not observed here. */
internal val LocalChromeDriver = staticCompositionLocalOf<ChromeDriver?> { null }

/**
 * Preferred sector is 72 dp. Narrow windows shrink it, but not below 48 dp until that
 * would overflow the capsule. The island width is three sectors plus the 12 dp insets.
 */
internal fun navigationSectorWidth(containerWidth: Dp): Dp {
    if (!containerWidth.value.isFinite() || containerWidth <= 0.dp) return Metric.navSectorWidth
    val gutters = Space.lg * 2 + Metric.navIslandInset * 2
    val raw = ((containerWidth - gutters) / 3).coerceAtLeast(0.dp)
    return when {
        raw >= Metric.navSectorWidth -> Metric.navSectorWidth
        raw >= Metric.touch -> raw
        else -> raw
    }
}

internal fun navigationIslandWidth(sectorWidth: Dp): Dp = sectorWidth * 3 + Metric.navIslandInset * 2

@Composable
internal fun systemTouchExplorationEnabled(): Boolean {
    val context = LocalContext.current
    val manager = remember(context) {
        context.getSystemService(Context.ACCESSIBILITY_SERVICE) as? AccessibilityManager
    }
    var enabled by remember { mutableStateOf(manager?.isTouchExplorationEnabled == true) }
    DisposableEffect(manager) {
        if (manager == null) return@DisposableEffect onDispose {}
        val listener = AccessibilityManager.TouchExplorationStateChangeListener { enabled = it }
        manager.addTouchExplorationStateChangeListener(listener)
        enabled = manager.isTouchExplorationEnabled
        onDispose { manager.removeTouchExplorationStateChangeListener(listener) }
    }
    return enabled
}

/**
 * Observes the vertical scroller this modifier is attached to.
 * It does not wrap [androidx.compose.foundation.pager.HorizontalPager], and it consumes nothing,
 * so a horizontal child and the pager still receive their own gestures.
 * A finger is the pointer that is already down on the initial pass. Semantic ScrollBy is also
 * reported as user input in this Compose version, and it has no pointer down, so it is ignored.
 */
@Composable
internal fun Modifier.chromeScroll(): Modifier {
    val driver = LocalChromeDriver.current ?: return this
    val connection = remember(driver) { chromeConnection(driver) }
    return this
        .pointerInput(driver) {
            awaitPointerEventScope {
                while (true) {
                    val event = awaitPointerEvent(PointerEventPass.Initial)
                    val down = event.changes.any { it.pressed }
                    if (down && !driver.pointerDown) driver.onPointerDown()
                    else if (!down && driver.pointerDown) driver.onPointerUp()
                    driver.pointerDown = down
                }
            }
        }
        .nestedScroll(connection)
}

/**
 * Floating title, action and, on the three root tabs, the navigation island.
 * They share one progress: 0 is at rest, 1 is off screen. Progress is read in placement and draw,
 * not in this composition, so the page underneath is not recomposed on every scroll step.
 */
@Composable
internal fun JournalChrome(
    title: String,
    actionLabel: String,
    onAction: () -> Unit,
    tabs: List<String>? = null,
    selectedTab: String? = null,
    onTab: (String) -> Unit = {},
    autoHide: Boolean = true,
    /** Continuous page position. The terracotta line follows it; semantics use the nearest page. */
    selection: Float? = null,
    /** When a root transition settles on another page, bring the islands back. */
    settledTab: String? = null,
    content: @Composable () -> Unit,
) {
    val density = LocalDensity.current
    val colors = LocalJournalColors.current
    val hidePx = with(density) { Metric.islandHideDistance.toPx() }
    val progress = remember { mutableFloatStateOf(0f) }
    val track = remember(hidePx) { ChromeTrack(hidePx) }
    val scope = rememberCoroutineScope()
    val driver = remember(track, progress, scope) { ChromeDriver(track, progress, scope) }
    driver.autoHide = autoHide
    LaunchedEffect(autoHide) { if (!autoHide) driver.snapVisible() }
    LaunchedEffect(settledTab) { if (settledTab != null) driver.show() }
    val fullyHidden by remember { derivedStateOf { progress.floatValue >= 0.999f } }
    val statusPx = WindowInsets.statusBars.getTop(density)
    val navPx = WindowInsets.navigationBars.getBottom(density)
    val gapPx = with(density) { Space.sm.roundToPx() }
    var titleBandPx by remember {
        mutableIntStateOf(with(density) { (Metric.topBar + Space.sm).roundToPx() })
    }
    val tabsKey = tabs?.joinToString("\u0000")
    var bandPx by remember(tabsKey) {
        mutableIntStateOf(if (tabs == null) 0 else with(density) { (Metric.navSectorHeight + Space.lg).roundToPx() })
    }
    val topPx = statusPx + titleBandPx + gapPx
    val bottomPx = navPx + bandPx
    Box(Modifier.fillMaxSize().background(colors.background).testTag("chrome-root")) {
        Box(Modifier.fillMaxSize().imePadding()) {
            CompositionLocalProvider(
                LocalChromeObstruction provides ChromeObstruction(
                    top = with(density) { topPx.toDp() },
                    bottom = with(density) { bottomPx.toDp() },
                ),
                LocalChromeDriver provides driver,
            ) { content() }
        }
        ChromeProgressLabel(progress)
        Column(Modifier.fillMaxWidth().statusBarsPadding()
            .onSizeChanged { if (it.height > 0) titleBandPx = it.height }
            .padding(start = Space.lg, end = Space.lg, top = Space.sm)
            .islandMotion(-(statusPx + titleBandPx), progress, fullyHidden)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top) {
                BoxWithConstraints(Modifier.weight(1f)) {
                    Box(Modifier.widthIn(max = maxWidth).wrapContentWidth(Alignment.Start).testTag("chrome-title")) {
                        Surface(shape = JournalShape.islandCapsule, color = colors.surface,
                            border = BorderStroke(Metric.border, colors.outline),
                            shadowElevation = Metric.shadow) {
                            Text(title, Modifier.padding(horizontal = Space.lg, vertical = Space.md).semantics { heading() },
                                style = JournalType.section)
                        }
                    }
                }
                Spacer(Modifier.width(Space.sm))
                Box(Modifier.testTag("chrome-action").size(Metric.touch)
                    .clip(JournalShape.islandAction)
                    .then(if (fullyHidden) Modifier else Modifier
                        .semantics(mergeDescendants = true) { contentDescription = actionLabel }
                        .clickable(onClick = onAction))) {
                    Surface(Modifier.fillMaxSize(), shape = JournalShape.islandAction,
                        color = colors.surface,
                        border = BorderStroke(Metric.border, colors.outline),
                        shadowElevation = Metric.shadow) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { LineIcon(actionLabel) }
                    }
                }
            }
        }
        if (tabs != null) {
            BoxWithConstraints(Modifier.align(Alignment.BottomCenter).fillMaxWidth().navigationBarsPadding()) {
                val sector = navigationSectorWidth(maxWidth)
                val fallback = tabs.indexOf(selectedTab).coerceAtLeast(0)
                val position = (selection ?: fallback.toFloat()).coerceIn(0f, tabs.lastIndex.toFloat())
                val active = position.roundToInt().coerceIn(0, tabs.lastIndex)
                Box(Modifier.fillMaxWidth()
                    .onSizeChanged { if (it.height > 0) bandPx = it.height }
                    .padding(start = Space.lg, end = Space.lg, top = Space.sm, bottom = Space.lg)
                    .islandMotion(navPx + bandPx, progress, fullyHidden),
                    contentAlignment = Alignment.Center) {
                    Box(Modifier.wrapContentWidth().testTag("chrome-tabs")) {
                        Surface(shape = JournalShape.islandCapsule, color = colors.surface,
                            border = BorderStroke(Metric.border, colors.outline), shadowElevation = Metric.shadow) {
                            Box(Modifier.padding(horizontal = Metric.navIslandInset)) {
                                Box(Modifier.align(Alignment.TopStart)
                                    .offset(x = sector * position + (sector - Metric.indicatorWidth) / 2)
                                    .padding(top = Space.sm)
                                    .size(Metric.indicatorWidth, Metric.indicatorHeight)
                                    .background(colors.accent)
                                    .testTag("tab-indicator"))
                                Row(Modifier.selectableGroup()) {
                                    tabs.forEachIndexed { index, label ->
                                        val selected = index == active
                                        Box(Modifier.size(sector, Metric.navSectorHeight)
                                            .then(if (fullyHidden) Modifier else Modifier.selectable(
                                                selected = selected,
                                                role = Role.Tab,
                                                onClick = { onTab(label) },
                                            ).semantics { contentDescription = label }),
                                            contentAlignment = Alignment.Center) {
                                            CompositionLocalProvider(LocalContentColor provides
                                                if (selected) colors.accent else colors.textSecondary) {
                                                LineIcon(label)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ChromeProgressLabel(progress: MutableFloatState) {
    val value = progress.floatValue
    Box(Modifier.testTag("chrome-progress").semantics {
        stateDescription = String.format(Locale.US, "%.4f", value)
        hideFromAccessibility()
    })
}

/**
 * [distancePx] is the full travel, positive downward. Progress is read while placing and drawing
 * so the pager is not recomposed for every pixel of the gesture.
 * Fully hidden islands drop their click targets. The offset moves the rest with the drawing.
 */
private fun Modifier.islandMotion(distancePx: Int, progress: MutableFloatState, fullyHidden: Boolean): Modifier =
    offset { IntOffset(0, (distancePx * progress.floatValue).roundToInt()) }
        .graphicsLayer { alpha = islandAlpha(progress.floatValue) }
        .then(if (fullyHidden) Modifier.clearAndSetSemantics {}.focusProperties { canFocus = false } else Modifier)

private fun chromeConnection(driver: ChromeDriver) = object : NestedScrollConnection {
    override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset = Offset.Zero
    override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
        driver.onScroll(source, consumed, available)
        return Offset.Zero
    }
    override suspend fun onPreFling(available: Velocity): Velocity {
        driver.onPreFling()
        return Velocity.Zero
    }
    override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
        driver.onPostFling()
        return Velocity.Zero
    }
}

/**
 * Applies vertical scroll to one [ChromeTrack]. While the finger is down, progress is written
 * directly and no animation is running. One settle animation starts only after that gesture and
 * its fling, and a new finger cancels it before the next delta is applied.
 */
internal class ChromeDriver(
    private val track: ChromeTrack,
    private val progress: MutableFloatState,
    private val scope: CoroutineScope,
) {
    var pointerDown = false
    var autoHide = true

    init {
        track.snap(progress.floatValue)
    }

    private var userGesture = false
    private var flinging = false
    private var awaitingEnd = false
    private var releaseToken = 0
    private var epoch = 0
    private var job: Job? = null

    fun onPointerDown() {
        releaseToken++
        flinging = false
        awaitingEnd = true
        cancelSettle()
    }

    fun onPointerUp() {
        val token = releaseToken
        scope.launch {
            // The scrollable dispatches its fling on this same turn, before the next resume.
            yield()
            if (token != releaseToken || pointerDown || flinging || !awaitingEnd || !autoHide) return@launch
            awaitingEnd = false
            settle()
        }
    }

    fun onScroll(source: NestedScrollSource, consumed: Offset, available: Offset) {
        if (!autoHide) return
        if (source == NestedScrollSource.UserInput && !pointerDown) return
        val user = source == NestedScrollSource.UserInput && pointerDown
        val flingSample = source == NestedScrollSource.SideEffect && flinging
        if (!user && !flingSample) return
        when (val sample = classifyScroll(consumed.x, consumed.y, available.x, available.y)) {
            is ChromeSample.Moved -> {
                if (user) {
                    userGesture = true
                    flinging = false
                    cancelSettle()
                }
                awaitingEnd = true
                progress.floatValue = track.move(sample.towardEnd)
            }
            ChromeSample.OverscrollAtStart -> {
                if (user) {
                    userGesture = true
                    flinging = false
                    cancelSettle()
                }
                awaitingEnd = true
                progress.floatValue = track.showCompletely()
            }
            ChromeSample.OverscrollAtEnd, ChromeSample.Ignored -> Unit
        }
    }

    fun onPreFling() {
        flinging = userGesture
    }

    fun onPostFling() {
        // A new finger already down owns the progress; the fling that just ended must not pull it away.
        val shouldSettle = awaitingEnd && autoHide && !pointerDown
        flinging = false
        userGesture = false
        if (shouldSettle) {
            awaitingEnd = false
            settle()
        }
    }

    fun snapVisible() {
        releaseToken++
        awaitingEnd = false
        userGesture = false
        flinging = false
        cancelSettle()
        track.showCompletely()
        progress.floatValue = 0f
    }

    fun show() {
        // A finished page change wins over a list fling that is still reporting samples.
        releaseToken++
        flinging = false
        userGesture = false
        awaitingEnd = false
        if (track.progress <= 0.001f && progress.floatValue <= 0.001f) return
        animateTo(0f)
    }

    private fun settle() {
        animateTo(track.settleTarget())
    }

    private fun animateTo(target: Float) {
        val from = progress.floatValue.coerceIn(0f, 1f)
        track.snap(from)
        if (abs(from - target) <= 0.001f) {
            track.snap(target)
            progress.floatValue = target
            return
        }
        cancelSettle()
        val ticket = epoch
        job = scope.launch {
            val anim = Animatable(from)
            anim.updateBounds(0f, 1f)
            anim.animateChrome(target, settleDurationMillis(from, target)) { value ->
                if (ticket == epoch) {
                    track.snap(value)
                    progress.floatValue = value
                }
            }
            if (ticket == epoch) {
                track.snap(target)
                progress.floatValue = target
            }
        }
    }

    private fun cancelSettle() {
        epoch++
        job?.cancel()
        job = null
    }
}
