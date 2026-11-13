package ru.gleb.readingjournal

import android.graphics.Bitmap
import androidx.compose.runtime.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import ru.gleb.readingjournal.testing.PreviewHostActivity
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Rule
import org.junit.Test
import ru.gleb.readingjournal.demo.*
import java.io.File

class PreviewRenderTest {
    @get:Rule val rule = createAndroidComposeRule<PreviewHostActivity>()
    @Test fun catalogPreviewsRenderAndPressedAndFocusStatesWork() {
        for (isDark in listOf(false, true)) {
            rule.runOnUiThread { rule.activity.previewState.dark = isDark }
            rule.onNodeWithText("Основное действие").performScrollTo().assertIsDisplayed()
            screenshot("preview-${if (isDark) "dark" else "light"}")
            rule.onNodeWithText("Основное действие").performTouchInput { down(center) }
            screenshot("pressed-$isDark")
            rule.onNodeWithText("Основное действие").performTouchInput { cancel() }
            rule.onNodeWithText("Демонстрационный текст").performScrollTo().performClick().assertIsFocused()
            screenshot("focused-$isDark")
            rule.onNodeWithText("Основное действие").performScrollTo()
        }
    }
    @Test fun previewContentAndThemeSurviveActivityRecreation() {
        for (dark in listOf(false, true)) {
            rule.runOnUiThread { rule.activity.previewState.dark = dark }
            rule.activityRule.scenario.recreate()
            rule.onNodeWithText("Основное действие").performScrollTo().assertIsDisplayed()
            rule.runOnUiThread { org.junit.Assert.assertEquals(dark, rule.activity.previewState.dark) }
            rule.onNodeWithText("Демонстрационный текст").performScrollTo().performClick().assertIsFocused()
        }
    }

    private fun screenshot(name: String) {
        val file = File(InstrumentationRegistry.getInstrumentation().targetContext.filesDir, "screenshots/$name.png")
        file.parentFile?.mkdirs()
        rule.waitForIdle()
        file.outputStream().use { stream ->
            check(requireNotNull(InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot()).compress(Bitmap.CompressFormat.PNG, 100, stream))
        }
    }
}
