package ru.gleb.readingjournal.app

import androidx.lifecycle.SavedStateHandle
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
class ShellViewModel(private val saved: SavedStateHandle) : ViewModel() {
    private val mutableState = MutableStateFlow(ShellState())
    val state = mutableState.asStateFlow()
    fun selectTheme(theme: ThemeMode) { mutableState.value = mutableState.value.copy(theme = theme) }

    /**
     * Selected root page. [androidx.compose.foundation.pager.rememberPagerState] also writes the
     * page into the instance bundle, but a second configuration change can save that bundle before
     * the restored page is applied, so the next activity comes back on «Читаю сейчас».
     * This handle is already in memory, so the later save keeps the page the reader actually left.
     */
    var rootPage: Int
        get() = saved[ROOT_PAGE] ?: 0
        set(value) { saved[ROOT_PAGE] = value }

    private companion object {
        const val ROOT_PAGE = "rootPage"
    }
}
