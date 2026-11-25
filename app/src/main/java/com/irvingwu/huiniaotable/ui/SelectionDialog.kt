package com.irvingwu.huiniaotable.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import com.irvingwu.huiniaotable.ui.compat.AlertDialog
import com.irvingwu.huiniaotable.ui.compat.Icon
import com.irvingwu.huiniaotable.ui.compat.AppTheme
import com.irvingwu.huiniaotable.ui.compat.RadioButton
import com.irvingwu.huiniaotable.ui.compat.Text
import com.irvingwu.huiniaotable.ui.compat.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.irvingwu.huiniaotable.t

/**
 * 单选确认对话框。
 *
 * 在 Android 原生系统设置与 Google 官方移动应用中，移动端选项型偏好一律采用带单选按钮的
 * 居中 Dialog（Single-Choice Dialog），遵循手指点按热区与视觉焦点的官方标准规范，
 * 彻底替换掉桌面端样式的悬浮小下拉菜单（DropdownMenu）。
 */
@Composable
fun <T> SingleChoiceDialog(
    icon: ImageVector? = null,
    title: String,
    items: List<T>,
    selectedItem: T?,
    itemTitle: (T) -> String,
    itemSubtitle: ((T) -> String?)? = null,
    onItemSelected: (T) -> Unit,
    onDismissRequest: () -> Unit,
    dismissButtonText: String = t("取消", "Cancel"),
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        icon = icon?.let {
            {
                Icon(
                    imageVector = it,
                    contentDescription = null,
                    tint = AppTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp),
                )
            }
        },
        title = {
            Text(
                text = title,
                style = AppTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                items.forEach { item ->
                    val isSelected = (item == selectedItem)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(AppTheme.shapes.small)
                            .selectable(
                                selected = isSelected,
                                onClick = {
                                    onItemSelected(item)
                                    onDismissRequest()
                                },
                                role = Role.RadioButton,
                            )
                            .padding(vertical = 10.dp, horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        RadioButton(
                            selected = isSelected,
                            onClick = null,
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = itemTitle(item),
                                style = AppTheme.typography.bodyLarge,
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                color = if (isSelected) {
                                    AppTheme.colorScheme.primary
                                } else {
                                    AppTheme.colorScheme.onSurface
                                },
                            )
                            itemSubtitle?.invoke(item)?.let { sub ->
                                Text(
                                    text = sub,
                                    style = AppTheme.typography.bodySmall,
                                    color = AppTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text(dismissButtonText)
            }
        },
    )
}

/**
 * 按分类（如校区）展示小标题的分组单选确认对话框。
 */
@Composable
fun <T> GroupedSingleChoiceDialog(
    icon: ImageVector? = null,
    title: String,
    groupedItems: Map<String, List<T>>,
    selectedItem: T?,
    itemTitle: (T) -> String,
    onItemSelected: (T) -> Unit,
    onDismissRequest: () -> Unit,
    dismissButtonText: String = t("取消", "Cancel"),
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        icon = icon?.let {
            {
                Icon(
                    imageVector = it,
                    contentDescription = null,
                    tint = AppTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp),
                )
            }
        },
        title = {
            Text(
                text = title,
                style = AppTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                groupedItems.forEach { (groupTitle, items) ->
                    if (groupTitle.isNotBlank()) {
                        Text(
                            text = groupTitle,
                            style = AppTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = AppTheme.colorScheme.primary,
                            modifier = Modifier.padding(start = 8.dp, top = 10.dp, bottom = 4.dp),
                        )
                    }
                    items.forEach { item ->
                        val isSelected = (item == selectedItem)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(AppTheme.shapes.small)
                                .selectable(
                                    selected = isSelected,
                                    onClick = {
                                        onItemSelected(item)
                                        onDismissRequest()
                                    },
                                    role = Role.RadioButton,
                                )
                                .padding(vertical = 10.dp, horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            RadioButton(
                                selected = isSelected,
                                onClick = null,
                            )
                            Text(
                                text = itemTitle(item),
                                style = AppTheme.typography.bodyLarge,
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                color = if (isSelected) {
                                    AppTheme.colorScheme.primary
                                } else {
                                    AppTheme.colorScheme.onSurface
                                },
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text(dismissButtonText)
            }
        },
    )
}
