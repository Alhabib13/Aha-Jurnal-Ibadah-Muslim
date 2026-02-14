package com.islami.Aha.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColorScheme = lightColorScheme(
    primary = Emerald,
    onPrimary = Color.White,
    primaryContainer = EmeraldLight,
    onPrimaryContainer = Gray900,

    secondary = Gold,
    onSecondary = Color.White,
    secondaryContainer = GoldLight,
    onSecondaryContainer = Gray900,

    tertiary = Gold,
    onTertiary = Gray900,
    tertiaryContainer = GoldLight,
    onTertiaryContainer = Gray900,

    background = BackgroundLight,
    onBackground = Gray900,

    surface = SurfaceWhite,
    onSurface = Gray900,
    surfaceVariant = Gray100,
    onSurfaceVariant = Gray600,

    error = ErrorRed,
    onError = Color.White,
    errorContainer = ErrorRed.copy(alpha = 0.1f),
    onErrorContainer = ErrorRed,

    outline = Gray300,
    outlineVariant = Gray200,

    scrim = Gray900.copy(alpha = 0.5f)
)

private val DarkColorScheme = darkColorScheme(
    primary = EmeraldMuted,
    onPrimary = Color.White,
    primaryContainer = EmeraldDark,
    onPrimaryContainer = EmeraldLight,

    secondary = GoldMuted,
    onSecondary = Color.White,
    secondaryContainer = Gold.copy(alpha = 0.3f),
    onSecondaryContainer = GoldMuted,

    tertiary = GoldMuted,
    onTertiary = Color.White,
    tertiaryContainer = Gold.copy(alpha = 0.3f),
    onTertiaryContainer = GoldMuted,

    background = BackgroundDark,
    onBackground = TextPrimaryDark,

    surface = SurfaceDark,
    onSurface = TextPrimaryDark,
    surfaceVariant = CardDark,
    onSurfaceVariant = TextSecondaryDark,

    error = ErrorRed,
    onError = Color.White,
    errorContainer = ErrorRed.copy(alpha = 0.2f),
    onErrorContainer = ErrorRed,

    outline = Gray600,
    outlineVariant = Gray700,

    scrim = Color.Black.copy(alpha = 0.7f)
)

@Composable
fun HabitIslamiTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !darkTheme
                isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
