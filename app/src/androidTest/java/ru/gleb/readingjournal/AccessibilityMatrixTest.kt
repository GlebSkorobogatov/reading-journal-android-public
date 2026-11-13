package ru.gleb.readingjournal

import android.graphics.Bitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Rule
import org.junit.Test
import ru.gleb.readingjournal.app.*
import java.io.File
import java.io.FileInputStream
import kotlin.math.abs

/** Uses Android system font_scale, including separate dialog windows, not a local density override. */
class AccessibilityMatrixTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()
    @Test fun themesAtAllRequiredFontScales() {
        val originalScale = rule.activity.resources.configuration.fontScale
        try {
            for (theme in listOf(ThemeMode.LIGHT, ThemeMode.DARK)) {
                for (fontScale in listOf(1f, 1.3f, 1.5f, 2f)) {
                    systemFontScale(fontScale)
                    rule.waitUntil(30_000) { abs(rule.activity.resources.configuration.fontScale - fontScale) < 0.01f }
                    rule.onNode(hasText("Библиотека") and hasClickAction()).performClick()
                    rule.onNodeWithText("Библиотека · UI-01").assertIsDisplayed()
                    rule.onNodeWithContentDescription("Настройки").performClick()
                    rule.onNode(hasText(theme.label, substring = true) and hasClickAction()).performScrollTo().performClick()
                    rule.onNodeWithText("Каталог компонентов · debug").performScrollTo().performClick()
                    rule.onNodeWithText("Недоступное действие").performScrollTo().assertIsNotEnabled()
                    rule.onNodeWithText("Открыть диалог").performScrollTo().performClick()
                    rule.onNodeWithText("Закрыть").assertIsDisplayed()
                    save("dialog-${theme.name}-$fontScale")
                    rule.onNodeWithText("Закрыть").performClick()
                    rule.onNodeWithContentDescription("Назад").performClick()
                    rule.onNodeWithContentDescription("Назад").performClick()
                    rule.onNode(hasText("Главная") and hasClickAction()).performClick()
                }
            }
        } finally {
            systemFontScale(originalScale)
            rule.waitUntil(30_000) {
                abs(rule.activity.resources.configuration.fontScale - originalScale) < 0.01f
            }
        }
    }
    private fun systemFontScale(scale: Float) {
        val descriptor = InstrumentationRegistry.getInstrumentation().uiAutomation.executeShellCommand("settings put system font_scale $scale")
        FileInputStream(descriptor.fileDescriptor).use { it.readBytes() }
        descriptor.close()
    }
    private fun save(name: String) {
        val file = File(rule.activity.filesDir, "screenshots/$name.png")
        file.parentFile?.mkdirs()
        rule.waitForIdle()
        file.outputStream().use { stream ->
            check(requireNotNull(InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot())
                .compress(Bitmap.CompressFormat.PNG, 100, stream))
        }
    }
}
