package com.irvingwu.huiniaotable.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.irvingwu.huiniaotable.data.AppState
import com.irvingwu.huiniaotable.t
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.window.WindowDialog

@Composable
fun CalendarSyncDialog(
    show: Boolean,
    state: AppState,
    onDismissRequest: () -> Unit,
    onConfirmSync: () -> Unit,
    onClearSync: () -> Unit,
    onNavigateToSetTermStart: () -> Unit,
) {
    val hasClasses = !state.schedule?.courses.isNullOrEmpty()
    WindowDialog(
        show = show,
        title = t("同步到系统日历", "Sync to calendar"),
        summary = when {
            state.needsTermStart -> t("先设置开学日期，以便确定每节课的上课日期。", "Set the term start to put each class on the correct date.")
            !hasClasses -> t("当前课表没有可同步的课程。", "There are no scheduled classes to sync.")
            else -> t("同步第 1–${state.lastTeachingWeek} 周的课程。再次同步会更新原日程。", "Sync weeks 1–${state.lastTeachingWeek}. Syncing again updates existing events.")
        },
        onDismissRequest = onDismissRequest,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (state.needsTermStart || hasClasses) TextButton(
                text = if (state.needsTermStart) t("设置开学日期", "Set term start") else t("同步课程", "Sync classes"),
                colors = ButtonDefaults.textButtonColorsPrimary(),
                onClick = { onDismissRequest(); if (state.needsTermStart) onNavigateToSetTermStart() else onConfirmSync() },
                modifier = Modifier.fillMaxWidth(),
            )
            if (hasClasses && !state.needsTermStart) TextButton(
                text = t("清除已导入的日程", "Clear imported events"),
                colors = ButtonDefaults.textButtonColors(textColor = MiuixTheme.colorScheme.error),
                onClick = { onDismissRequest(); onClearSync() },
                modifier = Modifier.fillMaxWidth(),
            )
            TextButton(text = t("取消", "Cancel"), onClick = onDismissRequest, modifier = Modifier.fillMaxWidth())
        }
    }
}
