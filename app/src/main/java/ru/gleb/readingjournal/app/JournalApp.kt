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
import androidx.navigation.compose.*
import ru.gleb.readingjournal.BuildConfig
import ru.gleb.readingjournal.demo.ComponentCatalog
import ru.gleb.readingjournal.ui.designsystem.*

private val tabs = listOf("Главная", "Библиотека", "Достижения")
@Composable
fun JournalApp(model: ShellViewModel = viewModel()) {
    val state by model.state.collectAsStateWithLifecycle()
    JournalTheme(state.theme) {
        val dark = LocalJournalDark.current
        val activity = LocalActivity.current as? androidx.activity.ComponentActivity
        SideEffect {
            activity?.enableEdgeToEdge(
                statusBarStyle = if (dark) SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
                    else SystemBarStyle.light(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT),
                navigationBarStyle = if (dark) SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
                    else SystemBarStyle.light(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT))
        }
        JournalShell(state, model::selectTheme)
    }
}
@Composable
fun JournalShell(state: ShellState, onTheme: (ThemeMode) -> Unit) {
    val nav = rememberNavController()
    val entry by nav.currentBackStackEntryAsState()
    val route = entry?.destination?.route ?: tabs.first()
    val isTab = route in tabs
    // Back from a secondary root tab returns Home; Home delegates to Android.
    BackHandler(isTab && route != tabs.first()) { nav.popBackStack(tabs.first(), false) }
    Surface(Modifier.fillMaxSize().testTag("theme-${LocalJournalDark.current}"), color = LocalJournalColors.current.background) {
        NavHost(nav, tabs.first(), Modifier.fillMaxSize()
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
            tabs.forEach { tab -> composable(tab) {
                ShellPage(tab, onAction = { nav.navigate("Настройки") { launchSingleTop = true } },
                    onTab = { selected ->
                        nav.navigate(selected) {
                            popUpTo(nav.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }) {
                    BoxWithConstraints(Modifier.fillMaxSize().testTag("placeholder-viewport")) {
                        val viewportHeight = maxHeight
                        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState())
                            .heightIn(min = viewportHeight).padding(Space.lg),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally) {
                            JournalEmpty("$tab · UI-01", "Демонстрационная заглушка. Полноценный экран ещё не реализован.",
                                Modifier.fillMaxWidth().testTag("placeholder-content"), centered = true)
                        }
                    }
                }
            } }
            composable("Настройки") {
                ShellPage("Настройки", onAction = { nav.popBackStack() }) {
                    Column(Modifier.fillMaxSize().testTag("settings-content").navigationBarsPadding()
                        .verticalScroll(rememberScrollState()).padding(Space.lg),
                        verticalArrangement = Arrangement.spacedBy(Space.lg)) {
                        Text("Демонстрационные настройки UI-01. Выбор темы не сохраняется после завершения процесса.")
                        Text("Тема", Modifier.semantics { heading() }, style = JournalType.section)
                        ThemeMode.entries.forEach { mode ->
                            JournalChip(mode.label, state.theme == mode, { onTheme(mode) }, Modifier.fillMaxWidth())
                        }
                        if (BuildConfig.DEBUG) JournalButton("Каталог компонентов · debug", {
                            nav.navigate("Компоненты") { launchSingleTop = true }
                        })
                    }
                }
            }
            if (BuildConfig.DEBUG) composable("Компоненты") {
                ShellPage("Компоненты", onAction = { nav.popBackStack() }) { ComponentCatalog() }
            }
        }
    }
}

private fun AnimatedContentTransitionScope<NavBackStackEntry>.isSettingsReturn() =
    initialState.destination.route == "Настройки" && targetState.destination.route in tabs

/** Destination-owned chrome keeps outgoing Settings full-size throughout a pop. */
@Composable
private fun ShellPage(title: String, onAction: () -> Unit, onTab: ((String) -> Unit)? = null,
    content: @Composable () -> Unit) {
    Scaffold(containerColor = LocalJournalColors.current.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            JournalTopBar(title) {
                val label = if (onTab != null) "Настройки" else "Назад"
                JournalIconButton(label, onAction) { LineIcon(label) }
            }
        },
        bottomBar = { if (onTab != null) JournalBottomBar(tabs, title, onTab) { LineIcon(it) } }
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding).consumeWindowInsets(padding).imePadding()) { content() }
    }
}
