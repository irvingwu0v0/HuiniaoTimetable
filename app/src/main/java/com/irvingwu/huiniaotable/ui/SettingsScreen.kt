package com.irvingwu.huiniaotable.ui

import android.os.Build
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.EditCalendar
import androidx.compose.material.icons.outlined.HolidayVillage
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.LockReset
import androidx.compose.material.icons.outlined.Nightlight
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.School
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
 * 「系统设置」页面：严格依从 Google Material You (Material 3) 设计规范。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsContent(
    state: AppState,
    onRefresh: () -> Unit,
    onLogin: () -> Unit,
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
    onLogout: () -> Unit,
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
            icon = Icons.Outlined.School,
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
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = t("输入您所在高校的强智教务系统入口网址：", "Enter your university's academic system URL:"),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
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
        // ---- 账户与凭据认证 ----
        item {
            SettingsCard {
                SettingRow(
                    icon = Icons.Outlined.AccountCircle,
                    title = t("统一认证学号", "Student ID"),
                    subtitle = state.studentId.ifBlank { t("未建立有效教务会话", "No active academic session") },
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                SettingRow(
                    icon = Icons.Outlined.Language,
                    title = t("教务系统网址", "Academic system URL"),
                    subtitle = state.jwxtBaseUrl.ifBlank { t("未配置教务系统网址", "Academic system URL not configured") },
                    onClick = { showJwxtDialog = true },
                    trailing = {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            modifier = Modifier.size(18.dp),
                        )
                    },
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                if (!state.loggedIn) {
                    SettingRow(
                        icon = Icons.Outlined.LockReset,
                        title = t("凭据鉴权已失效", "Sign-in expired"),
                        subtitle = t("当前展示离线课表，请重新登录鉴权", "Showing the offline timetable. Sign in again."),
                        onClick = onLogin,
                    )
                    FilledTonalButton(
                        onClick = onLogin,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                    ) {
                        Text(t("重新登入教务系统", "Sign in again"))
                    }
                }
                OutlinedButton(
                    onClick = onLogout,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 6.dp),
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.Logout,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.error,
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(t("退出当前账户并清除本地凭据", "Sign out and clear local credentials"), color = MaterialTheme.colorScheme.error)
                }
            }
        }

        // ---- 学期与教学周 ----
        item {
            SettingsCard {
                SettingRow(
                    icon = Icons.Outlined.School,
                    title = t("当前生效学期", "Active semester"),
                    subtitle = state.term.ifBlank { t("教务系统缺省当前学期", "Academic system default") },
                    onClick = if (state.terms.size > 1) { { showTermDialog = true } } else null,
                    trailing = if (state.terms.size > 1) {
                        {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = MaterialTheme.colorScheme.surfaceContainerHighest,
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                ) {
                                    Text(
                                        text = t("切换", "Change"),
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.primary,
                                    )
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                        tint = MaterialTheme.colorScheme.primary,
                                    )
                                }
                            }
                        }
                    } else null,
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                SettingRow(
                    icon = Icons.Outlined.CalendarMonth,
                    title = t("学期开学首周基准日", "Term start date"),
                    subtitle = state.termStart?.let {
                        t(
                            "首周：${it.format(dateFormatter)} · 第 ${state.currentWeek} 教学周",
                            "First week: ${it.format(dateFormatter)} · Week ${state.currentWeek}",
                        )
                    } ?: t("未设定基准日期", "Start date not set"),
                    onClick = { showStartPicker = true },
                    trailing = {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            modifier = Modifier.size(18.dp),
                        )
                    },
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                SettingRow(
                    icon = Icons.Outlined.Sync,
                    title = t("课表最近同步时间", "Last timetable sync"),
                    subtitle = formatTime(state.fetchedAt),
                )
                FilledTonalButton(
                    onClick = onRefresh,
                    enabled = !state.refreshing,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Sync,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        if (state.refreshing) {
                            t("正在同步教务系统…", "Syncing academic system…")
                        } else {
                            t("立即执行远程数据同步", "Sync now")
                        },
                    )
                }
            }
        }

        // ---- 作息时间与校区楼宇 ----
        item {
            SettingsCard {
                SettingRow(
                    icon = Icons.Outlined.Schedule,
                    title = t("作息方案与校区楼宇设置", "Schedules, campuses and buildings"),
                    subtitle = "${state.scheduleConfigName} · ${state.campus.label} · ${state.timeTable.label}",
                    onClick = onOpenClassTimes,
                    trailing = {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            modifier = Modifier.size(18.dp),
                        )
                    },
                )
            }
        }

        // ---- 节假日与调休编排 ----
        item {
            SettingsCard {
                SettingRow(
                    icon = Icons.Outlined.HolidayVillage,
                    title = t("节假日与调休编排", "Holidays and makeup days"),
                    subtitle = when (state.weekendMode) {
                        WeekendMode.Auto -> t("周末智能判定", "Smart weekend display")
                        WeekendMode.Always -> t("周末完整周期", "Show full week")
                        WeekendMode.Hide -> t("仅展示工作日", "Weekdays only")
                    } + if (state.holidayRegion != HolidayRegion.NONE) " · ${state.holidayRegion.displayName}" else "",
                    onClick = onOpenHolidays,
                    trailing = {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            modifier = Modifier.size(18.dp),
                        )
                    },
                )
            }
        }

        // ---- 课表显示 ----
        item {
            SettingsCard {
                SettingRow(
                    icon = Icons.Outlined.Nightlight,
                    title = t("隐藏晚自习（最后两节）", "Hide evening classes (last two periods)"),
                    subtitle = if (state.hideEvening) {
                        t("课表已隐藏最后两节时段", "The last two periods are hidden")
                    } else {
                        t("课表完整显示全部节次", "All periods are shown")
                    },
                    trailing = {
                        Switch(
                            checked = state.hideEvening,
                            onCheckedChange = onHideEvening,
                        )
                    },
                )
            }
        }

        // ---- 通知与系统日程 ----
        item {
            SettingsCard {
                SettingRow(
                    icon = Icons.Outlined.NotificationsActive,
                    title = t("上课实时通知（Live Updates）", "Live class updates"),
                    subtitle = t("课前 15 分钟提醒与上课常驻进度", "15-minute reminders and live class progress"),
                    trailing = {
                        Switch(
                            checked = state.classNotify,
                            onCheckedChange = onClassNotify,
                        )
                    },
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                SettingRow(
                    icon = Icons.Outlined.EditCalendar,
                    title = t("一键写入系统日程", "Add all classes to calendar"),
                    subtitle = t("将当前学期全部排课写入手机系统日历", "Write this semester's classes to the system calendar"),
                    onClick = onSyncCalendar,
                    trailing = {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            modifier = Modifier.size(18.dp),
                        )
                    },
                )
            }
        }

        // ---- 视觉与主题规范 ----
        item {
            SettingsCard {
                SettingRow(
                    icon = Icons.Outlined.Palette,
                    title = t("Material You 动态色彩体系", "Material You dynamic colour"),
                    subtitle = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        t("跟随系统壁纸基色自动变色", "Colours follow the system wallpaper")
                    } else {
                        t("低于 Android 12，采用默认基准色", "Android 12+ is required; using the default palette")
                    },
                    trailing = {
                        Switch(
                            checked = state.dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S,
                            enabled = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S,
                            onCheckedChange = onDynamicColor,
                        )
                    },
                )


            }
        }

        // ---- 系统关于与凭据合规 ----
        item {
            SettingsCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
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
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = t("版本 $versionName", "Version $versionName"),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                Spacer(Modifier.height(10.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                SettingRow(
                    icon = Icons.Outlined.Shield,
                    title = t("本地离线沙盒安全保障", "Local secure storage"),
                    subtitle = t("凭据由硬件 KeyStore 加密存储，无云端中转", "Credentials are encrypted with hardware-backed KeyStore"),
                )
            }
        }
    }
}

@Composable
private fun SettingsCard(content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
        shape = MaterialTheme.shapes.medium,
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            content()
        }
    }
}

@Composable
private fun SettingRow(
    icon: ImageVector? = null,
    title: String,
    subtitle: String? = null,
    onClick: (() -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
) {
    val rowModifier = Modifier
        .fillMaxWidth()
        .clip(MaterialTheme.shapes.small)
        .then(
            if (onClick != null) {
                Modifier.clickable(onClick = onClick)
            } else {
                Modifier
            },
        )
        .padding(vertical = 8.dp, horizontal = 4.dp)

    Row(
        modifier = rowModifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp),
            )
        }
        Column(Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                ),
            )
            if (!subtitle.isNullOrBlank()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        trailing?.invoke()
    }
}

private fun formatTime(epochMillis: Long): String {
    if (epochMillis <= 0L) return t("尚未执行同步", "Never synced")
    return Instant.ofEpochMilli(epochMillis)
        .atZone(ZoneId.systemDefault())
        .format(timeFormatter)
}
