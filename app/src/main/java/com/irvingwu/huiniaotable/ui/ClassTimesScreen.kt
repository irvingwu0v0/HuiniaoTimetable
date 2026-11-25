package com.irvingwu.huiniaotable.ui

import com.irvingwu.huiniaotable.ui.icons.AppIcons

import android.app.TimePickerDialog
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import com.irvingwu.huiniaotable.ui.compat.AlertDialog
import com.irvingwu.huiniaotable.ui.compat.Button
import com.irvingwu.huiniaotable.ui.compat.ButtonDefaults
import com.irvingwu.huiniaotable.ui.compat.Card
import com.irvingwu.huiniaotable.ui.compat.CardDefaults
import com.irvingwu.huiniaotable.ui.compat.FilledTonalButton
import com.irvingwu.huiniaotable.ui.compat.FilterChip
import com.irvingwu.huiniaotable.ui.compat.HorizontalDivider
import com.irvingwu.huiniaotable.ui.compat.Icon
import com.irvingwu.huiniaotable.ui.compat.IconButton
import com.irvingwu.huiniaotable.ui.compat.AppTheme
import com.irvingwu.huiniaotable.ui.compat.OutlinedButton
import com.irvingwu.huiniaotable.ui.compat.OutlinedTextField
import com.irvingwu.huiniaotable.ui.compat.Scaffold
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import top.yukonga.miuix.kmp.basic.Icon as MiuixIcon
import top.yukonga.miuix.kmp.basic.IconButton as MiuixIconButton
import top.yukonga.miuix.kmp.basic.Scaffold as MiuixScaffold
import top.yukonga.miuix.kmp.basic.TopAppBar as MiuixTopAppBar
import com.irvingwu.huiniaotable.data.AppState
import com.irvingwu.huiniaotable.data.Campus
import com.irvingwu.huiniaotable.data.CampusBuilding
import com.irvingwu.huiniaotable.data.ClassTimes
import com.irvingwu.huiniaotable.data.ClockTime
import com.irvingwu.huiniaotable.t

/**
 * 作息时间与校区楼宇独立配置页面。
 */
@Composable
fun ClassTimesScreen(
    state: AppState,
    onBack: () -> Unit,
    onSelectCampus: (Campus) -> Unit,
    onSelectBuilding: (CampusBuilding) -> Unit,
    onSelectBuildingScheme: (String, CampusBuilding) -> Unit,
    onImportConfig: (String) -> Boolean,
    onResetConfig: () -> Unit,
    onUpdatePeriodTime: (schemeId: String, slotIndex: Int, start: String, end: String) -> Unit,
    onSelectLunchBreakSlot: (Int) -> Unit = {},
    onSaveScheme: (schemeId: String, label: String, campus: String, periods: List<ClockTime>, isDefault: Boolean) -> Unit = { _, _, _, _, _ -> },
    onDeleteScheme: (schemeId: String) -> Unit = {},
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    val schemes = ClassTimes.allBuildings
    var selectedSchemeId by remember(schemes) {
        mutableStateOf(schemes.firstOrNull()?.id ?: ClassTimes.defaultBuilding.id)
    }
    val activeScheme = schemes.firstOrNull { it.id == selectedSchemeId } ?: ClassTimes.defaultBuilding

    // 专属方案编辑页面状态（null 表示在主配置页，CampusBuilding 表示编辑现有方案，empty 表示新建方案）
    var editingScheme by remember { mutableStateOf<CampusBuilding?>(null) }
    var isCreatingScheme by remember { mutableStateOf(false) }

    if (isCreatingScheme) {
        SchemeEditScreen(
            initialScheme = null,
            allCampuses = ClassTimes.campuses,
            onBack = { isCreatingScheme = false },
            onSave = { id, label, campus, periods, isDefault ->
                onSaveScheme(id, label, campus, periods, isDefault)
                selectedSchemeId = id
                isCreatingScheme = false
            },
        )
        return
    }

    if (editingScheme != null) {
        SchemeEditScreen(
            initialScheme = editingScheme,
            allCampuses = ClassTimes.campuses,
            onBack = { editingScheme = null },
            onSave = { id, label, campus, periods, isDefault ->
                onSaveScheme(id, label, campus, periods, isDefault)
                selectedSchemeId = id
                editingScheme = null
            },
            onDelete = { id ->
                onDeleteScheme(id)
                selectedSchemeId = ClassTimes.defaultBuilding.id
                editingScheme = null
            },
        )
        return
    }

    BackHandler(onBack = onBack)

    var showImportDialog by remember { mutableStateOf(false) }
    var showExportDialog by remember { mutableStateOf(false) }
    var showDefaultSchemeDialog by remember { mutableStateOf(false) }
    var showLunchBreakDialog by remember { mutableStateOf(false) }
    var activeBuildingKey by remember { mutableStateOf<String?>(null) }

    val groupedSchemes = remember(schemes) {
        schemes.groupBy { it.campus.ifBlank { t("主校区", "Main Campus") } }
    }

    // 默认作息方案单选对话框（选项下面不要有小字）
    if (showDefaultSchemeDialog) {
        GroupedSingleChoiceDialog(
            icon = AppIcons.Schedule,
            title = t("未指定楼宇默认作息方案", "Default schedule for unmatched buildings"),
            groupedItems = groupedSchemes,
            selectedItem = state.timeTable,
            itemTitle = { it.label },
            onItemSelected = { onSelectBuilding(it) },
            onDismissRequest = { showDefaultSchemeDialog = false },
        )
    }

    // 中午午休开始时段单选对话框
    val activeSchemePeriods = ClassTimes.periods(activeScheme)
    val defaultGapSlot = remember(activeSchemePeriods) { ClassTimes.findLargestGapSlot(activeSchemePeriods) }
    val defaultP1 = activeSchemePeriods.getOrNull(defaultGapSlot - 1)
    val defaultP2 = activeSchemePeriods.getOrNull(defaultGapSlot)
    val defaultGapMinutes = if (defaultP1 != null && defaultP2 != null) ClassTimes.gapMinutesBetween(defaultP1, defaultP2) else 0

    if (showLunchBreakDialog) {
        data class LunchChoice(val slot: Int, val title: String, val subtitle: String)
        val lunchChoices = buildList {
            add(
                LunchChoice(
                    slot = 0,
                    title = t("默认", "Default"),
                    subtitle = t(
                        "第 $defaultGapSlot 节课后 相差 $defaultGapMinutes 分钟",
                        "$defaultGapMinutes min after period $defaultGapSlot",
                    ),
                ),
            )
            for (s in 1 until activeSchemePeriods.size) {
                val p1 = activeSchemePeriods[s - 1]
                val p2 = activeSchemePeriods[s]
                val gap = ClassTimes.gapMinutesBetween(p1, p2)
                add(
                    LunchChoice(
                        slot = s,
                        title = t("第 $s 节后", "After period $s"),
                        subtitle = t("与下一节相差 $gap 分钟", "$gap min until the next period"),
                    ),
                )
            }
        }
        val currentChoice = lunchChoices.firstOrNull { it.slot == state.lunchBreakSlotOverride } ?: lunchChoices.first()

        SingleChoiceDialog(
            icon = AppIcons.WbSunny,
            title = t("选择中午午休时段", "Select lunch break"),
            items = lunchChoices,
            selectedItem = currentChoice,
            itemTitle = { it.title },
            itemSubtitle = { it.subtitle },
            onItemSelected = { choice ->
                onSelectLunchBreakSlot(choice.slot)
                if (choice.slot == 0) {
                    Toast.makeText(
                        context,
                        t(
                            "已恢复为默认午休时段（第 $defaultGapSlot 节后）",
                            "Default lunch break restored (after period $defaultGapSlot)",
                        ),
                        Toast.LENGTH_SHORT,
                    ).show()
                } else {
                    Toast.makeText(
                        context,
                        t(
                            "午休时段已设为第 ${choice.slot} 节课后",
                            "Lunch break set after period ${choice.slot}",
                        ),
                        Toast.LENGTH_SHORT,
                    ).show()
                }
            },
            onDismissRequest = { showLunchBreakDialog = false },
        )
    }

    // 教学楼专属作息方案单选对话框（打开 dialog 分校区小标题展示作息方案）
    activeBuildingKey?.let { buildingKey ->
        GroupedSingleChoiceDialog(
            icon = AppIcons.Domain,
            title = t("配置「$buildingKey」作息方案", "Schedule for $buildingKey"),
            groupedItems = groupedSchemes,
            selectedItem = state.schemeForKey(buildingKey),
            itemTitle = { it.label },
            onItemSelected = { onSelectBuildingScheme(buildingKey, it) },
            onDismissRequest = { activeBuildingKey = null },
        )
    }

    // 导入 JSON 对话框
    if (showImportDialog) {
        ImportJsonDialog(
            onDismiss = { showImportDialog = false },
            onConfirm = { json ->
                val success = onImportConfig(json)
                if (success) {
                    Toast.makeText(context, t("作息配置已成功导入", "Schedule imported"), Toast.LENGTH_SHORT).show()
                    showImportDialog = false
                } else {
                    Toast.makeText(context, t("导入失败：JSON 格式不正确", "Import failed: invalid JSON"), Toast.LENGTH_LONG).show()
                }
            },
        )
    }

    // 导出 JSON 对话框
    if (showExportDialog) {
        val exportedJson = remember { ClassTimes.exportJson() }
        AlertDialog(
            onDismissRequest = { showExportDialog = false },
            title = {
                Text(
                    text = t("导出作息配置 JSON", "Export schedule JSON"),
                    style = AppTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = t("以下为当前生效作息规范的完整结构化定义：", "The complete active schedule definition:"),
                        style = AppTheme.typography.bodySmall,
                        color = AppTheme.colorScheme.onSurfaceVariant,
                    )
                    OutlinedTextField(
                        value = exportedJson,
                        onValueChange = {},
                        readOnly = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(240.dp),
                        textStyle = AppTheme.typography.bodySmall,
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        clipboardManager.setText(AnnotatedString(exportedJson))
                        Toast.makeText(context, t("已将配置复制到剪贴板", "Configuration copied"), Toast.LENGTH_SHORT).show()
                        showExportDialog = false
                    },
                ) {
                    Icon(imageVector = AppIcons.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(t("复制到剪贴板", "Copy to clipboard"))
                }
            },
            dismissButton = {
                TextButton(onClick = { showExportDialog = false }) {
                    Text(t("关闭", "Close"))
                }
            },
        )
    }

    MiuixScaffold(
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
                title = t("作息时间与校区楼宇", "Schedules, campuses and buildings"),
                actions = {
                    IconButton(onClick = { isCreatingScheme = true }) {
                        Icon(imageVector = AppIcons.Add, contentDescription = t("新建方案", "New schedule"))
                    }
                },
            )
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // ---- 方案概览与管理 ----
            item {
                ClassTimesCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Icon(
                            imageVector = AppIcons.Schedule,
                            contentDescription = null,
                            tint = AppTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp),
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = state.scheduleConfigName,
                                style = AppTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Text(
                                text = t(
                                    "包含 ${schemes.size} 套楼宇方案 · 基准校区：${state.campus.label}",
                                    "${schemes.size} building schedules · Base campus: ${state.campus.label}",
                                ),
                                style = AppTheme.typography.bodySmall,
                                color = AppTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }

                    Spacer(Modifier.height(4.dp))
                    HorizontalDivider(color = AppTheme.colorScheme.outlineVariant)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        OutlinedButton(
                            onClick = { showImportDialog = true },
                            modifier = Modifier.weight(1f),
                        ) {
                            Icon(imageVector = AppIcons.FileUpload, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(t("导入 JSON", "Import JSON"))
                        }
                        OutlinedButton(
                            onClick = { showExportDialog = true },
                            modifier = Modifier.weight(1f),
                        ) {
                            Icon(imageVector = AppIcons.FileDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(t("导出 JSON", "Export JSON"))
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        FilledTonalButton(
                            onClick = { isCreatingScheme = true },
                            modifier = Modifier.weight(1f),
                        ) {
                            Icon(imageVector = AppIcons.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(t("新建作息方案", "New schedule"))
                        }
                        FilledTonalButton(
                            onClick = {
                                onResetConfig()
                                Toast.makeText(context, t("已恢复标准作息", "Standard schedule restored"), Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.weight(1f),
                        ) {
                            Icon(imageVector = AppIcons.RestartAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(t("恢复标准作息", "Restore standard schedule"))
                        }
                    }
                }
            }

            // ---- 图形化节次起止时间与编辑入口 ----
            item {
                ClassTimesCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            text = t("图形化节次起止时间", "Period start and end times"),
                            style = AppTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                        TextButton(onClick = { editingScheme = activeScheme }) {
                            Icon(imageVector = AppIcons.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(t("编辑此方案", "Edit schedule"))
                        }
                    }

                    if (schemes.size > 1) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            schemes.forEach { scheme ->
                                FilterChip(
                                    selected = scheme.id == selectedSchemeId,
                                    onClick = { selectedSchemeId = scheme.id },
                                    label = { Text(scheme.fullLabel) },
                                )
                            }
                        }
                        HorizontalDivider(color = AppTheme.colorScheme.outlineVariant)
                    }

                    val periods = ClassTimes.periods(activeScheme)
                    periods.forEachIndexed { idx, clockTime ->
                        val slot = idx + 1
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(AppTheme.shapes.small)
                                .clickable { editingScheme = activeScheme }
                                .padding(vertical = 8.dp, horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = AppTheme.colorScheme.secondaryContainer,
                                modifier = Modifier.size(36.dp),
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = "$slot",
                                        style = AppTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = AppTheme.colorScheme.onSecondaryContainer,
                                    )
                                }
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = t("第 $slot 节", "Period $slot"),
                                    style = AppTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Medium,
                                )
                                Text(
                                    text = "${clockTime.start} — ${clockTime.end}",
                                    style = AppTheme.typography.bodySmall,
                                    color = AppTheme.colorScheme.onSurfaceVariant,
                                )
                            }

                            Icon(
                                imageVector = AppIcons.Edit,
                                contentDescription = t("修改时间", "Change time"),
                                tint = AppTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp),
                            )
                        }

                        if (slot == state.effectiveLunchBreakSlot && idx < periods.size - 1) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = AppTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
                                modifier = Modifier
                                    .padding(vertical = 4.dp)
                                    .fillMaxWidth(),
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                ) {
                                    Icon(
                                        imageVector = AppIcons.WbSunny,
                                        contentDescription = null,
                                        tint = AppTheme.colorScheme.primary,
                                        modifier = Modifier.size(14.dp),
                                    )
                                    Text(
                                        text = t("中午午休时段（第 $slot 节后开始）", "Lunch break (after period $slot)"),
                                        style = AppTheme.typography.labelSmall,
                                        color = AppTheme.colorScheme.onPrimaryContainer,
                                        fontWeight = FontWeight.Medium,
                                    )
                                }
                            }
                        }

                        if (idx < periods.size - 1) {
                            HorizontalDivider(
                                color = AppTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                            )
                        }
                    }
                }
            }

            // ---- 基准校区与默认作息方案 ----
            item {
                ClassTimesCard {
                    ClassTimesSettingRow(
                        icon = AppIcons.Apartment,
                        title = t("默认基准校区", "Default campus"),
                        subtitle = t(
                            "未匹配到特定教学楼时，按该校区默认时段核算",
                            "Classes in unmatched buildings use this campus's schedule",
                        ),
                    )
                    SingleChoiceSegmentedButtonRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                    ) {
                        Campus.entries.forEachIndexed { index, campus ->
                            SegmentedButton(
                                shape = SegmentedButtonDefaults.itemShape(index = index, count = Campus.entries.size),
                                onClick = { onSelectCampus(campus) },
                                selected = (campus == state.campus),
                                icon = { SegmentedButtonDefaults.Icon(active = (campus == state.campus)) },
                            ) {
                                Text(campus.label)
                            }
                        }
                    }

                    Spacer(Modifier.height(8.dp))
                    HorizontalDivider(color = AppTheme.colorScheme.outlineVariant)

                    ClassTimesSettingRow(
                        icon = AppIcons.Schedule,
                        title = t("未指定楼宇默认作息", "Default schedule for unmatched buildings"),
                        subtitle = "${state.timeTable.label} (${state.timeTable.campus})",
                        onClick = { showDefaultSchemeDialog = true },
                        trailing = {
                            Icon(
                                imageVector = AppIcons.KeyboardArrowRight,
                                contentDescription = null,
                                tint = AppTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                modifier = Modifier.size(18.dp),
                            )
                        },
                    )

                    Spacer(Modifier.height(8.dp))
                    HorizontalDivider(color = AppTheme.colorScheme.outlineVariant)

                    ClassTimesSettingRow(
                        icon = AppIcons.WbSunny,
                        title = t("中午午休开始时段", "Lunch break start"),
                        subtitle = t("第 ${state.effectiveLunchBreakSlot} 节后", "After period ${state.effectiveLunchBreakSlot}"),
                        onClick = { showLunchBreakDialog = true },
                        trailing = {
                            Icon(
                                imageVector = AppIcons.KeyboardArrowRight,
                                contentDescription = null,
                                tint = AppTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                modifier = Modifier.size(18.dp),
                            )
                        },
                    )
                }
            }

            // ---- 课表教学楼方案映射（不分校区显示所有楼宇） ----
            if (state.buildingKeys.isNotEmpty()) {
                item {
                    ClassTimesCard {
                        Text(
                            text = t("楼宇作息映射", "Building schedule mapping"),
                            style = AppTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = AppTheme.colorScheme.primary,
                        )
                        state.buildingKeys.forEachIndexed { index, key ->
                            val scheme = state.schemeForKey(key)
                            if (index > 0) {
                                HorizontalDivider(
                                    modifier = Modifier.padding(vertical = 2.dp),
                                    color = AppTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                                )
                            }
                            ClassTimesSettingRow(
                                icon = AppIcons.Domain,
                                title = key,
                                onClick = { activeBuildingKey = key },
                                trailing = {
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = AppTheme.colorScheme.secondaryContainer,
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(2.dp),
                                        ) {
                                            Text(
                                                text = scheme.label,
                                                style = AppTheme.typography.labelSmall.copy(fontSize = 11.sp),
                                                fontWeight = FontWeight.Medium,
                                                color = AppTheme.colorScheme.onSecondaryContainer,
                                            )
                                            Icon(
                                                imageVector = AppIcons.KeyboardArrowRight,
                                                contentDescription = null,
                                                modifier = Modifier.size(14.dp),
                                                tint = AppTheme.colorScheme.onSecondaryContainer,
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
}

/**
 * 专门的作息方案编辑页面：配置校区、方案名称，并在起止时间输入框旁提供可视化时间选择器。
 */
@Composable
fun SchemeEditScreen(
    initialScheme: CampusBuilding?,
    allCampuses: List<String>,
    onBack: () -> Unit,
    onSave: (id: String, label: String, campus: String, periods: List<ClockTime>, isDefault: Boolean) -> Unit,
    onDelete: ((String) -> Unit)? = null,
) {
    val context = LocalContext.current
    val isNew = (initialScheme == null)
    val schemeId = remember { initialScheme?.id ?: ClassTimes.generateNewSchemeId() }

    var label by remember { mutableStateOf(initialScheme?.label ?: "") }
    var campus by remember {
        mutableStateOf(initialScheme?.campus ?: (allCampuses.firstOrNull() ?: t("主校区", "Main Campus")))
    }

    val initialPeriods = remember(initialScheme) {
        val list = if (initialScheme != null) {
            ClassTimes.periods(initialScheme)
        } else {
            ClassTimes.DEFAULT_MAINLAND_PERIODS
        }
        list.map { it.start to it.end }
    }

    var periodTimes by remember {
        mutableStateOf(initialPeriods)
    }

    BackHandler(onBack = onBack)

    val timeRegex = Regex("^([01]?\\d|2[0-3]):[0-5]\\d$")

    val onPickTime: (slotIndex: Int, isStart: Boolean) -> Unit = { slotIndex, isStart ->
        val currentStr = if (isStart) periodTimes[slotIndex].first else periodTimes[slotIndex].second
        val parts = currentStr.split(":")
        val curH = parts.getOrNull(0)?.toIntOrNull() ?: 8
        val curM = parts.getOrNull(1)?.toIntOrNull() ?: 0
        TimePickerDialog(
            context,
            { _, hour, minute ->
                val formatted = String.format("%02d:%02d", hour, minute)
                val updated = periodTimes.toMutableList()
                val (s, e) = updated[slotIndex]
                updated[slotIndex] = if (isStart) formatted to e else s to formatted
                periodTimes = updated
            },
            curH,
            curM,
            true,
        ).show()
    }

    val saveAction = {
        val trimmedLabel = label.trim().ifBlank {
            if (isNew) t("新方案", "New schedule") else t("默认作息", "Default schedule")
        }
        val trimmedCampus = campus.trim().ifBlank { t("主校区", "Main Campus") }
        val anyInvalid = periodTimes.any { (s, e) -> !timeRegex.matches(s.trim()) || !timeRegex.matches(e.trim()) }
        if (anyInvalid) {
            Toast.makeText(
                context,
                t("请确保所有节次时间格式为 HH:mm（如 08:00）", "Use HH:mm for every period time (for example 08:00)"),
                Toast.LENGTH_SHORT,
            ).show()
        } else {
            val clockTimes = periodTimes.map { (s, e) -> ClockTime(s.trim(), e.trim()) }
            onSave(schemeId, trimmedLabel, trimmedCampus, clockTimes, isNew && ClassTimes.allBuildings.isEmpty())
            Toast.makeText(context, t("作息方案已保存", "Schedule saved"), Toast.LENGTH_SHORT).show()
            onBack()
        }
    }

    MiuixScaffold(
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
                title = if (isNew) t("新建作息方案", "New schedule") else t("编辑作息方案", "Edit schedule"),
                actions = {
                    TextButton(onClick = saveAction) {
                        Text(t("保存", "Save"), fontWeight = FontWeight.Bold)
                    }
                },
            )
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // 方案与校区卡片
            item {
                ClassTimesCard {
                    Text(
                        text = t("方案与校区基础设置", "Schedule and campus"),
                        style = AppTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    OutlinedTextField(
                        value = label,
                        onValueChange = { label = it },
                        label = { Text(t("方案 / 楼宇名称", "Schedule / building name")) },
                        placeholder = { Text(t("例如：第一教学楼、实验楼、图书馆", "e.g. Teaching Building 1, Lab, Library")) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = AppTheme.shapes.medium,
                    )
                    OutlinedTextField(
                        value = campus,
                        onValueChange = { campus = it },
                        label = { Text(t("所属校区", "Campus")) },
                        placeholder = { Text(t("例如：主校区、东校区、南校区", "e.g. Main Campus, East Campus, South Campus")) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = AppTheme.shapes.medium,
                    )
                    if (allCampuses.isNotEmpty()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            allCampuses.forEach { cName ->
                                FilterChip(
                                    selected = (campus == cName),
                                    onClick = { campus = cName },
                                    label = { Text(cName) },
                                )
                            }
                        }
                    }
                }
            }

            // 节次起止时间
            item {
                ClassTimesCard {
                    Text(
                        text = t("各节次起止时间", "Period start and end times"),
                        style = AppTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    periodTimes.forEachIndexed { idx, (start, end) ->
                        val slot = idx + 1
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = AppTheme.colorScheme.secondaryContainer,
                                    modifier = Modifier.size(28.dp),
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = "$slot",
                                            style = AppTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = AppTheme.colorScheme.onSecondaryContainer,
                                        )
                                    }
                                }
                                Text(
                                    text = t("第 $slot 节", "Period $slot"),
                                    style = AppTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium,
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                OutlinedTextField(
                                    value = start,
                                    onValueChange = { newS ->
                                        val updated = periodTimes.toMutableList()
                                        updated[idx] = newS to updated[idx].second
                                        periodTimes = updated
                                    },
                                    label = { Text(t("开始", "Start")) },
                                    singleLine = true,
                                    modifier = Modifier.weight(1f),
                                    trailingIcon = {
                                        IconButton(onClick = { onPickTime(idx, true) }) {
                                            Icon(
                                                imageVector = AppIcons.Schedule,
                                                contentDescription = t("选择第 $slot 节开始时间", "Select start time for period $slot"),
                                                tint = AppTheme.colorScheme.primary,
                                            )
                                        }
                                    },
                                )

                                OutlinedTextField(
                                    value = end,
                                    onValueChange = { newE ->
                                        val updated = periodTimes.toMutableList()
                                        updated[idx] = updated[idx].first to newE
                                        periodTimes = updated
                                    },
                                    label = { Text(t("结束", "End")) },
                                    singleLine = true,
                                    modifier = Modifier.weight(1f),
                                    trailingIcon = {
                                        IconButton(onClick = { onPickTime(idx, false) }) {
                                            Icon(
                                                imageVector = AppIcons.Schedule,
                                                contentDescription = t("选择第 $slot 节结束时间", "Select end time for period $slot"),
                                                tint = AppTheme.colorScheme.primary,
                                            )
                                        }
                                    },
                                )
                            }
                        }
                        if (idx < periodTimes.size - 1) {
                            HorizontalDivider(
                                modifier = Modifier.padding(vertical = 4.dp),
                                color = AppTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                            )
                        }
                    }
                }
            }

            // 操作按钮
            item {
                Button(
                    onClick = saveAction,
                    modifier = Modifier.fillMaxWidth(),
                    shape = AppTheme.shapes.medium,
                ) {
                    Text(t("保存作息方案", "Save schedule"))
                }
            }

            if (!isNew && onDelete != null && ClassTimes.allBuildings.size > 1) {
                item {
                    OutlinedButton(
                        onClick = {
                            onDelete(schemeId)
                            Toast.makeText(context, t("已删除作息方案", "Schedule deleted"), Toast.LENGTH_SHORT).show()
                            onBack()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = AppTheme.colorScheme.error),
                    ) {
                        Text(t("删除此作息方案", "Delete this schedule"))
                    }
                }
            }
        }
    }
}

@Composable
private fun ImportJsonDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    val clipboardManager = LocalClipboardManager.current
    var text by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = t("导入作息配置 JSON", "Import schedule JSON"),
                style = AppTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = t(
                        "粘贴包含 schemes 及 periods 的作息规范 JSON，系统将动态更新各校区时段与楼栋匹配规则：",
                        "Paste schedule JSON containing schemes and periods to update campus times and building matching:",
                    ),
                    style = AppTheme.typography.bodySmall,
                    color = AppTheme.colorScheme.onSurfaceVariant,
                )

                FilledTonalButton(
                    onClick = {
                        clipboardManager.getText()?.text?.let {
                            text = it
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(imageVector = AppIcons.ContentPaste, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(t("从剪贴板粘贴", "Paste from clipboard"))
                }

                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    label = { Text(t("JSON 内容", "JSON")) },
                    placeholder = { Text("{\\n  \"name\": \"Custom University Schedule\",\\n  \"schemes\": [...]\\n}") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    textStyle = AppTheme.typography.bodySmall,
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(text.trim()) },
                enabled = text.isNotBlank(),
            ) {
                Text(t("解析并应用", "Parse and apply"))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(t("取消", "Cancel"))
            }
        },
    )
}

@Composable
private fun ClassTimesCard(content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = AppTheme.colorScheme.surfaceContainerLow,
        ),
        shape = AppTheme.shapes.medium,
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            content()
        }
    }
}

@Composable
private fun ClassTimesSettingRow(
    icon: ImageVector? = null,
    title: String,
    subtitle: String? = null,
    onClick: (() -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
) {
    val rowModifier = Modifier
        .fillMaxWidth()
        .clip(AppTheme.shapes.small)
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
                tint = AppTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp),
            )
        }
        Column(Modifier.weight(1f)) {
            Text(
                text = title,
                style = AppTheme.typography.titleMedium.copy(
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                ),
            )
            if (!subtitle.isNullOrBlank()) {
                Text(
                    text = subtitle,
                    style = AppTheme.typography.bodySmall,
                    color = AppTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        trailing?.invoke()
    }
}
