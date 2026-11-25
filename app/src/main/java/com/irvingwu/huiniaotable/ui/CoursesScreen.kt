package com.irvingwu.huiniaotable.ui

import com.irvingwu.huiniaotable.ui.icons.AppIcons

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import com.irvingwu.huiniaotable.ui.compat.Card
import com.irvingwu.huiniaotable.ui.compat.CardDefaults
import com.irvingwu.huiniaotable.ui.compat.Icon
import com.irvingwu.huiniaotable.ui.compat.AppTheme
import com.irvingwu.huiniaotable.ui.compat.OutlinedButton
import com.irvingwu.huiniaotable.ui.compat.OutlinedCard
import com.irvingwu.huiniaotable.ui.compat.Surface
import com.irvingwu.huiniaotable.ui.compat.SuggestionChip
import com.irvingwu.huiniaotable.ui.compat.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.irvingwu.huiniaotable.t
import com.irvingwu.huiniaotable.data.AppState
import com.irvingwu.huiniaotable.data.Course
import com.irvingwu.huiniaotable.data.UnscheduledCourse
import com.irvingwu.huiniaotable.data.cleanClassLabel

/**
 * 「课程清单」页面：遵循 Miuix / HyperOS 设计规范。
 * 结构化呈现计划内排课教学课程与系统无排课记录课程，多维度集成图标与辅助标签。
 */
@Composable
fun CoursesContent(
    state: AppState,
    onRefresh: () -> Unit,
    onCourseClick: (Course) -> Unit,
    onClearPlacements: () -> Unit = {},
) {
    val schedule = state.schedule
    if (schedule == null || (schedule.distinctCourses.isEmpty() && schedule.unscheduled.isEmpty())) {
        EmptySchedulePlaceholder(
            hasData = schedule != null,
            refreshing = state.refreshing,
            onRefresh = onRefresh,
        )
        return
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("courses_list"),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        if (state.movedCourseCount > 0) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = AppTheme.colorScheme.surfaceContainerLow,
                    ),
                    shape = AppTheme.shapes.medium,
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Icon(
                            imageVector = AppIcons.RestartAlt,
                            contentDescription = null,
                            tint = AppTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp),
                        )
                        Column(Modifier.weight(1f)) {
                            Text(
                                text = t(
                                    "已调整 ${state.movedCourseCount} 门课程的位置",
                                    "${state.movedCourseCount} course positions changed",
                                ),
                                style = AppTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Text(
                                text = t("恢复后所有课程回到课表原始排布", "Restore every course to its original position"),
                                style = AppTheme.typography.bodySmall,
                                color = AppTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        OutlinedButton(onClick = onClearPlacements) {
                            Text(t("恢复所有调课", "Reset all changes"))
                        }
                    }
                }
            }
        }

        item {
            SectionHeader(
                icon = AppIcons.Class,
                title = t(
                    "计划内排课教学课程 (${schedule.distinctCourses.size} 门)",
                    "Scheduled courses (${schedule.distinctCourses.size})",
                ),
            )
        }

        items(
            items = schedule.distinctCourses.sortedWith(
                compareBy({ it.dayOfWeek }, { it.startSlot }, { it.name }),
            ),
            key = { it.key },
        ) { course ->
            CourseCard(
                course = course,
                onClick = { onCourseClick(course) },
            )
        }

        if (schedule.unscheduled.isNotEmpty()) {
            item {
                Spacer(Modifier.height(10.dp))
                SectionHeader(
                    icon = AppIcons.Info,
                    title = t(
                        "无课表记录教学计划 (${schedule.unscheduled.size} 门)",
                        "Courses without scheduled times (${schedule.unscheduled.size})",
                    ),
                )
            }
            items(
                items = schedule.unscheduled,
                key = { "unscheduled-${it.name}-${it.classLabel}-${it.code}" },
            ) { course ->
                UnscheduledCard(course = course)
            }
        }
    }
}

@Composable
private fun SectionHeader(icon: ImageVector, title: String) {
    Row(
        modifier = Modifier.padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = AppTheme.colorScheme.primary,
            modifier = Modifier.size(18.dp),
        )
        Text(
            text = title,
            style = AppTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = AppTheme.colorScheme.primary,
        )
    }
}

@Composable
private fun CourseCard(course: Course, onClick: () -> Unit) {
    OutlinedCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.outlinedCardColors(
            containerColor = AppTheme.colorScheme.surfaceContainerLow,
        ),
        shape = AppTheme.shapes.medium,
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = course.name,
                    style = AppTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f),
                )
                course.classLabel?.cleanClassLabel()?.let { label ->
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = AppTheme.colorScheme.surfaceContainerHighest,
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Icon(
                                imageVector = AppIcons.Groups,
                                contentDescription = null,
                                modifier = Modifier.size(12.dp),
                                tint = AppTheme.colorScheme.onSurfaceVariant,
                            )
                            Text(
                                text = label,
                                style = AppTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                color = AppTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }

            // 元数据标签条
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                MetaPill(
                    icon = AppIcons.Schedule,
                    text = "${course.dayText} ${course.slotText}",
                )
                MetaPill(
                    icon = AppIcons.DateRange,
                    text = course.weekText,
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                course.room?.let { r ->
                    MetaPill(
                        icon = AppIcons.LocationOn,
                        text = r,
                    )
                }
                course.teacher?.let { t ->
                    MetaPill(
                        icon = AppIcons.Person,
                        text = t,
                    )
                }
            }
        }
    }
}

@Composable
private fun MetaPill(icon: ImageVector, text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(13.dp),
            tint = AppTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = text,
            style = AppTheme.typography.bodySmall,
            color = AppTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun UnscheduledCard(course: UnscheduledCourse) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = AppTheme.colorScheme.surfaceContainerLowest,
        ),
        shape = AppTheme.shapes.medium,
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = course.name,
                    style = AppTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f),
                )
                course.classLabel?.cleanClassLabel()?.let { label ->
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = AppTheme.colorScheme.surfaceContainerHighest,
                    ) {
                        Text(
                            text = label,
                            style = AppTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            color = AppTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        )
                    }
                }
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                course.teacher?.let { t ->
                    MetaPill(icon = AppIcons.Person, text = t)
                }
                course.category?.let { cat ->
                    Surface(
                        shape = CircleShape,
                        color = AppTheme.colorScheme.secondaryContainer,
                    ) {
                        Text(
                            text = cat,
                            style = AppTheme.typography.labelSmall.copy(fontSize = 9.sp),
                            color = AppTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp),
                        )
                    }
                }
                course.attribute?.let { attr ->
                    Surface(
                        shape = CircleShape,
                        color = AppTheme.colorScheme.tertiaryContainer,
                    ) {
                        Text(
                            text = attr,
                            style = AppTheme.typography.labelSmall.copy(fontSize = 9.sp),
                            color = AppTheme.colorScheme.onTertiaryContainer,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp),
                        )
                    }
                }
            }

            course.code?.let { c ->
                Text(
                    text = t("课程代码：$c", "Course code: $c"),
                    style = AppTheme.typography.labelSmall,
                    color = AppTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                )
            }
        }
    }
}
