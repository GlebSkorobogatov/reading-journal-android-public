package ru.gleb.readingjournal.app

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.activity.SystemBarStyle
import androidx.activity.BackEventCompat
import androidx.activity.compose.BackHandler
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.interaction.DragInteraction
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerDefaults
import androidx.compose.foundation.pager.PagerSnapDistance
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.Modifier
import androidx.activity.compose.LocalActivity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavType
import androidx.navigation.navArgument
import android.net.Uri
import androidx.navigation.compose.*
import ru.gleb.readingjournal.BuildConfig
import ru.gleb.readingjournal.demo.ComponentCatalog
import ru.gleb.readingjournal.demo.DemoAction
import ru.gleb.readingjournal.demo.DemoScenario
import ru.gleb.readingjournal.demo.ReadingDemoUi
import ru.gleb.readingjournal.demo.ReadingDemoViewModel
import ru.gleb.readingjournal.ui.overview.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import ru.gleb.readingjournal.ui.designsystem.*

private enum class RootTab(val route: String, val title: String) {
    READING("CurrentlyReading", "Читаю сейчас"),
    STATISTICS("StatisticsOverview", "Статистика"),
    LIBRARY("Library", "Библиотека")
}

private val tabTitles = RootTab.entries.map { it.title }
private const val RootRoute = "Root"
private const val BookRoute = "Book/{bookId}"
private const val RecordRoute = "RecordReading/{bookId}/{cycleId}"

@Composable
fun JournalApp(model: ShellViewModel = viewModel(), demo: ReadingDemoViewModel = viewModel()) {
    val state = model.state.collectAsStateWithLifecycle()
    val overview = demo.state.collectAsStateWithLifecycle()
    JournalTheme(state.value.theme) {
        val dark = LocalJournalDark.current
        val activity = LocalActivity.current as? androidx.activity.ComponentActivity
        SideEffect {
            activity?.enableEdgeToEdge(
                statusBarStyle = if (dark) SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
                    else SystemBarStyle.light(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT),
                navigationBarStyle = if (dark) SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
                    else SystemBarStyle.light(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT))
        }
        JournalShell(state, model::selectTheme, overview, demo::onAction, model)
    }
}
@Composable
private fun JournalShell(
    state: State<ShellState>,
    onTheme: (ThemeMode) -> Unit,
    overview: State<ReadingDemoUi>,
    onDemoAction: (DemoAction) -> Unit,
    shell: ShellViewModel,
) {
    val nav = rememberNavController()
    Surface(Modifier.fillMaxSize().testTag("theme-${LocalJournalDark.current}"), color = LocalJournalColors.current.background) {
        // Navigation 2.10 draws the forward destination above the previous one, and on a pop
        // keeps the outgoing destination above (its z is not lowered). Settings therefore slides
        // over the root. Each destination paints its own opaque background in JournalChrome;
        // without that, the root shows through the sliding sheet and the two chromes mix.
        // Record and the other children keep the crossfade. Predictive Back seeks the same slide
        // and must not use the platform scaleOut.
        NavHost(nav, RootRoute, Modifier.fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal)).clipToBounds(),
            enterTransition = {
                if (targetState.destination.route == "Настройки") slideInHorizontally(tween(Motion.transition)) { it }
                else fadeIn(tween(Motion.transition))
            },
            exitTransition = {
                if (targetState.destination.route == "Настройки") ExitTransition.None
                else fadeOut(tween(Motion.transition))
            },
            popEnterTransition = {
                if (isSettingsReturn()) EnterTransition.None else fadeIn(tween(Motion.transition))
            },
            popExitTransition = {
                if (isSettingsReturn()) slideOutHorizontally(tween(Motion.transition)) { it }
                else fadeOut(tween(Motion.transition))
            },
            predictivePopEnterTransition = { _ ->
                if (isSettingsReturn()) EnterTransition.None else fadeIn(tween(Motion.transition))
            },
            predictivePopExitTransition = { edge ->
                if (isSettingsReturn()) slideOutHorizontally(tween(Motion.transition, easing = LinearEasing)) {
                    if (edge == BackEventCompat.EDGE_LEFT) it else -it
                } else fadeOut(tween(Motion.transition))
            }) {
            composable(RootRoute) { RootPager(nav, overview, onDemoAction, shell) }
            StatisticsDestination.entries.forEach { destination -> composable(destination.route) {
                ShellPage(destination.title, onAction = { nav.popBackStack() }) {
                    val context = when (destination) {
                        StatisticsDestination.DYNAMICS, StatisticsDestination.RECORDS ->
                            "Период обзора: ${overview.value.statistics.period.label}"
                        else -> null
                    }
                    FuturePage("Экран в разработке", context)
                }
            } }
            composable(BookRoute, arguments = listOf(navArgument("bookId") { type = NavType.StringType })) { page ->
                val bookId = BookId(requireNotNull(page.arguments?.getString("bookId")))
                val book = overview.value.reading.books.firstOrNull { it.id == bookId }
                ShellPage("Книга", onAction = { nav.popBackStack() }) {
                    FuturePage(book?.title ?: "Книга недоступна", book?.author)
                }
            }
            composable(RecordRoute, arguments = listOf(
                navArgument("bookId") { type = NavType.StringType },
                navArgument("cycleId") { type = NavType.StringType }
            )) { page ->
                val bookId = BookId(requireNotNull(page.arguments?.getString("bookId")))
                val cycleId = CycleId(requireNotNull(page.arguments?.getString("cycleId")))
                val book = overview.value.reading.books.firstOrNull { it.id == bookId && it.cycleId == cycleId }
                ShellPage("Записать чтение", onAction = { nav.popBackStack() }) {
                    Box(Modifier.fillMaxSize().testTag("record-destination-${bookId.value}-${cycleId.value}")) {
                        FuturePage(book?.title ?: "Цикл недоступен", book?.author)
                    }
                }
            }
            composable("Настройки") {
                ShellPage("Настройки", onAction = { nav.popBackStack() }) {
                    val chrome = LocalChromeObstruction.current
                    Column(Modifier.chromeScroll().fillMaxSize().testTag("settings-content").verticalScroll(rememberScrollState())
                        .padding(start = Space.lg, end = Space.lg, bottom = chrome.bottom + Space.lg,
                            top = chrome.top + Space.lg),
                        verticalArrangement = Arrangement.spacedBy(Space.lg)) {
                        Text("Демонстрационные настройки UI-01. Выбор темы не сохраняется после завершения процесса.")
                        Text("Тема", Modifier.semantics { heading() }, style = JournalType.section)
                        ThemeMode.entries.forEach { mode ->
                            JournalChip(mode.label, state.value.theme == mode, { onTheme(mode) }, Modifier.fillMaxWidth())
                        }
                        if (BuildConfig.DEBUG) {
                            JournalButton("Каталог компонентов · debug", {
                                nav.navigate("Компоненты") { launchSingleTop = true }
                            })
                            JournalButton("Наборы демонстрации · debug", {
                                nav.navigate("DemoScenarios") { launchSingleTop = true }
                            })
                        }
                    }
                }
            }
            if (BuildConfig.DEBUG) {
                composable("Компоненты") {
                    ShellPage("Компоненты", onAction = { nav.popBackStack() }) { ComponentCatalog() }
                }
                composable("DemoScenarios") {
                    ShellPage("Наборы демонстрации", onAction = { nav.popBackStack() }) {
                        val chrome = LocalChromeObstruction.current
                        Column(Modifier.chromeScroll().fillMaxSize().verticalScroll(rememberScrollState())
                            .padding(start = Space.lg, end = Space.lg, bottom = chrome.bottom + Space.lg,
                                top = chrome.top + Space.lg),
                            verticalArrangement = Arrangement.spacedBy(Space.md)) {
                            DemoScenario.entries.forEach { scenario ->
                                JournalChip(scenario.title, overview.value.scenario == scenario,
                                    { onDemoAction(DemoAction.SelectScenario(scenario)) }, Modifier.fillMaxWidth())
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun AnimatedContentTransitionScope<NavBackStackEntry>.isSettingsReturn() =
    initialState.destination.route == "Настройки" && targetState.destination.route == RootRoute

/**
 * One chrome and one pager. The pager still performs the gesture. The selected page is kept in
 * [ShellViewModel.rootPage], not in the pager's own saver: that saver also stores the fractional
 * offset, and a second configuration change can persist the pair before it has settled, so the
 * next layout snaps onto the neighbouring page. The pager is created on the saved page with no
 * offset, and a later gesture moves it from there. A tab or Back stores its page before the
 * animation, so a finger that is still settling cannot put the previous page back.
 * Scroll positions live beside the pager, so leaving a page or opening a child does not drop them.
 * HorizontalPager performs the gesture; a child that can scroll horizontally still receives it first.
 */
@Composable
private fun RootPager(
    nav: androidx.navigation.NavHostController,
    overview: State<ReadingDemoUi>,
    onDemoAction: (DemoAction) -> Unit,
    shell: ShellViewModel,
) {
    val pageCount = RootTab.entries.size
    val pager = remember {
        RootPagerState(shell.rootPage.coerceIn(0, pageCount - 1)) { pageCount }
    }
    // A tab or Back chooses the page immediately. The settling finger must not write the page it
    // was leaving over that choice. The epoch is not a clock: it only orders those two writers.
    val choiceEpoch = remember { intArrayOf(0) }
    fun choosePage(page: Int) {
        choiceEpoch[0] = choiceEpoch[0] + 1
        shell.rootPage = page
    }
    LaunchedEffect(pager) {
        pager.interactionSource.interactions.collect { interaction ->
            if (interaction is DragInteraction.Stop || interaction is DragInteraction.Cancel) {
                val epoch = choiceEpoch[0]
                snapshotFlow { pager.isScrollInProgress }.first { !it }
                shell.rootPage = rootPageCommittedByGesture(
                    epoch, choiceEpoch[0], pager.currentPage, shell.rootPage,
                )
            }
        }
    }
    val scope = rememberCoroutineScope()
    val readingScroll = rememberLazyListState()
    val statisticsScroll = rememberScrollState()
    // No bounce, and the release keeps the finger's velocity. A fixed-duration tween ignored it,
    // so a short flick and a long drag finished in the same 350 ms and felt stuck.
    val pageSpring = remember {
        spring<Float>(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow)
    }
    val awayFromStart by remember { derivedStateOf { pager.currentPage != 0 || pager.settledPage != 0 } }
    val index by remember { derivedStateOf { pager.currentPage.coerceIn(0, RootTab.entries.lastIndex) } }
    val settled by remember { derivedStateOf { pager.settledPage.coerceIn(0, RootTab.entries.lastIndex) } }
    val position by remember { derivedStateOf { pager.currentPage + pager.currentPageOffsetFraction } }
    BackHandler(awayFromStart) {
        choosePage(0)
        scope.launch { pager.animateScrollToPage(0, animationSpec = pageSpring) }
    }
    ShellPage(
        title = RootTab.entries[index].title,
        onAction = { nav.navigate("Настройки") { launchSingleTop = true } },
        onTab = { label ->
            val target = RootTab.entries.indexOfFirst { it.title == label }
            if (target >= 0) {
                choosePage(target)
                scope.launch { pager.animateScrollToPage(target, animationSpec = pageSpring) }
            }
        },
        selection = position,
        settledTab = RootTab.entries[settled].title,
    ) {
        HorizontalPager(
            state = pager,
            modifier = Modifier.fillMaxSize().testTag("root-pager"),
            beyondViewportPageCount = 1,
            flingBehavior = PagerDefaults.flingBehavior(
                state = pager,
                pagerSnapDistance = PagerSnapDistance.atMost(1),
                snapAnimationSpec = pageSpring,
                snapPositionalThreshold = 0.5f,
            ),
            key = { RootTab.entries[it].route },
        ) { page ->
            when (RootTab.entries[page]) {
                RootTab.READING -> CurrentlyReading(
                    overview.value.reading, readingScroll,
                    onOpenBook = { bookId -> nav.navigate(bookPath(bookId)) { launchSingleTop = true } },
                    onRecord = { bookId, cycleId ->
                        nav.navigate(recordPath(bookId, cycleId)) { launchSingleTop = true }
                    },
                    onTogglePin = { onDemoAction(DemoAction.TogglePin(it)) },
                    onMovePin = { bookId, direction -> onDemoAction(DemoAction.MovePin(bookId, direction)) },
                )
                RootTab.STATISTICS -> StatisticsOverview(
                    overview.value.statistics, statisticsScroll,
                    onStepPeriod = { onDemoAction(DemoAction.StepPeriod(it)) },
                    onOpen = { nav.navigate(it.route) { launchSingleTop = true } },
                )
                RootTab.LIBRARY -> FuturePage("Библиотека · UI-03")
            }
        }
    }
}

/** Destination-owned chrome keeps outgoing Settings full-size throughout a pop. */
@Composable
private fun ShellPage(title: String, onAction: () -> Unit, onTab: ((String) -> Unit)? = null,
    selection: Float? = null, settledTab: String? = null, content: @Composable () -> Unit) {
    JournalChrome(title, actionLabel = if (onTab != null) "Настройки" else "Назад", onAction = onAction,
        tabs = if (onTab != null) tabTitles else null, selectedTab = title, onTab = { onTab?.invoke(it) },
        autoHide = !systemTouchExplorationEnabled(), selection = selection, settledTab = settledTab,
        content = content)
}


private fun bookPath(bookId: BookId) = "Book/${Uri.encode(bookId.value)}"
private fun recordPath(bookId: BookId, cycleId: CycleId) =
    "RecordReading/${Uri.encode(bookId.value)}/${Uri.encode(cycleId.value)}"

@Composable
private fun FuturePage(title: String, context: String? = null) {
    val chrome = LocalChromeObstruction.current
    BoxWithConstraints(Modifier.fillMaxSize().padding(top = chrome.top, bottom = chrome.bottom)
        .testTag("placeholder-viewport")) {
        val viewportHeight = maxHeight
        Column(Modifier.chromeScroll().fillMaxWidth().verticalScroll(rememberScrollState())
            .heightIn(min = viewportHeight).padding(Space.lg),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally) {
            val explanation = listOfNotNull(
                context, "Демонстрационная заглушка. Полноценный экран ещё не реализован."
            ).joinToString("\n\n")
            JournalEmpty(title, explanation,
                Modifier.fillMaxWidth().testTag("placeholder-content"), centered = true)
        }
    }
}

/**
 * A pager that is not saved by itself. [ShellViewModel.rootPage] is the selected page, and this
 * state is created there with a zero offset so a configuration change cannot snap to a neighbour.
 * [pageCount] is snapshot state, the same contract as the foundation pager: a plain getter would
 * not be observed when the layout measures.
 */
private class RootPagerState(
    initialPage: Int,
    pageCount: () -> Int,
) : PagerState(initialPage, 0f) {
    private val pageCountState = mutableStateOf(pageCount)
    override val pageCount: Int get() = pageCountState.value.invoke()
}

/**
 * The page a released finger may store. A tab or Back that happened while the finger was settling
 * already stored its own page, and that choice wins. Otherwise the page the pager settled on is
 * the new one.
 */
internal fun rootPageCommittedByGesture(
    epochAtRelease: Int,
    epochNow: Int,
    settledPage: Int,
    chosenPage: Int,
): Int = if (epochAtRelease == epochNow) settledPage else chosenPage
