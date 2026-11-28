package com.irvingwu.huiniaotable.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.irvingwu.huiniaotable.t
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.NumberPicker
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.window.WindowDialog
import java.time.LocalDate

@Composable
fun PickDateDialog(
    title: String,
    current: LocalDate?,
    onDismiss: () -> Unit,
    onConfirm: (LocalDate) -> Unit,
) {
    var date by rememberSaveable { mutableStateOf(current ?: LocalDate.now()) }
    var visible by remember { mutableStateOf(true) }
    var confirm by remember { mutableStateOf(false) }
    WindowDialog(show = visible, title = title, onDismissRequest = { visible = false }, onDismissFinished = {
        if (confirm) onConfirm(date) else onDismiss()
    }) {
        Row(Modifier.fillMaxWidth()) {
            NumberPicker(date.year, { date = date.withYear(it) }, Modifier.weight(1.35f), range = 1..9999,
                label = { t("${it}年", it.toString()) }, visibleItemCount = 3, textStyle = MiuixTheme.textStyles.title3)
            NumberPicker(date.monthValue, { date = date.withMonth(it) }, Modifier.weight(1f), range = 1..12,
                label = { t("${it}月", it.toString()) }, visibleItemCount = 3, textStyle = MiuixTheme.textStyles.title3)
            NumberPicker(date.dayOfMonth, { date = date.withDayOfMonth(it) }, Modifier.weight(1f), range = 1..date.lengthOfMonth(),
                label = { t("${it}日", it.toString()) }, visibleItemCount = 3, textStyle = MiuixTheme.textStyles.title3)
        }
        Row(Modifier.fillMaxWidth().padding(top = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TextButton(text = t("取消", "Cancel"), onClick = { visible = false }, modifier = Modifier.weight(1f))
            TextButton(text = t("确定", "OK"), onClick = { confirm = true; visible = false },
                colors = ButtonDefaults.textButtonColorsPrimary(), modifier = Modifier.weight(1f))
        }
    }
}
