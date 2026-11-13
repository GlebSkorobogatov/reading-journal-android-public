package ru.gleb.readingjournal

import android.content.res.Configuration
import android.graphics.Bitmap
import androidx.test.platform.app.InstrumentationRegistry
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import org.junit.Rule
import org.junit.Test
import ru.gleb.readingjournal.app.*
import ru.gleb.readingjournal.demo.*
import ru.gleb.readingjournal.ui.designsystem.*
import java.io.File

/** Actual emulator renders, not approved golden images and not physical A55 evidence. */
class RenderingTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()
    @Test fun bothThemesAndLargeTextRemainNavigable() {
        rule.onNodeWithContentDescription("Настройки").performClick()
        for (theme in listOf("Светлая", "Тёмная")) {
            rule.onNodeWithText(theme).performScrollTo().performClick()
            screenshot("settings-$theme")
        }
        rule.onNodeWithText("Каталог компонентов · debug").performScrollTo().performClick()
        screenshot("catalog-dark")
        rule.onNodeWithText("Открыть нижнюю панель").performScrollTo().performClick()
        screenshot("sheet-dark")
        rule.onNodeWithText("Закрыть панель").performScrollTo().performClick()
    }
    private fun screenshot(name: String) {
        val context = rule.activity
        val target = File(context.filesDir, "screenshots/$name.png")
        target.parentFile?.mkdirs()
        rule.waitForIdle()
        target.outputStream().use { stream ->
            check(requireNotNull(InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot()).compress(Bitmap.CompressFormat.PNG, 100, stream))
        }
    }
}
