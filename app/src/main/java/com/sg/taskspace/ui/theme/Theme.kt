package com.sg.taskspace.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// === AMOLED Dark Color Scheme (Pitch Black) ===
private val AmoledDarkColorScheme = darkColorScheme(
    primary = PrimaryIndigoDark,
    onPrimary = Color.White,
    primaryContainer = PrimaryIndigoContainer,
    onPrimaryContainer = Color(0xFFE0E7FF),
    secondary = SecondaryEmeraldDark,
    onSecondary = Color.Black,
    secondaryContainer = SecondaryEmeraldContainer,
    onSecondaryContainer = Color(0xFFD1FAE5),
    tertiary = PriorityMedium,
    onTertiary = Color.Black,
    background = AmoledBackground,
    onBackground = TextWhite,
    surface = AmoledSurface,
    onSurface = TextWhite,
    surfaceVariant = AmoledCard,
    onSurfaceVariant = TextWhiteMuted,
    outline = AmoledBorder,
    error = PriorityHigh,
    onError = Color.White,
    errorContainer = PriorityHighContainer,
    onErrorContainer = Color(0xFFFFE4E6)
)

// === Standard Dark Color Scheme ===
private val DarkColorScheme = darkColorScheme(
    primary = PrimaryIndigoDark,
    onPrimary = Color.White,
    primaryContainer = PrimaryIndigoContainer,
    onPrimaryContainer = Color(0xFFE0E7FF),
    secondary = SecondaryEmeraldDark,
    onSecondary = Color.Black,
    secondaryContainer = SecondaryEmeraldContainer,
    onSecondaryContainer = Color(0xFFD1FAE5),
    tertiary = PriorityMedium,
    onTertiary = Color.Black,
    background = DarkBackground,
    onBackground = TextWhite,
    surface = DarkSurface,
    onSurface = TextWhite,
    surfaceVariant = DarkCard,
    onSurfaceVariant = TextWhiteMuted,
    outline = DarkBorder,
    error = PriorityHigh,
    onError = Color.White,
    errorContainer = PriorityHighContainer,
    onErrorContainer = Color(0xFFFFE4E6)
)

// === Light Color Scheme ===
private val LightColorScheme = lightColorScheme(
    primary = PrimaryIndigo,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE0E7FF),
    onPrimaryContainer = Color(0xFF3730A3),
    secondary = SecondaryEmerald,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFD1FAE5),
    onSecondaryContainer = Color(0xFF065F46),
    tertiary = PriorityMedium,
    onTertiary = Color.White,
    background = LightBackground,
    onBackground = TextBlack,
    surface = LightSurface,
    onSurface = TextBlack,
    surfaceVariant = LightCard,
    onSurfaceVariant = TextBlackMuted,
    outline = LightBorder,
    error = PriorityHigh,
    onError = Color.White,
    errorContainer = Color(0xFFFFE4E6),
    onErrorContainer = Color(0xFF7F1D1D)
)

@Composable
fun TaskSpaceTheme(
    themeMode: String = "AMOLED Dark",
    fontFamily: String = "Default",
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val isDark = themeMode != "Light"

    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (isDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        themeMode == "AMOLED Dark" -> AmoledDarkColorScheme
        themeMode == "Dark" -> DarkColorScheme
        themeMode == "Light" -> LightColorScheme
        else -> {
            // System Default: use Android dark/light
            val isSystemDark = androidx.compose.foundation.isSystemInDarkTheme()
            if (isSystemDark) DarkColorScheme else LightColorScheme
        }
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !isDark
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = getTypography(fontFamily),
        content = content
    )
}
