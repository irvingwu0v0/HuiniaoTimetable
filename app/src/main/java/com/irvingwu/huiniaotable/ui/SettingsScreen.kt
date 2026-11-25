package com.irvingwu.huiniaotable.ui

import com.irvingwu.huiniaotable.ui.icons.AppIcons

import android.os.Build
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import com.irvingwu.huiniaotable.ui.compat.DropdownMenu
import com.irvingwu.huiniaotable.ui.compat.DropdownMenuItem
import top.yukonga.miuix.kmp.preference.ArrowPreference
import top.yukonga.miuix.kmp.preference.SwitchPreference
import top.yukonga.miuix.kmp.basic.Card as MiuixCard
import top.yukonga.miuix.kmp.basic.TextButton as MiuixTextButton
import top.yukonga.miuix.kmp.basic.Icon as MiuixIcon
import top.yukonga.miuix.kmp.theme.MiuixTheme
import com.irvingwu.huiniaotable.data.HolidayRegion
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import com.irvingwu.huiniaotable.ui.compat.AlertDialog
import com.irvingwu.huiniaotable.ui.compat.Button
import com.irvingwu.huiniaotable.ui.compat.Card
import com.irvingwu.huiniaotable.ui.compat.CardDefaults
import com.irvingwu.huiniaotable.ui.compat.FilledTonalButton
import com.irvingwu.huiniaotable.ui.compat.HorizontalDivider
import com.irvingwu.huiniaotable.ui.compat.Icon
import com.irvingwu.huiniaotable.ui.compat.AppTheme
import com.irvingwu.huiniaotable.ui.compat.OutlinedButton
import com.irvingwu.huiniaotable.ui.compat.OutlinedTextField
import com.irvingwu.huiniaotable.ui.compat.Surface
import com.irvingwu.huiniaotable.ui.compat.Switch
import com.irvingwu.huiniaotable.ui.compat.Text
import com.irvingwu.huiniaotable.ui.compat.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.irvingwu.huiniaotable.R
import com.irvingwu.huiniaotable.t
import com.irvingwu.huiniaotable.data.AppState
import com.irvingwu.huiniaotable.data.Campus
import com.irvingwu.huiniaotable.data.CampusBuilding
import com.irvingwu.huiniaotable.data.WeekendMode
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
    onRefresh: () -> Unit,
    onImport: () -> Unit,
    onSyncCalendar: () -> Unit,
    onSelectTerm: (String) -> Unit,
    onSetTermStart: (LocalDate) -> Unit,
    onSelectCampus: (Campus) -> Unit,
    onSelectBuilding: (CampusBuilding) -> Unit,
    onSelectBuildingScheme: (String, CampusBuilding) -> Unit,
    onSelectWeekendMode: (WeekendMode) -> Unit,
    onHideEvening: (Boolean) -> Unit,
    onRefreshHolidays: () -> Unit,
    onAddHolidayOverride: (LocalDate, String, Boolean) -> Unit,
    onRemoveHolidayOverride: (LocalDate) -> Unit,
    onSetMakeup: (LocalDate, Int) -> Unit,
    onClearMakeup: (LocalDate) -> Unit,
    onDynamicColor: (Boolean) -> Unit,
    onClassNotify: (Boolean) -> Unit,
    onOpenDebug: () -> Unit,
    onClearTimetable: () -> Unit,
    onSetJwxtBaseUrl: (String) -> Unit = {},
    onOpenClassTimes: () -> Unit = {},
    onOpenHolidays: () -> Unit = {},
) {
    val context = LocalContext.current
    val versionName = remember {
        runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }
            .getOrNull() ?: "1.2"
    }

    var showTermDialog by remember { mutableStateOf(false) }
    var showStartPicker by remember { mutableStateOf(false) }
    var showJwxtDialog by remember { mutableStateOf(false) }
    var logoTaps by remember { mutableStateOf(0) }

    // 学期选择单选对话框
    if (showTermDialog) {
        SingleChoiceDialog(
            icon = AppIcons.School,
            title = t("选择生效学期", "Select semester"),
            items = state.terms,
            selectedItem = state.terms.firstOrNull { it.value == state.term },
            itemTitle = { it.label },
            onItemSelected = { onSelectTerm(it.value) },
            onDismissRequest = { showTermDialog = false },
        )
    }

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

    // 修改教务系统网址对话框
    if (showJwxtDialog) {
        var urlInput by remember { mutableStateOf(state.jwxtBaseUrl) }
        AlertDialog(
            onDismissRequest = { showJwxtDialog = false },
            title = {
                Text(
                    text = t("配置教务系统网址", "Academic system URL"),
                    style = AppTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = t("输入您所在高校的强智教务系统入口网址：", "Enter your university's academic system URL:"),
                        style = AppTheme.typography.bodySmall,
                        color = AppTheme.colorScheme.onSurfaceVariant,
                    )
                    OutlinedTextField(
                        value = urlInput,
                        onValueChange = { urlInput = it },
                        label = { Text(t("教务系统网址 (URL)", "Academic system URL")) },
                        placeholder = { Text("https://jwxt.XXXX.edu.cn/") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onSetJwxtBaseUrl(urlInput.trim())
                        showJwxtDialog = false
                    },
                ) {
                    Text(t("保存", "Save"))
                }
            },
            dismissButton = {
                TextButton(onClick = { showJwxtDialog = false }) { Text(t("取消", "Cancel")) }
            },
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("settings_list"),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        // ---- 教务系统与课表来源 ----
        item {
            SettingsCard {
                SettingRow(
                    icon = AppIcons.Language,
                    title = t("教务系统网址", "Academic system URL"),
                    subtitle = state.jwxtBaseUrl.ifBlank { t("未配置教务系统网址", "Academic system URL not configured") },
                    onClick = { showJwxtDialog = true },
                )
                HorizontalDivider(color = AppTheme.colorScheme.outlineVariant)
                FilledTonalButton(
                    onClick = onImport,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                ) {
                    Text(t("打开网页登录并重新导入课表", "Open web page to import again"))
                }
                MiuixTextButton(
                    text = t("清除已导入的课表", "Clear imported timetable"),
                    onClick = onClearTimetable,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        // ---- 学期与教学周 ----
        item {
            SettingsCard {
                SettingRow(
                    icon = AppIcons.School,
                    title = t("当前生效学期", "Active semester"),
                    subtitle = state.term.ifBlank { t("教务系统缺省当前学期", "Academic system default") },
                    onClick = if (state.terms.size > 1) { { showTermDialog = true } } else null,
                )

                HorizontalDivider(color = AppTheme.colorScheme.outlineVariant)

                SettingRow(
                    icon = AppIcons.CalendarMonth,
                    title = t("学期开学首周基准日", "Term start date"),
                    subtitle = state.termStart?.let {
                        t(
                            "首周：${it.format(dateFormatter)} · 第 ${state.currentWeek} 教学周",
                            "First week: ${it.format(dateFormatter)} · Week ${state.currentWeek}",
                        )
                    } ?: t("未设定基准日期", "Start date not set"),
                    onClick = { showStartPicker = true },
                )

                HorizontalDivider(color = AppTheme.colorScheme.outlineVariant)

                SettingRow(
                    icon = AppIcons.Sync,
                    title = t("课表最近同步时间", "Last timetable sync"),
                    subtitle = formatTime(state.fetchedAt),
                )
                Button(
                    onClick = onRefresh,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(t("打开网页登录并重新导入课表", "Open web page to import again"))
                }
            }
        }

        // ---- 作息时间与校区楼宇 ----
        item {
            SettingsCard {
                SettingRow(
                    icon = AppIcons.Schedule,
                    title = t("作息方案与校区楼宇设置", "Schedules, campuses and buildings"),
                    subtitle = "${state.scheduleConfigName} · ${state.campus.label} · ${state.timeTable.label}",
                    onClick = onOpenClassTimes,
                )
            }
        }

        // ---- 节假日与调休编排 ----
        item {
            SettingsCard {
                SettingRow(
                    icon = AppIcons.HolidayVillage,
                    title = t("节假日与调休编排", "Holidays and makeup days"),
                    subtitle = when (state.weekendMode) {
                        WeekendMode.Auto -> t("周末智能判定", "Smart weekend display")
                        WeekendMode.Always -> t("周末完整周期", "Show full week")
                        WeekendMode.Hide -> t("仅展示工作日", "Weekdays only")
                    } + if (state.holidayRegion != HolidayRegion.NONE) " · ${state.holidayRegion.displayName}" else "",
                    onClick = onOpenHolidays,
                )
            }
        }

        // ---- 课表显示 ----
        item {
            SettingsCard {
                SwitchPreference(
                    checked = state.hideEvening,
                    onCheckedChange = onHideEvening,
                    title = t("隐藏晚自习（最后两节）", "Hide evening classes (last two periods)"),
                    summary = if (state.hideEvening) {
                        t("课表已隐藏最后两节时段", "The last two periods are hidden")
                    } else {
                        t("课表完整显示全部节次", "All periods are shown")
                    },
                    startAction = prefIcon(AppIcons.Nightlight),
                )
            }
        }

        // ---- 通知与系统日程 ----
        item {
            SettingsCard {
                SwitchPreference(
                    checked = state.classNotify,
                    onCheckedChange = onClassNotify,
                    title = t("上课实时通知（Live Updates）", "Live class updates"),
                    summary = t("课前 15 分钟提醒与上课常驻进度", "15-minute reminders and live class progress"),
                    startAction = prefIcon(AppIcons.NotificationsActive),
                )

                HorizontalDivider(color = AppTheme.colorScheme.outlineVariant)

                SettingRow(
                    icon = AppIcons.EditCalendar,
                    title = t("一键写入系统日程", "Add all classes to calendar"),
                    subtitle = t("将当前学期全部排课写入手机系统日历", "Write this semester's classes to the system calendar"),
                    onClick = onSyncCalendar,
                )
            }
        }

        // ---- 视觉与主题规范 ----
        item {
            SettingsCard {
                SwitchPreference(
                    checked = state.dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S,
                    onCheckedChange = onDynamicColor,
                    title = t("系统动态取色（Monet）", "System dynamic colour (Monet)"),
                    summary = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        t("跟随系统壁纸基色自动变色", "Colours follow the system wallpaper")
                    } else {
                        t("低于 Android 12，采用默认基准色", "Android 12+ is required; using the default palette")
                    },
                    startAction = prefIcon(AppIcons.Palette),
        enabled = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S,
                )


            }
        }

        // ---- 系统关于与隐私说明 ----
        item {
            SettingsCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
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
                    title = t("零凭据存储", "No credentials stored"),
                    subtitle = t("登录在网页内完成，App 只保存课表与教务系统网址", "Sign-in happens in the web page; only the timetable and site URL are kept"),
                )
            }
        }
    }
}

@Composable
private fun SettingsCard(content: @Composable () -> Unit) {
    MiuixCard(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 16.dp,
    ) {
        content()
    }
}

/** Miuix Preference 左侧图标的统一写法。 */
private fun prefIcon(vector: ImageVector): @Composable () -> Unit = {
    MiuixIcon(
        imageVector = vector,
        contentDescription = null,
        tint = MiuixTheme.colorScheme.primary,
        modifier = Modifier.size(22.dp),
    )
}

@Composable
private fun SettingRow(
    icon: ImageVector? = null,
    title: String,
    subtitle: String? = null,
    onClick: (() -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
) {
    ArrowPreference(
        title = title,
        summary = subtitle?.takeIf { it.isNotBlank() },
        onClick = onClick,
        startAction = icon?.let { vector ->
            {
                MiuixIcon(
                    imageVector = vector,
                    contentDescription = null,
                    tint = MiuixTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp),
                )
            }
        },
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
