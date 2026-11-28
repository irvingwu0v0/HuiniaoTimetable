package com.irvingwu.huiniaotable.ui

import com.irvingwu.huiniaotable.data.AppState
import com.irvingwu.huiniaotable.data.Course
import com.irvingwu.huiniaotable.data.SlotRow

internal data class Meeting(
    val course: Course,
    val row: Int,
    val end: Int,
    val lane: Int = 0,
    val lanes: Int = 1,
)

internal fun dayLayout(state: AppState, day: Int, week: Int, rows: List<SlotRow>): List<Meeting> {
    val schedule = state.schedule ?: return emptyList()
    val meetings = schedule.courses.flatMap { course ->
        state.positionsOf(course, week).distinct().mapNotNull { (weekday, index) ->
            if (weekday != day) return@mapNotNull null
            if (index !in rows.indices) return@mapNotNull null
            Meeting(course, index, (index + course.endSlot - course.startSlot + 1).coerceAtMost(rows.size))
        }
    }.sortedWith(compareBy({ it.row }, { it.end }, { it.course.name }))
    return buildList {
        val group = mutableListOf<Meeting>()
        val ends = mutableListOf<Int>()
        fun flush() {
            addAll(group.map { it.copy(lanes = ends.size) })
            group.clear()
            ends.clear()
        }
        for (meeting in meetings) {
            if (ends.all { it <= meeting.row }) flush()
            val lane = ends.indexOfFirst { it <= meeting.row }.let { if (it < 0) ends.size else it }
            if (lane == ends.size) ends.add(meeting.end) else ends[lane] = meeting.end
            group.add(meeting.copy(lane = lane))
        }
        flush()
    }
}
