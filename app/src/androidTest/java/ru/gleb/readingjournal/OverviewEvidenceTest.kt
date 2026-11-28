package ru.gleb.readingjournal

import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.graphics.Bitmap
import android.os.Build
import android.util.Log
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.text.TextLayoutResult
import androidx.lifecycle.ViewModelProvider
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import ru.gleb.readingjournal.app.MainActivity
import ru.gleb.readingjournal.app.ShellViewModel
import ru.gleb.readingjournal.app.ThemeMode
import ru.gleb.readingjournal.demo.DemoAction
import ru.gleb.readingjournal.demo.DemoScenario
import ru.gleb.readingjournal.demo.ReadingDemoViewModel
import ru.gleb.readingjournal.ui.overview.Direction
import ru.gleb.readingjournal.ui.overview.Period
import java.io.File
import java.io.FileInputStream
import kotlin.math.abs

/** Full device-screen captures from the running application, with actual Android font scale and orientation. */
class OverviewEvidenceTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()
    private val tab = SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Tab)
    private val metricKeys = listOf("pages", "cycles", "streak")

    @Test fun bothScreensFitInActualAndroidConfigurationMatrix() {
        val original = rule.activity.resources.configuration.fontScale
        val animator = command("settings get global animator_duration_scale").trim()
        try {
            command("settings put global animator_duration_scale 0")
            configureDemo(DemoScenario.LONG_TEXT, allTime = true)
            for (scale in listOf(1f, 1.3f, 1.5f, 2f)) {
                command("settings put system font_scale $scale")
                for (landscape in listOf(false, true)) {
                    orientation(landscape)
                    val expected = if (landscape) Configuration.ORIENTATION_LANDSCAPE else Configuration.ORIENTATION_PORTRAIT
                    rule.waitUntil(30_000) {
                        val config = rule.activity.resources.configuration
                        config.orientation == expected && abs(config.fontScale - scale) < .01f
                    }
                    for (mode in listOf(ThemeMode.LIGHT, ThemeMode.DARK)) {
                        theme(mode)
                        val prefix = "${mode.name}-font$scale-${if (landscape) "landscape" else "portrait"}"
                        select("Читаю сейчас")
                        rule.onNodeWithTag("reading-feed").performScrollToIndex(0)
                        rule.onNodeWithTag("reading-subtitle").assertIsDisplayed()
                        assertViewport("reading-feed")
                        capture("$prefix-reading", mode)
                        rule.onNodeWithTag("reading-feed").performScrollToNode(hasTestTag("record-atlas"))
                        rule.onNodeWithTag("record-atlas").assertIsDisplayed()
                        if (landscape || scale >= 1.5f) capture("$prefix-reading-card", mode)

                        select("Статистика")
                        rule.onNodeWithTag("xp-card").performScrollTo()
                        assertViewport("statistics-feed")
                        capture("$prefix-statistics", mode)
                        assertMetricsFit(mode, listOf("128,5K", "142", "248"), "128 540 страниц")
                        capture("$prefix-metrics", mode)
                        rule.onNodeWithTag("period-NEXT").performScrollTo().assertIsNotEnabled()
                        rule.onNodeWithTag("overview-ReadingRecords").performScrollTo()
                        if (landscape || scale >= 1.5f) capture("$prefix-statistics-outlets", mode)
                    }
                }
            }
        } finally {
            command("settings put system font_scale $original")
            if (animator == "null") command("settings delete global animator_duration_scale")
            else command("settings put global animator_duration_scale $animator")
            rule.runOnUiThread { rule.activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED }
            rule.waitUntil(30_000) { abs(rule.activity.resources.configuration.fontScale - original) < .01f }
        }
    }

    @Test fun evidenceForScenariosAndAllChildRoutes() {
        orientation(false)
        rule.waitUntil(30_000) { rule.activity.resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT }
        theme(ThemeMode.SYSTEM)
        configureDemo(DemoScenario.EMPTY)
        rule.onNodeWithTag("reading-empty").assertIsDisplayed()
        capture("system-empty", ThemeMode.SYSTEM)
        select("Статистика")
        assertMetricsFit(ThemeMode.SYSTEM, listOf("0", "0", "0"), "0 страниц")
        select("Читаю сейчас")
        configureDemo(DemoScenario.SINGLE)
        rule.onNodeWithTag("book-coast").assertIsDisplayed()
        capture("system-single", ThemeMode.SYSTEM)
        configureDemo(DemoScenario.MULTIPLE)
        capture("system-reading", ThemeMode.SYSTEM)
        rule.onNodeWithTag("reading-feed").performScrollToNode(hasTestTag("open-atlas"))
        rule.onNodeWithTag("open-atlas").performClick()
        capture("system-book-stub", ThemeMode.SYSTEM)
        back()
        rule.onNodeWithTag("reading-feed").performScrollToNode(hasTestTag("record-atlas"))
        rule.onNodeWithTag("record-atlas").performClick()
        capture("system-record-stub", ThemeMode.SYSTEM)
        back()
        configureDemo(DemoScenario.MANY)
        rule.onNodeWithTag("reading-feed").performScrollToNode(hasTestTag("record-extra-12"))
        capture("system-many-last-book", ThemeMode.SYSTEM)
        configureDemo(DemoScenario.MULTIPLE, allTime = true)
        select("Статистика")
        assertMetricsFit(ThemeMode.SYSTEM, listOf("1,7K", "7", "3"), "1 795 страниц")
        configureDemo(DemoScenario.HISTORICAL, allTime = true)
        assertMetricsFit(ThemeMode.SYSTEM, listOf("—", "2", "0"),
            "Неизвестно. У завершённых циклов нет сведений о страницах")
        rule.onNodeWithTag("xp-card").performScrollTo()
        capture("system-statistics-unknown", ThemeMode.SYSTEM)
        for (route in listOf("xp-card", "overview-ReadingDynamics", "overview-ActivityCalendar", "overview-ReadingRecords")) {
            rule.onNodeWithTag(route).performScrollTo().performClick()
            capture("system-$route-stub", ThemeMode.SYSTEM)
            back()
        }
        select("Библиотека")
        capture("system-library-stub", ThemeMode.SYSTEM)
    }

    private fun assertMetricsFit(mode: ThemeMode, expectedValues: List<String>, exactPagesDescription: String) {
        rule.onNodeWithTag("overview-metrics").performScrollTo().assertIsDisplayed()
        rule.onNodeWithTag("overview-pages")
            .assert(hasContentDescription("Прочитанные страницы"))
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, exactPagesDescription))
        val card = layoutBounds("overview-metrics")
        val cells = metricKeys.associateWith { key -> layoutBounds("overview-$key") }
        val leftDivider = layoutBounds("overview-divider-pages")
        val rightDivider = layoutBounds("overview-divider-cycles")
        val ordered = listOf(cells.getValue("pages"), leftDivider, cells.getValue("cycles"),
            rightDivider, cells.getValue("streak"))
        val context = "API=${Build.VERSION.SDK_INT} theme=$mode config=${rule.activity.resources.configuration}"
        assertTrue("Equal metric widths: $cells ($context)",
            cells.values.maxOf { it.width } - cells.values.minOf { it.width } <= 1f)
        assertTrue("Both dividers have nonzero width ($context)", leftDivider.width > 0f && rightDivider.width > 0f)
        ordered.forEach { bounds -> assertInside(bounds, card, context) }
        ordered.zipWithNext().forEach { (left, right) ->
            assertTrue("Metric regions and dividers do not overlap: $left, $right ($context)",
                left.right <= right.left + 1f)
        }
        for ((index, key) in metricKeys.withIndex()) {
            val cell = cells.getValue(key)
            val valueTag = "overview-$key-value"
            val labelTag = "overview-$key-label"
            rule.onNodeWithTag(valueTag, useUnmergedTree = true).assertTextEquals(expectedValues[index])
            for (tag in listOf(valueTag, labelTag)) {
                assertInside(layoutBounds(tag), cell, "$tag ($context)")
                assertTextFits(tag, mode)
            }
        }
    }

    private fun layoutBounds(tag: String): Rect {
        val coordinates = rule.onNodeWithTag(tag, useUnmergedTree = true)
            .fetchSemanticsNode().layoutInfo.coordinates
        return Rect(coordinates.positionInRoot(), Size(coordinates.size.width.toFloat(), coordinates.size.height.toFloat()))
    }

    private fun assertInside(inner: Rect, outer: Rect, context: String) {
        // Allow one pixel for fractional positioning, independently of the strict text overflow check.
        assertTrue("$inner fits inside $outer ($context)",
            inner.left >= outer.left - 1f && inner.top >= outer.top - 1f &&
                inner.right <= outer.right + 1f && inner.bottom <= outer.bottom + 1f)
    }

    private fun assertTextFits(tag: String, mode: ThemeMode) {
        val node = rule.onNodeWithTag(tag, useUnmergedTree = true)
        val layouts = mutableListOf<TextLayoutResult>()
        node.performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(layouts) }
        val config = rule.activity.resources.configuration
        val root = rule.onRoot().fetchSemanticsNode().size
        val card = rule.onNodeWithTag("overview-metrics").fetchSemanticsNode().size
        val cells = metricKeys.map { key ->
            rule.onNodeWithTag("overview-$key", useUnmergedTree = true).fetchSemanticsNode().size
        }
        val text = node.fetchSemanticsNode().size
        val orientation = if (config.orientation == Configuration.ORIENTATION_LANDSCAPE) "landscape" else "portrait"
        val context = "API=${Build.VERSION.SDK_INT} fontScale=${config.fontScale} orientation=$orientation " +
            "theme=$mode screenDp=${config.screenWidthDp}x${config.screenHeightDp} rootPx=$root " +
            "densityDpi=${config.densityDpi} cardPx=$card cellsPx=$cells tag=$tag nodePx=$text"
        val measurement = layouts.joinToString { layout ->
            val input = layout.layoutInput
            val lines = (0 until layout.lineCount).joinToString { line ->
                "(${layout.getLineLeft(line)},${layout.getLineTop(line)})-" +
                    "(${layout.getLineRight(line)},${layout.getLineBottom(line)})"
            }
            "text=«${input.text.text}» fontSize=${input.style.fontSize} constraints=${input.constraints} " +
                "layoutPx=${layout.size} paragraphPx=${layout.multiParagraph.width}x${layout.multiParagraph.height} " +
                "lines=${layout.lineCount} lineBounds=[$lines] " +
                "maxLines=${input.maxLines} softWrap=${input.softWrap} overflow=${input.overflow} " +
                "didOverflowWidth=${layout.didOverflowWidth} didOverflowHeight=${layout.didOverflowHeight} " +
                "hasVisualOverflow=${layout.hasVisualOverflow}"
        }
        val diagnostic = "$context $measurement"
        Log.i("OverviewMetricLayout", diagnostic)
        assertTrue("Text layout present for $tag: $diagnostic", layouts.isNotEmpty())
        assertTrue("No visual overflow in $tag: $diagnostic", layouts.none { it.hasVisualOverflow })
        for (layout in layouts) {
            assertTrue("Text layout agrees with the measured node: $diagnostic", layout.size == text)
            assertTrue("All text is laid out: $diagnostic", layout.lineCount > 0 &&
                layout.getLineEnd(layout.lineCount - 1) == layout.layoutInput.text.length)
            for (line in 0 until layout.lineCount) {
                assertTrue("Unclipped line $line in $tag: $diagnostic", !layout.isLineEllipsized(line))
                assertInside(Rect(layout.getLineLeft(line), layout.getLineTop(line),
                    layout.getLineRight(line), layout.getLineBottom(line)),
                    Rect(0f, 0f, text.width.toFloat(), text.height.toFloat()), diagnostic)
            }
        }
    }

    private fun assertViewport(tag: String) {
        val viewport = rule.onNodeWithTag(tag).fetchSemanticsNode().boundsInRoot
        val density = rule.activity.resources.displayMetrics.density
        val tabs = rule.onAllNodes(tab).fetchSemanticsNodes()
        assertTrue(viewport.height > 0f)
        for (item in tabs) {
            assertTrue("The feed extends under the island instead of stopping above it",
                viewport.bottom > item.boundsInRoot.top + 1f)
            assertTrue(item.size.width / density >= 47.5f && item.size.height / density >= 47.5f)
        }
    }

    private fun select(label: String) = rule.onNode(tab and hasContentDescription(label)).performClick()
    private fun back() = rule.runOnUiThread { rule.activity.onBackPressedDispatcher.onBackPressed() }

    private fun orientation(landscape: Boolean) = rule.runOnUiThread {
        rule.activity.requestedOrientation = if (landscape) ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        else ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
    }

    private fun theme(mode: ThemeMode) = rule.runOnIdle {
        ViewModelProvider(rule.activity)[ShellViewModel::class.java].selectTheme(mode)
    }

    private fun configureDemo(scenario: DemoScenario, allTime: Boolean = false) = rule.runOnIdle {
        val model = ViewModelProvider(rule.activity)[ReadingDemoViewModel::class.java]
        model.onAction(DemoAction.SelectScenario(scenario))
        if (allTime) while (model.state.value.statistics.period != Period.ALL) {
            model.onAction(DemoAction.StepPeriod(Direction.NEXT))
        }
    }

    private fun capture(name: String, mode: ThemeMode) {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val automation = instrumentation.uiAutomation
        val appPackage = instrumentation.targetContext.packageName
        var activeWindow = "none"
        // A displayed Activity can lack input focus. The active, visible accessibility window
        // identifies the app on the display that UiAutomation is about to capture.
        try {
            rule.waitUntil(15_000) {
                val root = automation.rootInActiveWindow
                activeWindow = "${root?.packageName} (visible=${root?.isVisibleToUser})"
                root?.packageName == appPackage && root.isVisibleToUser
            }
        } catch (error: androidx.compose.ui.test.ComposeTimeoutException) {
            val focus = command("dumpsys window displays").lineSequence()
                .filter { it.contains("mCurrentFocus") || it.contains("mFocusedApp") }
                .joinToString()
            throw AssertionError("Capture $name: expected $appPackage, active window=$activeWindow. $focus", error)
        }
        // Compose waits for the scheduled draw as well as composition and animations.
        rule.waitForIdle()
        val context = rule.activity
        val configuration = context.resources.configuration
        assertTrue("Evidence contains an invalid source SHA", BuildConfig.SOURCE_SHA.matches(Regex("[0-9a-f]{40}")))
        val directory = File(context.filesDir, "screenshots")
        directory.mkdirs()
        val filename = "rev02-${BuildConfig.SOURCE_SHA.take(8)}-$name.png"
        val image = requireNotNull(automation.takeScreenshot()) { "No device screenshot for $name" }
        try {
            val root = automation.rootInActiveWindow
            assertTrue("The app is still visible in the active window after capture $name",
                root?.packageName == appPackage && root.isVisibleToUser)
            assertTrue("Screenshot orientation matches Android configuration for $name",
                (image.width > image.height) == (configuration.orientation == Configuration.ORIENTATION_LANDSCAPE))
            File(directory, filename).outputStream().use {
                check(image.compress(Bitmap.CompressFormat.PNG, 100, it))
            }
            File(directory, "rev02-metadata.txt").appendText(
                "$filename | sourceSha=${BuildConfig.SOURCE_SHA} | API=${Build.VERSION.SDK_INT} | theme=${mode.name}" +
                    " | fontScale=${configuration.fontScale} | orientation=${configuration.orientation}" +
                    " | density=${configuration.densityDpi} | screen=${image.width}x${image.height}" +
                    " | version=${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})" +
                    " | locale=${configuration.locales[0].toLanguageTag()}" +
                    " | scenario=${ViewModelProvider(context)[ReadingDemoViewModel::class.java].state.value.scenario.name}" +
                    " | animator=${command("settings get global animator_duration_scale").trim()}\n"
            )
        } finally {
            image.recycle()
        }
    }

    private fun command(shellCommand: String): String =
        InstrumentationRegistry.getInstrumentation().uiAutomation.executeShellCommand(shellCommand).use { descriptor ->
            FileInputStream(descriptor.fileDescriptor).bufferedReader().use { it.readText() }
        }
}
