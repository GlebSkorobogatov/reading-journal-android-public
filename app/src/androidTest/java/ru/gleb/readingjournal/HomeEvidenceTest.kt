package ru.gleb.readingjournal

import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.graphics.Bitmap
import android.os.Build
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.ViewModelProvider
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import ru.gleb.readingjournal.app.*
import ru.gleb.readingjournal.demo.HomeDemoViewModel
import ru.gleb.readingjournal.ui.home.*
import java.io.File
import java.io.FileInputStream
import kotlin.math.abs

/** Screenshots come only from the installed APK on the actual CI emulator. No golden claims. */
class HomeEvidenceTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()
    private fun event(event: HomeEvent) { rule.runOnUiThread {
        ViewModelProvider(rule.activity)[HomeDemoViewModel::class.java].dispatch(event)
    } }
    private fun theme(mode: ThemeMode) { rule.runOnUiThread {
        ViewModelProvider(rule.activity)[ShellViewModel::class.java].selectTheme(mode)
    } }
    private fun scroll(tag: String) {
        rule.onNodeWithTag("home-feed").performScrollToNode(hasTestTag(tag))
        rule.onNodeWithTag(tag).assertIsDisplayed()
    }

    @Test fun bothThemesAllBlocksAndDemoStatesHaveRealRenderEvidence() {
        for (mode in listOf(ThemeMode.LIGHT, ThemeMode.DARK)) {
            theme(mode)
            event(HomeEvent.Scenario(HomeScenario.MULTIPLE))
            captureFeed("${mode.name}-multiple")
            for (section in HomeSection.entries) {
                event(HomeEvent.Section(section))
                scroll("section-${section.name}")
                screenshot("${mode.name}-${section.name}-top")
                val frames = if (section == HomeSection.RECORDS) 3 else 2
                repeat(frames) { frame ->
                    swipeFeed()
                    screenshot("${mode.name}-${section.name}-${frame + 1}")
                }
            }
            for (scenario in listOf(HomeScenario.RUNNING, HomeScenario.PAUSED)) {
                event(HomeEvent.Scenario(scenario))
                rule.onNodeWithTag("home-feed").performScrollToIndex(0)
                screenshot("${mode.name}-${scenario.name}")
                rule.onNode(hasText("Библиотека") and hasClickAction()).performClick()
                rule.onNodeWithTag("home-mini-timer").assertIsDisplayed()
                screenshot("${mode.name}-${scenario.name}-mini-library")
                rule.onNodeWithTag("home-mini-timer").performClick()
            }
            event(HomeEvent.Scenario(HomeScenario.EMPTY))
            captureFeed("${mode.name}-empty")
            event(HomeEvent.Scenario(HomeScenario.UNKNOWN))
            scroll("metric-pages")
            screenshot("${mode.name}-unknown")
            event(HomeEvent.Scenario(HomeScenario.INCOMPLETE))
            event(HomeEvent.Measure(ChartMeasure.PAGES))
            event(HomeEvent.Grouping(ChartGrouping.MONTH))
            event(HomeEvent.Section(HomeSection.DYNAMICS))
            scroll("reading-chart")
            screenshot("${mode.name}-incomplete-month-markers")
            scroll("undated-toggle")
            event(HomeEvent.ToggleUndated)
            screenshot("${mode.name}-undated")
        }
    }

    @Test fun bothOrientationsEveryFontScaleAndReducedMotionKeepActionsAccessible() {
        val originalScale = rule.activity.resources.configuration.fontScale
        val originalOrientation = rule.activity.resources.configuration.orientation
        val animatorScale = command("settings get global animator_duration_scale").trim().toFloatOrNull()
        try {
            command("settings put global animator_duration_scale 0")
            for (mode in listOf(ThemeMode.LIGHT, ThemeMode.DARK)) {
                theme(mode)
                for (fontScale in listOf(1f, 1.3f, 1.5f, 2f)) {
                    for (orientation in listOf(Configuration.ORIENTATION_PORTRAIT, Configuration.ORIENTATION_LANDSCAPE)) {
                        command("settings put system font_scale $fontScale")
                        rule.runOnUiThread { rule.activity.requestedOrientation = if (orientation == Configuration.ORIENTATION_PORTRAIT)
                            ActivityInfo.SCREEN_ORIENTATION_PORTRAIT else ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE }
                        rule.waitUntil(30_000) {
                            val config = rule.activity.resources.configuration
                            config.orientation == orientation && abs(config.fontScale - fontScale) < .01f
                        }
                        event(HomeEvent.Scenario(HomeScenario.LONG_TEXT))
                        event(HomeEvent.Start("atlas"))
                        scroll("timer-toggle")
                        rule.onNodeWithTag("timer-toggle").assertIsEnabled().performClick()
                        rule.onNodeWithTag("home-mini-timer").assertIsDisplayed()
                        val feed = rule.onNodeWithTag("home-feed").fetchSemanticsNode().boundsInRoot
                        val mini = rule.onNodeWithTag("home-mini-timer").fetchSemanticsNode().boundsInRoot
                        val nav = rule.onNodeWithTag("shell-navigation").fetchSemanticsNode().boundsInRoot
                        assertTrue("A usable viewport survives the mini panel", feed.height > 0f)
                        assertTrue("The mini panel must not cover the feed", feed.bottom <= mini.top + 1f)
                        assertTrue("The mini panel must not cover navigation", mini.bottom <= nav.top + 1f)
                        val density = rule.activity.resources.displayMetrics.density
                        rule.onAllNodes(hasClickAction()).fetchSemanticsNodes().forEach { node ->
                            val size = node.layoutInfo.coordinates.size
                            assertTrue("Touch width >=48dp", size.width / density >= 47.5f)
                            assertTrue("Touch height >=48dp", size.height / density >= 47.5f)
                        }
                        screenshot("${mode.name}-font$fontScale-orientation$orientation-reduced-motion")
                        event(HomeEvent.Section(HomeSection.SPEED))
                        event(HomeEvent.Period(HomePeriod.MONTH))
                        rule.onNodeWithContentDescription("Настройки").performClick()
                        rule.onNodeWithContentDescription("Назад").performClick()
                        scroll("expanded-SPEED")
                    }
                }
            }
        } finally {
            command("settings put system font_scale $originalScale")
            command(if (animatorScale == null) "settings delete global animator_duration_scale" else "settings put global animator_duration_scale $animatorScale")
            rule.runOnUiThread { rule.activity.requestedOrientation = if (originalOrientation == Configuration.ORIENTATION_LANDSCAPE)
                ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE else ActivityInfo.SCREEN_ORIENTATION_PORTRAIT }
            rule.waitUntil(30_000) {
                val config = rule.activity.resources.configuration
                config.orientation == originalOrientation && abs(config.fontScale - originalScale) < .01f
            }
        }
    }

    /** Overlapping actual viewport images, until the end of the long vertical feed. */
    private fun captureFeed(name: String) {
        rule.onNodeWithTag("home-feed").performScrollToIndex(0)
        for (frame in 0 until 30) {
            screenshot("$name-feed-$frame")
            val footer = rule.onAllNodesWithTag("home-footer").fetchSemanticsNodes().firstOrNull()?.boundsInRoot
            val viewport = rule.onNodeWithTag("home-feed").fetchSemanticsNode().boundsInRoot
            if (footer != null && footer.top >= viewport.top && footer.bottom <= viewport.bottom + 1f) return
            swipeFeed()
        }
        fail("The sequential screenshot sequence never reached the feed footer")
    }
    private fun swipeFeed() {
        rule.onNodeWithTag("home-feed").performTouchInput { swipeUp(startY = height * .8f, endY = height * .25f, durationMillis = 250) }
        rule.waitForIdle()
    }
    private fun screenshot(name: String) {
        rule.waitForIdle()
        val context = rule.activity
        val sourceSha = InstrumentationRegistry.getArguments().getString("sourceSha") ?: "local-uncommitted"
        val directory = File(context.filesDir, "screenshots")
        check(directory.exists() || directory.mkdirs())
        val filename = "ui02-api${Build.VERSION.SDK_INT}-${sourceSha.take(8)}-$name.png"
        File(directory, filename).outputStream().use { stream ->
            check(requireNotNull(InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot())
                .compress(Bitmap.CompressFormat.PNG, 100, stream))
        }
        val config = context.resources.configuration
        File(directory, "ui02-metadata.txt").appendText(
            "$filename | sourceSha=$sourceSha | version=${BuildConfig.VERSION_NAME}(${BuildConfig.VERSION_CODE}) | " +
                "API=${Build.VERSION.SDK_INT} | fontScale=${config.fontScale} | orientation=${config.orientation} | " +
                "density=${context.resources.displayMetrics.densityDpi} | locale=${config.locales[0]} | emulator; not A55\n")
    }
    private fun command(value: String): String {
        val descriptor = InstrumentationRegistry.getInstrumentation().uiAutomation.executeShellCommand(value)
        return try { FileInputStream(descriptor.fileDescriptor).use { String(it.readBytes()) } } finally { descriptor.close() }
    }
}
