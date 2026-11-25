package com.irvingwu.huiniaotable.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.irvingwu.huiniaotable.t
import com.irvingwu.huiniaotable.ui.compat.AlertDialog
import com.irvingwu.huiniaotable.ui.compat.OutlinedTextField
import com.irvingwu.huiniaotable.ui.compat.Text
import com.irvingwu.huiniaotable.ui.compat.TextButton
import java.time.LocalDate

/** 日期选择对话框：三个数字输入框，保持轻量与一致的外观。 */
@Composable
fun PickDateDialog(
    title: String,
    current: LocalDate?,
    onDismiss: () -> Unit,
    onConfirm: (LocalDate) -> Unit,
) {
    val initial = current ?: LocalDate.now()
    var year by remember { mutableStateOf(initial.year.toString()) }
    var month by remember { mutableStateOf(initial.monthValue.toString()) }
    var day by remember { mutableStateOf(initial.dayOfMonth.toString()) }
    val parsed = runCatching {
        LocalDate.of(year.trim().toInt(), month.trim().toInt(), day.trim().toInt())
    }.getOrNull()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = year,
                        onValueChange = { year = it.filter(Char::isDigit).take(4) },
                        label = { Text(t("年", "Year")) },
                        singleLine = true,
                        modifier = Modifier.weight(1.4f),
                    )
                    OutlinedTextField(
                        value = month,
                        onValueChange = { month = it.filter(Char::isDigit).take(2) },
                        label = { Text(t("月", "Month")) },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                    )
                    OutlinedTextField(
                        value = day,
                        onValueChange = { day = it.filter(Char::isDigit).take(2) },
                        label = { Text(t("日", "Day")) },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                    )
                }
                Text(
                    if (parsed == null) t("请输入有效日期", "Enter a valid date") else parsed.toString(),
                )
            }
        },
        confirmButton = {
            TextButton(
                text = t("确定", "OK"),
                onClick = { parsed?.let(onConfirm) ?: onDismiss() },
                enabled = parsed != null,
            )
        },
        dismissButton = {
            TextButton(text = t("取消", "Cancel"), onClick = onDismiss)
        },
        modifier = Modifier.fillMaxWidth(),
    )
}
