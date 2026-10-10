package com.irvingwu.huiniaotable.ui

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.NotificationsOff
import androidx.compose.material.icons.outlined.GridOff
import androidx.compose.material.icons.outlined.Science
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
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
@OptIn(ExperimentalMaterial3Api::class)
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
            snackbarHostState.showSnackbar(if (granted) "已授予通知权限" else "未授予通知权限")
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = "返回",
                        )
                    }
                },
                title = {
                    Text(
                        text = "开发者调试",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
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
                        text = "立刻发送一条真实的通知，用于核对样式与系统「实时更新」的呈现效果，无需等到实际上课时段。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
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
                            imageVector = Icons.Outlined.Bolt,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("立即发送「正在上课」实时更新预览")
                    }

                    FilledTonalButton(
                        onClick = {
                            val message = ClassNotifier.previewReminder(context)
                            coroutineScope.launch { snackbarHostState.showSnackbar(message) }
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.NotificationsActive,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("发送「课前 15 分钟」提醒预览")
                    }

                    OutlinedButton(
                        onClick = {
                            ClassNotifier.clearNotifications(context)
                            coroutineScope.launch { snackbarHostState.showSnackbar("已清除通知") }
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.DeleteSweep,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("清除实时通知 / 提醒")
                    }
                }
            }

            item {
                DebugCard {
                    Text(
                        text = "长按课程方块可拖动到空白格子。这里可一次性清除所有手动拖动的位置调整，恢复课表原始排布。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    OutlinedButton(
                        onClick = {
                            TimetableRepository.clearAllCoursePlacements()
                            coroutineScope.launch { snackbarHostState.showSnackbar("已清除全部手动排布") }
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.GridOff,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("清除全部手动排布")
                    }
                }
            }

            item {
                DebugCard {
                    DebugValueRow(
                        label = "系统 Live Updates",
                        value = if (ClassNotifier.liveUpdatesSupported) "支持（Android 16+）" else "不支持，降级为普通进度通知",
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    DebugValueRow(
                        label = "通知开关",
                        value = if (state.classNotify) "已开启" else "已关闭",
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    DebugValueRow(
                        label = "通知权限",
                        value = if (ClassNotifier.canPost(context)) "已授予" else "未授予",
                    )
                    if (!ClassNotifier.canPost(context) && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        Spacer(Modifier.height(8.dp))
                        OutlinedButton(
                            onClick = { permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.NotificationsOff,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(Modifier.width(8.dp))
                            Text("申请通知权限")
                        }
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    DebugValueRow(
                        label = "开学基准日",
                        value = state.termStart?.toString() ?: "未设置",
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    DebugValueRow(label = "当前教学周", value = "第 ${state.currentWeek} 周")
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    DebugValueRow(
                        label = "课表课程数",
                        value = "${state.schedule?.courses?.size ?: 0} 节",
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    DebugValueRow(label = "最近同步", value = formatDebugTime(state.fetchedAt))
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
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
        shape = MaterialTheme.shapes.medium,
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
            imageVector = Icons.Outlined.Science,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(18.dp),
        )
        Text(
            text = label,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private val debugTimeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("MM-dd HH:mm")

private fun formatDebugTime(epochMillis: Long): String {
    if (epochMillis <= 0L) return "尚未同步"
    return Instant.ofEpochMilli(epochMillis)
        .atZone(ZoneId.systemDefault())
        .format(debugTimeFormatter)
}
