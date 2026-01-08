package com.lojasocial.app.ui.theme

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
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = LojaSocialPrimary,
    onPrimary = LojaSocialOnPrimary,
    background = Color.Black,
    onBackground = Color.White,
    surface = Color(0xFF1F1F1F),
    onSurface = Color(0xFFD1D5DB),
    onSurfaceVariant = Color(0xFF9CA3AF),
    secondary = PurpleGrey80,
    tertiary = Pink80
)

private val LightColorScheme = lightColorScheme(
    primary = LojaSocialPrimary,
    onPrimary = LojaSocialOnPrimary,
    background = LojaSocialBackground,
    onBackground = LojaSocialOnBackground,
    surface = LojaSocialSurface,
    onSurface = LojaSocialOnSurface,
    onSurfaceVariant = LojaSocialOnSurfaceVariant,
    secondary = PurpleGrey40,
    tertiary = Pink40
)

@Composable
fun LojaSocialTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
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
    val statusBarColor = colorScheme.background
    
    SideEffect {
        val window = (view.context as? Activity)?.window ?: return@SideEffect
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = statusBarColor.toArgb()
        
        // Set status bar icon color (dark icons for light background, light icons for dark background)
        val insetsController = WindowCompat.getInsetsController(window, view)
        insetsController.isAppearanceLightStatusBars = !darkTheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}