package com.irvingwu.huiniaotable.ui

import com.irvingwu.huiniaotable.ui.icons.AppIcons

import androidx.compose.ui.input.nestedscroll.nestedScroll
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import com.irvingwu.huiniaotable.ui.compat.DropdownMenu
import com.irvingwu.huiniaotable.ui.compat.DropdownMenuItem
import top.yukonga.miuix.kmp.basic.Icon as MiuixIcon
import top.yukonga.miuix.kmp.basic.IconButton as MiuixIconButton
import top.yukonga.miuix.kmp.basic.Scaffold as MiuixScaffold
import top.yukonga.miuix.kmp.basic.TopAppBar as MiuixTopAppBar
import com.irvingwu.huiniaotable.data.HolidayRegion
import com.irvingwu.huiniaotable.data.HolidaySource
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
import com.irvingwu.huiniaotable.ui.compat.AlertDialog
import com.irvingwu.huiniaotable.ui.compat.Button
import com.irvingwu.huiniaotable.ui.compat.FilledTonalButton
import com.irvingwu.huiniaotable.ui.compat.FilterChip
import com.irvingwu.huiniaotable.ui.compat.HorizontalDivider
import com.irvingwu.huiniaotable.ui.compat.Icon
import com.irvingwu.huiniaotable.ui.compat.IconButton
import com.irvingwu.huiniaotable.ui.compat.AppTheme
import com.irvingwu.huiniaotable.ui.compat.OutlinedButton
import com.irvingwu.huiniaotable.ui.compat.OutlinedTextField
import com.irvingwu.huiniaotable.ui.compat.SegmentedButton
import com.irvingwu.huiniaotable.ui.compat.SegmentedButtonDefaults
import com.irvingwu.huiniaotable.ui.compat.SingleChoiceSegmentedButtonRow
import com.irvingwu.huiniaotable.ui.compat.Surface
import com.irvingwu.huiniaotable.ui.compat.Text
import com.irvingwu.huiniaotable.ui.compat.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.irvingwu.huiniaotable.data.AppState
import com.irvingwu.huiniaotable.data.HolidayDay
import com.irvingwu.huiniaotable.data.MakeupGuesser
import com.irvingwu.huiniaotable.data.WeekendMode
import com.irvingwu.huiniaotable.t

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
            title = t("选取日期", "Select date"),
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

    val scrollBehavior = MiuixScrollBehavior()
    MiuixScaffold(
        topBar = {
            MiuixTopAppBar(
                scrollBehavior = scrollBehavior,
                navigationIcon = {
                    MiuixIconButton(onClick = onBack) {
                        MiuixIcon(
                            imageVector = AppIcons.ArrowBack,
                            contentDescription = t("返回", "Back"),
                        )
                    }
                },
                title = t("节假日与调休", "Holidays & makeup days"),
            )
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .nestedScroll(scrollBehavior.nestedScrollConnection),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            // ---- 周末展示策略 ----
            item {
                PreferenceGroup {
                    PreferenceLine(
                        icon = AppIcons.ViewWeek,
                        title = t("周末列展示模式", "Weekend display"),
                        subtitle = when (state.weekendMode) {
                            WeekendMode.Auto -> t("按排课与调休智能显示周末", "Show weekends when classes or makeup days require it")
                            WeekendMode.Always -> t("固定展示周一至周日全周期", "Always show Monday through Sunday")
                            WeekendMode.Hide -> t("固定仅展示工作日", "Always show weekdays only")
                        },
                    )

                    SingleChoiceSegmentedButtonRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                    ) {
                        val modes = listOf(
                            WeekendMode.Auto to t("智能判定", "Smart"),
                            WeekendMode.Always to t("完整周期", "Full week"),
                            WeekendMode.Hide to t("仅工作日", "Weekdays"),
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
                PreferenceGroup {
                    var showRegionDropdown by remember { mutableStateOf(false) }

                    Box {
                        PreferenceLine(
                            icon = AppIcons.Place,
                            title = t("选择放假方案", "Holiday schedule"),
                            subtitle = state.holidayRegion.displayName,
                            onClick = { showRegionDropdown = true },
                            trailing = {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                ) {
                                    Text(
                                        text = state.holidayRegion.displayName,
                                        style = AppTheme.typography.bodyMedium,
                                        color = AppTheme.colorScheme.primary,
                                    )
                                    Icon(
                                        imageVector = AppIcons.ArrowDropDown,
                                        contentDescription = t("选择自治区放假安排", "Select regional holiday data"),
                                        tint = AppTheme.colorScheme.onSurfaceVariant,
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
                                            color = if (state.holidayRegion == region) AppTheme.colorScheme.primary else AppTheme.colorScheme.onSurface,
                                        )
                                    },
                                    leadingIcon = if (state.holidayRegion == region) {
                                        {
                                            Icon(
                                                imageVector = AppIcons.Check,
                                                contentDescription = null,
                                                tint = AppTheme.colorScheme.primary,
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
                        color = AppTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                    )

                    var showSourceDropdown by remember { mutableStateOf(false) }

                    Box {
                        PreferenceLine(
                            icon = AppIcons.Dns,
                            title = t("数据同步源", "Data source"),
                            subtitle = when (state.holidaySource) {
                                HolidaySource.CUSTOM -> if (state.holidayCustomMirror.isNotBlank()) {
                                    t("自定义镜像", "Custom mirror")
                                } else {
                                    state.holidaySource.description
                                }
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
                                        style = AppTheme.typography.bodyMedium,
                                        color = AppTheme.colorScheme.primary,
                                    )
                                    Icon(
                                        imageVector = AppIcons.ArrowDropDown,
                                        contentDescription = t("选择数据同步源", "Select data source"),
                                        tint = AppTheme.colorScheme.onSurfaceVariant,
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
                                                color = if (state.holidaySource == source) AppTheme.colorScheme.primary else AppTheme.colorScheme.onSurface,
                                            )
                                            Text(
                                                text = source.description,
                                                style = AppTheme.typography.bodySmall,
                                                color = AppTheme.colorScheme.onSurfaceVariant,
                                            )
                                        }
                                    },
                                    leadingIcon = if (state.holidaySource == source) {
                                        {
                                            Icon(
                                                imageVector = AppIcons.Check,
                                                contentDescription = null,
                                                tint = AppTheme.colorScheme.primary,
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
                        color = AppTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                    )
                    PreferenceLine(icon = AppIcons.Sync, title = t("同步节假日", "Sync holidays"),
                        subtitle = formatTime(state.holidayFetchedAt), onClick = onRefreshHolidays)
                }
            }

            // ---- 法定节假日与调休安排（按节日小标题分割） ----
            if (festivalSchedules.isNotEmpty()) {
                    festivalSchedules.forEach { festival ->
                    item(key = festival.name) {
                        PreferenceGroup {
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
                                        imageVector = AppIcons.DateRange,
                                        contentDescription = null,
                                        tint = AppTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp),
                                    )
                                    Text(
                                        text = festival.name,
                                        style = AppTheme.typography.titleMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = AppTheme.colorScheme.primary,
                                    )
                                }
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = AppTheme.colorScheme.secondaryContainer,
                                ) {
                                    Text(
                                        text = t(
                                            "放假 ${festival.offDays.size} 天" +
                                                if (festival.makeupDays.isNotEmpty()) " · 调休 ${festival.makeupDays.size} 天" else "",
                                            "${festival.offDays.size} days off" +
                                                if (festival.makeupDays.isNotEmpty()) " · ${festival.makeupDays.size} makeup days" else "",
                                        ),
                                        style = AppTheme.typography.labelSmall,
                                        color = AppTheme.colorScheme.onSecondaryContainer,
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
                                    text = t("放假安排：$dateSpan（全天停课放假）", "Holiday: $dateSpan (no classes)"),
                                    style = AppTheme.typography.bodySmall,
                                    color = AppTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(top = 2.dp),
                                )
                            }

                            // 调休补课明细（若有）
                            if (festival.makeupDays.isNotEmpty()) {
                                HorizontalDivider(
                                    modifier = Modifier.padding(vertical = 4.dp),
                                    color = AppTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                                )
                                Text(
                                    text = t("调休补课安排（轻触可校准对应补课星期）：", "Makeup classes (tap to choose the weekday):"),
                                    style = AppTheme.typography.labelSmall,
                                    color = AppTheme.colorScheme.onSurfaceVariant,
                                )
                                festival.makeupDays.forEach { day ->
                                    val weekday = MakeupGuesser.weekdayLabel(day.date.dayOfWeek.value)
                                    val currentTarget = state.makeupMap[day.date]
                                    val isOverridden = state.makeupOverrides.containsKey(day.date)

                                    PreferenceLine(
                                        icon = AppIcons.EditCalendar,
                                        title = "${day.date.format(dateFormatter)} ($weekday)",
                                        subtitle = when (currentTarget) {
                                            null, 0 -> t("调休上班 · 不执行补课", "Workday · no makeup classes")
                                            else -> t(
                                                "调休补课 · 执行 ${MakeupGuesser.weekdayLabel(currentTarget)} 课表",
                                                "Makeup classes · follow the ${MakeupGuesser.weekdayLabel(currentTarget)} timetable",
                                            )
                                        },
                                        onClick = { activeMakeupDay = day },
                                        trailing = {
                                            Surface(
                                                shape = RoundedCornerShape(6.dp),
                                                color = if (isOverridden) AppTheme.colorScheme.primaryContainer else AppTheme.colorScheme.surfaceContainerHighest,
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                                                ) {
                                                    Text(
                                                        text = when (currentTarget) {
                                                            null, 0 -> t("不补课", "No makeup")
                                                            else -> t(
                                                                "补${MakeupGuesser.weekdayLabel(currentTarget)}",
                                                                "Makeup: ${MakeupGuesser.weekdayLabel(currentTarget)}",
                                                            )
                                                        },
                                                        style = AppTheme.typography.labelSmall.copy(fontSize = 11.sp),
                                                        fontWeight = FontWeight.Medium,
                                                        color = if (isOverridden) AppTheme.colorScheme.onPrimaryContainer else AppTheme.colorScheme.onSurfaceVariant,
                                                    )
                                                    Icon(
                                                        imageVector = AppIcons.KeyboardArrowRight,
                                                        contentDescription = null,
                                                        modifier = Modifier.size(14.dp),
                                                        tint = if (isOverridden) AppTheme.colorScheme.onPrimaryContainer else AppTheme.colorScheme.onSurfaceVariant,
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
                PreferenceGroup {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            text = t("校级调休或个性化放假：", "School-specific holidays or makeup days:"),
                            style = AppTheme.typography.bodySmall,
                            color = AppTheme.colorScheme.onSurfaceVariant,
                        )
                        OutlinedButton(
                            onClick = {
                                overrideDate = LocalDate.now()
                                showOverrideDatePicker = true
                            },
                        ) {
                            Icon(imageVector = AppIcons.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(t("添加自定义安排", "Add custom day"))
                        }
                    }

                    if (state.holidayOverrides.isEmpty()) {
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = t(
                                "暂无自定义节假日，系统完全依据法定公共假期运行。",
                                "No custom holidays. The app follows the official public holiday schedule.",
                            ),
                            style = AppTheme.typography.bodySmall,
                            color = AppTheme.colorScheme.outline,
                        )
                    } else {
                        state.holidayOverrides.forEach { item ->
                            HorizontalDivider(
                                modifier = Modifier.padding(vertical = 4.dp),
                                color = AppTheme.colorScheme.outlineVariant,
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                Icon(
                                    imageVector = AppIcons.DateRange,
                                    contentDescription = null,
                                    tint = AppTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp),
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "${item.date.format(dateFormatter)} · ${item.name}",
                                        style = AppTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium,
                                    )
                                    Text(
                                        text = if (item.isOffDay) {
                                            t("法定/校级休假 (课程停排)", "Holiday (no classes)")
                                        } else {
                                            t("调休教学日", "Makeup school day")
                                        },
                                        style = AppTheme.typography.bodySmall,
                                        color = if (item.isOffDay) AppTheme.colorScheme.primary else AppTheme.colorScheme.tertiary,
                                    )
                                }
                                IconButton(onClick = { onRemoveHolidayOverride(item.date) }) {
                                    Icon(
                                        imageVector = AppIcons.Close,
                                        contentDescription = t("删除", "Delete"),
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
                text = t("配置日期：${date.format(dateFormatter)}", "Date: ${date.format(dateFormatter)}"),
                style = AppTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(t("节假日或调休事由", "Holiday or makeup reason")) },
                    placeholder = { Text(t("如：校庆日 / 运动会调休", "e.g. Anniversary / Sports day makeup")) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )

                Text(
                    text = t("日程属性设定：", "Day type:"),
                    style = AppTheme.typography.labelMedium,
                    color = AppTheme.colorScheme.onSurfaceVariant,
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = isOffDay,
                        onClick = { isOffDay = true },
                        label = { Text(t("休假 (停排课程)", "Holiday (no classes)")) },
                    )
                    FilterChip(
                        selected = !isOffDay,
                        onClick = { isOffDay = false },
                        label = { Text(t("调休教学日", "Makeup school day")) },
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(name.ifBlank { t("自定义安排", "Custom day") }, isOffDay) },
            ) {
                Text(t("保存配置", "Save"))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(t("取消", "Cancel")) }
        },
    )
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
                text = t("自定义数据源 / ghproxy", "Custom source / ghproxy"),
                style = AppTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = t(
                        "支持输入自建 ghproxy 代理前缀或完整的 holiday-cn 镜像仓库根地址：",
                        "Enter a self-hosted ghproxy prefix or the root URL of a holiday-cn mirror:",
                    ),
                    style = AppTheme.typography.bodySmall,
                    color = AppTheme.colorScheme.onSurfaceVariant,
                )
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    label = { Text(t("镜像地址或代理 URL", "Mirror or proxy URL")) },
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
                Text(t("保存并同步", "Save and sync"))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(t("取消", "Cancel")) }
        },
    )
}

private fun formatTime(epochMillis: Long): String {
    if (epochMillis <= 0L) return t("尚未执行同步", "Never synced")
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
        1 to t("周一", "Mon"), 2 to t("周二", "Tue"), 3 to t("周三", "Wed"), 4 to t("周四", "Thu"),
        5 to t("周五", "Fri"), 6 to t("周六", "Sat"), 7 to t("周日", "Sun"),
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = AppIcons.EditCalendar,
                contentDescription = null,
                tint = AppTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp),
            )
        },
        title = {
            Text(
                text = t("${date.format(dateFormatter)} 调休安排", "${date.format(dateFormatter)} makeup arrangement"),
                style = AppTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    text = t("选择对应补课星期：", "Choose the weekday to follow:"),
                    style = AppTheme.typography.labelMedium,
                    color = AppTheme.colorScheme.onSurfaceVariant,
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    weekdays.take(4).forEach { (code, name) ->
                        val selected = (currentTarget == code)
                        Surface(
                            onClick = { onSelectWeekday(code) },
                            shape = AppTheme.shapes.small,
                            color = if (selected) AppTheme.colorScheme.primary else AppTheme.colorScheme.surfaceContainerHighest,
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp),
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = name,
                                    style = AppTheme.typography.labelLarge,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (selected) AppTheme.colorScheme.onPrimary else AppTheme.colorScheme.onSurface,
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
                            shape = AppTheme.shapes.small,
                            color = if (selected) AppTheme.colorScheme.primary else AppTheme.colorScheme.surfaceContainerHighest,
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp),
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = name,
                                    style = AppTheme.typography.labelLarge,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (selected) AppTheme.colorScheme.onPrimary else AppTheme.colorScheme.onSurface,
                                )
                            }
                        }
                    }
                }

                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 4.dp),
                    color = AppTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
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
                        Text(if (isRest) t("当前：休息", "Current: day off") else t("休息", "Day off"))
                    }
                    FilledTonalButton(
                        onClick = onRestore,
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(t("恢复系统推算", "Restore automatic"))
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(t("关闭", "Close"))
            }
        },
    )
}
