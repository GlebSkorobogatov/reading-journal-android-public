package ru.gleb.readingjournal

import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.graphics.Bitmap
import android.os.Build
import android.os.SystemClock
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.semantics.SemanticsActions
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
    private fun event(event: HomeEvent) {
        rule.runOnUiThread { ViewModelProvider(rule.activity)[HomeDemoViewModel::class.java].dispatch(event) }
        rule.waitForIdle()
    }
    private fun theme(mode: ThemeMode) {
        rule.runOnUiThread { ViewModelProvider(rule.activity)[ShellViewModel::class.java].selectTheme(mode) }
        rule.waitForIdle()
    }
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
                    scrollFeed()
                    screenshot("${mode.name}-${section.name}-${frame + 1}")
                }
            }
            for (scenario in listOf(HomeScenario.RUNNING, HomeScenario.PAUSED)) {
                event(HomeEvent.Scenario(scenario))
                rule.onNodeWithTag("home-feed").performScrollToIndex(0)
                rule.onNodeWithTag("timer-atlas").assertIsDisplayed()
                screenshot("${mode.name}-${scenario.name}")
                rule.onNode(hasText("Библиотека") and hasClickAction()).performClick()
                rule.onNodeWithTag("home-mini-timer").assertIsDisplayed()
                screenshot("${mode.name}-${scenario.name}-mini-library")
                rule.onNodeWithTag("home-mini-timer").performClick()
                rule.waitForIdle()
                rule.onNodeWithTag("timer-atlas").assertIsDisplayed()
            }
            event(HomeEvent.Finish)
            rule.onNodeWithTag("home-sheet").assertIsDisplayed()
            screenshot("${mode.name}-finishing-sheet")
            event(HomeEvent.Dismiss)
            for (scenario in listOf(HomeScenario.SINGLE, HomeScenario.PENDING)) {
                event(HomeEvent.Scenario(scenario))
                captureFeed("${mode.name}-${scenario.name}")
            }
            scroll("start-atlas")
            rule.onNodeWithTag("pending-atlas").assertIsDisplayed()
            rule.onNodeWithTag("start-atlas").assertIsNotEnabled()
            screenshot("${mode.name}-pending-book")
            rule.onNodeWithTag("home-feed").performScrollToNode(hasText("Завершить запись сеанса"))
            rule.onNodeWithText("Завершить запись сеанса").performClick()
            rule.onNodeWithText("Не помню страницы").performScrollTo().performClick()
            scroll("start-atlas")
            rule.onNodeWithTag("unknown-atlas").assertIsDisplayed()
            rule.onNodeWithTag("start-atlas").assertIsEnabled()
            screenshot("${mode.name}-unknown-book")
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
            scroll("undated-1")
            rule.onNodeWithTag("undated-0").assertIsDisplayed()
            rule.onNodeWithTag("undated-1").assertIsDisplayed()
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
                        rule.onNodeWithTag("home-feed").performScrollToIndex(0)
                        rule.onNodeWithTag("home-books-heading").assertIsDisplayed()
                        screenshot("${mode.name}-font$fontScale-orientation$orientation-heading")
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
                        scroll("timer-finish")
                        rule.onNodeWithTag("timer-finish").assertIsDisplayed().assertIsEnabled()
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
            scrollFeed()
        }
        fail("The sequential screenshot sequence never reached the feed footer")
    }
    private fun scrollFeed() {
        val feed = rule.onNodeWithTag("home-feed")
        val distance = feed.fetchSemanticsNode().boundsInRoot.height * .7f
        // A fixed viewport fraction guarantees overlap; a fling can jump past content.
        feed.performSemanticsAction(SemanticsActions.ScrollBy) { scroll -> scroll(0f, distance) }
        rule.waitForIdle()
    }
    private fun screenshot(name: String) {
        // Also finish draw-only Compose transitions before observing native pixels.
        rule.mainClock.advanceTimeBy(1_000)
        rule.waitForIdle()
        val automation = InstrumentationRegistry.getInstrumentation().uiAutomation
        // Compose idleness does not wait for SurfaceFlinger, window fades or Android's
        // rotation animation. Require a stable, foreground app image, never an ANR
        // dialog, launcher frame or half-rendered navigation transition.
        var previous: Bitmap? = null
        var stableSince = SystemClock.uptimeMillis()
        var focus = "not sampled"
        var windowDump = "not sampled"
        var accessiblePackage: String? = null
        try {
            rule.waitUntil(15_000) {
                val current = requireNotNull(automation.takeScreenshot())
                val unchanged = previous?.sameAs(current) == true
                previous?.recycle()
                previous = current
                if (!unchanged) stableSince = SystemClock.uptimeMillis()
                val pixelsStable = unchanged && SystemClock.uptimeMillis() - stableSince >= 750
                // On older Android, accessibility's active root can remain attached
                // to a dismissed dialog. WindowManager owns actual window focus.
                val stable = if (pixelsStable) {
                    // Focus lives in the display section on recent Android, not
                    // the `windows` subsection. The full dump covers API 26+.
                    windowDump = command("dumpsys window")
                    focus = windowDump.lineSequence()
                        .firstOrNull { it.contains("mCurrentFocus=") }?.trim() ?: "no focused window"
                    accessiblePackage = automation.rootInActiveWindow?.packageName?.toString()
                    val foreground = focus.contains("${BuildConfig.APPLICATION_ID}/")
                    if (foreground && accessiblePackage != BuildConfig.APPLICATION_ID) {
                        android.util.Log.i("HomeEvidence", "$name: native focus=$focus; accessibility package=$accessiblePackage")
                    }
                    foreground
                } else false
                if (!stable) SystemClock.sleep(100)
                stable
            }
            writeScreenshot(name, requireNotNull(previous))
        } catch (failure: Throwable) {
            // Keep the actual rejected frame and focus evidence for diagnosis.
            previous?.let { writeScreenshot("$name-REJECTED", it) }
            File(rule.activity.filesDir, "screenshots/ui02-$name-window-dump.txt").writeText(windowDump)
            throw AssertionError("Rejected screenshot $name: $focus; accessibility package=$accessiblePackage", failure)
        } finally {
            previous?.recycle()
        }
    }
    private fun writeScreenshot(name: String, bitmap: Bitmap) {
        val context = rule.activity
        val sourceSha = InstrumentationRegistry.getArguments().getString("sourceSha") ?: "local-uncommitted"
        val directory = File(context.filesDir, "screenshots")
        check(directory.exists() || directory.mkdirs())
        val filename = "ui02-api${Build.VERSION.SDK_INT}-${sourceSha.take(8)}-$name.png"
        File(directory, filename).outputStream().use { stream ->
            check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream))
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
