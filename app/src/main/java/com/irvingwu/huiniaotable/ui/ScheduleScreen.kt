package com.irvingwu.huiniaotable.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.ui.res.painterResource
import com.irvingwu.huiniaotable.R
import com.irvingwu.huiniaotable.ui.icons.AppIcons
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.irvingwu.huiniaotable.data.*
import com.irvingwu.huiniaotable.t
import top.yukonga.miuix.kmp.basic.*
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.window.WindowBottomSheet
import top.yukonga.miuix.kmp.window.WindowDialog
import java.time.LocalDate

@Composable
internal fun TodaySheet(state: AppState, show: Boolean, onDismiss: () -> Unit) {
    WindowBottomSheet(show = show, title = t("今日课程", "Today"), onDismissRequest = onDismiss) {
        val today = LocalDate.now()
        val lessons = remember(state, today) { state.classesOnDate(today) }
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
            if (lessons.isEmpty()) Text(state.holidayOn(today)?.name ?: t("今天没有课", "No classes today"))
            lessons.forEach { course ->
                BasicComponent(title = course.name, summary = listOfNotNull(state.timeTextFor(course).ifBlank { null }, course.room).joinToString(" · "))
            }
        }
    }
}

@Composable
fun CourseDetailSheet(course: Course?, state: AppState, onDismiss: () -> Unit,
    onResetPosition: (Course) -> Unit = {}, onStartPlacement: (Course, Int) -> Unit = { _, _ -> }) {
    if (course == null) return
    var moving by remember(course) { mutableStateOf(false) }
    var visible by remember(course) { mutableStateOf(true) }
    if (moving) {
        WeekJumpDialog(state.selectedWeek, state.maxWeek, { onStartPlacement(course, it) }, { moving = false; visible = true }, t("调至哪一周？", "Move to week"))
        return
    }
    var pickWeekAfterDismiss by remember(course) { mutableStateOf(false) }
    WindowBottomSheet(show = visible, title = course.name, onDismissRequest = { visible = false }, onDismissFinished = {
        if (pickWeekAfterDismiss) moving = true else onDismiss()
    }) {
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(listOfNotNull(course.dayText, course.slotText, state.timeTextFor(course).ifBlank { null }).joinToString(" · "))
            Text(course.weekText, color = MiuixTheme.colorScheme.onSurfaceVariantSummary)
            course.room?.let { BasicComponent(startAction = { PreferenceIcon(AppIcons.LocationOn) }, title = t("教室", "Room"), summary = it) }
            course.teacher?.let { BasicComponent(startAction = { PreferenceIcon(AppIcons.Person) }, title = t("教师", "Instructor"), summary = it) }
            course.classLabel?.let { BasicComponent(startAction = { PreferenceIcon(AppIcons.Groups) }, title = t("教学班", "Class group"), summary = it.cleanClassLabel()) }
            BasicComponent(startAction = { PreferenceIcon(AppIcons.Schedule) }, title = t("作息方案", "Period schedule"), summary = state.schemeFor(course).fullLabel)
            if (state.isCourseMoved(course)) BasicComponent(startAction = { PreferenceIcon(AppIcons.EditCalendar) }, title = t("已调整周次", "Changed weeks"), summary = state.placementWeeksOf(course).joinToString(", "))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (state.isCourseMoved(course)) TextButton(text = t("恢复原位", "Reset"), onClick = { onResetPosition(course); visible = false }, modifier = Modifier.weight(1f))
                TextButton(text = t("调课", "Move class"), onClick = { pickWeekAfterDismiss = true; visible = false }, modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
internal fun WeekJumpDialog(currentWeek: Int, maxWeek: Int, onSelect: (Int) -> Unit, onDismiss: () -> Unit, title: String, show: Boolean = true) {
    WindowDialog(show = show, title = title, onDismissRequest = onDismiss) {
        Column(Modifier.heightIn(max = 340.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            (1..maxWeek).chunked(4).forEach { group ->
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    group.forEach { week -> TextButton(text = week.toString(), onClick = { onSelect(week) }, modifier = Modifier.weight(1f),
                        insideMargin = PaddingValues(0.dp), colors = if (week == currentWeek) ButtonDefaults.textButtonColorsPrimary() else ButtonDefaults.textButtonColors()) }
                    repeat(4 - group.size) { Spacer(Modifier.weight(1f)) }
                }
            }
        }
    }
}

@Composable
fun EmptySchedulePlaceholder(
    hasData: Boolean,
    onImport: () -> Unit,
) {
    Column(Modifier.fillMaxSize().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center) {
        Image(painterResource(R.drawable.ic_app_logo), t("应用图标", "App icon"), Modifier.size(80.dp))
        Spacer(Modifier.height(20.dp))
        Text(if (hasData) t("暂无排课", "No scheduled classes") else t("导入你的课表", "Import your timetable"),
            style = MiuixTheme.textStyles.title2, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(8.dp))
        Text(t("从学校教务网页或本地课表文件导入。", "Use your school's timetable page or a saved timetable file."),
            style = MiuixTheme.textStyles.body1, color = MiuixTheme.colorScheme.onSurfaceVariantSummary, textAlign = TextAlign.Center)
        Spacer(Modifier.height(24.dp))
        TextButton(colors = ButtonDefaults.textButtonColorsPrimary(), text = t("导入课表", "Import timetable"), onClick = onImport)
    }
}
