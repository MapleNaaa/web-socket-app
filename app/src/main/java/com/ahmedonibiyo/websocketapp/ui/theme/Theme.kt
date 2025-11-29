package com.ahmedonibiyo.websocketapp.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK
}

private val WebSocketLightColorScheme = lightColorScheme(
    primary = WebSocketPrimaryLight,
    onPrimary = Color.White,
    secondary = WebSocketSecondaryLight,
    onSecondary = Color.White,
    tertiary = WebSocketTertiaryLight,
    onTertiary = Color.White,
    background = WebSocketBackgroundLight,
    onBackground = Color.Black,
    surface = WebSocketSurfaceLight,
    onSurface = Color.Black,
    outline = WebSocketOutlineLight
)

private val WebSocketDarkColorScheme = darkColorScheme(
    primary = WebSocketPrimaryLight,          // 这里可以继续复用你的主色
    onPrimary = Color.White,
    secondary = WebSocketSecondaryLight,
    onSecondary = Color.Black,
    tertiary = WebSocketTertiaryLight,
    onTertiary = Color.Black,
    background = Color(0xFF121212),
    onBackground = Color.White,
    surface = Color(0xFF1E1E1E),
    onSurface = Color.White,
    outline = Color(0xFF444444)
)



@Composable
fun WebSocketAppTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    content: @Composable () -> Unit
) {
    // 1. 根据 themeMode 决定是否使用暗色
    val systemDark = isSystemInDarkTheme()
    val useDark = when (themeMode) {
        ThemeMode.SYSTEM -> systemDark
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }

    // 2. 选择配色方案
    val colorScheme = if (useDark) WebSocketDarkColorScheme else WebSocketLightColorScheme

    // 3. 设置状态栏颜色 & 图标颜色
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window

            window.statusBarColor = colorScheme.background.toArgb()
            // true 表示状态栏图标是深色（适合浅色背景），所以要反着来
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !useDark
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}