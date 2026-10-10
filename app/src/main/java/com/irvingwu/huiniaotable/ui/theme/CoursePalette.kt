package com.irvingwu.huiniaotable.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance

/** 一门课在课表里的三段配色，语义对齐 Material You 的「容器色 / 其上内容色 / 强调色」。 */
data class CourseColors(
    val container: Color,
    val onContainer: Color,
    val accent: Color,
)

/**
 * 课程色板：为一学期里互不相同的课程各自分配一个稳定、饱和、且在浅/深色主题下都清晰可读的
 * 色块。
 *
 * 设计依据（Material 3 / Material You 色彩体系，参见 material-foundation/material-color-utilities
 * 与 m3.material.io 的 Color roles）：
 *  - 沿用「色调板（tonal palette）」思路：同一个色相只改变色调；浅色主题取 tone≈90 的容器色
 *    配 tone≈10 的深色前景，深色主题取 tone≈30 的容器色配 tone≈90 的浅色前景，与规范中
 *    PrimaryContainer / onPrimaryContainer 的明度关系保持一致，因此文本对比度足够；
 *  - 全部色相由当前动态主题的 primary 色相在色环上均分旋转得到：开启 Material You 动态取色时
 *    会跟随壁纸基色整体偏移，关闭时跟随灰鸟青品牌色，始终保持同一套视觉气质；
 *  - 同一门课按课程名归一化后始终命中同一色相，保证「同一课程同一颜色」。
 */
class CoursePalette internal constructor(private val tones: List<CourseColors>) {

    private val cache = HashMap<String, CourseColors>()

    /** 色板容量（可用的不同色相数量）。 */
    val size: Int get() = tones.size

    /** 取某门课的配色；[key] 传课程名即可。 */
    fun forKey(key: String): CourseColors =
        cache.getOrPut(key) { tones[mix(key) % tones.size] }

    /** 稳定的整数哈希：同一个 key 永远得到同一个下标（不依赖运行环境）。 */
    private fun mix(key: String): Int {
        var hash = key.hashCode()
        hash = hash xor (hash ushr 16)
        hash *= 0x7feb352d
        hash = hash xor (hash ushr 15)
        hash *= 0x846ca68b.toInt()
        hash = hash xor (hash ushr 16)
        return hash and 0x7fffffff
    }
}

/** 课程色板容量：16 种色相，兼顾色彩丰富度与相邻色相的可辨识度。 */
private const val HUE_COUNT = 16

/** 依据当前主题生成课程色板；主题（动态取色结果 / 明暗）变化时自动重建。 */
@Composable
fun rememberCoursePalette(): CoursePalette {
    val seed = MaterialTheme.colorScheme.primary
    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    return remember(seed, isDark) { buildCoursePalette(seed, isDark) }
}

private fun buildCoursePalette(seed: Color, dark: Boolean): CoursePalette {
    val baseHue = seed.hueDegrees()
    val tones = (0 until HUE_COUNT).map { index ->
        val hue = (baseHue + index * (360f / HUE_COUNT)) % 360f
        toneFor(hue, dark)
    }
    return CoursePalette(tones)
}

private fun toneFor(hue: Float, dark: Boolean): CourseColors =
    if (dark) {
        CourseColors(
            container = Color.hsl(hue, 0.33f, 0.26f),
            onContainer = Color.hsl(hue, 0.52f, 0.90f),
            accent = Color.hsl(hue, 0.60f, 0.72f),
        )
    } else {
        CourseColors(
            container = Color.hsl(hue, 0.58f, 0.87f),
            onContainer = Color.hsl(hue, 0.72f, 0.21f),
            accent = Color.hsl(hue, 0.62f, 0.42f),
        )
    }

/** 取颜色的色相（度，0~360）。 */
private fun Color.hueDegrees(): Float {
    val max = maxOf(red, green, blue)
    val min = minOf(red, green, blue)
    val delta = max - min
    if (delta == 0f) return 210f
    val hue = when (max) {
        red -> 60f * (((green - blue) / delta) % 6f)
        green -> 60f * (((blue - red) / delta) + 2f)
        else -> 60f * (((red - green) / delta) + 4f)
    }
    return (hue + 360f) % 360f
}
