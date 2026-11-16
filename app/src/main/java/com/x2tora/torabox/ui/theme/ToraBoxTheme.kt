package com.x2tora.torabox.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import com.x2tora.torabox.model.ThemeColorStyle
import com.x2tora.torabox.model.ThemeOption
import top.yukonga.miuix.kmp.blur.LayerBackdrop
import top.yukonga.miuix.kmp.theme.ColorSchemeMode
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.ThemeColorSpec
import top.yukonga.miuix.kmp.theme.ThemeController
import top.yukonga.miuix.kmp.theme.ThemePaletteStyle

val LocalIsDarkTheme = compositionLocalOf { false }
val LocalAppBackdrop = compositionLocalOf<LayerBackdrop?> { null }
val LocalLiquidGlassEnabled = compositionLocalOf { false }
val LocalWideDualLayoutEnabled = compositionLocalOf { false }

val ToraBoxAccentPalette: List<Pair<String, Long>> = listOf(
    "블루" to 0xFF3482FF,
    "에메랄드" to 0xFF10B981,
    "코랄" to 0xFFFF6B4A,
    "바이올렛" to 0xFF7C5CFF,
    "앰버" to 0xFFF59E0B,
    "로즈" to 0xFFEC4899,
    "시안" to 0xFF06B6D4,
    "인디고" to 0xFF4F46E5
)

@Composable
fun ToraBoxTheme(
    themeOption: ThemeOption,
    colorStyle: ThemeColorStyle,
    customKeyColorHex: Long? = null,
    content: @Composable () -> Unit
) {
    val systemDark = isSystemInDarkTheme()
    val effectiveDark = when (themeOption) {
        ThemeOption.SYSTEM -> systemDark
        ThemeOption.LIGHT -> false
        ThemeOption.DARK -> true
    }

    val useMonet = colorStyle != ThemeColorStyle.DEFAULT
    val colorSchemeMode = when (themeOption) {
        ThemeOption.SYSTEM -> if (useMonet) ColorSchemeMode.MonetSystem else ColorSchemeMode.System
        ThemeOption.LIGHT -> if (useMonet) ColorSchemeMode.MonetLight else ColorSchemeMode.Light
        ThemeOption.DARK -> if (useMonet) ColorSchemeMode.MonetDark else ColorSchemeMode.Dark
    }

    val resolvedKeyColor: Color? = when (colorStyle) {
        ThemeColorStyle.DEFAULT -> null
        ThemeColorStyle.MATERIAL_YOU -> null
        ThemeColorStyle.CUSTOM -> Color(customKeyColorHex ?: 0xFF3482FF)
    }
    val controller = remember(colorSchemeMode, colorStyle, customKeyColorHex) {
        ThemeController(
            colorSchemeMode = colorSchemeMode,
            keyColor = resolvedKeyColor,
            colorSpec = ThemeColorSpec.Spec2025,
            paletteStyle = ThemePaletteStyle.TonalSpot
        )
    }

    CompositionLocalProvider(LocalIsDarkTheme provides effectiveDark) {
        MiuixTheme(controller = controller) {
            content()
        }
    }
}
