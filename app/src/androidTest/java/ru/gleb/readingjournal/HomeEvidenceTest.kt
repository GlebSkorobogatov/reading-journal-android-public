package ru.gleb.readingjournal

import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.graphics.Bitmap
import android.os.Build
import android.os.SystemClock
import android.view.InputDevice
import android.view.MotionEvent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.layout.positionInRoot
import androidx.lifecycle.ViewModelProvider
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import ru.gleb.readingjournal.app.*
import ru.gleb.readingjournal.demo.HomeDemoViewModel
import ru.gleb.readingjournal.ui.home.*
import java.io.File
import java.io.FileInputStream
import kotlin.math.abs

/** Real emulator captures from this installed APK, never Preview or a generated mockup. */
class HomeEvidenceTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()
    @Before fun usePhoneTouchInput() {
        // Use Android's real input dispatcher. setInTouchMode alone can leave an
        // API 26 window in keyboard mode until it receives an actual touch event.
        // The heading is inert: this changes input mode, never reading data.
        rule.waitForIdle()
        val coordinates = rule.onNodeWithTag("home-books-heading").fetchSemanticsNode().layoutInfo.coordinates
        val center = coordinates.localToScreen(Offset(coordinates.size.width / 2f, coordinates.size.height / 2f))
        check(center.x.isFinite() && center.y.isFinite())
        val automation = InstrumentationRegistry.getInstrumentation().uiAutomation
        val downTime = SystemClock.uptimeMillis()
        for (action in listOf(MotionEvent.ACTION_DOWN, MotionEvent.ACTION_UP)) {
            val event = MotionEvent.obtain(downTime, SystemClock.uptimeMillis(), action, center.x, center.y, 0)
            event.source = InputDevice.SOURCE_TOUCHSCREEN
            try { assertTrue("Native phone touch is delivered", automation.injectInputEvent(event, true)) }
            finally { event.recycle() }
        }
        rule.waitForIdle()
        rule.waitUntil(5_000) { rule.activity.window.decorView.isInTouchMode }
        assertTrue("Phone evidence uses touch mode", rule.activity.window.decorView.isInTouchMode)
    }
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
    private fun close() { rule.onNodeWithText("Закрыть панель").performScrollTo().performClick(); rule.waitForIdle() }

    @Test fun bothThemesEverySectionAndAllPageBasedStatesHaveRealEvidence() {
        for (mode in listOf(ThemeMode.LIGHT, ThemeMode.DARK)) {
            theme(mode)
            event(HomeEvent.Scenario(HomeScenario.MULTIPLE))
            captureFeed("${mode.name}-multiple")
            for (section in HomeSection.entries) {
                event(HomeEvent.Section(section))
                scroll("section-${section.name}")
                captureUntil("${mode.name}-${section.name}", when (section) {
                    HomeSection.DYNAMICS -> "dynamics-end"
                    HomeSection.CALENDAR -> "calendar-end"
                    HomeSection.COMPARISON -> "comparison-end"
                    HomeSection.RECORDS -> "records-coverage"
                })
            }
            for (scenario in listOf(HomeScenario.SINGLE, HomeScenario.EMPTY, HomeScenario.NO_PAGES, HomeScenario.HISTORICAL)) {
                event(HomeEvent.Scenario(scenario))
                captureFeed("${mode.name}-${scenario.name}")
                if (scenario == HomeScenario.HISTORICAL) {
                    event(HomeEvent.Section(HomeSection.RECORDS))
                    scroll("section-RECORDS")
                    captureUntil("${mode.name}-historical-records", "records-coverage")
                }
            }
            event(HomeEvent.Scenario(HomeScenario.PINNED))
            rule.onNodeWithTag("home-feed").performScrollToIndex(0)
            screenshot("${mode.name}-pinned-garden-first")
            scroll("record-reading-atlas")
            screenshot("${mode.name}-pinned-atlas-second")
            event(HomeEvent.Scenario(HomeScenario.BOOKMARK))
            scroll("record-reading-atlas")
            rule.onNodeWithTag("bookmark-atlas").assertExists()
            screenshot("${mode.name}-independent-bookmark")
            rule.onNodeWithTag("record-reading-atlas").performClick()
            rule.onNodeWithTag("home-sheet").assertIsDisplayed()
            settleSheet()
            screenshot("${mode.name}-record-reading-entry")
            close()
            event(HomeEvent.Scenario(HomeScenario.MULTIPLE))
            event(HomeEvent.Period(HomePeriod.ALL))
            scroll("home-metrics")
            screenshot("${mode.name}-all-time-metrics")
            event(HomeEvent.Section(HomeSection.CALENDAR))
            scroll("heatmap-scroll")
            rule.onNodeWithTag("heat-2026-11-01").performScrollTo()
            screenshot("${mode.name}-calendar-page-boundaries")
            rule.onNodeWithTag("heat-2026-01-01").performScrollTo()
            screenshot("${mode.name}-calendar-year-start")
            rule.onNodeWithTag("heat-2026-12-31").performScrollTo()
            screenshot("${mode.name}-calendar-year-end")
            rule.onNodeWithTag("heat-2026-11-12").performScrollTo().performClick()
            settleSheet()
            screenshot("${mode.name}-day-entry-by-book")
            close()
            event(HomeEvent.Section(HomeSection.DYNAMICS))
            event(HomeEvent.Grouping(ChartGrouping.MONTH))
            scroll("reading-chart")
            screenshot("${mode.name}-monthly-pages")
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
                        rule.onNodeWithTag("home-feed").performScrollToIndex(0)
                        rule.onNodeWithTag("home-books-heading").assertIsDisplayed()
                        val actionAlreadyVisible = isFullyVisible("record-reading-atlas")
                        screenshot("${mode.name}-font$fontScale-orientation$orientation-heading")
                        scroll("record-reading-atlas")
                        rule.onNodeWithTag("record-reading-atlas").assertIsEnabled()
                        val feed = rule.onNodeWithTag("home-feed").fetchSemanticsNode().boundsInRoot
                        val nav = rule.onNodeWithTag("shell-navigation").fetchSemanticsNode().boundsInRoot
                        assertTrue("The feed must not overlap navigation", feed.bottom <= nav.top + 1f)
                        assertTrue("A usable viewport remains", feed.height > 0f)
                        val density = rule.activity.resources.displayMetrics.density
                        rule.onAllNodes(hasClickAction()).fetchSemanticsNodes().forEach { node ->
                            val size = node.layoutInfo.coordinates.size
                            assertTrue("Touch width >=48dp", size.width / density >= 47.5f)
                            assertTrue("Touch height >=48dp", size.height / density >= 47.5f)
                        }
                        // When the first viewport already contains the complete action,
                        // it proves both states. Do not save an identical second PNG.
                        if (!actionAlreadyVisible) screenshot("${mode.name}-font$fontScale-orientation$orientation-record-action")
                        scroll("metric-pages")
                        captureUntil("${mode.name}-font$fontScale-orientation$orientation-metrics", "metric-streak-label")
                        event(HomeEvent.Section(HomeSection.RECORDS))
                        event(HomeEvent.Period(HomePeriod.MONTH))
                        rule.onNodeWithContentDescription("Настройки").performClick()
                        rule.onNodeWithContentDescription("Назад").performClick()
                        scroll("expanded-RECORDS")
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

    private fun isFullyVisible(tag: String): Boolean {
        val node = rule.onAllNodesWithTag(tag).fetchSemanticsNodes().firstOrNull() ?: return false
        val top = node.layoutInfo.coordinates.positionInRoot().y
        val viewport = rule.onNodeWithTag("home-feed").fetchSemanticsNode().boundsInRoot
        return top >= viewport.top && top + node.layoutInfo.coordinates.size.height <= viewport.bottom
    }

    private fun settleSheet() {
        // A real touch outside the handle dismisses its transient hover tooltip.
        rule.onNodeWithTag("home-sheet-body").performTouchInput { click() }
        rule.onNodeWithTag("home-sheet-close").performScrollTo().assertIsDisplayed()
        // Tooltip dismissal uses Compose's test clock, not the native uptime clock.
        rule.mainClock.advanceTimeBy(5_000)
        rule.waitForIdle()
        rule.onNode(isPopup()).assertDoesNotExist()
    }

    private fun captureFeed(name: String) {
        rule.onNodeWithTag("home-feed").performScrollToIndex(0)
        captureUntil("$name-feed", "home-footer")
    }
    /** Overlapping viewports until the complete last text is visible, not a fixed PNG count. */
    private fun captureUntil(name: String, endTag: String) {
        for (frame in 0 until 30) {
            screenshot("$name-$frame")
            // Metric cards merge descendants for TalkBack; inspect their label's
            // actual coordinates without changing the accessible merged semantics.
            val end = rule.onAllNodesWithTag(endTag, useUnmergedTree = true).fetchSemanticsNodes().firstOrNull()
            val viewport = rule.onNodeWithTag("home-feed").fetchSemanticsNode().boundsInRoot
            if (end != null) {
                val top = end.layoutInfo.coordinates.positionInRoot().y
                val bottom = top + end.layoutInfo.coordinates.size.height
                if (top >= viewport.top && bottom <= viewport.bottom + 1f) return
            }
            scrollFeed()
        }
        fail("The screenshot sequence never reached $endTag")
    }
    private fun scrollFeed() {
        val feed = rule.onNodeWithTag("home-feed")
        val distance = feed.fetchSemanticsNode().boundsInRoot.height * .7f
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
                "API=${Build.VERSION.SDK_INT} | theme=${name.substringBefore('-')} | scenario=${ViewModelProvider(context)[HomeDemoViewModel::class.java].state.value.scenario.name} | capture=$name | size=${bitmap.width}x${bitmap.height} | fontScale=${config.fontScale} | orientation=${config.orientation} | " +
                "density=${context.resources.displayMetrics.densityDpi} | locale=${config.locales[0]} | inputMode=${if (context.window.decorView.isInTouchMode) "touch" else "keyboard"} | emulator; not A55\n")
    }
    private fun command(value: String): String {
        val descriptor = InstrumentationRegistry.getInstrumentation().uiAutomation.executeShellCommand(value)
        return try { FileInputStream(descriptor.fileDescriptor).use { String(it.readBytes()) } } finally { descriptor.close() }
    }
}
