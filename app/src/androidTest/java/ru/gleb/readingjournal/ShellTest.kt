package ru.gleb.readingjournal

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import ru.gleb.readingjournal.app.MainActivity

@RunWith(AndroidJUnit4::class)
class ShellTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()
    @Test fun tabsSettingsBackAndRotation() {
        rule.onNodeWithText("Главная · UI-01").assertIsDisplayed()
        rule.onNodeWithText("Библиотека", useUnmergedTree = true).performClick()
        rule.onNodeWithText("Библиотека · UI-01").assertIsDisplayed()
        rule.onNode(hasText("Библиотека") and hasClickAction()).assertIsSelected()
        rule.onNodeWithContentDescription("Настройки").performClick()
        rule.activityRule.scenario.recreate()
        rule.onNodeWithText("Тема").assertIsDisplayed()
        rule.onNodeWithContentDescription("Назад").performClick()
        rule.onNodeWithText("Библиотека · UI-01").assertIsDisplayed()
        rule.onNodeWithText("Достижения").performClick()
        rule.onNodeWithText("Достижения · UI-01").assertIsDisplayed()
        rule.runOnUiThread { rule.activity.onBackPressedDispatcher.onBackPressed() }
        rule.onNodeWithText("Главная · UI-01").assertIsDisplayed()
    }
    @Test fun themesAndComponentSemantics() {
        rule.onNodeWithContentDescription("Настройки").performClick()
        rule.onNodeWithText("Тёмная").performClick()
        rule.onNodeWithTag("theme-true").assertExists()
        rule.activityRule.scenario.recreate()
        rule.onNodeWithText("✓ Тёмная").assertIsSelected()
        rule.onNodeWithText("Светлая").performClick()
        rule.onNodeWithTag("theme-false").assertExists()
        rule.onNodeWithText("Каталог компонентов · debug").performScrollTo().performClick()
        rule.onNodeWithText("Недоступное действие").performScrollTo().assertIsNotEnabled()
        rule.onNodeWithContentDescription("Недоступная кнопка-иконка").performScrollTo().assertIsNotEnabled()
        rule.onNodeWithText("Длинная русскоязычная подпись выбранного элемента").performScrollTo().performClick()
        rule.onNodeWithText("✓ Длинная русскоязычная подпись выбранного элемента").assertIsSelected()
        rule.onNodeWithText("Поле с ошибкой").performScrollTo().assertExists()
        rule.onNodeWithText("Неактивное поле").performScrollTo().assertIsNotEnabled()
        rule.onNodeWithText("— · Данные: неизвестно").performScrollTo().assertIsDisplayed()
        rule.onNodeWithText("Открыть диалог").performScrollTo().performClick()
        rule.onNodeWithText("Демонстрационный диалог").assertIsDisplayed()
        rule.onNodeWithText("Закрыть").performClick()
        rule.onNodeWithText("Открыть нижнюю панель").performScrollTo().performClick()
        rule.onNodeWithText("Демонстрационная нижняя панель").assertIsDisplayed()
        rule.onNodeWithText("Закрыть панель").performScrollTo().performClick()
    }
}
