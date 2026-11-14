package ru.gleb.readingjournal.demo

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import ru.gleb.readingjournal.app.ThemeMode
import ru.gleb.readingjournal.ui.designsystem.*
import ru.gleb.readingjournal.ui.home.*
import java.time.LocalDate

@Composable
fun HomeRoute(state: HomeUiState, onEvent: (HomeEvent) -> Unit) {
    HomeScreen(state, HomeFixtures.chart(state), HomeFixtures.year(state), onEvent)
}

@Composable
fun HomeScenarioPicker(selected: HomeScenario, onSelect: (HomeScenario) -> Unit) {
    Column(Modifier.fillMaxSize().navigationBarsPadding().verticalScroll(rememberScrollState()).padding(Space.lg),
        verticalArrangement = Arrangement.spacedBy(Space.md)) {
        Text("Главная · сценарии UI-02", Modifier.semantics { heading() }, style = JournalType.section)
        Text("Только локальные примеры. Выбор сбрасывает демонстрационное состояние и возвращает на Главную.")
        HomeScenario.entries.forEach { scenario -> JournalChip(scenario.label, scenario == selected,
            { onSelect(scenario) }, Modifier.fillMaxWidth()) }
    }
}

@Preview(name = "UI-02 Light", locale = "ru", widthDp = 393, heightDp = 852)
@Composable
fun HomeLightPreview() { JournalTheme(ThemeMode.LIGHT) { Surface { HomeRoute(HomeFixtures.create()) {} } } }
@Preview(name = "UI-02 Dark", locale = "ru", widthDp = 393, heightDp = 852)
@Composable
fun HomeDarkPreview() { JournalTheme(ThemeMode.DARK) { Surface { HomeRoute(HomeFixtures.create()) {} } } }
@Preview(name = "UI-02 Large text", locale = "ru", widthDp = 393, heightDp = 852, fontScale = 2f)
@Composable
fun HomeLargePreview() { JournalTheme { Surface { HomeRoute(HomeFixtures.create(HomeScenario.LONG_TEXT)) {} } } }
