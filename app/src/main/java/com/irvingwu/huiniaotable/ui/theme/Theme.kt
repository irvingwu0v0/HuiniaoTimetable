package com.irvingwu.huiniaotable.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import com.irvingwu.huiniaotable.ui.compat.LocalContentColor
import com.irvingwu.huiniaotable.ui.compat.LocalTextStyle
import top.yukonga.miuix.kmp.theme.ColorSchemeMode
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.ThemeController

/**
 * 应用主题：完全基于 Miuix（HyperOS 设计语言）。
 *
 * [dynamicColor] 打开时使用系统动态取色（Monet）从壁纸取色，关闭时使用 Miuix 自带的浅色/深色方案。
 */
@Composable
fun HuiniaoTimetableTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit,
) {
    val mode = when {
        dynamicColor && darkTheme -> ColorSchemeMode.MonetDark
        dynamicColor -> ColorSchemeMode.MonetLight
        darkTheme -> ColorSchemeMode.Dark
        else -> ColorSchemeMode.Light
    }
    val controller = remember(mode) { ThemeController(mode) }

    MiuixTheme(controller = controller) {
        CompositionLocalProvider(
            LocalTextStyle provides MiuixTheme.textStyles.body1,
            LocalContentColor provides MiuixTheme.colorScheme.onBackground,
            content = content,
        )
    }
}
