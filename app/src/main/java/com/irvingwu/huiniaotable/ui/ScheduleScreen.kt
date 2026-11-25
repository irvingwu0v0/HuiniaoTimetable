package com.irvingwu.huiniaotable.ui

import com.irvingwu.huiniaotable.ui.icons.AppIcons

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateOffsetAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import com.irvingwu.huiniaotable.ui.compat.AlertDialog
import com.irvingwu.huiniaotable.ui.compat.AssistChip
import com.irvingwu.huiniaotable.ui.compat.AssistChipDefaults
import com.irvingwu.huiniaotable.ui.compat.Card
import com.irvingwu.huiniaotable.ui.compat.CardDefaults
import com.irvingwu.huiniaotable.ui.compat.FilledTonalButton
import com.irvingwu.huiniaotable.ui.compat.FilterChip
import com.irvingwu.huiniaotable.ui.compat.HorizontalDivider
import com.irvingwu.huiniaotable.ui.compat.Icon
import com.irvingwu.huiniaotable.ui.compat.IconButton
import com.irvingwu.huiniaotable.ui.compat.ListItem
import com.irvingwu.huiniaotable.ui.compat.LocalContentColor
import com.irvingwu.huiniaotable.ui.compat.ListItemDefaults
import com.irvingwu.huiniaotable.ui.compat.AppTheme
import com.irvingwu.huiniaotable.ui.compat.ModalBottomSheet
import com.irvingwu.huiniaotable.ui.compat.OutlinedCard
import com.irvingwu.huiniaotable.ui.compat.Surface
import com.irvingwu.huiniaotable.ui.compat.SuggestionChip
import com.irvingwu.huiniaotable.ui.compat.Text
import com.irvingwu.huiniaotable.ui.compat.TextButton
import com.irvingwu.huiniaotable.ui.compat.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.irvingwu.huiniaotable.data.AppState
import com.irvingwu.huiniaotable.data.CampusBuilding
import com.irvingwu.huiniaotable.data.ClassTimes
import com.irvingwu.huiniaotable.data.Course
import com.irvingwu.huiniaotable.data.HolidayDay
import com.irvingwu.huiniaotable.data.Schedule
import com.irvingwu.huiniaotable.data.SlotRow
import com.irvingwu.huiniaotable.data.WEEKDAY_NAMES
import com.irvingwu.huiniaotable.data.cleanClassLabel
import com.irvingwu.huiniaotable.data.TimetableRepository
import com.irvingwu.huiniaotable.data.weekdayShortName
import com.irvingwu.huiniaotable.t
import androidx.compose.runtime.rememberUpdatedState
import com.irvingwu.huiniaotable.ui.theme.CoursePalette
import com.irvingwu.huiniaotable.ui.theme.rememberCoursePalette
import kotlin.math.roundToInt
import java.time.LocalDate
import java.time.LocalTime

private val SlotLabelWidth = 46.dp
private val GridHeaderHeight = 46.dp
private val PagePadding = 12.dp

/**
 * 课表主视图：遵循 Miuix / HyperOS 设计规范。
 * 无需滚动、铺满全屏，整合了基准周设置、周次指示器、今日概览条、自适应作息时间与调休映射。
 */
@Composable
fun ScheduleContent(
    state: AppState,
    onRefresh: () -> Unit,
    onSelectWeek: (Int) -> Unit,
    onToday: () -> Unit,
    onSetTermStart: (LocalDate) -> Unit,
    onCourseClick: (Course) -> Unit,
    placementCourse: Course? = null,
    onPlaceCourse: (Course, Int, Int, Int, Int?, Int?) -> Unit = { _, _, _, _, _, _ -> },
    onCancelPlacement: () -> Unit = {},
) {
    val schedule = state.schedule
    if (schedule == null || schedule.courses.isEmpty()) {
        EmptySchedulePlaceholder(
            hasData = schedule != null,
            onImport = onRefresh,
        )
        return
    }

    var showTodaySheet by rememberSaveable { mutableStateOf(false) }

    Column(Modifier.fillMaxSize()) {
        if (state.needsTermStart) {
            TermStartCard(
                currentStart = state.termStart,
                onSetTermStart = onSetTermStart,
            )
        }

        WeekSelector(
            state = state,
            onSelectWeek = onSelectWeek,
            onToday = onToday,
        )

        TodayBar(
            state = state,
            schedule = schedule,
            onClick = { showTodaySheet = true },
        )

        // 调课引导条：选好周次后提示点击空白格放置
        if (placementCourse != null) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = AppTheme.colorScheme.primaryContainer,
                    contentColor = AppTheme.colorScheme.onPrimaryContainer,
                ),
                shape = AppTheme.shapes.medium,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = PagePadding, vertical = 3.dp),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Icon(
                        imageVector = AppIcons.EventRepeat,
                        contentDescription = null,
                        modifier = Modifier.size(22.dp),
                    )
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = t("调课中 · 第 ${state.selectedWeek} 周", "Moving · Week ${state.selectedWeek}"),
                            style = AppTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            text = t(
                                "点击任意空白格子，把《${placementCourse.name}》放到那里",
                                "Tap an empty slot to place ${placementCourse.name} there",
                            ),
                            style = AppTheme.typography.bodySmall,
                            color = AppTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    TextButton(onClick = onCancelPlacement) {
                        Text(t("取消", "Cancel"))
                    }
                }
            }
        }

        // 全局单实例课程色板，避免每个格子独立计算与重组
        val coursePalette = rememberCoursePalette()

        // 正在拖动课程方块时，屏蔽左右滑动切周
        val dragActive = remember { mutableStateOf(false) }
        var totalDragX by remember { mutableFloatStateOf(0f) }
        val density = LocalDensity.current
        val swipeThresholdPx = remember(density) { with(density) { 48.dp.toPx() } }

        val currentSelectedWeek by rememberUpdatedState(state.selectedWeek)
        val currentMaxWeek by rememberUpdatedState(state.maxWeek)
        val currentOnSelectWeek by rememberUpdatedState(onSelectWeek)
        val currentPlacementCourse by rememberUpdatedState(placementCourse)

        // 周次切换时应用平滑转场动画，支持在课表区域左右滑动切周
        AnimatedContent(
            targetState = state.selectedWeek,
            transitionSpec = {
                val slideSpec = tween<IntOffset>(durationMillis = 220)
                val fadeSpec = tween<Float>(durationMillis = 220)
                if (targetState > initialState) {
                    (slideInHorizontally(animationSpec = slideSpec) { width -> width / 3 } + fadeIn(animationSpec = fadeSpec))
                        .togetherWith(slideOutHorizontally(animationSpec = slideSpec) { width -> -width / 3 } + fadeOut(animationSpec = fadeSpec))
                } else {
                    (slideInHorizontally(animationSpec = slideSpec) { width -> -width / 3 } + fadeIn(animationSpec = fadeSpec))
                        .togetherWith(slideOutHorizontally(animationSpec = slideSpec) { width -> width / 3 } + fadeOut(animationSpec = fadeSpec))
                }
            },
            label = "TimetableGridTransition",
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = PagePadding)
                .pointerInput(Unit) {
                    detectHorizontalDragGestures(
                        onDragStart = { totalDragX = 0f },
                        onDragEnd = {
                            if (!dragActive.value && currentPlacementCourse == null) {
                                if (totalDragX < -swipeThresholdPx) {
                                    if (currentSelectedWeek < currentMaxWeek) {
                                        currentOnSelectWeek(currentSelectedWeek + 1)
                                    }
                                } else if (totalDragX > swipeThresholdPx) {
                                    if (currentSelectedWeek > 1) {
                                        currentOnSelectWeek(currentSelectedWeek - 1)
                                    }
                                }
                            }
                            totalDragX = 0f
                        },
                        onDragCancel = { totalDragX = 0f },
                        onHorizontalDrag = { _, dragAmount ->
                            if (!dragActive.value && currentPlacementCourse == null) totalDragX += dragAmount
                        },
                    )
                },
        ) { targetWeek ->
            TimetableGrid(
                state = state,
                schedule = schedule,
                week = targetWeek,
                palette = coursePalette,
                onCourseClick = onCourseClick,
                onMoveCourse = { course, week2, day, row, fromDay, fromRow ->
                    onPlaceCourse(course, week2, day, row, fromDay, fromRow)
                },
                placementCourse = placementCourse,
                onPlace = { course, day, row ->
                    onPlaceCourse(course, targetWeek, day, row, null, null)
                },
                dragActive = dragActive,
                modifier = Modifier.fillMaxSize(),
            )
        }

        Spacer(Modifier.height(4.dp))
    }

    if (showTodaySheet) {
        TodaySheet(
            state = state,
            schedule = schedule,
            onDismiss = { showTodaySheet = false },
        )
    }
}

/**
 * 教学周起始基准设置卡片（未配置开学日期时呈现）。
 */
@Composable
private fun TermStartCard(
    currentStart: LocalDate?,
    onSetTermStart: (LocalDate) -> Unit,
) {
    var showPicker by rememberSaveable { mutableStateOf(false) }

    if (showPicker) {
        PickDateDialog(
            title = t("选取开学首周周一日期", "Select the first Monday of term"),
            current = currentStart,
            onDismiss = { showPicker = false },
            onConfirm = {
                onSetTermStart(it)
                showPicker = false
            },
        )
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = PagePadding, vertical = 6.dp),
        colors = CardDefaults.cardColors(
            containerColor = AppTheme.colorScheme.primaryContainer,
        ),
        shape = AppTheme.shapes.medium,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Icon(
                imageVector = AppIcons.CalendarMonth,
                contentDescription = null,
                tint = AppTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(24.dp),
            )

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = t("配置学期开学首周基准日", "Set the term start date"),
                    style = AppTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = AppTheme.colorScheme.onPrimaryContainer,
                )
                Text(
                    text = t(
                        "系统将以首周周一自动核算各教学周次与调休安排",
                        "Teaching weeks and makeup days are calculated from the first Monday",
                    ),
                    style = AppTheme.typography.bodySmall,
                    color = AppTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f),
                )
            }

            FilledTonalButton(
                onClick = { showPicker = true },
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
            ) {
                Text(t("配置日期", "Set date"), style = AppTheme.typography.labelLarge)
            }
        }
    }
}

/**
 * 水平周次选择指示器：支持平滑自动滚动到当前选中的周次。
 */
@Composable
private fun WeekSelector(
    state: AppState,
    onSelectWeek: (Int) -> Unit,
    onToday: () -> Unit,
) {
    val listState = rememberLazyListState()

    // 当选中的周次变化时（无论是点击「当前周」、滑动切周还是点击周次胶囊），自动将指示栏平滑滚动到对应项
    LaunchedEffect(state.selectedWeek) {
        val targetIndex = (state.selectedWeek).coerceAtLeast(0)
        listState.animateScrollToItem(targetIndex)
    }

    LazyRow(
        state = listState,
        contentPadding = PaddingValues(horizontal = PagePadding),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.height(44.dp),
    ) {
        item {
            AssistChip(
                onClick = onToday,
                label = { Text(t("当前周", "Current week"), fontWeight = FontWeight.Medium) },
                leadingIcon = {
                    Icon(
                        imageVector = AppIcons.Today,
                        contentDescription = null,
                        modifier = Modifier.size(AssistChipDefaults.IconSize),
                    )
                },
                colors = AssistChipDefaults.assistChipColors(
                    containerColor = AppTheme.colorScheme.surfaceContainerHigh,
                ),
            )
        }

        val totalWeeks = state.lastTeachingWeek.coerceAtLeast(state.currentWeek)
        items(totalWeeks) { index ->
            val week = index + 1
            val isCurrent = (week == state.currentWeek)
            val isSelected = (week == state.selectedWeek)

            FilterChip(
                selected = isSelected,
                onClick = { onSelectWeek(week) },
                label = {
                    Text(
                        text = if (isCurrent) t("第 $week 周 (当前)", "Week $week (Current)") else t("第 $week 周", "Week $week"),
                        fontWeight = if (isSelected || isCurrent) FontWeight.SemiBold else FontWeight.Normal,
                    )
                },
            )
        }
    }
}

/**
 * 今日教学日程概览横幅条。
 */
@Composable
private fun TodayBar(
    state: AppState,
    schedule: Schedule,
    onClick: () -> Unit,
) {
    val today = LocalDate.now()
    val holiday = state.holidayOn(today)
    val makeupLabel = state.makeupLabelFor(today, state.currentWeek)
    val effectiveDay = state.displayDayOfWeek(today, state.currentWeek)
    val courses = schedule.coursesOnDay(effectiveDay, state.currentWeek)
    val next = nextCourse(courses, state)

    val containerColor = when {
        holiday?.isOffDay == true -> AppTheme.colorScheme.errorContainer
        makeupLabel != null -> AppTheme.colorScheme.tertiaryContainer
        else -> AppTheme.colorScheme.surfaceContainerHigh
    }
    val contentColor = when {
        holiday?.isOffDay == true -> AppTheme.colorScheme.onErrorContainer
        makeupLabel != null -> AppTheme.colorScheme.onTertiaryContainer
        else -> AppTheme.colorScheme.onSurface
    }

    Card(
        colors = CardDefaults.cardColors(
            containerColor = containerColor,
            contentColor = contentColor,
        ),
        shape = AppTheme.shapes.medium,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = PagePadding, vertical = 3.dp)
            .clickable(onClick = onClick),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Icon(
                imageVector = when {
                    holiday?.isOffDay == true -> AppIcons.HolidayVillage
                    makeupLabel != null -> AppIcons.WorkHistory
                    else -> AppIcons.EventNote
                },
                contentDescription = null,
                modifier = Modifier.size(22.dp),
            )

            Column(Modifier.weight(1f)) {
                Text(
                    text = when {
                        holiday?.isOffDay == true -> t("法定节假日 · ${holiday.name}", "Public holiday · ${holiday.name}")
                        makeupLabel != null -> t(
                            "调休补课日 · 执行 $makeupLabel 安排 (${courses.size} 门)",
                            "Makeup day · ${makeupLabel} timetable (${courses.size} classes)",
                        )
                        else -> t(
                            "今日安排 · ${WEEKDAY_NAMES[today.dayOfWeek.value - 1]} · ${courses.size} 门课程",
                            "Today · ${WEEKDAY_NAMES[today.dayOfWeek.value - 1]} · ${courses.size} classes",
                        )
                    },
                    style = AppTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )

                if (holiday?.isOffDay != true) {
                    Text(
                        text = next?.let {
                            t(
                                "下节课程 · ${state.timeTextFor(it)} ${it.name} (${it.room ?: "场地待定"})",
                                "Next class · ${state.timeTextFor(it)} ${it.name} (${it.room ?: "Room TBD"})",
                            )
                        } ?: if (courses.isEmpty()) {
                            t("全日无教学计划安排", "No classes scheduled today")
                        } else {
                            t("今日教学安排已全部结束", "All classes have finished for today")
                        },
                        style = AppTheme.typography.bodySmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = contentColor.copy(alpha = 0.85f),
                    )
                }
            }

            Icon(
                imageVector = AppIcons.ExpandMore,
                contentDescription = t("查看今日课程详情", "View today's schedule"),
                tint = contentColor.copy(alpha = 0.7f),
            )
        }
    }
}

private fun nextCourse(courses: List<Course>, state: AppState): Course? {
    val now = LocalTime.now()
    return courses.firstOrNull { course ->
        val end = state.timeTextFor(course).substringAfter('-', "")
        runCatching { LocalTime.parse(end) }.getOrNull()?.isAfter(now) == true
    }
}

/**
 * 课表核心网格视图：按屏幕自适应比例平铺，不启用滚动容器。
 */
@Composable
private fun TimetableGrid(
    state: AppState,
    schedule: Schedule,
    week: Int,
    palette: CoursePalette,
    onCourseClick: (Course) -> Unit,
    onMoveCourse: (Course, Int, Int, Int, Int, Int) -> Unit,
    placementCourse: Course?,
    onPlace: (Course, Int, Int) -> Unit,
    dragActive: MutableState<Boolean>,
    modifier: Modifier = Modifier,
) {
    val columns = state.visibleDays(week)
    val today = LocalDate.now()
    val effectiveDays = columns.map { day ->
        val date = dateOf(day, week, state)
        date?.let { state.displayDayOfWeek(it, week) } ?: day
    }
    val effectiveByDay = columns.zip(effectiveDays).toMap()

    // 大节时段课程网格行（可隐藏晚自习第 9-10 节）
    val visibleRows = remember(schedule.slotRows, state.hideEvening) {
        if (state.hideEvening && schedule.slotRows.size > 1) {
            schedule.slotRows.dropLast(1)
        } else {
            schedule.slotRows
        }
    }
    // 每一行「是否已被课程占用」：跨大节的课会同时占用相邻两行，避免出现可放置的假空格。
    val occupiedRows: Set<Pair<Int, Int>> = remember(schedule, state.coursePlacements, week, visibleRows) {
        val occupied = HashSet<Pair<Int, Int>>()
        schedule.courses.forEach { course ->
            state.positionsOf(course, week).forEach { (day, _) ->
                visibleRows.forEachIndexed { idx, slotRow ->
                    if ((course.startSlot..course.endSlot).any { it in slotRow.startSlot..slotRow.endSlot }) {
                        occupied.add(day to idx)
                    }
                }
            }
        }
        occupied
    }
    // 中午午休分割条所在的行索引（根据设置的午休开始节次动态确定，默认是相差最大的两节之后）
    val noonDividerRowIndex = remember(visibleRows, state.effectiveLunchBreakSlot) {
        val targetSlot = state.effectiveLunchBreakSlot
        visibleRows.indexOfLast { it.startSlot <= targetSlot }
            .takeIf { it in 0 until (visibleRows.size - 1) } ?: -1
    }
    val rowSchemes = remember(schedule, week, effectiveDays, state.timeTable) {
        visibleRows.indices.map { rowIndex ->
            rowSchemeFor(state, schedule, rowIndex, week, effectiveDays)
        }
    }

    fun keyOf(day: Int, row: Int): Int = day * 100 + row

    fun isOffDayOf(day: Int): Boolean =
        dateOf(day, week, state)?.let { state.isOffDay(it) } == true

    // 各格子在根坐标下的矩形；用状态 Map 让课程方块层在首次布局后自动出现
    val cellBounds = remember { mutableStateMapOf<Int, Rect>() }
    val tilePositions = remember { HashMap<String, Offset>() }
    var gridOrigin by remember { mutableStateOf(Offset.Zero) }

    var dragging by remember { mutableStateOf<Course?>(null) }
    var dragSourceCell by remember { mutableStateOf<Pair<Int, Int>?>(null) }
    var hoverCell by remember { mutableStateOf<Pair<Int, Int>?>(null) }

    fun validTarget(cell: Pair<Int, Int>?): Boolean {
        if (cell == null) return false
        val (day, row) = cell
        if (isOffDayOf(day)) return false
        val eff = effectiveByDay[day] ?: day
        return occupiedRows.none { it.first == eff && it.second == row }
    }

    fun cellAt(point: Offset): Pair<Int, Int>? {
        cellBounds.forEach { (packed, rect) ->
            if (rect.contains(point)) return packed / 100 to packed % 100
        }
        return null
    }

    // 列（星期几在屏幕上的那一列）→ 该列实际按星期几的课显示（调休补课时二者不同）
    fun columnFor(effDay: Int): Int? =
        columns.firstOrNull { (effectiveByDay[it] ?: it) == effDay }

    fun rectOfColumn(columnDay: Int, row: Int): Rect? = cellBounds[keyOf(columnDay, row)]

    fun tileDragModifier(course: Course, effDay: Int, rowIndex: Int, tileKey: String): Modifier =
        Modifier
            .onGloballyPositioned { tilePositions[tileKey] = it.positionInRoot() }
            .pointerInput(tileKey, week) {
                var dragPointer = Offset.Zero
                detectDragGesturesAfterLongPress(
                    onDragStart = { local ->
                        dragging = course
                        dragSourceCell = effDay to rowIndex
                        dragActive.value = true
                        dragPointer = (tilePositions[tileKey] ?: Offset.Zero) + local
                        val newHover = cellAt(dragPointer).takeIf { validTarget(it) }
                        if (hoverCell != newHover) {
                            hoverCell = newHover
                        }
                    },
                    onDrag = { change, delta ->
                        change.consume()
                        dragPointer += delta
                        val newHover = cellAt(dragPointer).takeIf { validTarget(it) }
                        if (hoverCell != newHover) {
                            hoverCell = newHover
                        }
                    },
                    onDragEnd = {
                        val target = hoverCell
                        if (target != null && validTarget(target)) {
                            val targetEffDay = effectiveByDay[target.first] ?: target.first
                            val sourceCell = dragSourceCell ?: (effDay to rowIndex)
                            if (targetEffDay to target.second != sourceCell) {
                                onMoveCourse(course, week, targetEffDay, target.second, sourceCell.first, sourceCell.second)
                            }
                        }
                        dragging = null
                        dragSourceCell = null
                        hoverCell = null
                        dragActive.value = false
                    },
                    onDragCancel = {
                        dragging = null
                        dragSourceCell = null
                        hoverCell = null
                        dragActive.value = false
                    },
                )
            }

    Box(modifier.onGloballyPositioned { gridOrigin = it.positionInRoot() }) {
        Column(Modifier.fillMaxSize()) {
            // 星期与日期表头行
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(GridHeaderHeight),
            ) {
                Spacer(Modifier.width(SlotLabelWidth))
                columns.forEach { day ->
                    val date = dateOf(day, week, state)
                    DayHeader(
                        day = day,
                        date = date,
                        holiday = date?.let { state.holidayOn(it) },
                        makeupLabel = date?.let { state.makeupLabelFor(it, week) },
                        isToday = (date == today),
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            visibleRows.forEachIndexed { rowIndex, row ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight((row.endSlot - row.startSlot + 1).coerceAtLeast(1).toFloat()),
                ) {
                    SlotLabel(
                        row = row,
                        timeTable = rowSchemes.getOrElse(rowIndex) { state.timeTable },
                    )
                    columns.forEachIndexed { index, day ->
                        val eff = effectiveDays[index]
                        val offDay = isOffDayOf(day)
                        val isDropTarget = dragging != null && !offDay && occupiedRows.none { it.first == eff && it.second == rowIndex }
                        val isHovered = hoverCell == (day to rowIndex)
                        val isPlacementTarget = placementCourse != null && !offDay && occupiedRows.none { it.first == eff && it.second == rowIndex }
                        val packed = keyOf(day, rowIndex)

                        CourseCell(
                            isOffDay = offDay,
                            dropTarget = isDropTarget,
                            hovered = isHovered,
                            placementTarget = isPlacementTarget,
                            onPlaceTap = if (isPlacementTarget) {
                                {
                                    val targetEffDay = effectiveByDay[day] ?: day
                                    onPlace(placementCourse!!, targetEffDay, rowIndex)
                                }
                            } else null,
                            onBounds = { rect -> cellBounds[packed] = rect },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }

                // 在上午时段结束（中午午休）下方插入分割方块，区分上午与下午
                if (rowIndex == noonDividerRowIndex) {
                    NoonDividerBar()
                }
            }
        }

        // 课程方块层：按「节」精确定位，跨大节的三节课也只画一个连续方块。
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clipToBounds(),
        ) {
            for ((index, day) in columns.withIndex()) {
                if (isOffDayOf(day)) continue
                val eff = effectiveDays[index]
                for (course in schedule.courses) {
                    if (!course.activeIn(week)) continue
                    for ((_, rowIndex) in state.positionsOf(course, week).filter { it.first == eff }) {
                        val row = visibleRows.getOrNull(rowIndex) ?: continue
                        val cellRect = cellBounds[keyOf(day, rowIndex)] ?: continue
                        val rect = courseTileRect(course, row, cellRect, gridOrigin)
                        val tileKey = "${course.key}#$day#$rowIndex"
                        CourseTile(
                            course = course,
                            dimmed = false,
                            lifted = dragging?.key == course.key,
                            palette = palette,
                            onCourseClick = onCourseClick,
                            modifier = tileDragModifier(course, eff, rowIndex, tileKey)
                                .offset { IntOffset(rect.left.roundToInt(), rect.top.roundToInt()) }
                                .size(
                                    width = with(LocalDensity.current) { rect.width.toDp() },
                                    height = with(LocalDensity.current) { rect.height.toDp() },
                                ),
                        )
                    }
                }
            }
        }

        // 拖拽悬浮块：跟随落点动画移动
        val sourceRect = dragging?.let { course ->
            val (d, r) = dragSourceCell ?: state.positionOf(course, week)
            columnFor(d)?.let { column -> rectOfColumn(column, r) }
        }
        val targetRect = hoverCell?.let { (column, r) -> rectOfColumn(column, r) } ?: sourceRect
        var lastOverlayOffset by remember { mutableStateOf(Offset.Zero) }
        val currentTargetOffset = (targetRect?.topLeft)?.let { it - gridOrigin }
        if (currentTargetOffset != null) {
            lastOverlayOffset = currentTargetOffset
        }

        val overlayTopLeft by animateOffsetAsState(
            targetValue = lastOverlayOffset,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioLowBouncy,
                stiffness = Spring.StiffnessMediumLow,
            ),
            label = "courseDragOffset",
        )
        val visible = dragging != null && targetRect != null
        AnimatedVisibility(
            visible = visible,
            enter = fadeIn() + scaleIn(initialScale = 0.8f),
            exit = fadeOut() + scaleOut(targetScale = 0.8f),
            modifier = Modifier.offset {
                IntOffset(overlayTopLeft.x.roundToInt(), overlayTopLeft.y.roundToInt())
            },
        ) {
            dragging?.let { course ->
                val density = LocalDensity.current
                DragOverlay(
                    course = course,
                    palette = palette,
                    width = with(density) { (targetRect?.width ?: 0f).toDp() },
                    height = with(density) { (targetRect?.height ?: 0f).toDp() },
                )
            }
        }
    }
}

/**
 * 上午与下午之间的分割方块（位于第 3-4 节下方，仅在课表区域，不覆盖左侧时间列；约 32dp 浅灰色，无文字）。
 */
@Composable
private fun NoonDividerBar() {
    val isDark = isSystemInDarkTheme()
    // 柔和浅灰色
    val bgColor = if (isDark) Color(0xFF2E3330) else Color(0xFFD6DAD4)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(32.dp)
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // 左侧时间轴宽度留空，不贯穿到时间栏
        Spacer(Modifier.width(SlotLabelWidth))
        // 课表主体网格区域的浅灰分割方块，无文字
        Surface(
            color = bgColor,
            shape = AppTheme.shapes.small,
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .padding(horizontal = 1.dp),
        ) {}
    }
}

private fun dateOf(day: Int, week: Int, state: AppState): LocalDate? {
    val start = state.termStart ?: return null
    return mondayOf(start)
        .plusWeeks((week - 1).toLong())
        .plusDays((day - 1).toLong())
}

private fun rowSchemeFor(
    state: AppState,
    schedule: Schedule,
    rowIndex: Int,
    week: Int,
    effectiveDays: List<Int>,
): CampusBuilding {
    val schemes = effectiveDays.flatMap { day ->
        schedule.coursesAt(day, rowIndex, week).map { state.schemeFor(it) }
    }
    if (schemes.isEmpty()) return state.timeTable
    return schemes.groupingBy { it }.eachCount().maxByOrNull { it.value }?.key ?: state.timeTable
}

private fun mondayOf(date: LocalDate): LocalDate =
    date.minusDays((date.dayOfWeek.value - 1).toLong())

/**
 * 星期表头单元格：呈现星期、公历日期、休假标签、调休补课标签与今日高亮。
 */
@Composable
private fun DayHeader(
    day: Int,
    date: LocalDate?,
    holiday: HolidayDay?,
    makeupLabel: String?,
    isToday: Boolean,
    modifier: Modifier = Modifier,
) {
    val isOffDay = (holiday?.isOffDay == true)
    val isWorkday = (holiday != null && !holiday.isOffDay)

    val containerColor = when {
        isToday -> AppTheme.colorScheme.primary
        isOffDay -> AppTheme.colorScheme.surfaceContainerHighest
        isWorkday -> AppTheme.colorScheme.tertiaryContainer
        else -> AppTheme.colorScheme.surfaceContainerLow
    }
    val contentColor = when {
        isToday -> AppTheme.colorScheme.onPrimary
        isOffDay -> AppTheme.colorScheme.onSurfaceVariant
        isWorkday -> AppTheme.colorScheme.onTertiaryContainer
        else -> AppTheme.colorScheme.onSurface
    }

    Box(
        modifier = modifier
            .fillMaxHeight()
            .padding(horizontal = 1.dp, vertical = 2.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .clip(AppTheme.shapes.small)
                .background(containerColor),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = weekdayShortName(day),
                    style = AppTheme.typography.labelMedium,
                    fontWeight = if (isToday) FontWeight.Bold else FontWeight.SemiBold,
                    color = contentColor,
                )
                if (isOffDay) {
                    Box(
                        modifier = Modifier
                            .size(15.dp)
                            .background(AppTheme.colorScheme.error, CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = t("休", "Off"),
                            style = AppTheme.typography.labelSmall.copy(
                                fontSize = 8.sp,
                                lineHeight = 8.sp,
                                fontWeight = FontWeight.Bold,
                            ),
                            color = AppTheme.colorScheme.onError,
                            textAlign = TextAlign.Center,
                        )
                    }
                } else if (isWorkday) {
                    Box(
                        modifier = Modifier
                            .size(15.dp)
                            .background(AppTheme.colorScheme.tertiary, CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = t("班", "Class"),
                            style = AppTheme.typography.labelSmall.copy(
                                fontSize = 8.sp,
                                lineHeight = 8.sp,
                                fontWeight = FontWeight.Bold,
                            ),
                            color = AppTheme.colorScheme.onTertiary,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }

            Text(
                text = date?.let { "${it.monthValue}/${it.dayOfMonth}" }.orEmpty(),
                style = AppTheme.typography.labelSmall.copy(
                    fontSize = 10.sp,
                ),
                color = contentColor.copy(alpha = if (isToday) 0.95f else 0.75f),
            )

            makeupLabel?.let { label ->
                Text(
                    text = label,
                    style = AppTheme.typography.labelSmall.copy(fontSize = 8.sp),
                    fontWeight = FontWeight.Bold,
                    color = contentColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

/**
 * 节次时间轴标签：呈现大节标识与对应作息时段。
 */
@Composable
private fun SlotLabel(
    row: SlotRow,
    timeTable: CampusBuilding,
) {
    val times = ClassTimes.rowsFor(timeTable, row)
    val start = times.firstOrNull()?.start
    val end = times.lastOrNull()?.end

    Column(
        modifier = Modifier
            .width(SlotLabelWidth)
            .fillMaxHeight()
            .padding(vertical = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Surface(
            shape = RoundedCornerShape(4.dp),
            color = AppTheme.colorScheme.surfaceContainerHighest,
        ) {
            Text(
                text = "${row.startSlot}-${row.endSlot}",
                style = AppTheme.typography.labelSmall.copy(
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                ),
                color = AppTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
            )
        }

        Spacer(Modifier.height(2.dp))

        start?.let {
            Text(
                text = it,
                style = AppTheme.typography.labelSmall.copy(
                    fontSize = 9.sp,
                ),
                color = AppTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
        end?.let {
            Text(
                text = it,
                style = AppTheme.typography.labelSmall.copy(
                    fontSize = 9.sp,
                ),
                color = AppTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                textAlign = TextAlign.Center,
            )
        }
    }
}

/**
 * 课程格子容器：只负责背景、高亮与点击放置；课程方块由上层统一定位绘制。
 */
@Composable
private fun CourseCell(
    isOffDay: Boolean,
    dropTarget: Boolean,
    hovered: Boolean,
    placementTarget: Boolean = false,
    onPlaceTap: (() -> Unit)? = null,
    onBounds: (Rect) -> Unit,
    modifier: Modifier = Modifier,
) {
    val cellColor = if (isOffDay) {
        AppTheme.colorScheme.surfaceContainerHigh
    } else {
        AppTheme.colorScheme.surfaceContainerLowest
    }

    val targetColor = AppTheme.colorScheme.primary
    val highlight = when {
        hovered -> targetColor.copy(alpha = 0.30f)
        dropTarget -> targetColor.copy(alpha = 0.12f)
        placementTarget -> targetColor.copy(alpha = 0.14f)
        else -> Color.Transparent
    }
    val borderColor = when {
        hovered -> targetColor
        dropTarget -> targetColor.copy(alpha = 0.55f)
        placementTarget -> targetColor.copy(alpha = 0.60f)
        else -> Color.Transparent
    }
    val highlighted = dropTarget || hovered || placementTarget

    Surface(
        modifier = modifier
            .fillMaxHeight()
            .padding(1.dp)
            .onGloballyPositioned { onBounds(it.boundsInRoot()) }
            .then(
                if (highlighted) {
                    Modifier.border(
                        width = 1.5.dp,
                        color = borderColor,
                        shape = AppTheme.shapes.small,
                    )
                } else {
                    Modifier
                },
            )
            .then(
                if (onPlaceTap != null) {
                    Modifier.clickable(onClick = onPlaceTap)
                } else {
                    Modifier
                },
            ),
        shape = AppTheme.shapes.small,
        color = cellColor,
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .then(
                    if (highlighted) {
                        Modifier.background(highlight)
                    } else {
                        Modifier
                    }
                ),
        )
    }
}

/** 课程在网格里的绝对矩形：按「节」比例从单元格内截取，支持跨大节连续绘制。 */
private fun courseTileRect(course: Course, row: SlotRow, cellRect: Rect, gridOrigin: Offset): Rect {
    val span = (row.endSlot - row.startSlot + 1).coerceAtLeast(1)
    val startOffset = (course.startSlot - row.startSlot).coerceIn(0, span - 1)
    val endOffset = (course.endSlot - row.startSlot).coerceIn(startOffset, span - 1)
    val top = cellRect.top - gridOrigin.y + cellRect.height * (startOffset.toFloat() / span)
    val left = cellRect.left - gridOrigin.x
    val bottom = top + cellRect.height * ((endOffset - startOffset + 1).toFloat() / span)
    return Rect(left, top, left + cellRect.width, bottom)
}

/**
 * 课程方块：包含课程名称、地点图标及教室、人物图标及授课教师。
 */
@Composable
private fun CourseTile(
    course: Course,
    dimmed: Boolean,
    lifted: Boolean = false,
    palette: CoursePalette,
    onCourseClick: (Course) -> Unit,
    modifier: Modifier = Modifier,
) {
    // 同一门课（按课程名）始终命中同一组同一色相的色块。
    val colors = palette.forKey(course.name)

    val tileBg = if (dimmed) {
        AppTheme.colorScheme.surfaceContainerHighest
    } else {
        colors.container
    }
    val tileContent = if (dimmed) {
        AppTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f)
    } else {
        colors.onContainer
    }
    val accent = if (dimmed) {
        AppTheme.colorScheme.outlineVariant
    } else {
        colors.accent
    }

    Surface(
        modifier = modifier
            .then(
                if (lifted) {
                    Modifier.graphicsLayer {
                        scaleX = 0.9f
                        scaleY = 0.9f
                        alpha = 0.35f
                    }
                } else {
                    Modifier
                },
            )
            .clickable { onCourseClick(course) },
        shape = AppTheme.shapes.small,
        color = tileBg,
        contentColor = tileContent,
        shadowElevation = 0.dp,
    ) {
        BoxWithConstraints {
            // 单节课只有半格高度，空间不够时只显示课程名。
            val compact = maxHeight < 56.dp
            Column(
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                verticalArrangement = Arrangement.spacedBy(1.dp),
            ) {
                Text(
                    text = course.name,
                    style = AppTheme.typography.labelSmall.copy(lineHeight = 12.sp),
                    fontWeight = FontWeight.SemiBold,
                    maxLines = if (compact) 2 else 3,
                    overflow = TextOverflow.Ellipsis,
                )
                if (!compact) {
                    course.room?.let { room ->
                        TileInfoRow(
                            icon = AppIcons.LocationOn,
                            text = room,
                            tint = accent,
                        )
                    }
                    course.teacher?.let { teacher ->
                        TileInfoRow(
                            icon = AppIcons.Person,
                            text = teacher,
                            tint = accent,
                        )
                    }
                }
            }
        }
    }
}

/**
 * 拖拽跟随的悬浮课程块：浮起阴影 + 轻微放大，视觉上「离开」网格。
 */
@Composable
private fun DragOverlay(
    course: Course,
    palette: CoursePalette,
    width: Dp,
    height: Dp,
) {
    val colors = palette.forKey(course.name)
    Surface(
        modifier = Modifier
            .size(width = width, height = height)
            .graphicsLayer {
                scaleX = 1.04f
                scaleY = 1.04f
            },
        shape = AppTheme.shapes.small,
        color = colors.container,
        contentColor = colors.onContainer,
        shadowElevation = 14.dp,
        tonalElevation = 3.dp,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
            verticalArrangement = Arrangement.spacedBy(1.dp),
        ) {
            Text(
                text = course.name,
                style = AppTheme.typography.labelSmall.copy(lineHeight = 12.sp),
                fontWeight = FontWeight.SemiBold,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
            course.room?.let { room ->
                TileInfoRow(icon = AppIcons.LocationOn, text = room, tint = colors.accent)
            }
            course.teacher?.let { teacher ->
                TileInfoRow(icon = AppIcons.Person, text = teacher, tint = colors.accent)
            }
        }
    }
}

/**
 * 课程方块内元信息行（地点与教师标签）。
 */
@Composable
private fun TileInfoRow(
    icon: ImageVector,
    text: String,
    tint: Color = Color.Unspecified,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(1.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (tint == Color.Unspecified) LocalContentColor.current else tint,
            modifier = Modifier.size(10.dp),
        )
        Text(
            text = text,
            style = AppTheme.typography.labelSmall.copy(fontSize = 9.sp),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/**
 * 今日课程详情底部抽屉。
 */
@Composable
private fun TodaySheet(
    state: AppState,
    schedule: Schedule,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val today = LocalDate.now()
    val holiday = state.holidayOn(today)
    val makeupLabel = state.makeupLabelFor(today, state.currentWeek)
    val effectiveDay = state.displayDayOfWeek(today, state.currentWeek)
    val courses = schedule.coursesOnDay(effectiveDay, state.currentWeek)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = AppTheme.colorScheme.surfaceContainerLow,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Icon(
                    imageVector = AppIcons.EventNote,
                    contentDescription = null,
                    tint = AppTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp),
                )
                Text(
                    text = t(
                        "今日教学日程 · ${WEEKDAY_NAMES[today.dayOfWeek.value - 1]}",
                        "Today's classes · ${WEEKDAY_NAMES[today.dayOfWeek.value - 1]}",
                    ),
                    style = AppTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
            }

            if (holiday != null) {
                Surface(
                    shape = AppTheme.shapes.small,
                    color = if (holiday.isOffDay) AppTheme.colorScheme.errorContainer else AppTheme.colorScheme.tertiaryContainer,
                    contentColor = if (holiday.isOffDay) AppTheme.colorScheme.onErrorContainer else AppTheme.colorScheme.onTertiaryContainer,
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Icon(
                            imageVector = if (holiday.isOffDay) AppIcons.HolidayVillage else AppIcons.WorkHistory,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                        )
                        Text(
                            text = when {
                                holiday.isOffDay -> t("法定休假：${holiday.name}", "Public holiday: ${holiday.name}")
                                makeupLabel != null -> t(
                                    "调休补课安排：执行 $makeupLabel 课表",
                                    "Makeup schedule: $makeupLabel timetable",
                                )
                                else -> t("调休上班日：${holiday.name}", "Makeup workday: ${holiday.name}")
                            },
                            style = AppTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
            }

            HorizontalDivider(color = AppTheme.colorScheme.outlineVariant)

            if (courses.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = t("今日无任何教学计划安排", "No classes scheduled today"),
                        style = AppTheme.typography.bodyMedium,
                        color = AppTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                courses.forEach { course ->
                    OutlinedCard(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.outlinedCardColors(
                            containerColor = AppTheme.colorScheme.surface,
                        ),
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = AppTheme.colorScheme.primaryContainer,
                                ) {
                                    Text(
                                        text = state.timeTextFor(course),
                                        style = AppTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = AppTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    )
                                }
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = course.name,
                                    style = AppTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f),
                                )
                            }

                            Row(
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                course.room?.let { r ->
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                                    ) {
                                        Icon(
                                            imageVector = AppIcons.LocationOn,
                                            contentDescription = null,
                                            modifier = Modifier.size(14.dp),
                                            tint = AppTheme.colorScheme.onSurfaceVariant,
                                        )
                                        Text(
                                            text = r,
                                            style = AppTheme.typography.bodySmall,
                                            color = AppTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                }

                                course.teacher?.let { t ->
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                                    ) {
                                        Icon(
                                            imageVector = AppIcons.Person,
                                            contentDescription = null,
                                            modifier = Modifier.size(14.dp),
                                            tint = AppTheme.colorScheme.onSurfaceVariant,
                                        )
                                        Text(
                                            text = t,
                                            style = AppTheme.typography.bodySmall,
                                            color = AppTheme.colorScheme.onSurfaceVariant,
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
}

/**
 * 课程详情底部抽屉。
 */
@Composable
fun CourseDetailSheet(
    course: Course?,
    state: AppState,
    onDismiss: () -> Unit,
    onResetPosition: (Course) -> Unit = {},
    onStartPlacement: (Course, Int) -> Unit = { _, _ -> },
) {
    if (course == null) return
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scheme = state.schemeFor(course)
    val buildingKey = ClassTimes.buildingKeyOf(course.campus, course.room)
    var showWeekJump by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = AppTheme.colorScheme.surfaceContainerLow,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = course.name,
                        style = AppTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f),
                    )
                    // 调课：选择目标教学周并跳转
                    IconButton(onClick = { showWeekJump = true }) {
                        Icon(
                            imageVector = AppIcons.EventRepeat,
                            contentDescription = t("调课 · 跳到指定教学周", "Move class · jump to a week"),
                            tint = AppTheme.colorScheme.primary,
                        )
                    }
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    course.classLabel?.cleanClassLabel()?.let { label ->
                        SuggestionChip(
                            onClick = {},
                            label = { Text(label, style = AppTheme.typography.labelSmall) },
                            icon = {
                                Icon(
                                    imageVector = AppIcons.Groups,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp),
                                )
                            },
                        )
                    }
                    buildingKey?.let { b ->
                        SuggestionChip(
                            onClick = {},
                            label = { Text(b, style = AppTheme.typography.labelSmall) },
                            icon = {
                                Icon(
                                    imageVector = AppIcons.Apartment,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp),
                                )
                            },
                        )
                    }
                }
            }

            HorizontalDivider(color = AppTheme.colorScheme.outlineVariant)

            DetailListItem(
                icon = AppIcons.Schedule,
                headline = t("教学时段", "Time"),
                supporting = "${course.dayText} ${course.slotText} (${ClassTimes.timeTextFor(course.startSlot, course.endSlot, scheme).ifBlank { t("作息待定", "Time TBD") }})",
            )

            DetailListItem(
                icon = AppIcons.MeetingRoom,
                headline = t("教学场地", "Location"),
                supporting = course.room ?: t("未指定具体教室", "No room specified"),
            )

            DetailListItem(
                icon = AppIcons.Person,
                headline = t("主讲教师", "Instructor"),
                supporting = course.teacher ?: t("教师信息未录入", "Instructor not provided"),
            )

            DetailListItem(
                icon = AppIcons.DateRange,
                headline = t("教学周次", "Teaching weeks"),
                supporting = course.weekText,
            )

            DetailListItem(
                icon = AppIcons.Apartment,
                headline = t("所属作息方案", "Schedule"),
                supporting = scheme.fullLabel,
            )

            if (state.isCourseMoved(course)) {
                HorizontalDivider(color = AppTheme.colorScheme.outlineVariant)
                val removed = state.removedWeeksOf(course)
                val targets = state.placementWeeksOf(course)
                val separator = t("、", ", ")
                val summary = when {
                    removed.isNotEmpty() && targets.isNotEmpty() ->
                        t(
                            "第 ${removed.joinToString(separator)} 周的原课已调至第 ${targets.joinToString(separator)} 周",
                            "Moved the class from week ${removed.joinToString(separator)} to week ${targets.joinToString(separator)}",
                        )
                    removed.isNotEmpty() ->
                        t(
                            "第 ${removed.joinToString(separator)} 周的原课已调走",
                            "Moved the class out of week ${removed.joinToString(separator)}",
                        )
                    else ->
                        t(
                            "第 ${targets.joinToString(separator)} 周的位置已调整",
                            "Changed the position in week ${targets.joinToString(separator)}",
                        )
                }
                Text(
                    text = t("$summary。", "$summary."),
                    style = AppTheme.typography.bodySmall,
                    color = AppTheme.colorScheme.onSurfaceVariant,
                )
                TextButton(
                    onClick = {
                        onResetPosition(course)
                        onDismiss()
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(
                        imageVector = AppIcons.RestartAlt,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(t("恢复默认位置", "Reset position"))
                }
            }
        }
    }

    if (showWeekJump) {
        WeekJumpDialog(
            currentWeek = state.selectedWeek,
            maxWeek = state.lastTeachingWeek,
            onSelect = { week ->
                showWeekJump = false
                onStartPlacement(course, week)
            },
            onDismiss = { showWeekJump = false },
        )
    }
}

/**
 * 「调课」周次选择对话框：以课表网格样式列出全部教学周，选择后跳到该周查看/调整。
 */
@Composable
private fun WeekJumpDialog(
    currentWeek: Int,
    maxWeek: Int,
    onSelect: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    val weeks = (1..maxWeek.coerceAtLeast(1)).toList()
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = AppIcons.EventRepeat,
                contentDescription = null,
                tint = AppTheme.colorScheme.primary,
            )
        },
        title = {
            Text(
                text = t("调课 · 选择目标周次", "Move class · choose a week"),
                style = AppTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 360.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                weeks.chunked(4).forEach { chunk ->
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        chunk.forEach { week ->
                            val selected = week == currentWeek
                            Surface(
                                onClick = { onSelect(week) },
                                shape = AppTheme.shapes.small,
                                color = if (selected) {
                                    AppTheme.colorScheme.primary
                                } else {
                                    AppTheme.colorScheme.surfaceContainerHighest
                                },
                                contentColor = if (selected) {
                                    AppTheme.colorScheme.onPrimary
                                } else {
                                    AppTheme.colorScheme.onSurfaceVariant
                                },
                                modifier = Modifier.weight(1f),
                            ) {
                                Text(
                                    text = t("第 $week 周", "Week $week"),
                                    style = AppTheme.typography.labelMedium,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 10.dp),
                                )
                            }
                        }
                        repeat(4 - chunk.size) { Spacer(Modifier.weight(1f)) }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
            ) { Text(t("取消", "Cancel")) }
        },
        dismissButton = null,
    )
}

@Composable
private fun DetailListItem(
    icon: ImageVector,
    headline: String,
    supporting: String,
) {
    ListItem(
        leadingContent = {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = AppTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp),
            )
        },
        headlineContent = {
            Text(
                text = headline,
                style = AppTheme.typography.labelMedium,
                color = AppTheme.colorScheme.onSurfaceVariant,
            )
        },
        supportingContent = {
            Text(
                text = supporting,
                style = AppTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
            )
        },
        colors = ListItemDefaults.colors(
            containerColor = Color.Transparent,
        ),
    )
}

/**
 * 课表空状态占位展示。
 */
@Composable
fun EmptySchedulePlaceholder(
    hasData: Boolean,
    onImport: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = AppIcons.Class,
            contentDescription = null,
            tint = AppTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
            modifier = Modifier.size(64.dp),
        )

        Spacer(Modifier.height(16.dp))

        Text(
            text = if (hasData) {
                t("当前学期暂无课表数据", "No timetable data for this semester")
            } else {
                t("尚未同步教务课表数据", "Timetable has not been synced")
            },
            style = AppTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )

        Spacer(Modifier.height(6.dp))

        Text(
            text = if (hasData) {
                t("请轻触下方按钮从教务系统网页重新导入", "Tap the button below to import again from the academic system page")
            } else {
                t("请先在网页里登录教务系统，再导入课表", "Sign in to your academic system page, then import the timetable")
            },
            style = AppTheme.typography.bodyMedium,
            color = AppTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )

        Spacer(Modifier.height(24.dp))

        FilledTonalButton(onClick = onImport) {
            Icon(
                imageVector = AppIcons.Public,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.width(8.dp))
            Text(t("打开网页登录导入课表", "Open web page to import"))
        }
    }
}
