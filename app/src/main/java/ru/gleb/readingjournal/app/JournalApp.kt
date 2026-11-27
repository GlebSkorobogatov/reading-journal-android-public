package ru.gleb.readingjournal.app

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.activity.SystemBarStyle
import androidx.activity.BackEventCompat
import androidx.activity.compose.BackHandler
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.Modifier
import androidx.activity.compose.LocalActivity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
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
import ru.gleb.readingjournal.ui.designsystem.*

private enum class RootTab(val route: String, val title: String) {
    READING("CurrentlyReading", "Читаю сейчас"),
    STATISTICS("StatisticsOverview", "Статистика"),
    LIBRARY("Library", "Библиотека")
}

private val tabTitles = RootTab.entries.map { it.title }
private val tabRoutes = RootTab.entries.map { it.route }
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
        JournalShell(state, model::selectTheme, overview, demo::onAction)
    }
}
@Composable
private fun JournalShell(
    state: State<ShellState>,
    onTheme: (ThemeMode) -> Unit,
    overview: State<ReadingDemoUi>,
    onDemoAction: (DemoAction) -> Unit,
) {
    val nav = rememberNavController()
    val entry by nav.currentBackStackEntryAsState()
    val start = RootTab.READING.route
    val route = entry?.destination?.route ?: start
    // Preserve a secondary root's scroll state when Back returns to the start tab.
    BackHandler(route in tabRoutes && route != start) {
        nav.popBackStack(start, inclusive = false, saveState = true)
    }
    Surface(Modifier.fillMaxSize().testTag("theme-${LocalJournalDark.current}"), color = LocalJournalColors.current.background) {
        NavHost(nav, start, Modifier.fillMaxSize()
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
            // Navigation 2.10 has separate predictive defaults, including scaleOut(0.7).
            // Translate the opaque, full-size Settings page with the gesture; never shrink it.
            predictivePopEnterTransition = { _ ->
                if (isSettingsReturn()) EnterTransition.None else fadeIn(tween(Motion.transition))
            },
            predictivePopExitTransition = { edge ->
                if (isSettingsReturn()) slideOutHorizontally(tween(Motion.transition, easing = LinearEasing)) {
                    if (edge == BackEventCompat.EDGE_LEFT) it else -it
                } else fadeOut(tween(Motion.transition))
            }) {
            RootTab.entries.forEach { tab -> composable(tab.route) {
                ShellPage(tab.title, onAction = { nav.navigate("Настройки") { launchSingleTop = true } },
                    onTab = { label ->
                        val selected = RootTab.entries.first { it.title == label }
                        if (selected != tab) nav.navigate(selected.route) {
                            popUpTo(nav.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }) {
                    when (tab) {
                        RootTab.READING -> CurrentlyReading(
                            overview.value.reading, rememberLazyListState(),
                            onOpenBook = { bookId -> nav.navigate(bookPath(bookId)) { launchSingleTop = true } },
                            onRecord = { bookId, cycleId ->
                                nav.navigate(recordPath(bookId, cycleId)) { launchSingleTop = true }
                            },
                            onTogglePin = { onDemoAction(DemoAction.TogglePin(it)) },
                            onMovePin = { bookId, direction -> onDemoAction(DemoAction.MovePin(bookId, direction)) }
                        )
                        RootTab.STATISTICS -> StatisticsOverview(
                            overview.value.statistics, rememberScrollState(),
                            onStepPeriod = { onDemoAction(DemoAction.StepPeriod(it)) },
                            onOpen = { nav.navigate(it.route) { launchSingleTop = true } }
                        )
                        RootTab.LIBRARY -> FuturePage("Библиотека · UI-03")
                    }
                }
            } }
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
                    Column(Modifier.fillMaxSize().testTag("settings-content").verticalScroll(rememberScrollState())
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
                        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())
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
    initialState.destination.route == "Настройки" && targetState.destination.route in tabRoutes

/** Destination-owned chrome keeps outgoing Settings full-size throughout a pop. */
@Composable
private fun ShellPage(title: String, onAction: () -> Unit, onTab: ((String) -> Unit)? = null,
    content: @Composable () -> Unit) {
    JournalChrome(title, actionLabel = if (onTab != null) "Настройки" else "Назад", onAction = onAction,
        tabs = if (onTab != null) tabTitles else null, selectedTab = title,
        onTab = { onTab?.invoke(it) }, autoHide = !systemTouchExplorationEnabled(), content = content)
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
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState())
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
