package com.irvingwu.huiniaotable.ui

import com.irvingwu.huiniaotable.ui.icons.AppIcons

import android.Manifest
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import com.irvingwu.huiniaotable.ui.compat.Button
import com.irvingwu.huiniaotable.ui.compat.Card
import com.irvingwu.huiniaotable.ui.compat.CardDefaults
import com.irvingwu.huiniaotable.ui.compat.FilledTonalButton
import com.irvingwu.huiniaotable.ui.compat.HorizontalDivider
import com.irvingwu.huiniaotable.ui.compat.Icon
import com.irvingwu.huiniaotable.ui.compat.IconButton
import com.irvingwu.huiniaotable.ui.compat.AppTheme
import com.irvingwu.huiniaotable.ui.compat.OutlinedButton
import com.irvingwu.huiniaotable.ui.compat.Scaffold
import com.irvingwu.huiniaotable.ui.compat.SnackbarHost
import com.irvingwu.huiniaotable.ui.compat.SnackbarHostState
import com.irvingwu.huiniaotable.ui.compat.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import top.yukonga.miuix.kmp.basic.Icon as MiuixIcon
import top.yukonga.miuix.kmp.basic.IconButton as MiuixIconButton
import top.yukonga.miuix.kmp.basic.Scaffold as MiuixScaffold
import top.yukonga.miuix.kmp.basic.TopAppBar as MiuixTopAppBar
import com.irvingwu.huiniaotable.t
import com.irvingwu.huiniaotable.data.AppState
import com.irvingwu.huiniaotable.data.TimetableRepository
import com.irvingwu.huiniaotable.notify.ClassNotifier
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * 开发者调试界面：连点设置页应用图标 10 次进入。
 *
 * 目前提供上课实时通知（Live Updates）的即时预览能力：无需等到真实的上课时间，
 * 即可立刻看到「课前 15 分钟提醒」与「正在上课实时进度」两条通知的真实呈现。
 */
@Composable
fun DebugScreen(
    state: AppState,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    BackHandler(onBack = onBack)

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        coroutineScope.launch {
            snackbarHostState.showSnackbar(
                if (granted) {
                    t("已授予通知权限", "Notification permission granted")
                } else {
                    t("未授予通知权限", "Notification permission not granted")
                },
            )
        }
    }

    MiuixScaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            MiuixTopAppBar(
                navigationIcon = {
                    MiuixIconButton(onClick = onBack) {
                        MiuixIcon(
                            imageVector = AppIcons.ArrowBack,
                            contentDescription = t("返回", "Back"),
                        )
                    }
                },
                title = t("开发者调试", "Developer tools"),
            )
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
                DebugCard {
                    Text(
                        text = t(
                            "立刻发送一条真实的通知，用于核对样式与系统「实时更新」的呈现效果，无需等到实际上课时段。",
                            "Send a real notification now to check its style and the system Live Update appearance without waiting for class.",
                        ),
                        style = AppTheme.typography.bodySmall,
                        color = AppTheme.colorScheme.onSurfaceVariant,
                    )

                    Spacer(Modifier.height(4.dp))

                    Button(
                        onClick = {
                            val message = ClassNotifier.previewLiveUpdate(context)
                            coroutineScope.launch { snackbarHostState.showSnackbar(message) }
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(
                            imageVector = AppIcons.Bolt,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(t("立即发送「正在上课」实时更新预览", "Preview an in-class Live Update"))
                    }

                    FilledTonalButton(
                        onClick = {
                            val message = ClassNotifier.previewReminder(context)
                            coroutineScope.launch { snackbarHostState.showSnackbar(message) }
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(
                            imageVector = AppIcons.NotificationsActive,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(t("发送「课前 15 分钟」提醒预览", "Preview a 15-minute reminder"))
                    }

                    OutlinedButton(
                        onClick = {
                            ClassNotifier.clearNotifications(context)
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar(t("已清除通知", "Notifications cleared"))
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(
                            imageVector = AppIcons.DeleteSweep,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(t("清除实时通知 / 提醒", "Clear live updates / reminders"))
                    }
                }
            }

            item {
                DebugCard {
                    Text(
                        text = t(
                            "长按课程方块可拖动到空白格子。这里可一次性清除所有手动拖动的位置调整，恢复课表原始排布。",
                            "Long-press a course to drag it into an empty slot. Reset every manual position here.",
                        ),
                        style = AppTheme.typography.bodySmall,
                        color = AppTheme.colorScheme.onSurfaceVariant,
                    )
                    OutlinedButton(
                        onClick = {
                            TimetableRepository.clearAllCoursePlacements()
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar(t("已清除全部手动排布", "All manual positions cleared"))
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(
                            imageVector = AppIcons.GridOff,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(t("清除全部手动排布", "Clear all manual positions"))
                    }
                }
            }

            item {
                DebugCard {
                    DebugValueRow(
                        label = t("系统 Live Updates", "System Live Updates"),
                        value = if (ClassNotifier.liveUpdatesSupported) {
                            t("支持（Android 16+）", "Supported (Android 16+)")
                        } else {
                            t("不支持，降级为普通进度通知", "Unsupported; using a standard progress notification")
                        },
                    )
                    HorizontalDivider(color = AppTheme.colorScheme.outlineVariant)
                    DebugValueRow(
                        label = t("通知开关", "Notifications"),
                        value = if (state.classNotify) t("已开启", "On") else t("已关闭", "Off"),
                    )
                    HorizontalDivider(color = AppTheme.colorScheme.outlineVariant)
                    DebugValueRow(
                        label = t("通知权限", "Notification permission"),
                        value = if (ClassNotifier.canPost(context)) t("已授予", "Granted") else t("未授予", "Not granted"),
                    )
                    if (!ClassNotifier.canPost(context) && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        Spacer(Modifier.height(8.dp))
                        OutlinedButton(
                            onClick = { permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Icon(
                                imageVector = AppIcons.NotificationsOff,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(t("申请通知权限", "Request notification permission"))
                        }
                    }
                    HorizontalDivider(color = AppTheme.colorScheme.outlineVariant)
                    DebugValueRow(
                        label = t("开学基准日", "Term start date"),
                        value = state.termStart?.toString() ?: t("未设置", "Not set"),
                    )
                    HorizontalDivider(color = AppTheme.colorScheme.outlineVariant)
                    DebugValueRow(
                        label = t("当前教学周", "Current week"),
                        value = t("第 ${state.currentWeek} 周", "Week ${state.currentWeek}"),
                    )
                    HorizontalDivider(color = AppTheme.colorScheme.outlineVariant)
                    DebugValueRow(
                        label = t("课表课程数", "Timetable entries"),
                        value = t("${state.schedule?.courses?.size ?: 0} 节", "${state.schedule?.courses?.size ?: 0}"),
                    )
                    HorizontalDivider(color = AppTheme.colorScheme.outlineVariant)
                    DebugValueRow(label = t("最近同步", "Last sync"), value = formatDebugTime(state.fetchedAt))
                }
            }
        }
    }
}

@Composable
private fun DebugCard(content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = AppTheme.colorScheme.surfaceContainerLow,
        ),
        shape = AppTheme.shapes.medium,
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            content = content,
        )
    }
}

@Composable
private fun DebugValueRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(
            imageVector = AppIcons.Science,
            contentDescription = null,
            tint = AppTheme.colorScheme.primary,
            modifier = Modifier.size(18.dp),
        )
        Text(
            text = label,
            style = AppTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = value,
            style = AppTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = AppTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private val debugTimeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("MM-dd HH:mm")

private fun formatDebugTime(epochMillis: Long): String {
    if (epochMillis <= 0L) return t("尚未同步", "Never synced")
    return Instant.ofEpochMilli(epochMillis)
        .atZone(ZoneId.systemDefault())
        .format(debugTimeFormatter)
}
