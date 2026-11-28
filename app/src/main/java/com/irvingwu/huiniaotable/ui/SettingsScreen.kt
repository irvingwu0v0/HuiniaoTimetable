package com.irvingwu.huiniaotable.ui


import com.irvingwu.huiniaotable.ui.icons.AppIcons
import android.os.Build
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.preference.ArrowPreference
import top.yukonga.miuix.kmp.preference.SwitchPreference
import top.yukonga.miuix.kmp.basic.Card as MiuixCard
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import com.irvingwu.huiniaotable.ui.compat.AlertDialog
import com.irvingwu.huiniaotable.ui.compat.HorizontalDivider
import com.irvingwu.huiniaotable.ui.compat.AppTheme
import com.irvingwu.huiniaotable.ui.compat.Surface
import com.irvingwu.huiniaotable.ui.compat.Text
import com.irvingwu.huiniaotable.ui.compat.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.irvingwu.huiniaotable.R
import com.irvingwu.huiniaotable.t
import com.irvingwu.huiniaotable.data.AppState
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/** 连点应用图标解锁开发者调试模式的次数。 */
private const val DEBUG_UNLOCK_TAPS = 10

private val dateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
private val timeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")

/**
 * 「系统设置」页面：严格依从 Miuix / HyperOS 设计规范。
 */
@Composable
fun SettingsContent(
    state: AppState,
    onImport: () -> Unit,
    onSyncCalendar: () -> Unit,
    onSetTermStart: (LocalDate) -> Unit,
    onHideEvening: (Boolean) -> Unit,
    onDynamicColor: (Boolean) -> Unit,
    onClassNotify: (Boolean) -> Unit,
    onOpenDebug: () -> Unit,
    onClearTimetable: () -> Unit,
    onOpenClassTimes: () -> Unit = {},
    onOpenHolidays: () -> Unit = {},
) {
    val context = LocalContext.current
    val versionName = remember {
        runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }
            .getOrNull() ?: "1.2"
    }

    var showStartPicker by remember { mutableStateOf(false) }
    var confirmClear by remember { mutableStateOf(false) }
    var logoTaps by remember { mutableStateOf(0) }

    // 开学首周日期选择器
    if (showStartPicker) {
        PickDateDialog(
            title = t("选取开学首周周一日期", "Select the first Monday of term"),
            current = state.termStart,
            onDismiss = { showStartPicker = false },
            onConfirm = {
                onSetTermStart(it)
                showStartPicker = false
            },
        )
    }

    if (confirmClear) AlertDialog(
        onDismissRequest = { confirmClear = false },
        title = { Text(t("清除课表？", "Clear timetable?")) },
        text = { Text(t("本机已导入的课程和调课记录将被清除。", "Imported classes and local moves will be removed.")) },
        confirmButton = { TextButton(onClick = { confirmClear = false; onClearTimetable() }) { Text(t("清除", "Clear")) } },
        dismissButton = { TextButton(onClick = { confirmClear = false }) { Text(t("取消", "Cancel")) } },
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("settings_list"),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            SmallTitle(text = t("学期与课表", "Semester"))
            SettingsCard {
                SettingRow(icon = AppIcons.FileUpload, title = t("导入课表", "Import timetable"),
                    subtitle = state.schedule?.let { t("最近导入：${formatTime(it.fetchedAt)}", "Last import: ${formatTime(it.fetchedAt)}") }, onClick = onImport)
                SettingRow(icon = AppIcons.CalendarMonth, title = t("开学日期", "Term start"), subtitle = state.termStart?.format(dateFormatter) ?: t("未设置", "Not set"), onClick = { showStartPicker = true })
                SettingRow(icon = AppIcons.Schedule, title = t("作息时间", "Period times"), subtitle = state.timeTable.label, onClick = onOpenClassTimes)
                SettingRow(icon = AppIcons.HolidayVillage, title = t("节假日与调休", "Holidays & makeup days"), subtitle = state.weekendMode.label, onClick = onOpenHolidays)
            }
        }
        item {
            SmallTitle(text = t("显示", "Appearance"))
            SettingsCard {
                SwitchPreference(checked = state.hideEvening, onCheckedChange = onHideEvening,
                    startAction = { PreferenceIcon(AppIcons.VisibilityOff) },
                    title = t("隐藏最后两节", "Hide last two periods"))
                SwitchPreference(checked = state.dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S,
                    onCheckedChange = onDynamicColor, enabled = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S,
                    startAction = { PreferenceIcon(AppIcons.Palette) },
                    title = t("系统动态取色", "Wallpaper colours"))
            }
        }
        item {
            SmallTitle(text = t("提醒与日历", "Reminders & calendar"))
            SettingsCard {
                SwitchPreference(checked = state.classNotify, onCheckedChange = onClassNotify,
                    startAction = { PreferenceIcon(AppIcons.NotificationsActive) },
                    title = t("上课提醒", "Class reminders"), summary = t("提前 15 分钟提醒", "15 minutes before class"))
                if (state.schedule != null) SettingRow(icon = AppIcons.EditCalendar, title = t("同步到系统日历", "Sync to calendar"), onClick = onSyncCalendar)
            }
        }
        if (state.schedule != null) item {
            SettingsCard { SettingRow(icon = AppIcons.DeleteSweep, title = t("清除已导入的课表", "Clear imported timetable"), onClick = { confirmClear = true }) }
        }
        // ---- 系统关于与隐私说明 ----
        item {
            SettingsCard {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Surface(
                        shape = CircleShape,
                        color = AppTheme.colorScheme.surfaceContainerHigh,
                        modifier = Modifier
                            .size(54.dp)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                            ) {
                                logoTaps += 1
                                val remaining = DEBUG_UNLOCK_TAPS - logoTaps
                                when {
                                    remaining <= 0 -> {
                                        logoTaps = 0
                                        Toast.makeText(
                                            context,
                                            t("已进入开发者调试模式", "Developer mode enabled"),
                                            Toast.LENGTH_SHORT,
                                        ).show()
                                        onOpenDebug()
                                    }
                                    remaining <= 5 -> Toast.makeText(
                                        context,
                                        t(
                                            "再点击 $remaining 次进入开发者调试模式",
                                            "$remaining more taps to enable developer mode",
                                        ),
                                        Toast.LENGTH_SHORT,
                                    ).show()
                                }
                            },
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.fillMaxSize().padding(6.dp),
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.ic_app_logo),
                                contentDescription = t("应用图标", "App icon"),
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Fit,
                            )
                        }
                    }
                    Column {
                        Text(
                            text = stringResource(R.string.app_name),
                            style = AppTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = t("版本 $versionName", "Version $versionName"),
                            style = AppTheme.typography.bodyMedium,
                            color = AppTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                Spacer(Modifier.height(10.dp))
                HorizontalDivider(color = AppTheme.colorScheme.outlineVariant)

                SettingRow(
                    icon = AppIcons.Shield,
                    title = t("隐私", "Privacy"),
                    subtitle = t("课表在本机解析，不保存学号或密码。", "Timetables are read locally. The app does not store your student ID or password."),
                )
            }
        }
    }
}

@Composable
private fun SettingsCard(content: @Composable () -> Unit) {
    MiuixCard(
        modifier = Modifier.fillMaxWidth(),
    ) {
        content()
    }
}

@Composable
private fun SettingRow(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    onClick: (() -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
) {
    if (onClick == null) {
        top.yukonga.miuix.kmp.basic.BasicComponent(title = title, summary = subtitle, startAction = { PreferenceIcon(icon) })
        return
    }
    ArrowPreference(
        title = title,
        summary = subtitle?.takeIf { it.isNotBlank() },
        onClick = onClick,
        startAction = { PreferenceIcon(icon) },
        endActions = {
            trailing?.invoke()
        },
    )
}

private fun formatTime(epochMillis: Long): String {
    if (epochMillis <= 0L) return t("尚未执行同步", "Never synced")
    return Instant.ofEpochMilli(epochMillis)
        .atZone(ZoneId.systemDefault())
        .format(timeFormatter)
}
