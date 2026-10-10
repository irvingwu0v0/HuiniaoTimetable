package com.irvingwu.huiniaotable.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import com.irvingwu.huiniaotable.data.HolidayRegion
import com.irvingwu.huiniaotable.data.HolidaySource
import androidx.compose.material.icons.outlined.Dns
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material.icons.outlined.EditCalendar
import androidx.compose.material.icons.outlined.HolidayVillage
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material.icons.outlined.ViewWeek
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.irvingwu.huiniaotable.data.AppState
import com.irvingwu.huiniaotable.data.HolidayDay
import com.irvingwu.huiniaotable.data.MakeupGuesser
import com.irvingwu.huiniaotable.data.WeekendMode

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private data class FestivalSchedule(
    val name: String,
    val offDays: List<HolidayDay>,
    val makeupDays: List<HolidayDay>,
)

private val dateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
private val timeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")

/**
 * 节假日与调休编排独立配置页面。
 *
 * 核心能力：
 * 1. 周末列展示策略（智能判定 / 完整周期 / 仅工作日）；
 * 2. 法定节假日数据同步与状态展示；
 * 3. 调休补课星期独立校准；
 * 4. 自定义休假与调休教学日添加与删除。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HolidaysScreen(
    state: AppState,
    onBack: () -> Unit,
    onSelectWeekendMode: (WeekendMode) -> Unit,
    onSelectHolidayRegion: (HolidayRegion) -> Unit,
    onSelectHolidaySource: (HolidaySource) -> Unit,
    onSetHolidayCustomMirror: (String) -> Unit = {},
    onRefreshHolidays: () -> Unit,
    onAddHolidayOverride: (LocalDate, String, Boolean) -> Unit,
    onRemoveHolidayOverride: (LocalDate) -> Unit,
    onSetMakeup: (LocalDate, Int) -> Unit,
    onClearMakeup: (LocalDate) -> Unit,
) {
    BackHandler(onBack = onBack)

    var activeMakeupDay by remember { mutableStateOf<HolidayDay?>(null) }
    var showOverrideDatePicker by remember { mutableStateOf(false) }
    var showOverrideDialog by remember { mutableStateOf(false) }
    var overrideDate by remember { mutableStateOf<LocalDate?>(null) }
    var showCustomMirrorDialog by remember { mutableStateOf(false) }

    val festivalSchedules = remember(state.holidays) {
        val days = state.holidays?.days?.values?.sortedBy { it.date }.orEmpty()
        val map = linkedMapOf<String, MutableList<HolidayDay>>()
        days.forEach { day ->
            map.getOrPut(day.name) { mutableListOf() }.add(day)
        }
        map.map { (name, list) ->
            val off = list.filter { it.isOffDay }.sortedBy { it.date }
            val makeup = list.filterNot { it.isOffDay }.sortedBy { it.date }
            FestivalSchedule(name, off, makeup)
        }
    }

    // 调休补课安排弹窗选项（像调课弹窗一样快捷选择周几、休息或恢复系统推算）
    activeMakeupDay?.let { day ->
        val currentSelected = state.makeupMap[day.date] ?: 0
        MakeupDayDialog(
            date = day.date,
            currentTarget = currentSelected,
            isOverridden = state.makeupOverrides.containsKey(day.date),
            onSelectWeekday = { weekday ->
                onSetMakeup(day.date, weekday)
                activeMakeupDay = null
            },
            onRest = {
                onSetMakeup(day.date, 0)
                activeMakeupDay = null
            },
            onRestore = {
                onClearMakeup(day.date)
                activeMakeupDay = null
            },
            onDismiss = { activeMakeupDay = null },
        )
    }

    // 新增自定义假期日期选取器
    if (showOverrideDatePicker && overrideDate != null) {
        PickDateDialog(
            title = "选取日期",
            current = overrideDate,
            onDismiss = {
                overrideDate = null
                showOverrideDatePicker = false
            },
            onConfirm = {
                overrideDate = it
                showOverrideDatePicker = false
                showOverrideDialog = true
            },
        )
    }

    // 自定义数据源或 ghproxy 地址配置对话框
    if (showCustomMirrorDialog) {
        CustomMirrorDialog(
            initialValue = state.holidayCustomMirror,
            onDismiss = { showCustomMirrorDialog = false },
            onConfirm = { url ->
                showCustomMirrorDialog = false
                onSetHolidayCustomMirror(url)
            },
        )
    }

    // 新增自定义假期配置对话框
    if (showOverrideDialog && overrideDate != null) {
        CustomHolidayDialog(
            date = overrideDate!!,
            onDismiss = {
                showOverrideDialog = false
                overrideDate = null
            },
            onConfirm = { name, isOffDay ->
                onAddHolidayOverride(overrideDate!!, name, isOffDay)
                showOverrideDialog = false
                overrideDate = null
            },
        )
    }

    Scaffold(
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
                        text = "节假日与调休编排",
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
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            // ---- 周末展示策略 ----
            item {
                HolidaysCard {
                    HolidaysSettingRow(
                        icon = Icons.Outlined.ViewWeek,
                        title = "周末列展示模式",
                        subtitle = when (state.weekendMode) {
                            WeekendMode.Auto -> "按排课与调休智能显示周末"
                            WeekendMode.Always -> "固定展示周一至周日全周期"
                            WeekendMode.Hide -> "固定仅展示工作日"
                        },
                    )

                    SingleChoiceSegmentedButtonRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                    ) {
                        val modes = listOf(
                            WeekendMode.Auto to "智能判定",
                            WeekendMode.Always to "完整周期",
                            WeekendMode.Hide to "仅工作日",
                        )
                        modes.forEachIndexed { index, (mode, label) ->
                            SegmentedButton(
                                shape = SegmentedButtonDefaults.itemShape(index = index, count = modes.size),
                                onClick = { onSelectWeekendMode(mode) },
                                selected = (mode == state.weekendMode),
                                icon = { SegmentedButtonDefaults.Icon(active = (mode == state.weekendMode)) },
                            ) {
                                Text(label)
                            }
                        }
                    }
                }
            }

            // ---- 法定节假日数据服务 ----
            item {
                HolidaysCard {
                    var showRegionDropdown by remember { mutableStateOf(false) }

                    Box {
                        HolidaysSettingRow(
                            icon = Icons.Outlined.Place,
                            title = "选择放假方案",
                            subtitle = state.holidayRegion.displayName,
                            onClick = { showRegionDropdown = true },
                            trailing = {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                ) {
                                    Text(
                                        text = state.holidayRegion.displayName,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.primary,
                                    )
                                    Icon(
                                        imageVector = Icons.Default.ArrowDropDown,
                                        contentDescription = "选择自治区放假安排",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            },
                        )

                        DropdownMenu(
                            expanded = showRegionDropdown,
                            onDismissRequest = { showRegionDropdown = false },
                        ) {
                            HolidayRegion.entries.forEach { region ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = region.displayName,
                                            fontWeight = if (state.holidayRegion == region) FontWeight.SemiBold else FontWeight.Normal,
                                            color = if (state.holidayRegion == region) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                        )
                                    },
                                    leadingIcon = if (state.holidayRegion == region) {
                                        {
                                            Icon(
                                                imageVector = Icons.Outlined.Check,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(18.dp),
                                            )
                                        }
                                    } else null,
                                    onClick = {
                                        showRegionDropdown = false
                                        onSelectHolidayRegion(region)
                                    },
                                )
                            }
                        }
                    }

                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 4.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                    )

                    var showSourceDropdown by remember { mutableStateOf(false) }

                    Box {
                        HolidaysSettingRow(
                            icon = Icons.Outlined.Dns,
                            title = "数据同步源",
                            subtitle = when (state.holidaySource) {
                                HolidaySource.CUSTOM -> if (state.holidayCustomMirror.isNotBlank()) "自定义镜像" else state.holidaySource.description
                                else -> state.holidaySource.description
                            },
                            onClick = { showSourceDropdown = true },
                            trailing = {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                ) {
                                    Text(
                                        text = state.holidaySource.displayName,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.primary,
                                    )
                                    Icon(
                                        imageVector = Icons.Default.ArrowDropDown,
                                        contentDescription = "选择数据同步源",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            },
                        )

                        DropdownMenu(
                            expanded = showSourceDropdown,
                            onDismissRequest = { showSourceDropdown = false },
                        ) {
                            HolidaySource.entries.forEach { source ->
                                DropdownMenuItem(
                                    text = {
                                        Column {
                                            Text(
                                                text = source.displayName,
                                                fontWeight = if (state.holidaySource == source) FontWeight.SemiBold else FontWeight.Normal,
                                                color = if (state.holidaySource == source) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                            )
                                            Text(
                                                text = source.description,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            )
                                        }
                                    },
                                    leadingIcon = if (state.holidaySource == source) {
                                        {
                                            Icon(
                                                imageVector = Icons.Outlined.Check,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(18.dp),
                                            )
                                        }
                                    } else null,
                                    onClick = {
                                        showSourceDropdown = false
                                        if (source == HolidaySource.CUSTOM) {
                                            showCustomMirrorDialog = true
                                        } else {
                                            onSelectHolidaySource(source)
                                        }
                                    },
                                )
                            }
                        }
                    }

                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 4.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                    )
                    HolidaysSettingRow(
                        icon = Icons.Outlined.HolidayVillage,
                        title = "法定节假日数据状态",
                        subtitle = state.holidays?.let { "已加载 ${it.days.size} 个公历节假日及调休节点" } ?: "未加载法定假日数据",
                    )
                    HolidaysSettingRow(
                        icon = Icons.Outlined.Sync,
                        title = "节假日数据更新时间",
                        subtitle = formatTime(state.holidayFetchedAt),
                    )
                    FilledTonalButton(
                        onClick = onRefreshHolidays,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                    ) {
                        Icon(imageVector = Icons.Outlined.Sync, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("同步最新法定放假及调休安排")
                    }
                }
            }

            // ---- 法定节假日与调休安排（按节日小标题分割） ----
            if (festivalSchedules.isNotEmpty()) {
                    festivalSchedules.forEach { festival ->
                    item(key = festival.name) {
                        HolidaysCard {
                            // 节日小标题与标签
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.DateRange,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp),
                                    )
                                    Text(
                                        text = festival.name,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.primary,
                                    )
                                }
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = MaterialTheme.colorScheme.secondaryContainer,
                                ) {
                                    Text(
                                        text = "放假 ${festival.offDays.size} 天" +
                                            if (festival.makeupDays.isNotEmpty()) " · 调休 ${festival.makeupDays.size} 天" else "",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                    )
                                }
                            }

                            // 放假日期明细
                            if (festival.offDays.isNotEmpty()) {
                                val first = festival.offDays.first().date
                                val last = festival.offDays.last().date
                                val dateSpan = if (first == last) {
                                    first.format(dateFormatter)
                                } else {
                                    "${first.format(dateFormatter)} ~ ${last.format(dateFormatter)}"
                                }
                                Text(
                                    text = "放假安排：$dateSpan（全天停课放假）",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(top = 2.dp),
                                )
                            }

                            // 调休补课明细（若有）
                            if (festival.makeupDays.isNotEmpty()) {
                                HorizontalDivider(
                                    modifier = Modifier.padding(vertical = 4.dp),
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                                )
                                Text(
                                    text = "调休补课安排（轻触可校准对应补课星期）：",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                festival.makeupDays.forEach { day ->
                                    val weekday = MakeupGuesser.weekdayLabel(day.date.dayOfWeek.value)
                                    val currentTarget = state.makeupMap[day.date]
                                    val isOverridden = state.makeupOverrides.containsKey(day.date)

                                    HolidaysSettingRow(
                                        icon = Icons.Outlined.EditCalendar,
                                        title = "${day.date.format(dateFormatter)} ($weekday)",
                                        subtitle = when (currentTarget) {
                                            null, 0 -> "调休上班 · 不执行补课"
                                            else -> "调休补课 · 执行 ${MakeupGuesser.weekdayLabel(currentTarget)} 课表"
                                        },
                                        onClick = { activeMakeupDay = day },
                                        trailing = {
                                            Surface(
                                                shape = RoundedCornerShape(6.dp),
                                                color = if (isOverridden) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHighest,
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                                                ) {
                                                    Text(
                                                        text = when (currentTarget) {
                                                            null, 0 -> "不补课"
                                                            else -> "补${MakeupGuesser.weekdayLabel(currentTarget)}"
                                                        },
                                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                                                        fontWeight = FontWeight.Medium,
                                                        color = if (isOverridden) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                                                    )
                                                    Icon(
                                                        imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                                                        contentDescription = null,
                                                        modifier = Modifier.size(14.dp),
                                                        tint = if (isOverridden) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                                                    )
                                                }
                                            }
                                        },
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // ---- 自定义节假日与特殊调休 ----
            item {
                HolidaysCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            text = "校级调休或个性化放假：",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        OutlinedButton(
                            onClick = {
                                overrideDate = LocalDate.now()
                                showOverrideDatePicker = true
                            },
                        ) {
                            Icon(imageVector = Icons.Outlined.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("添加自定义安排")
                        }
                    }

                    if (state.holidayOverrides.isEmpty()) {
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "暂无自定义节假日，系统完全依据法定公共假期运行。",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline,
                        )
                    } else {
                        state.holidayOverrides.forEach { item ->
                            HorizontalDivider(
                                modifier = Modifier.padding(vertical = 4.dp),
                                color = MaterialTheme.colorScheme.outlineVariant,
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.DateRange,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp),
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "${item.date.format(dateFormatter)} · ${item.name}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium,
                                    )
                                    Text(
                                        text = if (item.isOffDay) "法定/校级休假 (课程停排)" else "调休教学日",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (item.isOffDay) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.tertiary,
                                    )
                                }
                                IconButton(onClick = { onRemoveHolidayOverride(item.date) }) {
                                    Icon(
                                        imageVector = Icons.Outlined.Close,
                                        contentDescription = "删除",
                                        modifier = Modifier.size(18.dp),
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CustomHolidayDialog(
    date: LocalDate,
    onDismiss: () -> Unit,
    onConfirm: (String, Boolean) -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var isOffDay by remember { mutableStateOf(true) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "配置日期：${date.format(dateFormatter)}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("节假日或调休事由") },
                    placeholder = { Text("如：校庆日 / 运动会调休") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )

                Text(
                    text = "日程属性设定：",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = isOffDay,
                        onClick = { isOffDay = true },
                        label = { Text("休假 (停排课程)") },
                    )
                    FilterChip(
                        selected = !isOffDay,
                        onClick = { isOffDay = false },
                        label = { Text("调休教学日") },
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(name.ifBlank { "自定义安排" }, isOffDay) },
            ) {
                Text("保存配置")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        },
    )
}

@Composable
private fun HolidaysSectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 10.dp, bottom = 2.dp),
    )
}

@Composable
private fun HolidaysCard(content: @Composable () -> Unit) {
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
        ) {
            content()
        }
    }
}

@Composable
private fun HolidaysSettingRow(
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
        .padding(vertical = 8.dp, horizontal = 2.dp)

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

@Composable
private fun CustomMirrorDialog(
    initialValue: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var text by remember {
        mutableStateOf(
            initialValue.ifBlank { "https://ghproxy.net/https://raw.githubusercontent.com/irvingwu0v0/holiday-cn/master" },
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "自定义数据源 / ghproxy",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "支持输入自建 ghproxy 代理前缀或完整的 holiday-cn 镜像仓库根地址：",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    label = { Text("镜像地址或代理 URL") },
                    placeholder = { Text("https://ghproxy.net/https://raw.githubusercontent.com/irvingwu0v0/holiday-cn/master") },
                    singleLine = false,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (text.isNotBlank()) {
                        onConfirm(text.trim())
                    }
                },
            ) {
                Text("保存并同步")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        },
    )
}

private fun formatTime(epochMillis: Long): String {
    if (epochMillis <= 0L) return "尚未执行同步"
    return Instant.ofEpochMilli(epochMillis)
        .atZone(ZoneId.systemDefault())
        .format(timeFormatter)
}

@Composable
private fun MakeupDayDialog(
    date: LocalDate,
    currentTarget: Int,
    isOverridden: Boolean,
    onSelectWeekday: (Int) -> Unit,
    onRest: () -> Unit,
    onRestore: () -> Unit,
    onDismiss: () -> Unit,
) {
    val weekdays = listOf(
        1 to "周一", 2 to "周二", 3 to "周三", 4 to "周四",
        5 to "周五", 6 to "周六", 7 to "周日",
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Outlined.EditCalendar,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp),
            )
        },
        title = {
            Text(
                text = "${date.format(dateFormatter)} 调休安排",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    text = "选择对应补课星期：",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    weekdays.take(4).forEach { (code, name) ->
                        val selected = (currentTarget == code)
                        Surface(
                            onClick = { onSelectWeekday(code) },
                            shape = MaterialTheme.shapes.small,
                            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHighest,
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp),
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = name,
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                                )
                            }
                        }
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    weekdays.drop(4).forEach { (code, name) ->
                        val selected = (currentTarget == code)
                        Surface(
                            onClick = { onSelectWeekday(code) },
                            shape = MaterialTheme.shapes.small,
                            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHighest,
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp),
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = name,
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                                )
                            }
                        }
                    }
                }

                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 4.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    val isRest = (currentTarget == 0)
                    OutlinedButton(
                        onClick = onRest,
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(if (isRest) "当前：休息" else "休息")
                    }
                    FilledTonalButton(
                        onClick = onRestore,
                        modifier = Modifier.weight(1f),
                    ) {
                        Text("恢复系统推算")
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("关闭")
            }
        },
    )
}
