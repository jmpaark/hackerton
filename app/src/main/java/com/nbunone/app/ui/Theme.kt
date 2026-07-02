package com.nbunone.app.ui

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.material3.Typography
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import com.nbunone.app.R
import com.nbunone.app.data.FlagType

val Pretendard = FontFamily(
    Font(R.font.pretendard_regular, FontWeight.Normal),
    Font(R.font.pretendard_medium, FontWeight.Medium),
    Font(R.font.pretendard_semibold, FontWeight.SemiBold),
    Font(R.font.pretendard_bold, FontWeight.Bold),
    Font(R.font.pretendard_bold, FontWeight.Black)
)

private val BaseType = Typography()
val AppTypography = Typography(
    displayLarge = BaseType.displayLarge.copy(fontFamily = Pretendard),
    displayMedium = BaseType.displayMedium.copy(fontFamily = Pretendard),
    displaySmall = BaseType.displaySmall.copy(fontFamily = Pretendard),
    headlineLarge = BaseType.headlineLarge.copy(fontFamily = Pretendard, fontWeight = FontWeight.Bold),
    headlineMedium = BaseType.headlineMedium.copy(fontFamily = Pretendard, fontWeight = FontWeight.Bold),
    headlineSmall = BaseType.headlineSmall.copy(fontFamily = Pretendard, fontWeight = FontWeight.Bold),
    titleLarge = BaseType.titleLarge.copy(fontFamily = Pretendard, fontWeight = FontWeight.SemiBold),
    titleMedium = BaseType.titleMedium.copy(fontFamily = Pretendard, fontWeight = FontWeight.SemiBold),
    titleSmall = BaseType.titleSmall.copy(fontFamily = Pretendard, fontWeight = FontWeight.SemiBold),
    bodyLarge = BaseType.bodyLarge.copy(fontFamily = Pretendard, letterSpacing = 0.sp),
    bodyMedium = BaseType.bodyMedium.copy(fontFamily = Pretendard, letterSpacing = 0.sp),
    bodySmall = BaseType.bodySmall.copy(fontFamily = Pretendard, letterSpacing = 0.sp),
    labelLarge = BaseType.labelLarge.copy(fontFamily = Pretendard, fontWeight = FontWeight.Medium),
    labelMedium = BaseType.labelMedium.copy(fontFamily = Pretendard, fontWeight = FontWeight.Medium),
    labelSmall = BaseType.labelSmall.copy(fontFamily = Pretendard, fontWeight = FontWeight.Medium)
)

/** 선택 가능한 액센트 컬러 */
data class Accent(val key: String, val label: String, val light: Color, val dark: Color)

val ACCENTS = listOf(
    Accent("indigo", "인디고", Color(0xFF4F46E5), Color(0xFF8B95F8)),
    Accent("emerald", "에메랄드", Color(0xFF059669), Color(0xFF34D399)),
    Accent("rose", "로즈", Color(0xFFE11D48), Color(0xFFFB7185)),
    Accent("amber", "앰버", Color(0xFFD97706), Color(0xFFFBBF24)),
    Accent("violet", "바이올렛", Color(0xFF7C3AED), Color(0xFFB49AFB))
)

val ChartColors = listOf(
    Color(0xFF6366F1), Color(0xFF10B981), Color(0xFFF59E0B),
    Color(0xFFEF4444), Color(0xFF8B5CF6), Color(0xFF06B6D4)
)

val Green = Color(0xFF10B981)
val Red = Color(0xFFEF4444)
val Amber = Color(0xFFF59E0B)

val LocalDark = staticCompositionLocalOf { false }

// 화면 코드에서 쓰는 어댑티브 컬러 별칭
val Indigo: Color @Composable get() = MaterialTheme.colorScheme.primary
val IndigoLight: Color @Composable get() = MaterialTheme.colorScheme.primaryContainer
val Slate: Color @Composable get() = MaterialTheme.colorScheme.onSurfaceVariant
val CardBg: Color @Composable get() = MaterialTheme.colorScheme.surface
val TrackBg: Color @Composable get() = MaterialTheme.colorScheme.surfaceVariant

/** 플래그 카드 배경/전경 색 (라이트·다크 대응) */
@Composable
fun flagColors(flag: FlagType): Pair<Color, Color> {
    val dark = LocalDark.current
    return when (flag) {
        FlagType.MISMATCH -> if (dark) Color(0xFF33270B) to Color(0xFFFCD34D)
        else Color(0xFFFEF3C7) to Color(0xFF92400E)
        FlagType.FREE_RIDER -> if (dark) Color(0xFF3B1519) to Color(0xFFFCA5A5)
        else Color(0xFFFEE2E2) to Color(0xFF991B1B)
        FlagType.UNSUNG -> if (dark) Color(0xFF13294A) to Color(0xFF93C5FD)
        else Color(0xFFDBEAFE) to Color(0xFF1E40AF)
    }
}

@Composable
fun warnBadgeColors(): Pair<Color, Color> = flagColors(FlagType.MISMATCH)

@Composable
fun successColors(): Pair<Color, Color> =
    if (LocalDark.current) Color(0xFF0C2F24) to Color(0xFF6EE7B7)
    else Color(0xFFD1FAE5) to Color(0xFF065F46)

@Composable
fun NbunoneTheme(
    themeMode: String = "system",
    accentKey: String = "indigo",
    content: @Composable () -> Unit
) {
    val dark = when (themeMode) {
        "dark" -> true
        "light" -> false
        else -> isSystemInDarkTheme()
    }
    val accent = ACCENTS.firstOrNull { it.key == accentKey } ?: ACCENTS[0]

    val scheme = if (dark) {
        val surface = Color(0xFF131B2C)
        darkColorScheme(
            primary = accent.dark,
            onPrimary = Color(0xFF0B1120),
            primaryContainer = accent.dark.copy(alpha = 0.16f).compositeOver(surface),
            onPrimaryContainer = accent.dark,
            secondary = Color(0xFF94A3B8),
            background = Color(0xFF0B1120),
            onBackground = Color(0xFFE5EAF3),
            surface = surface,
            onSurface = Color(0xFFE5EAF3),
            surfaceVariant = Color(0xFF1E293B),
            onSurfaceVariant = Color(0xFF94A3B8),
            outline = Color(0xFF334155),
            outlineVariant = Color(0xFF273349),
            error = Color(0xFFF87171)
        )
    } else {
        lightColorScheme(
            primary = accent.light,
            onPrimary = Color.White,
            primaryContainer = accent.light.copy(alpha = 0.10f).compositeOver(Color.White),
            onPrimaryContainer = accent.light,
            secondary = Color(0xFF64748B),
            background = Color(0xFFF6F7FB),
            onBackground = Color(0xFF0F172A),
            surface = Color.White,
            onSurface = Color(0xFF0F172A),
            surfaceVariant = Color(0xFFEEF1F6),
            onSurfaceVariant = Color(0xFF64748B),
            outline = Color(0xFFCBD5E1),
            outlineVariant = Color(0xFFE2E8F0),
            error = Color(0xFFDC2626)
        )
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = scheme.background.toArgb()
            window.navigationBarColor = scheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !dark
        }
    }

    CompositionLocalProvider(LocalDark provides dark) {
        MaterialTheme(colorScheme = scheme, typography = AppTypography, content = content)
    }
}
