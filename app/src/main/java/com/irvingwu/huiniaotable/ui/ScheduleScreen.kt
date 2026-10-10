package com.irvingwu.huiniaotable.ui

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.EventNote
import androidx.compose.material.icons.outlined.Apartment
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.Class
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material.icons.outlined.Event
import androidx.compose.material.icons.outlined.EventRepeat
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.HolidayVillage
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.MeetingRoom
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material.icons.outlined.Today
import androidx.compose.material.icons.outlined.RestartAlt
import androidx.compose.material.icons.outlined.WorkHistory
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Surface
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
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
 * 课表主视图：遵循 Material You 规范。
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
            refreshing = state.refreshing,
            onRefresh = onRefresh,
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
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                ),
                shape = MaterialTheme.shapes.medium,
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
                        imageVector = Icons.Outlined.EventRepeat,
                        contentDescription = null,
                        modifier = Modifier.size(22.dp),
                    )
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = "调课中 · 第 ${state.selectedWeek} 周",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            text = "点击任意空白格子，把《${placementCourse.name}》放到那里",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    TextButton(onClick = onCancelPlacement) {
                        Text("取消")
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

        // 周次切换时应用 Material 3 运动转场动画，支持在课表区域左右滑动切周
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
            title = "选取开学首周周一日期",
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
            containerColor = MaterialTheme.colorScheme.primaryContainer,
        ),
        shape = MaterialTheme.shapes.medium,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Icon(
                imageVector = Icons.Outlined.CalendarMonth,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(24.dp),
            )

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "配置学期开学首周基准日",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
                Text(
                    text = "系统将以首周周一自动核算各教学周次与调休安排",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f),
                )
            }

            FilledTonalButton(
                onClick = { showPicker = true },
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
            ) {
                Text("配置日期", style = MaterialTheme.typography.labelLarge)
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
                label = { Text("当前周", fontWeight = FontWeight.Medium) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Outlined.Today,
                        contentDescription = null,
                        modifier = Modifier.size(AssistChipDefaults.IconSize),
                    )
                },
                colors = AssistChipDefaults.assistChipColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
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
                        text = if (isCurrent) "第 $week 周 (当前)" else "第 $week 周",
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
        holiday?.isOffDay == true -> MaterialTheme.colorScheme.errorContainer
        makeupLabel != null -> MaterialTheme.colorScheme.tertiaryContainer
        else -> MaterialTheme.colorScheme.surfaceContainerHigh
    }
    val contentColor = when {
        holiday?.isOffDay == true -> MaterialTheme.colorScheme.onErrorContainer
        makeupLabel != null -> MaterialTheme.colorScheme.onTertiaryContainer
        else -> MaterialTheme.colorScheme.onSurface
    }

    Card(
        colors = CardDefaults.cardColors(
            containerColor = containerColor,
            contentColor = contentColor,
        ),
        shape = MaterialTheme.shapes.medium,
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
                    holiday?.isOffDay == true -> Icons.Outlined.HolidayVillage
                    makeupLabel != null -> Icons.Outlined.WorkHistory
                    else -> Icons.AutoMirrored.Outlined.EventNote
                },
                contentDescription = null,
                modifier = Modifier.size(22.dp),
            )

            Column(Modifier.weight(1f)) {
                Text(
                    text = when {
                        holiday?.isOffDay == true -> "法定节假日 · ${holiday.name}"
                        makeupLabel != null -> "调休补课日 · 执行 $makeupLabel 安排 (${courses.size} 门)"
                        else -> "今日安排 · ${WEEKDAY_NAMES[today.dayOfWeek.value - 1]} · ${courses.size} 门课程"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )

                if (holiday?.isOffDay != true) {
                    Text(
                        text = next?.let { "下节课程 · ${state.timeTextFor(it)} ${it.name} (${it.room ?: "场地待定"})" }
                            ?: if (courses.isEmpty()) "全日无教学计划安排" else "今日教学安排已全部结束",
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = contentColor.copy(alpha = 0.85f),
                    )
                }
            }

            Icon(
                imageVector = Icons.Outlined.ExpandMore,
                contentDescription = "查看今日课程详情",
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

    // 预构建当周课程槽位索引，35 格查询降至 O(1)，阻断全表重复计算
    val coursesBySlot: Map<Pair<Int, Int>, List<Course>> = remember(schedule, state.coursePlacements, week) {
        val map = HashMap<Pair<Int, Int>, MutableList<Course>>()
        schedule.courses.forEach { course ->
            state.positionsOf(course, week).forEach { pos ->
                map.getOrPut(pos) { ArrayList() }.add(course)
            }
        }
        map
    }

    // 大节时段课程网格行（可隐藏晚自习第 9-10 节）
    val visibleRows = remember(schedule.slotRows, state.hideEvening) {
        if (state.hideEvening && schedule.slotRows.size > 1) {
            schedule.slotRows.dropLast(1)
        } else {
            schedule.slotRows
        }
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

    // 各格子在根坐标下的矩形，使用非状态 Map 避免 onGloballyPositioned 触发重组风暴
    val cellBounds = remember { HashMap<Int, Rect>() }
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
        return coursesBySlot[eff to row].isNullOrEmpty()
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
                        .weight(1f),
                ) {
                    SlotLabel(
                        row = row,
                        timeTable = rowSchemes.getOrElse(rowIndex) { state.timeTable },
                    )
                    columns.forEachIndexed { index, day ->
                        val eff = effectiveDays[index]
                        val offDay = isOffDayOf(day)
                        val cellCourses = coursesBySlot[eff to rowIndex] ?: emptyList()
                        val isDropTarget = dragging != null && !offDay && cellCourses.isEmpty()
                        val isHovered = hoverCell == (day to rowIndex)
                        val isPlacementTarget = placementCourse != null && !offDay && cellCourses.isEmpty()
                        val packed = keyOf(day, rowIndex)

                        CourseCell(
                            courses = cellCourses,
                            isOffDay = offDay,
                            dropTarget = isDropTarget,
                            hovered = isHovered,
                            placementTarget = isPlacementTarget,
                            draggingKey = dragging?.key,
                            palette = palette,
                            onCourseClick = onCourseClick,
                            onPlaceTap = if (isPlacementTarget) {
                                {
                                    val targetEffDay = effectiveByDay[day] ?: day
                                    onPlace(placementCourse!!, targetEffDay, rowIndex)
                                }
                            } else null,
                            onBounds = { rect -> cellBounds[packed] = rect },
                            tileModifier = { course ->
                                val tileKey = "${course.key}#$eff#$rowIndex"
                                Modifier
                                    .onGloballyPositioned {
                                        tilePositions[tileKey] = it.positionInRoot()
                                    }
                                    .pointerInput(tileKey, week) {
                                        var dragPointer = Offset.Zero
                                        detectDragGesturesAfterLongPress(
                                            onDragStart = { local ->
                                                dragging = course
                                                dragSourceCell = eff to rowIndex
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
                                                    val sourceCell = dragSourceCell ?: (eff to rowIndex)
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
                            },
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
            shape = MaterialTheme.shapes.small,
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
        isToday -> MaterialTheme.colorScheme.primary
        isOffDay -> MaterialTheme.colorScheme.surfaceContainerHighest
        isWorkday -> MaterialTheme.colorScheme.tertiaryContainer
        else -> MaterialTheme.colorScheme.surfaceContainerLow
    }
    val contentColor = when {
        isToday -> MaterialTheme.colorScheme.onPrimary
        isOffDay -> MaterialTheme.colorScheme.onSurfaceVariant
        isWorkday -> MaterialTheme.colorScheme.onTertiaryContainer
        else -> MaterialTheme.colorScheme.onSurface
    }

    Box(
        modifier = modifier
            .fillMaxHeight()
            .padding(horizontal = 1.dp, vertical = 2.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .clip(MaterialTheme.shapes.small)
                .background(containerColor),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = WEEKDAY_NAMES[day - 1].removePrefix("星期"),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = if (isToday) FontWeight.Bold else FontWeight.SemiBold,
                    color = contentColor,
                )
                if (isOffDay) {
                    Box(
                        modifier = Modifier
                            .size(15.dp)
                            .background(MaterialTheme.colorScheme.error, CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "休",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 8.sp,
                                lineHeight = 8.sp,
                                fontWeight = FontWeight.Bold,
                            ),
                            color = MaterialTheme.colorScheme.onError,
                            textAlign = TextAlign.Center,
                        )
                    }
                } else if (isWorkday) {
                    Box(
                        modifier = Modifier
                            .size(15.dp)
                            .background(MaterialTheme.colorScheme.tertiary, CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "班",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 8.sp,
                                lineHeight = 8.sp,
                                fontWeight = FontWeight.Bold,
                            ),
                            color = MaterialTheme.colorScheme.onTertiary,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }

            Text(
                text = date?.let { "${it.monthValue}/${it.dayOfMonth}" }.orEmpty(),
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 10.sp,
                ),
                color = contentColor.copy(alpha = if (isToday) 0.95f else 0.75f),
            )

            makeupLabel?.let { label ->
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp),
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
            color = MaterialTheme.colorScheme.surfaceContainerHighest,
        ) {
            Text(
                text = row.label.replace("第", "").replace("节", "").trim(),
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
            )
        }

        Spacer(Modifier.height(2.dp))

        start?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 9.sp,
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
        end?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 9.sp,
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                textAlign = TextAlign.Center,
            )
        }
    }
}

/**
 * 课程格子容器：休假当天整个格子背景统一填充灰色。
 */
@Composable
private fun CourseCell(
    courses: List<Course>,
    isOffDay: Boolean,
    dropTarget: Boolean,
    hovered: Boolean,
    placementTarget: Boolean = false,
    draggingKey: String?,
    palette: CoursePalette,
    onCourseClick: (Course) -> Unit,
    onPlaceTap: (() -> Unit)? = null,
    onBounds: (Rect) -> Unit,
    tileModifier: (Course) -> Modifier,
    modifier: Modifier = Modifier,
) {
    val cellColor = if (isOffDay) {
        MaterialTheme.colorScheme.surfaceContainerHigh
    } else {
        MaterialTheme.colorScheme.surfaceContainerLowest
    }

    val highlighted = dropTarget || hovered || placementTarget
    val targetColor = MaterialTheme.colorScheme.primary

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
                        shape = MaterialTheme.shapes.small,
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
        shape = MaterialTheme.shapes.small,
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
        ) {
            if (courses.isNotEmpty()) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    courses.forEach { course ->
                        CourseTile(
                            course = course,
                            dimmed = isOffDay,
                            lifted = draggingKey == course.key,
                            palette = palette,
                            onCourseClick = onCourseClick,
                            modifier = tileModifier(course)
                                .weight(1f)
                                .fillMaxWidth(),
                        )
                    }
                }
            }
        }
    }
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
    // 同一门课（按课程名）始终命中同一组 Material You 色块。
    val colors = palette.forKey(course.name)

    val tileBg = if (dimmed) {
        MaterialTheme.colorScheme.surfaceContainerHighest
    } else {
        colors.container
    }
    val tileContent = if (dimmed) {
        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f)
    } else {
        colors.onContainer
    }
    val accent = if (dimmed) {
        MaterialTheme.colorScheme.outlineVariant
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
        shape = MaterialTheme.shapes.small,
        color = tileBg,
        contentColor = tileContent,
        shadowElevation = 0.dp,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
            verticalArrangement = Arrangement.spacedBy(1.dp),
        ) {
            Text(
                text = course.name,
                style = MaterialTheme.typography.labelSmall.copy(lineHeight = 12.sp),
                fontWeight = FontWeight.SemiBold,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
            course.room?.let { room ->
                TileInfoRow(
                    icon = Icons.Outlined.LocationOn,
                    text = room,
                    tint = accent,
                )
            }
            course.teacher?.let { teacher ->
                TileInfoRow(
                    icon = Icons.Outlined.Person,
                    text = teacher,
                    tint = accent,
                )
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
        shape = MaterialTheme.shapes.small,
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
                style = MaterialTheme.typography.labelSmall.copy(lineHeight = 12.sp),
                fontWeight = FontWeight.SemiBold,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
            course.room?.let { room ->
                TileInfoRow(icon = Icons.Outlined.LocationOn, text = room, tint = colors.accent)
            }
            course.teacher?.let { teacher ->
                TileInfoRow(icon = Icons.Outlined.Person, text = teacher, tint = colors.accent)
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
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/**
 * 今日课程详情底部抽屉。
 */
@OptIn(ExperimentalMaterial3Api::class)
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
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
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
                    imageVector = Icons.AutoMirrored.Outlined.EventNote,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp),
                )
                Text(
                    text = "今日教学日程 · ${WEEKDAY_NAMES[today.dayOfWeek.value - 1]}",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
            }

            if (holiday != null) {
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = if (holiday.isOffDay) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.tertiaryContainer,
                    contentColor = if (holiday.isOffDay) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onTertiaryContainer,
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Icon(
                            imageVector = if (holiday.isOffDay) Icons.Outlined.HolidayVillage else Icons.Outlined.WorkHistory,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                        )
                        Text(
                            text = when {
                                holiday.isOffDay -> "法定休假：${holiday.name}"
                                makeupLabel != null -> "调休补课安排：执行 $makeupLabel 课表"
                                else -> "调休上班日：${holiday.name}"
                            },
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            if (courses.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "今日无任何教学计划安排",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                courses.forEach { course ->
                    OutlinedCard(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.outlinedCardColors(
                            containerColor = MaterialTheme.colorScheme.surface,
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
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                ) {
                                    Text(
                                        text = state.timeTextFor(course),
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    )
                                }
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = course.name,
                                    style = MaterialTheme.typography.titleMedium,
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
                                            imageVector = Icons.Outlined.LocationOn,
                                            contentDescription = null,
                                            modifier = Modifier.size(14.dp),
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                        Text(
                                            text = r,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                }

                                course.teacher?.let { t ->
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.Person,
                                            contentDescription = null,
                                            modifier = Modifier.size(14.dp),
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                        Text(
                                            text = t,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
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
@OptIn(ExperimentalMaterial3Api::class)
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
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
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
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f),
                    )
                    // 调课：选择目标教学周并跳转
                    IconButton(onClick = { showWeekJump = true }) {
                        Icon(
                            imageVector = Icons.Outlined.EventRepeat,
                            contentDescription = "调课 · 跳到指定教学周",
                            tint = MaterialTheme.colorScheme.primary,
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
                            label = { Text(label, style = MaterialTheme.typography.labelSmall) },
                            icon = {
                                Icon(
                                    imageVector = Icons.Outlined.Groups,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp),
                                )
                            },
                        )
                    }
                    buildingKey?.let { b ->
                        SuggestionChip(
                            onClick = {},
                            label = { Text(b, style = MaterialTheme.typography.labelSmall) },
                            icon = {
                                Icon(
                                    imageVector = Icons.Outlined.Apartment,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp),
                                )
                            },
                        )
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            DetailListItem(
                icon = Icons.Outlined.Schedule,
                headline = "教学时段",
                supporting = "${course.dayText} ${course.slotText} (${ClassTimes.timeTextFor(course.startSlot, course.endSlot, scheme).ifBlank { "作息待定" }})",
            )

            DetailListItem(
                icon = Icons.Outlined.MeetingRoom,
                headline = "教学场地",
                supporting = course.room ?: "未指定具体教室",
            )

            DetailListItem(
                icon = Icons.Outlined.Person,
                headline = "主讲教师",
                supporting = course.teacher ?: "教师信息未录入",
            )

            DetailListItem(
                icon = Icons.Outlined.DateRange,
                headline = "教学周次",
                supporting = course.weekText,
            )

            DetailListItem(
                icon = Icons.Outlined.Apartment,
                headline = "所属作息方案",
                supporting = scheme.fullLabel,
            )

            if (state.isCourseMoved(course)) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                val removed = state.removedWeeksOf(course)
                val targets = state.placementWeeksOf(course)
                val summary = when {
                    removed.isNotEmpty() && targets.isNotEmpty() ->
                        "第 ${removed.joinToString("、")} 周的原课已调至第 ${targets.joinToString("、")} 周"
                    removed.isNotEmpty() ->
                        "第 ${removed.joinToString("、")} 周的原课已调走"
                    else ->
                        "第 ${targets.joinToString("、")} 周的位置已调整"
                }
                Text(
                    text = "$summary。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                TextButton(
                    onClick = {
                        onResetPosition(course)
                        onDismiss()
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.RestartAlt,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("恢复默认位置")
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
                imageVector = Icons.Outlined.EventRepeat,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
        },
        title = {
            Text(
                text = "调课 · 选择目标周次",
                style = MaterialTheme.typography.titleLarge,
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
                                shape = MaterialTheme.shapes.small,
                                color = if (selected) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.surfaceContainerHighest
                                },
                                contentColor = if (selected) {
                                    MaterialTheme.colorScheme.onPrimary
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                },
                                modifier = Modifier.weight(1f),
                            ) {
                                Text(
                                    text = "第 $week 周",
                                    style = MaterialTheme.typography.labelMedium,
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
            ) { Text("取消") }
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
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp),
            )
        },
        headlineContent = {
            Text(
                text = headline,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
        supportingContent = {
            Text(
                text = supporting,
                style = MaterialTheme.typography.bodyLarge,
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
    refreshing: Boolean,
    onRefresh: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = Icons.Outlined.Class,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
            modifier = Modifier.size(64.dp),
        )

        Spacer(Modifier.height(16.dp))

        Text(
            text = if (hasData) "当前学期暂无课表数据" else "尚未同步教务课表数据",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )

        Spacer(Modifier.height(6.dp))

        Text(
            text = if (hasData) "请轻触下方按钮同步最新教学计划" else "请验证统一身份凭据并同步教务系统课表",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )

        Spacer(Modifier.height(24.dp))

        FilledTonalButton(
            onClick = onRefresh,
            enabled = !refreshing,
        ) {
            Icon(
                imageVector = Icons.Outlined.Sync,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.width(8.dp))
            Text(if (refreshing) "正在同步教务系统…" else "同步课表数据")
        }
    }
}
