package ru.gleb.readingjournal.ui.designsystem

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.gleb.readingjournal.R
import ru.gleb.readingjournal.app.ThemeMode

@Immutable
data class JournalColors(
    val background: Color,
    val surface: Color,
    val surfaceMuted: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val outline: Color,
    val outlineStrong: Color,
    val accent: Color,
    val onAccent: Color,
    val accentSoft: Color,
    val onAccentSoft: Color,
    val progressTrack: Color,
    val success: Color,
    val warning: Color,
    val error: Color,
    val disabledText: Color
)
val LightColors = JournalColors(
    background = Color(0xFFF7F4ED),
    surface = Color(0xFFFFFCF6),
    surfaceMuted = Color(0xFFEEE9DF),
    textPrimary = Color(0xFF2B2924),
    textSecondary = Color(0xFF625C52),
    outline = Color(0xFFDED7CA),
    outlineStrong = Color(0xFF8B8070),
    accent = Color(0xFF98583F),
    onAccent = Color(0xFFFFFFFF),
    accentSoft = Color(0xFFF1DFD3),
    onAccentSoft = Color(0xFF75422E),
    progressTrack = Color(0xFFE5DED1),
    success = Color(0xFF426A50),
    warning = Color(0xFF805B23),
    error = Color(0xFFA24038),
    disabledText = Color(0xFF777064)
)
val DarkColors = JournalColors(
    background = Color(0xFF262420),
    surface = Color(0xFF302D28),
    surfaceMuted = Color(0xFF3B3730),
    textPrimary = Color(0xFFF3EEE5),
    textSecondary = Color(0xFFC1B7A9),
    outline = Color(0xFF514B41),
    outlineStrong = Color(0xFF8F8474),
    accent = Color(0xFFD99A7D),
    onAccent = Color(0xFF241914),
    accentSoft = Color(0xFF49362D),
    onAccentSoft = Color(0xFFF2BFA6),
    progressTrack = Color(0xFF494238),
    success = Color(0xFFA5C6A9),
    warning = Color(0xFFE3BD76),
    error = Color(0xFFF0A29A),
    disabledText = Color(0xFFA29788)
)

val LightHeatmap = listOf(0xFFE5DED1, 0xFFE9D0BF, 0xFFD9AB91, 0xFFB97A5D, 0xFF98583F).map { Color(it) }
val DarkHeatmap = listOf(0xFF494238, 0xFF634438, 0xFF885A47, 0xFFB97A5D, 0xFFD99A7D).map { Color(it) }
val CoverColors = listOf(0xFFE4D3B6, 0xFFE6C9B9, 0xFFD3D8C0, 0xFFDFD8CC, 0xFFC9C2B6).map { Color(it) }
val CoverText = Color(0xFF2B2924)
val LocalJournalColors = staticCompositionLocalOf { LightColors }
val LocalJournalDark = staticCompositionLocalOf { false }
object Space {
    val xs = 4.dp; val sm = 8.dp; val md = 12.dp
    val lg = 16.dp; val xl = 24.dp; val xxl = 32.dp
}
object JournalShape {
    val card = RoundedCornerShape(16.dp)
    val largeCard = RoundedCornerShape(20.dp)
    val control = RoundedCornerShape(12.dp)
    val capsule = RoundedCornerShape(50)
    val sheet = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
}
object Metric {
    val border = 1.dp; val focusBorder = 2.dp; val shadow = 0.dp
    val touch = 48.dp; val icon = 24.dp; val compactIcon = 20.dp
    val navigation = 64.dp; val topBar = 56.dp; val timer = 56.dp
    val indicatorWidth = 24.dp; val indicatorHeight = 2.dp; val progress = 8.dp
    const val coverRatio = 2f / 3f
}
object Motion {
    const val fast = 150; const val normal = 200; const val transition = 250
    // Compose animation APIs apply Android's animator duration scale, including zero.
}
@OptIn(androidx.compose.ui.text.ExperimentalTextApi::class)
private fun family(resource: Int, weights: List<Int>) = FontFamily(
    weights.map { weight -> Font(resource, FontWeight(weight), variationSettings =
        FontVariation.Settings(FontVariation.weight(weight))) }
)
val Lora = family(R.font.lora, listOf(400, 600))
val Manrope = family(R.font.manrope, listOf(400, 500, 600, 700))
object JournalType {
    private fun style(family: FontFamily, size: Int, line: Int, weight: Int) = TextStyle(
        fontFamily = family, fontSize = size.sp, lineHeight = line.sp,
        fontWeight = FontWeight(weight), letterSpacing = 0.sp)
    val screen = style(Lora, 28, 36, 600)
    val section = style(Lora, 22, 30, 600)
    val card = style(Manrope, 16, 23, 600)
    val book = style(Lora, 24, 32, 600)
    val number = style(Manrope, 28, 35, 700).copy(fontFeatureSettings = "tnum")
    val body = style(Manrope, 16, 24, 400)
    val secondary = style(Manrope, 14, 20, 400)
    val caption = style(Manrope, 12, 17, 500)
    val button = style(Manrope, 14, 20, 600)
    val navigation = style(Manrope, 12, 17, 600)
    val literary = style(Lora, 16, 25, 400)
}
@Composable
fun JournalTheme(mode: ThemeMode = ThemeMode.SYSTEM, content: @Composable () -> Unit) {
    val dark = mode.isDark(isSystemInDarkTheme())
    val c = if (dark) DarkColors else LightColors
    val scheme = (if (dark) darkColorScheme() else lightColorScheme()).copy(
        primary = c.accent, onPrimary = c.onAccent,
        primaryContainer = c.accentSoft, onPrimaryContainer = c.onAccentSoft,
        secondary = c.textSecondary, onSecondary = c.surface,
        secondaryContainer = c.surfaceMuted, onSecondaryContainer = c.textPrimary,
        tertiary = c.success, onTertiary = c.background,
        tertiaryContainer = c.surfaceMuted, onTertiaryContainer = c.success,
        background = c.background, onBackground = c.textPrimary,
        surface = c.surface, onSurface = c.textPrimary,
        surfaceVariant = c.surfaceMuted, onSurfaceVariant = c.textSecondary,
        surfaceTint = c.accent, outline = c.outlineStrong, outlineVariant = c.outline,
        error = c.error, onError = c.background,
        errorContainer = c.surfaceMuted, onErrorContainer = c.error,
        inverseSurface = c.textPrimary, inverseOnSurface = c.background,
        inversePrimary = if (dark) LightColors.accent else DarkColors.accent,
        surfaceContainer = c.surface, surfaceContainerHigh = c.surface,
        surfaceContainerHighest = c.surfaceMuted, surfaceContainerLow = c.surface,
        surfaceContainerLowest = c.background, surfaceBright = c.surface,
        surfaceDim = c.background)
    CompositionLocalProvider(LocalJournalColors provides c, LocalJournalDark provides dark) {
        MaterialTheme(colorScheme = scheme,
            typography = Typography(
                displayLarge = JournalType.screen, displayMedium = JournalType.screen,
                displaySmall = JournalType.screen, headlineLarge = JournalType.screen,
                headlineMedium = JournalType.book, headlineSmall = JournalType.section,
                titleLarge = JournalType.section, titleMedium = JournalType.card,
                titleSmall = JournalType.card, bodyLarge = JournalType.body,
                bodyMedium = JournalType.secondary, bodySmall = JournalType.caption,
                labelLarge = JournalType.button, labelMedium = JournalType.navigation,
                labelSmall = JournalType.caption),
            shapes = Shapes(small = JournalShape.control, medium = JournalShape.card,
                large = JournalShape.largeCard), content = content)
    }
}
