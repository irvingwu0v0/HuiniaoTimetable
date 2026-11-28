package com.irvingwu.huiniaotable.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.irvingwu.huiniaotable.data.AppState
import com.irvingwu.huiniaotable.data.Course
import com.irvingwu.huiniaotable.t
import com.irvingwu.huiniaotable.ui.theme.rememberCoursePalette
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.preference.ArrowPreference

@Composable
fun CoursesContent(
    state: AppState,
    onRefresh: () -> Unit,
    onCourseClick: (Course) -> Unit,
    onClearPlacements: () -> Unit = {},
) {
    val schedule = state.schedule
    if (schedule == null || (schedule.courses.isEmpty() && schedule.unscheduled.isEmpty())) {
        EmptySchedulePlaceholder(hasData = schedule != null, onImport = onRefresh)
        return
    }
    val days = remember(schedule) { schedule.distinctCourses.sortedBy { it.startSlot }.groupBy { it.dayOfWeek }.toSortedMap() }
    val palette = rememberCoursePalette()
    LazyColumn(Modifier.fillMaxSize().testTag("courses_list"), contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp)) {
        if (state.movedCourseCount > 0) item {
            TextButton(text = t("恢复所有调课", "Reset class moves"), onClick = onClearPlacements)
        }
        for ((day, courses) in days) {
            item(key = day) {
                SmallTitle(text = courses.first().dayText)
                Card(Modifier.fillMaxWidth()) {
                    courses.forEach { course ->
                        ArrowPreference(title = course.name, summary = listOfNotNull(course.slotText, course.room).joinToString(" · "),
                            startAction = { Spacer(Modifier.padding(end = 8.dp).size(5.dp, 30.dp).background(palette.forKey(course.name).accent, RoundedCornerShape(3.dp))) },
                            onClick = { onCourseClick(course) })
                    }
                }
            }
        }
        if (schedule.unscheduled.isNotEmpty()) item {
            SmallTitle(text = t("无课表课程 · ${schedule.unscheduled.size} 门", "Unscheduled · ${schedule.unscheduled.size}"))
            Card(Modifier.fillMaxWidth()) {
                schedule.unscheduled.forEach { course -> BasicComponent(title = course.name,
                    summary = listOfNotNull(course.teacher, course.classLabel, course.code, course.category, course.attribute).joinToString(" · ")) }
            }
        }
    }
}
