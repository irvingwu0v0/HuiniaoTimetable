package com.irvingwu.huiniaotable.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
internal fun PreferenceGroup(content: @Composable () -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) { content() }
    }
}

@Composable
internal fun PreferenceIcon(vector: ImageVector) {
    Icon(vector, null, Modifier.padding(end = 8.dp).size(24.dp), tint = MiuixTheme.colorScheme.onSurfaceContainer)
}

@Composable
internal fun PreferenceLine(icon: ImageVector? = null, title: String, subtitle: String? = null,
    onClick: (() -> Unit)? = null, trailing: (@Composable () -> Unit)? = null) {
    BasicComponent(title = title, summary = subtitle, onClick = onClick, insideMargin = PaddingValues(vertical = 10.dp),
        startAction = icon?.let { { PreferenceIcon(it) } },
        endActions = { trailing?.invoke() })
}
