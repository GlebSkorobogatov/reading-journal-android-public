package ru.gleb.readingjournal.demo

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import ru.gleb.readingjournal.app.ThemeMode
import ru.gleb.readingjournal.ui.designsystem.*

@Composable
fun ComponentCatalog() {
    val coverBackground = LocalJournalColors.current.accentSoft
    val coverLine = LocalJournalColors.current.accent
    val sampleCover = remember(coverBackground, coverLine) { object : Painter() {
        override val intrinsicSize = Size(300f, 100f)
        override fun DrawScope.onDraw() {
            drawRect(coverBackground)
            drawLine(coverLine, Offset.Zero, Offset(size.width, size.height), strokeWidth = 4f)
            drawLine(coverLine, Offset(size.width, 0f), Offset(0f, size.height), strokeWidth = 4f)
        }
    } }
    var text by remember { mutableStateOf("") }
    var selected by remember { mutableStateOf(false) }
    var dialog by remember { mutableStateOf(false) }
    var sheet by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().navigationBarsPadding().verticalScroll(rememberScrollState())
        .padding(Space.lg), verticalArrangement = Arrangement.spacedBy(Space.lg)) {
        Text("Каталог UI-01 · только демонстрация", Modifier.semantics { heading() }, style = JournalType.screen)
        Text("Офлайн-компоненты. Нажатия не изменяют книги или пользовательские данные.")
        JournalCard {
            Text("Кнопки и состояния", style = JournalType.section)
            ButtonKind.entries.forEach { kind ->
                JournalButton(when (kind) {
                    ButtonKind.PRIMARY -> "Основное действие"
                    ButtonKind.SECONDARY -> "Вторичное действие"
                    ButtonKind.DANGER -> "Опасное действие · демо"
                    ButtonKind.TEXT -> "Текстовая кнопка"
                }, { dialog = true }, kind = kind)
            }
            JournalButton("Недоступное действие", {}, enabled = false)
            JournalIconButton("Настройки · пример", { dialog = true }) { LineIcon("Настройки") }
            JournalIconButton("Недоступная кнопка-иконка", {}, enabled = false) { LineIcon("Настройки") }
            Text("Удерживайте кнопку для pressed; аппаратная клавиатура показывает focus.", style = JournalType.secondary)
        }
        JournalCard {
            Text("Статусы и выбор", style = JournalType.section)
            ReadingStatus.entries.forEach { JournalStatus(it) }
            JournalChip("Длинная русскоязычная подпись выбранного элемента", selected, { selected = !selected })
            JournalChip("Недоступный чип", false, {}, enabled = false)
        }
        JournalCard {
            Text("Поля", style = JournalType.section)
            JournalField(text, { text = it }, "Демонстрационный текст")
            JournalField("Ошибка", {}, "Поле с ошибкой", errorMessage = "Пример: проверьте введённое значение")
            JournalField("Недоступно", {}, "Неактивное поле", enabled = false)
            JournalProgress(0.4f, "Демонстрационный прогресс")
            JournalProgress(0f, "Подтверждённый ноль")
            JournalProgress(null, "Данные")
        }
        JournalCard(large = true) {
            Text("Обложка без обрезки", style = JournalType.section)
            // Narrow painter inside a 2:3 container demonstrates ContentScale.Fit.
            JournalCover("Демонстрационный литературный заголовок", "Автор", "demo", Modifier.width(160.dp))
            JournalCover("Пример изображения", "Демо", "image", Modifier.width(160.dp),
                painter = sampleCover)
        }
        JournalEmpty("Пустое состояние", "Здесь нет данных. Это отдельный пример компонента, а не готовый экран Библиотеки.")
        JournalCard {
            Text("Типографика", style = JournalType.section)
            listOf(
                "Заголовок экрана" to JournalType.screen, "Заголовок секции" to JournalType.section,
                "Заголовок карточки" to JournalType.card, "Название книги" to JournalType.book,
                "0123456789" to JournalType.number, "Основной русский текст" to JournalType.body,
                "Вторичный текст" to JournalType.secondary, "Компактная подпись" to JournalType.caption,
                "Кнопка" to JournalType.button, "Навигация" to JournalType.navigation,
                "Литературная запись — пример кириллицы" to JournalType.literary
            ).forEach { (label, style) -> Text(label, style = style) }
        }
        JournalCard {
            Text("Палитра · образцы без расчётов", style = JournalType.section)
            val c = LocalJournalColors.current
            listOf("Фон" to c.background, "Поверхность" to c.surface, "Акцент" to c.accent,
                "Успех" to c.success, "Предупреждение" to c.warning, "Ошибка" to c.error).forEach { (label, color) ->
                Text(label); Box(Modifier.fillMaxWidth().height(Space.xl).background(color))
            }
            Text("Heatmap: только образцы токенов, не статистика", style = JournalType.secondary)
            (if (LocalJournalDark.current) DarkHeatmap else LightHeatmap).forEachIndexed { i, color ->
                Text("Токен $i"); Box(Modifier.fillMaxWidth().height(Space.lg).background(color))
            }
        }
        JournalButton("Открыть диалог", { dialog = true })
        JournalButton("Открыть нижнюю панель", { sheet = true }, kind = ButtonKind.SECONDARY)
        JournalTopBar("Пример верхней панели") {
            JournalIconButton("Пример настроек", { dialog = true }) { LineIcon("Настройки") }
        }
        JournalBottomBar(listOf("Главная", "Библиотека", "Достижения"), "Главная", {}) { LineIcon(it) }
    }
    if (dialog) JournalDialog("Демонстрационный диалог", "Никакое реальное действие не выполнено. Можно закрыть кнопкой, касанием снаружи или системным «Назад».", { dialog = false })
    if (sheet) JournalSheet({ sheet = false }) {
        Text("Демонстрационная нижняя панель", Modifier.semantics { heading() }, style = JournalType.section)
        Text("Базовый прокручиваемый контейнер. Функции следующих этапов отсутствуют.")
    }
}

@Preview(name = "UI-01 Light", locale = "ru", widthDp = 393, heightDp = 852, showBackground = true)
@Composable
fun CatalogLightPreview() { JournalTheme(ThemeMode.LIGHT) { Surface { ComponentCatalog() } } }
@Preview(name = "UI-01 Dark", locale = "ru", widthDp = 393, heightDp = 852, showBackground = true)
@Composable
fun CatalogDarkPreview() { JournalTheme(ThemeMode.DARK) { Surface { ComponentCatalog() } } }
@Preview(name = "UI-01 Large text", locale = "ru", widthDp = 393, heightDp = 852, fontScale = 2f)
@Composable
fun CatalogLargePreview() { JournalTheme { Surface { ComponentCatalog() } } }
