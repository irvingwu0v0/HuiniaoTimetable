package com.irvingwu.huiniaotable.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.theme.miuixShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.irvingwu.huiniaotable.data.AppState
import com.irvingwu.huiniaotable.data.ClassTimes
import com.irvingwu.huiniaotable.data.Course
import com.irvingwu.huiniaotable.data.weekdayShortName
import com.irvingwu.huiniaotable.t
import androidx.compose.ui.draw.rotate
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.basic.ArrowRight
import com.irvingwu.huiniaotable.ui.theme.rememberCoursePalette
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.theme.MiuixTheme
import java.time.LocalDate
import kotlin.math.floor

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
        EmptySchedulePlaceholder(hasData = schedule != null, onImport = onRefresh)
        return
    }
    val week = state.selectedWeek
    var weekPicker by rememberSaveable { mutableStateOf(false) }
    var datePicker by rememberSaveable { mutableStateOf(false) }
    var agenda by rememberSaveable { mutableStateOf(false) }
    WeekJumpDialog(week, state.maxWeek, { onSelectWeek(it); weekPicker = false },
        { weekPicker = false }, title = t("教学周", "Teaching week"), show = weekPicker)
    if (datePicker) PickDateDialog(title = t("开学日期", "Term start"), current = state.termStart,
        onDismiss = { datePicker = false }, onConfirm = { onSetTermStart(it); datePicker = false })
    TodaySheet(state, show = agenda) { agenda = false }

    val dates = state.datesOf(week)
    val days = state.visibleDays(week)
    val rows = schedule.slotRows.let { if (state.hideEvening && it.size > 2) it.dropLast(2) else it }
    val effective = days.map { day -> dates.getOrNull(day - 1)?.let { state.displayDayOfWeek(it, week) } ?: day }
    val lessons = remember(state, days, rows) { days.mapIndexed { index, day ->
        if (dates.getOrNull(day - 1)?.let(state::isOffDay) == true) emptyList()
        else dayLayout(state, effective[index], week, rows)
    } }
    val palette = rememberCoursePalette()
    val colors = MiuixTheme.colorScheme
    val gutter = 36.dp
    val tileShape = miuixShape(8.dp)
    Column(Modifier.fillMaxSize().padding(horizontal = 12.dp)) {
        Row(Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f).clip(miuixShape(12.dp)).clickable { weekPicker = true }.padding(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(t("第 $week 周", "Week $week"), style = MiuixTheme.textStyles.title1, fontWeight = FontWeight.Medium)
                    Icon(MiuixIcons.Basic.ArrowRight, t("选择周次", "Choose week"), modifier = Modifier.padding(start = 6.dp).size(10.dp, 16.dp).rotate(90f), tint = colors.onSurfaceVariantSummary)
                }
                schedule.termLabel.takeIf { it.isNotBlank() }?.let {
                    Text(it, color = colors.onSurfaceVariantSummary, style = MiuixTheme.textStyles.footnote1, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            when {
                state.needsTermStart -> TextButton(text = t("设置开学日", "Set term start"), onClick = { datePicker = true })
                week != state.currentWeek -> TextButton(text = t("回到本周", "This week"), onClick = onToday)
                else -> TextButton(text = t("今日课程", "Today"), onClick = { agenda = true })
            }
        }
        if (placementCourse != null) Row(verticalAlignment = Alignment.CenterVertically) {
            Text(t("选择空位：${placementCourse.name}", "Choose a slot: ${placementCourse.name}"), Modifier.weight(1f))
            TextButton(text = t("取消", "Cancel"), onClick = onCancelPlacement)
        }
        Card(Modifier.fillMaxWidth(), insideMargin = PaddingValues(start = 2.dp, end = 4.dp, bottom = 4.dp)) {
            Row(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                Spacer(Modifier.width(gutter))
                days.forEach { day ->
                    val date = dates.getOrNull(day - 1)
                    val today = date == LocalDate.now()
                    val holiday = date?.let(state::holidayOn)
                    val makeup = date?.let { state.makeupLabelFor(it, week) }
                    Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(Modifier.size(28.dp).then(if (today) Modifier.background(colors.primary, miuixShape(14.dp)) else Modifier), contentAlignment = Alignment.Center) {
                            Text(weekdayShortName(day), style = MiuixTheme.textStyles.subtitle, color = if (today) colors.onPrimary else colors.onSurfaceContainer)
                        }
                        date?.let { Text("${it.monthValue}/${it.dayOfMonth}", fontSize = 11.sp, color = if (today) colors.primary else colors.onSurfaceVariantSummary) }
                        if (holiday != null) Text(makeup ?: if (holiday.isOffDay) t("休", "Off") else t("班", "Work"), fontSize = 10.sp, color = colors.primary)
                    }
                }
            }
            val density = LocalDensity.current
            BoxWithConstraints(Modifier.fillMaxWidth().heightIn(max = 76.dp * rows.size.coerceAtLeast(1)).fillMaxHeight().pointerInput(week, placementCourse, state.maxWeek) {
                if (placementCourse != null) return@pointerInput
                var distance = 0f
                detectHorizontalDragGestures(onDragStart = { distance = 0f }, onDragEnd = {
                    if (kotlin.math.abs(distance) >= 48.dp.toPx()) onSelectWeek((week + if (distance < 0) 1 else -1).coerceIn(1, state.maxWeek))
                }) { change, delta -> change.consume(); distance += delta }
            }) {
                if (rows.isEmpty()) return@BoxWithConstraints
                val rowHeight = (maxHeight / rows.size).coerceIn(48.dp, 76.dp)
                val colWidth = (maxWidth - gutter) / days.size
                var hover by remember(week) { mutableStateOf<Pair<Int, Int>?>(null) }
                fun accepts(column: Int, row: Int, course: Course): Boolean {
                    if (row !in rows.indices) return false
                    if (column !in days.indices || dates.getOrNull(days[column] - 1)?.let(state::isOffDay) == true) return false
                    val end = row + course.endSlot - course.startSlot + 1
                    return end <= rows.size && lessons[column].none { it.row < end && it.end > row }
                }
                Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                    Box(Modifier.fillMaxWidth().height(rowHeight * rows.size)) {
                        rows.forEachIndexed { index, row ->
                            val schemes = lessons.flatten().filter { it.row < index + 1 && it.end > index }.map { state.schemeFor(it.course) }
                            val times = ClassTimes.rowsFor(schemes.groupingBy { it }.eachCount().maxByOrNull { it.value }?.key ?: state.timeTable, row)
                            Row(Modifier.offset(y = rowHeight * index).fillMaxWidth().height(rowHeight)) {
                                Column(Modifier.width(gutter).fillMaxHeight(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(row.startSlot.toString(), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                    times.firstOrNull()?.start?.let { Text(it, fontSize = 10.sp, color = colors.onSurfaceVariantSummary) }
                                }
                                days.indices.forEach { column ->
                                    val available = placementCourse != null && accepts(column, index, placementCourse)
                                    Spacer(Modifier.width(colWidth).fillMaxHeight().padding(1.dp).then(when {
                                        hover == (column to index) -> Modifier.background(colors.primary.copy(alpha = .30f))
                                        available -> Modifier.background(colors.primary.copy(alpha = .09f)).clickable { onPlaceCourse(placementCourse!!, week, effective[column], index, null, null) }
                                        else -> Modifier
                                    }))
                                }
                            }
                            Spacer(Modifier.offset(y = rowHeight * index).fillMaxWidth().height(if (row.startSlot == state.effectiveLunchBreakSlot + 1) 2.dp else 1.dp).background(colors.onSurfaceVariantSummary.copy(alpha = .12f)))
                        }
                        lessons.forEachIndexed { column, entries -> entries.forEach { entry ->
                            val width = colWidth / entry.lanes
                            val x = colWidth * column + width * entry.lane
                            val y = rowHeight * entry.row
                            val tint = palette.forKey(entry.course.name)
                            val height = rowHeight * (entry.end - entry.row)
                            Column(Modifier.offset(x = gutter + x, y = y).size(width, height).padding(2.dp)
                                .clip(tileShape).background(tint.container)
                                .pointerInput(entry, lessons, rowHeight, colWidth) {
                                    var pointer = Offset.Zero
                                    fun target(): Pair<Int, Int>? = with(density) {
                                        val col = floor((x.toPx() + pointer.x) / colWidth.toPx()).toInt()
                                        val row = floor((y.toPx() + pointer.y) / rowHeight.toPx()).toInt()
                                        if (accepts(col, row, entry.course)) col to row else null
                                    }
                                    detectDragGesturesAfterLongPress(onDragStart = { pointer = it }, onDragEnd = {
                                        hover?.let { (col, row) -> onPlaceCourse(entry.course, week, effective[col], row, effective[column], entry.row) }; hover = null
                                    }, onDragCancel = { hover = null }) { change, delta -> change.consume(); pointer += delta; hover = target() }
                                }.clickable { onCourseClick(entry.course) }.padding(horizontal = 5.dp, vertical = 6.dp)) {
                                Text(entry.course.name, fontSize = 13.sp, lineHeight = 16.sp, fontWeight = FontWeight.SemiBold,
                                    color = tint.onContainer, maxLines = when { height < 52.dp -> 1; height >= 96.dp -> 5; else -> 3 }, overflow = TextOverflow.Ellipsis)
                                if (height >= 48.dp) entry.course.room?.let { Text(it, fontSize = 11.sp, lineHeight = 14.sp,
                                    color = tint.onContainer, maxLines = if (height < 72.dp) 1 else 3, overflow = TextOverflow.Ellipsis) }
                            }
                        } }
                    }
                }
            }
        }
    }
}
