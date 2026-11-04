package com.irvingwu.huiniaotable.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.EditCalendar
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.irvingwu.huiniaotable.t
import com.irvingwu.huiniaotable.data.AppState

/**
 * 一键全部添加到系统日程 / 一键清除全部导入日程对话框。
 *
 * 遵循 Material 3 纵向多按钮规范，按操作主次纵向排列，彻底解决水平挤压错位问题：
 *  1. [主要操作] 同步全部至系统日历 (Filled Button)
 *  2. [破坏性操作] 清除已导入日程 (Outlined Button, 警示色)
 *  3. [取消返回] 取消 (Text Button)
 */
@Composable
fun CalendarSyncDialog(
    state: AppState,
    onDismissRequest: () -> Unit,
    onConfirmSync: () -> Unit,
    onClearSync: () -> Unit,
    onNavigateToSetTermStart: () -> Unit,
) {
    val needsTermStart = state.needsTermStart
    val schedule = state.schedule
    val totalCourses = schedule?.courses?.size ?: 0

    AlertDialog(
        onDismissRequest = onDismissRequest,
        icon = {
            Icon(
                imageVector = Icons.Outlined.EditCalendar,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(28.dp),
            )
        },
        title = {
            Text(
                text = t("系统日历日程同步", "System calendar sync"),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (needsTermStart) {
                    Surface(
                        shape = MaterialTheme.shapes.small,
                        color = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer,
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.WarningAmber,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                            )
                            Text(
                                text = t(
                                    "尚未配置开学基准日期，系统无法将课程对应至具体公历日期。请先设定开学首周日期。",
                                    "Set the first week's start date so classes can be mapped to calendar dates.",
                                ),
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                    }
                } else if (totalCourses == 0) {
                    Text(
                        text = t("当前课表暂无排课数据，无法执行日程写入。", "There are no scheduled classes to export."),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    Text(
                        text = t(
                            "将依各教学楼作息时间，把本学期全部周次课程完整写入手机系统日历（Calendar Provider）。",
                            "All classes for this semester will be written to the system calendar using each building's schedule.",
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Surface(
                        shape = MaterialTheme.shapes.small,
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            FeaturePoint(
                                t("全学期覆盖", "Full semester"),
                                t(
                                    "完整导入第 1~${state.lastTeachingWeek} 周所有课程安排",
                                    "Import every class from weeks 1–${state.lastTeachingWeek}",
                                ),
                            )
                            FeaturePoint(
                                t("楼宇作息", "Building schedules"),
                                t("自动按教室所属楼栋对应起止时间", "Use the start and end times of each building"),
                            )
                            FeaturePoint(
                                t("调休适配", "Makeup days"),
                                t("自动排除法定放假日，补上调休课", "Skip public holidays and include makeup classes"),
                            )
                            FeaturePoint(
                                t("系统提醒", "System reminders"),
                                t("每节课前 15 分钟触发系统级日程提醒", "Remind 15 minutes before every class"),
                            )
                            FeaturePoint(
                                t("桌面组件", "Desktop widgets"),
                                t("写入后可直接在桌面日历小组件展示", "Show events in calendar widgets"),
                            )
                            FeaturePoint(
                                t("幂等更新", "Safe updates"),
                                t("重复同步自动覆盖旧记录，不产生重复日程", "Re-sync replaces old events without duplicates"),
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (needsTermStart) {
                    Button(
                        onClick = {
                            onDismissRequest()
                            onNavigateToSetTermStart()
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(t("前往设置开学日期", "Set start date"))
                    }
                } else if (totalCourses > 0) {
                    Button(
                        onClick = {
                            onDismissRequest()
                            onConfirmSync()
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Sync,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(t("同步全部至系统日历", "Sync all to calendar"))
                    }

                    OutlinedButton(
                        onClick = {
                            onDismissRequest()
                            onClearSync()
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Delete,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(t("清除已导入的课表日程", "Clear imported class events"), color = MaterialTheme.colorScheme.error)
                    }
                }

                TextButton(
                    onClick = onDismissRequest,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(t("取消", "Cancel"))
                }
            }
        },
        dismissButton = null,
    )
}

@Composable
private fun FeaturePoint(title: String, desc: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Icon(
            imageVector = Icons.Outlined.Check,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(14.dp),
        )
        Text(
            text = "$title：$desc",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
