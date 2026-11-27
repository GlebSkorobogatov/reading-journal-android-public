package ru.gleb.readingjournal.ui.designsystem

import android.content.Context
import android.view.accessibility.AccessibilityManager
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
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
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalViewConfiguration
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

/**
 * Space the scrollable content keeps clear at its start and end.
 * It is content padding, not a viewport inset, so items travel behind the islands.
 */
internal data class ChromeObstruction(val top: Dp = 0.dp, val bottom: Dp = 0.dp)

internal val LocalChromeObstruction = staticCompositionLocalOf { ChromeObstruction() }

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
 * Floating title, action and, on the three root tabs, the navigation island.
 * They share one progress: 0 is at rest, 1 is off screen. The scroll viewport is the whole screen.
 * Start and end clearance stay in the scroll content and do not follow the animation, so items do not jump.
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
    val slop = LocalViewConfiguration.current.touchSlop
    val model = remember(slop) { ChromeModel(slop) }
    val progress = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    val driver = remember(model) { ChromeDriver(model, progress, scope, Motion.island) }
    driver.autoHide = autoHide
    LaunchedEffect(autoHide) { if (!autoHide) driver.snapVisible() }
    LaunchedEffect(settledTab) { if (settledTab != null) driver.show() }
    val connection = remember(driver) { chromeConnection(driver) }
    val statusPx = WindowInsets.statusBars.getTop(density)
    val navPx = WindowInsets.navigationBars.getBottom(density)
    val gapPx = with(density) { Space.sm.roundToPx() }
    var titleBandPx by remember {
        mutableIntStateOf(with(density) { (Metric.topBar + Space.sm).roundToPx() })
    }
    val tabsKey = tabs?.joinToString("\u0000")
    var bandPx by remember(tabsKey) {
        mutableIntStateOf(if (tabs == null) 0 else with(density) { (Metric.navigation + Space.lg).roundToPx() })
    }
    val hidden = progress.value >= 1f
    val topPx = statusPx + titleBandPx + gapPx
    val bottomPx = navPx + bandPx
    val topTravel = -((statusPx + titleBandPx) * progress.value).roundToInt()
    val bottomTravel = ((navPx + bandPx) * progress.value).roundToInt()
    val alpha = islandAlpha(progress.value)
    Box(Modifier.fillMaxSize().testTag("chrome-root")) {
        Box(
            Modifier.fillMaxSize()
                .pointerInput(driver) {
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent(PointerEventPass.Initial)
                            driver.pointerDown = event.changes.any { it.pressed }
                        }
                    }
                }
                .nestedScroll(connection)
                .imePadding(),
        ) {
            CompositionLocalProvider(
                LocalChromeObstruction provides ChromeObstruction(
                    top = with(density) { topPx.toDp() },
                    bottom = with(density) { bottomPx.toDp() },
                ),
            ) { content() }
        }
        Box(Modifier.testTag("chrome-progress").semantics {
            stateDescription = String.format(Locale.US, "%.4f", progress.value)
            hideFromAccessibility()
        })
        Column(Modifier.fillMaxWidth().statusBarsPadding()
            .onSizeChanged { if (it.height > 0) titleBandPx = it.height }
            .padding(start = Space.lg, end = Space.lg, top = Space.sm)
            .islandTravel(topTravel, alpha, hidden)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top) {
                BoxWithConstraints(Modifier.weight(1f)) {
                    Box(Modifier.widthIn(max = maxWidth).wrapContentWidth(Alignment.Start).testTag("chrome-title")) {
                        Surface(shape = JournalShape.islandCapsule, color = LocalJournalColors.current.surface,
                            border = BorderStroke(Metric.border, LocalJournalColors.current.outline),
                            shadowElevation = Metric.shadow) {
                            Text(title, Modifier.padding(horizontal = Space.lg, vertical = Space.md).semantics { heading() },
                                style = JournalType.section)
                        }
                    }
                }
                Spacer(Modifier.width(Space.sm))
                Box(Modifier.testTag("chrome-action").size(Metric.touch)
                    .clip(JournalShape.islandAction)
                    .semantics(mergeDescendants = true) { contentDescription = actionLabel }
                    .clickable(enabled = !hidden, onClick = onAction)) {
                    Surface(Modifier.fillMaxSize(), shape = JournalShape.islandAction,
                        color = LocalJournalColors.current.surface,
                        border = BorderStroke(Metric.border, LocalJournalColors.current.outline),
                        shadowElevation = Metric.shadow) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { LineIcon(actionLabel) }
                    }
                }
            }
        }
        if (tabs != null) {
            val colors = LocalJournalColors.current
            Box(Modifier.align(Alignment.BottomCenter).fillMaxWidth().navigationBarsPadding()
                .onSizeChanged { if (it.height > 0) bandPx = it.height }
                .padding(start = Space.lg, end = Space.lg, top = Space.sm, bottom = Space.lg)
                .islandTravel(bottomTravel, alpha, hidden),
                contentAlignment = Alignment.Center) {
                val fallback = tabs.indexOf(selectedTab).coerceAtLeast(0)
                val position = (selection ?: fallback.toFloat()).coerceIn(0f, (tabs.lastIndex).toFloat())
                val active = position.roundToInt().coerceIn(0, tabs.lastIndex)
                Box(Modifier.wrapContentWidth().testTag("chrome-tabs")) {
                    Surface(shape = JournalShape.islandCapsule, color = colors.surface,
                        border = BorderStroke(Metric.border, colors.outline), shadowElevation = Metric.shadow) {
                        Box(Modifier.padding(horizontal = Space.sm, vertical = Space.xs)) {
                            // Drawn under the row so the line does not steal the touch target.
                            Box(Modifier.align(Alignment.TopStart)
                                .offset(x = Metric.touch * position + (Metric.touch - Metric.indicatorWidth) / 2)
                                .padding(top = Space.xs)
                                .size(Metric.indicatorWidth, Metric.indicatorHeight)
                                .background(colors.accent)
                                .testTag("tab-indicator"))
                            Row(Modifier.selectableGroup()) {
                                tabs.forEachIndexed { index, label ->
                                    val selected = index == active
                                    Box(Modifier.size(Metric.touch)
                                        .selectable(selected = selected, enabled = !hidden, role = Role.Tab,
                                            onClick = { if (!hidden) onTab(label) })
                                        .semantics { contentDescription = label },
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

private fun Modifier.islandTravel(travelPx: Int, alpha: Float, hidden: Boolean): Modifier = offset { IntOffset(0, travelPx) }
    .graphicsLayer { this.alpha = alpha }
    .then(if (hidden) Modifier.clearAndSetSemantics {}.focusProperties { canFocus = false } else Modifier)

private fun chromeConnection(driver: ChromeDriver) = object : NestedScrollConnection {
    override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset = Offset.Zero
    override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
        driver.onScroll(source, consumed, available)
        return Offset.Zero
    }
    override suspend fun onPreFling(available: Velocity): Velocity {
        driver.flinging = driver.userGesture
        return Velocity.Zero
    }
    override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
        driver.userGesture = false
        driver.flinging = false
        return Velocity.Zero
    }
}

internal class ChromeDriver(
    private val model: ChromeModel,
    private val progress: Animatable<Float, AnimationVector1D>,
    private val scope: CoroutineScope,
    private val durationMillis: Int,
) {
    var pointerDown = false
    var userGesture = false
    var flinging = false
    var autoHide = true
    private var job: Job? = null

    fun onScroll(source: NestedScrollSource, consumed: Offset, available: Offset) {
        if (!autoHide) return
        // Semantic ScrollBy is also UserInput in this Compose version. A finger is the pointer
        // that is already down on the initial pass; everything else is programmatic.
        if (source == NestedScrollSource.UserInput && !pointerDown) return
        if (source == NestedScrollSource.UserInput) userGesture = true
        val user = source == NestedScrollSource.UserInput && pointerDown
        val flingSample = source == NestedScrollSource.SideEffect && flinging && abs(consumed.y) >= model.slop / 2f
        val boundary = user || (source == NestedScrollSource.SideEffect && flinging)
        if (!user && !flingSample && !boundary) return
        when (val sample = classifyScroll(consumed.x, consumed.y, available.x, available.y)) {
            is ChromeSample.Moved -> if (user || flingSample) model.accept(sample.towardEnd)?.let(::animate)
            ChromeSample.OverscrollAtStart -> if (boundary) model.forceVisible()?.let(::animate)
            ChromeSample.OverscrollAtEnd, ChromeSample.Ignored -> Unit
        }
    }

    fun snapVisible() {
        model.reset()
        job?.cancel()
        job = scope.launch { progress.snapTo(0f) }
    }

    fun show() {
        model.forceVisible()?.let(::animate)
    }

    private fun animate(target: ChromeTarget) {
        val to = if (target == ChromeTarget.HIDDEN) 1f else 0f
        if (job?.isActive == true && progress.targetValue == to) return
        job?.cancel()
        job = scope.launch { progress.animateChrome(to, durationMillis) }
    }
}
