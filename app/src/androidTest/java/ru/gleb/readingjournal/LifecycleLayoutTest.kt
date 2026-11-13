package ru.gleb.readingjournal

import android.content.pm.ActivityInfo
import android.content.res.Configuration
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.Lifecycle
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Rule
import org.junit.Test
import ru.gleb.readingjournal.app.MainActivity
import java.io.FileInputStream

class LifecycleLayoutTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()
    @Test fun landscapeLargeTextAndBackgroundPreserveContext() {
        val originalScale = rule.activity.resources.configuration.fontScale
        val originalOrientation = rule.activity.resources.configuration.orientation
        try {
            rule.onNode(hasText("Библиотека") and hasClickAction()).performClick()
            rule.activityRule.scenario.moveToState(Lifecycle.State.CREATED)
            rule.activityRule.scenario.moveToState(Lifecycle.State.RESUMED)
            rule.onNodeWithText("Библиотека · UI-01").assertIsDisplayed()
            command("settings put system font_scale 2.0")
            rule.runOnUiThread { rule.activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE }
            rule.waitUntil(30_000) {
                val c = rule.activity.resources.configuration
                c.orientation == Configuration.ORIENTATION_LANDSCAPE && c.fontScale >= 1.99f
            }
            rule.onNodeWithContentDescription("Настройки").performClick()
            rule.onNodeWithText("Каталог компонентов · debug").performScrollTo().performClick()
            rule.onNodeWithText("Открыть диалог").performScrollTo().performClick()
            rule.onNodeWithText("Закрыть").assertIsDisplayed().performClick()
            rule.onNodeWithContentDescription("Назад").performClick()
            rule.onNodeWithContentDescription("Назад").performClick()
            rule.onNode(hasText("Библиотека") and hasClickAction()).assertIsSelected()
        } finally {
            command("settings put system font_scale $originalScale")
            rule.runOnUiThread { rule.activity.requestedOrientation = if (originalOrientation == Configuration.ORIENTATION_LANDSCAPE)
                ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE else ActivityInfo.SCREEN_ORIENTATION_PORTRAIT }
            // Shell settings/configuration changes are asynchronous on API 26. Do not
            // finish this test while its cleanup can still recreate the next host.
            rule.waitUntil(30_000) {
                val config = rule.activity.resources.configuration
                kotlin.math.abs(config.fontScale - originalScale) < 0.01f &&
                    config.orientation == originalOrientation
            }
        }
    }
    private fun command(command: String) {
        val descriptor = InstrumentationRegistry.getInstrumentation().uiAutomation.executeShellCommand(command)
        FileInputStream(descriptor.fileDescriptor).use { it.readBytes() }
        descriptor.close()
    }
}
