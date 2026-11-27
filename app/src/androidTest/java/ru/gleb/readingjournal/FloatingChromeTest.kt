package ru.gleb.readingjournal

import android.graphics.Bitmap
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.focusable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import kotlinx.coroutines.launch
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import ru.gleb.readingjournal.app.ThemeMode
import ru.gleb.readingjournal.ui.designsystem.JournalChrome
import ru.gleb.readingjournal.ui.designsystem.JournalTheme
import ru.gleb.readingjournal.ui.designsystem.LocalChromeObstruction

class FloatingChromeTest {
    @get:Rule val rule = createComposeRule()
    private val tab = SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Tab)
    private val labels = listOf("Читаю сейчас", "Статистика", "Библиотека")

    @Test fun islandsStartVisibleAndAStaticScreenCannotHideThem() {
        rule.setContent { SamplePage { Box(Modifier.fillMaxSize().testTag("static")) { Text("Коротко") } } }
        assertVisible()
        rule.onNodeWithTag("static").performTouchInput { swipeUp() }
        rule.waitForIdle()
        assertEquals(0f, progress(), 0.001f)
        assertVisible()
    }

    @Test fun shortContentAndHorizontalMotionDoNotHide() {
        rule.setContent {
            SamplePage {
                Column(Modifier.fillMaxSize()) {
                    Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).testTag("short")) {
                        Text("Одна строка")
                    }
                    Row(Modifier.horizontalScroll(rememberScrollState()).testTag("wide")) {
                        Box(Modifier.size(2000.dp, 48.dp))
                    }
                }
            }
        }
        rule.onNodeWithTag("short").performTouchInput { swipeUp() }
        rule.onNodeWithTag("wide").performTouchInput { swipeLeft() }
        rule.waitForIdle()
        assertEquals(0f, progress(), 0.001f)
        assertVisible()
    }

    @Test fun smallFingerMovementDoesNotToggle() {
        rule.setContent { SamplePage { SampleFeed() } }
        rule.onNodeWithTag("feed").performTouchInput {
            down(center)
            moveBy(Offset(0f, -2f))
            up()
        }
        rule.waitForIdle()
        assertEquals(0f, progress(), 0.001f)
        assertVisible()
    }

    @Test fun scrollingTowardTheEndHidesAndTheReverseShowsTogether() {
        rule.mainClock.autoAdvance = false
        rule.setContent { SamplePage { SampleFeed() } }
        rule.mainClock.advanceTimeByFrame()
        screenshot("floating-visible-light")
        val title = top("chrome-title")
        val action = top("chrome-action")
        val tabs = top("chrome-tabs")
        rule.onNodeWithTag("feed").performTouchInput { swipeUp(durationMillis = 16) }
        val started = progress()
        val scrolled = top("item-0")
        assertTrue("progress=$started", started > 0f && started < 0.5f)
        rule.mainClock.advanceTimeBy(100)
        val mid = progress()
        assertTrue("progress=$mid", mid > started && mid < 1f)
        assertEquals(scrolled, top("item-0"), 1.5f)
        assertEquals(title - top("chrome-title"), action - top("chrome-action"), 2f)
        assertTrue(title - top("chrome-title") > 1f && top("chrome-tabs") - tabs > 1f)
        screenshot("floating-mid-hide-light")
        rule.mainClock.advanceTimeBy(400)
        assertEquals(1f, progress(), 0.02f)
        rule.onNodeWithContentDescription("Настройки").assertDoesNotExist()
        rule.onAllNodes(tab).assertCountEquals(0)
        rule.onNodeWithTag("chrome-title").assertDoesNotExist()
        screenshot("floating-hidden-light")
        rule.onNodeWithTag("feed").performTouchInput { swipeDown(durationMillis = 16) }
        assertTrue(progress() < 1f)
        screenshot("floating-mid-show-light")
        rule.mainClock.advanceTimeBy(400)
        assertEquals(0f, progress(), 0.02f)
        assertVisible()
        screenshot("floating-restored-light")
    }

    @Test fun quickReversalContinuesFromTheCurrentProgress() {
        rule.mainClock.autoAdvance = false
        rule.setContent { SamplePage { SampleFeed() } }
        rule.mainClock.advanceTimeByFrame()
        rule.onNodeWithTag("feed").performTouchInput { swipeUp(durationMillis = 16) }
        val mid = progress()
        assertTrue("progress=$mid", mid > 0f && mid < 0.5f)
        rule.onNodeWithTag("feed").performTouchInput { swipeDown(durationMillis = 48) }
        assertTrue("progress=${progress()} mid=$mid", progress() < 0.9f)
        rule.mainClock.advanceTimeBy(500)
        assertEquals(0f, progress(), 0.02f)
    }

    @Test fun overscrollPastEitherEndDoesNotCreateAFalseHide() {
        rule.setContent { SamplePage { SampleFeed() } }
        rule.onNodeWithTag("feed").performScrollToNode(hasTestTag("item-29"))
        rule.onNodeWithTag("feed").performTouchInput { swipeUp() }
        rule.waitForIdle()
        assertEquals(0f, progress(), 0.02f)
        rule.onNodeWithTag("feed").performScrollToNode(hasTestTag("item-0"))
        rule.onNodeWithTag("feed").performTouchInput { swipeUp() }
        rule.waitForIdle()
        assertEquals(1f, progress(), 0.02f)
        rule.onNodeWithTag("feed").performTouchInput { swipeUp() }
        rule.waitForIdle()
        assertEquals(1f, progress(), 0.02f)
    }

    @Test fun programmaticScrollAndLayoutChangesDoNotHide() {
        rule.setContent {
            val state = rememberScrollState()
            val scope = rememberCoroutineScope()
            var extra by remember { mutableStateOf(false) }
            var shown by remember { mutableIntStateOf(0) }
            SamplePage {
                Column(Modifier.fillMaxSize()) {
                    Text("Вниз", Modifier.testTag("jump").clickable {
                        scope.launch {
                            state.animateScrollTo(state.maxValue)
                            shown = state.value
                        }
                    })
                    Text("Выше", Modifier.testTag("grow").clickable { extra = true })
                    Text(shown.toString(), Modifier.testTag("offset"))
                    Column(Modifier.weight(1f).verticalScroll(state).testTag("manual")) {
                        repeat(if (extra) 60 else 40) {
                            Text("Строка $it", Modifier.height(48.dp).testTag("row-$it"))
                        }
                    }
                }
            }
        }
        rule.onNodeWithTag("jump").performClick()
        rule.waitForIdle()
        val offset = rule.onNodeWithTag("offset").fetchSemanticsNode().config[SemanticsProperties.Text].single().text
        assertTrue("offset=$offset", offset.toInt() > 0)
        assertEquals(0f, progress(), 0.001f)
        rule.onNodeWithTag("grow").performClick()
        rule.waitForIdle()
        rule.onNodeWithTag("row-50").assertExists()
        assertEquals(0f, progress(), 0.001f)
        assertVisible()
    }

    @Test fun firstAndLastRowsStayReachableAboveTheIsland() {
        rule.setContent { SamplePage { SampleFeed() } }
        rule.onNodeWithTag("item-0").assertIsDisplayed()
        val titleBottom = rule.onNodeWithTag("chrome-title").fetchSemanticsNode().boundsInRoot.bottom
        assertTrue(rule.onNodeWithTag("item-0").fetchSemanticsNode().boundsInRoot.top >= titleBottom - 1f)
        rule.onNodeWithTag("feed").performScrollToNode(hasTestTag("item-29"))
        rule.onNodeWithTag("item-29").assertIsDisplayed()
        val item = rule.onNodeWithTag("item-29").fetchSemanticsNode().boundsInRoot
        val tabs = rule.onNodeWithTag("chrome-tabs").fetchSemanticsNode().boundsInRoot
        assertTrue(item.bottom <= tabs.top + 1f)
        assertEquals(0f, progress(), 0.001f)
    }

    @Test fun hiddenIslandsAreNotInteractiveAndContentFocusRemains() {
        val hits = intArrayOf(0)
        rule.setContent {
            SamplePage {
                Box(Modifier.fillMaxSize()) {
                    Column(Modifier.fillMaxSize()) {
                        Text("Фокус", Modifier.testTag("focus").focusable())
                        Box(Modifier.weight(1f)) { SampleFeed() }
                    }
                    Box(Modifier.align(Alignment.TopEnd).statusBarsPadding()
                        .padding(top = 8.dp, end = 16.dp).size(48.dp).testTag("under")
                        .clickable { hits[0]++ })
                }
            }
        }
        rule.onNodeWithTag("focus").performSemanticsAction(SemanticsActions.RequestFocus)
        rule.onNodeWithTag("focus").assertIsFocused()
        val point = rule.onNodeWithTag("chrome-action").fetchSemanticsNode().boundsInRoot.center
        rule.onNodeWithTag("feed").performTouchInput { swipeUp() }
        rule.waitForIdle()
        assertEquals(1f, progress(), 0.02f)
        rule.onNodeWithContentDescription("Настройки").assertDoesNotExist()
        rule.onAllNodes(tab).assertCountEquals(0)
        rule.onNodeWithTag("focus").assertIsFocused()
        rule.onRoot().performTouchInput { click(point) }
        assertEquals(1, hits[0])
    }

    @Test fun touchExplorationModeDoesNotAutoHide() {
        rule.setContent { SamplePage(autoHide = false) { SampleFeed() } }
        rule.onNodeWithTag("feed").performTouchInput { swipeUp() }
        rule.waitForIdle()
        assertEquals(0f, progress(), 0.001f)
        assertVisible()
    }

    @Test fun switchingTabsOrOpeningAChildRestoresTheIslands() {
        rule.setContent {
            var child by remember { mutableStateOf(false) }
            var selected by remember { mutableStateOf(labels.first()) }
            JournalTheme(ThemeMode.LIGHT) {
                key(if (child) "book" else selected) {
                    JournalChrome(
                        title = if (child) "Книга" else selected,
                        actionLabel = if (child) "Назад" else "Настройки",
                        onAction = {},
                        tabs = if (child) null else labels,
                        selectedTab = selected,
                        onTab = { selected = it },
                        content = {
                            Column(Modifier.fillMaxSize()) {
                                Text("К статистике", Modifier.testTag("go-stats").clickable { selected = "Статистика" })
                                Text(if (child) "К списку" else "К книге",
                                    Modifier.testTag("switch").clickable { child = !child })
                                Box(Modifier.weight(1f)) { SampleFeed() }
                            }
                        },
                    )
                }
            }
        }
        rule.onNodeWithTag("feed").performTouchInput { swipeUp() }
        rule.waitForIdle()
        assertEquals(1f, progress(), 0.02f)
        rule.onNodeWithTag("go-stats").performClick()
        rule.waitForIdle()
        assertEquals(0f, progress(), 0.02f)
        rule.onNode(tab and hasText("Статистика")).assertIsSelected()
        assertVisible()
        rule.onNodeWithTag("feed").performTouchInput { swipeUp() }
        rule.waitForIdle()
        rule.onNodeWithTag("switch").performClick()
        rule.waitForIdle()
        rule.onNodeWithContentDescription("Назад").assertIsDisplayed()
        rule.onAllNodes(tab).assertCountEquals(0)
        assertEquals(0f, progress(), 0.02f)
        rule.onNodeWithTag("switch").performClick()
        rule.waitForIdle()
        assertVisible()
        rule.onNode(tab and hasText("Статистика")).assertIsSelected()
    }

    @Test fun bothThemesOrientationsAndLargeTextKeepTheIslandsReadable() {
        val base = rule.density.density
        for (mode in listOf(ThemeMode.LIGHT, ThemeMode.DARK)) {
            for (scale in listOf(1f, 1.3f, 1.5f, 2f)) {
                for (landscape in listOf(false, true)) {
                    rule.setContent {
                        CompositionLocalProvider(LocalDensity provides Density(base, scale)) {
                            Box(Modifier.requiredSize(if (landscape) 800.dp else 360.dp,
                                if (landscape) 360.dp else 800.dp)) {
                                SamplePage(mode = mode) { Box(Modifier.fillMaxSize()) { Text("Экран") } }
                            }
                        }
                    }
                    assertVisible()
                    val action = rule.onNodeWithTag("chrome-action").fetchSemanticsNode().size
                    assertTrue("action ${action.width}x${action.height} scale=$scale",
                        action.width / base >= 47.5f && action.height / base >= 47.5f)
                    val nodes = rule.onAllNodes(tab).fetchSemanticsNodes()
                    assertEquals(3, nodes.size)
                    for (node in nodes) {
                        assertTrue(node.size.width / base >= 47.5f && node.size.height / base >= 47.5f)
                    }
                    val title = rule.onNodeWithTag("chrome-title").fetchSemanticsNode().boundsInRoot
                    val gear = rule.onNodeWithTag("chrome-action").fetchSemanticsNode().boundsInRoot
                    val root = rule.onNodeWithTag("chrome-root").fetchSemanticsNode().boundsInRoot
                    val tabs = rule.onNodeWithTag("chrome-tabs").fetchSemanticsNode().boundsInRoot
                    assertTrue(title.right <= gear.left + 1f)
                    assertTrue(tabs.left > root.left + base)
                    assertTrue(tabs.right < root.right - base)
                    assertTrue(tabs.bottom < root.bottom - base)
                    rule.onNodeWithText("Читаю сейчас").assertIsDisplayed()
                    rule.onNodeWithText("Статистика").assertIsDisplayed()
                    rule.onNodeWithText("Библиотека").assertIsDisplayed()
                }
            }
        }
    }

    @Test fun darkThemeShowsAndHidesTheSameIslands() {
        rule.setContent { SamplePage(mode = ThemeMode.DARK) { SampleFeed() } }
        screenshot("floating-visible-dark")
        rule.onNodeWithTag("feed").performTouchInput { swipeUp() }
        rule.waitForIdle()
        assertEquals(1f, progress(), 0.02f)
        screenshot("floating-hidden-dark")
    }

    @Composable private fun SamplePage(mode: ThemeMode = ThemeMode.LIGHT, autoHide: Boolean = true,
        content: @Composable () -> Unit) {
        JournalTheme(mode) {
            JournalChrome("Читаю сейчас", "Настройки", {}, labels, "Читаю сейчас", {}, autoHide, content)
        }
    }

    @Composable private fun SampleFeed() {
        val top = LocalChromeObstruction.current.top
        LazyColumn(Modifier.fillMaxSize().testTag("feed"),
            contentPadding = PaddingValues(top = top, bottom = 16.dp)) {
            items(30) { index -> Text("Строка $index", Modifier.fillMaxWidth().height(56.dp).testTag("item-$index")) }
        }
    }

    private fun assertVisible() {
        rule.onNodeWithContentDescription("Настройки").assertIsDisplayed()
        rule.onNodeWithTag("chrome-title").assertIsDisplayed()
        rule.onAllNodes(tab).assertCountEquals(3)
        rule.onNode(tab and hasText("Читаю сейчас")).assertIsSelected()
        assertEquals(0f, progress(), 0.02f)
    }

    private fun progress(): Float = rule.onNodeWithTag("chrome-progress", useUnmergedTree = true)
        .fetchSemanticsNode().config[SemanticsProperties.StateDescription].toFloat()

    private fun top(tag: String) = rule.onNodeWithTag(tag).fetchSemanticsNode().boundsInRoot.top

    private fun screenshot(name: String) {
        val bitmap = InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot() ?: return
        save(bitmap, name)
    }

    private fun save(bitmap: Bitmap, name: String) {
        val file = File(InstrumentationRegistry.getInstrumentation().targetContext.filesDir, "screenshots/$name.png")
        file.parentFile?.mkdirs()
        file.outputStream().use { check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)) }
    }
}
