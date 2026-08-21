package com.sg.taskspace.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.material3.ColorScheme
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

fun buildColorScheme(themeMode: String, colorPalette: String, isSystemDark: Boolean): ColorScheme {
    val isDark = themeMode != "Light" && (themeMode != "System" || isSystemDark)

    val (primaryColor, containerColor) = when (colorPalette) {
        "Emerald" -> if (isDark) Color(0xFF34D399) to Color(0xFF065F46) else Color(0xFF10B981) to Color(0xFFD1FAE5)
        "Rose" -> if (isDark) Color(0xFFFB7185) to Color(0xFF881337) else Color(0xFFF43F5E) to Color(0xFFFFE4E6)
        "Amber" -> if (isDark) Color(0xFFFBBF24) to Color(0xFF78350F) else Color(0xFFF59E0B) to Color(0xFFFEF3C7)
        "Violet" -> if (isDark) Color(0xFFA78BFA) to Color(0xFF4C1D95) else Color(0xFF8B5CF6) to Color(0xFFEDE9FE)
        "Teal" -> if (isDark) Color(0xFF2DD4BF) to Color(0xFF134E4A) else Color(0xFF14B8A6) to Color(0xFFCCFBF1)
        "Monochrome" -> {
            if (themeMode == "AMOLED Dark") Color(0xFFFFFFFF) to Color(0xFF141416)
            else if (isDark) Color(0xFFFFFFFF) to Color(0xFF202023)
            else Color(0xFF000000) to Color(0xFFF1F5F9)
        }
        else -> if (isDark) Color(0xFF818CF8) to Color(0xFF3730A3) else Color(0xFF6366F1) to Color(0xFFE0E7FF) // Indigo default
    }

    val onPrimaryColor = when {
        colorPalette == "Monochrome" && isDark -> Color.Black
        colorPalette == "Monochrome" && !isDark -> Color.White
        else -> Color.White
    }
    val isMonoAmoled = themeMode == "AMOLED Dark" && colorPalette == "Monochrome"

    return when (themeMode) {
        "AMOLED Dark" -> darkColorScheme(
            primary = primaryColor,
            onPrimary = onPrimaryColor,
            primaryContainer = if (colorPalette == "Monochrome") Color(0xFF18181B) else containerColor,
            onPrimaryContainer = Color.White,
            secondary = if (colorPalette == "Monochrome") Color(0xFFE4E4E7) else SecondaryEmeraldDark,
            onSecondary = Color.Black,
            secondaryContainer = if (colorPalette == "Monochrome") Color(0xFF18181B) else SecondaryEmeraldContainer,
            onSecondaryContainer = Color(0xFFD1FAE5),
            tertiary = PriorityMedium,
            onTertiary = Color.Black,
            background = AmoledBackground,
            onBackground = TextWhite,
            surface = AmoledSurface,
            onSurface = TextWhite,
            surfaceVariant = if (isMonoAmoled) Color(0xFF09090A) else Color(0xFF0C0C0E),
            onSurfaceVariant = TextWhiteMuted,
            outline = if (isMonoAmoled) Color(0xFF1A1A1D) else Color(0xFF1E1E22),
            error = PriorityHigh,
            onError = Color.White,
            errorContainer = PriorityHighContainer,
            onErrorContainer = Color(0xFFFFE4E6)
        )
        "Dark" -> darkColorScheme(
            primary = primaryColor,
            onPrimary = onPrimaryColor,
            primaryContainer = containerColor,
            onPrimaryContainer = Color.White,
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
        "Light" -> lightColorScheme(
            primary = primaryColor,
            onPrimary = onPrimaryColor,
            primaryContainer = containerColor,
            onPrimaryContainer = Color(0xFF1E1B4B),
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
        else -> { // System Default
            if (isSystemDark) {
                darkColorScheme(
                    primary = primaryColor,
                    onPrimary = onPrimaryColor,
                    primaryContainer = containerColor,
                    onPrimaryContainer = Color.White,
                    background = DarkBackground,
                    onBackground = TextWhite,
                    surface = DarkSurface,
                    onSurface = TextWhite,
                    surfaceVariant = DarkCard,
                    onSurfaceVariant = TextWhiteMuted
                )
            } else {
                lightColorScheme(
                    primary = primaryColor,
                    onPrimary = onPrimaryColor,
                    primaryContainer = containerColor,
                    onPrimaryContainer = Color(0xFF1E1B4B),
                    background = LightBackground,
                    onBackground = TextBlack,
                    surface = LightSurface,
                    onSurface = TextBlack,
                    surfaceVariant = LightCard,
                    onSurfaceVariant = TextBlackMuted
                )
            }
        }
    }
}

@Composable
fun TaskSpaceTheme(
    themeMode: String = "AMOLED Dark",
    fontFamily: String = "Default",
    colorPalette: String = "Indigo",
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val isDark = themeMode != "Light"
    val isSystemDark = androidx.compose.foundation.isSystemInDarkTheme()

    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (isDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        else -> buildColorScheme(themeMode, colorPalette, isSystemDark)
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
