package ru.gleb.readingjournal.app

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class ThemeMode(val label: String) {
    LIGHT("Светлая"), DARK("Тёмная"), SYSTEM("Системная");
    fun isDark(systemDark: Boolean) = when (this) {
        LIGHT -> false; DARK -> true; SYSTEM -> systemDark
    }
}
data class ShellState(val theme: ThemeMode = ThemeMode.SYSTEM)
class ShellViewModel : ViewModel() {
    private val mutableState = MutableStateFlow(ShellState())
    val state = mutableState.asStateFlow()
    fun selectTheme(theme: ThemeMode) { mutableState.value = mutableState.value.copy(theme = theme) }
}
