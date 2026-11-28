package com.irvingwu.huiniaotable.data

internal fun Schedule.byPeriod(): Schedule {
    val last = maxOf(slotRows.maxOfOrNull { it.endSlot } ?: 1, courses.maxOfOrNull { it.endSlot } ?: 1).coerceIn(1, 99)
    if (slotRows.size == last && slotRows.withIndex().all { (index, row) -> row.startSlot == index + 1 && row.endSlot == index + 1 } &&
        courses.all { it.rowIndex == it.startSlot - 1 }) return this
    return copy(slotRows = (1..last).map { SlotRow(it.toString(), it, it) }, courses = courses.map { it.copy(rowIndex = it.startSlot - 1) })
}

internal fun Schedule.placementsByPeriod(placements: Map<String, Int>): Map<String, Int> {
    val byKey = courses.associateBy { it.key }
    return placements.mapValues { (key, packed) ->
        if (packed <= 0) return@mapValues packed
        val course = byKey[key.substringAfter('#')] ?: return@mapValues packed
        val target = slotRows.getOrNull(packed % 100) ?: return@mapValues packed
        val original = slotRows.getOrNull(course.rowIndex) ?: return@mapValues packed
        val offset = (course.startSlot - original.startSlot).coerceIn(0, target.endSlot - target.startSlot)
        packed - packed % 100 + target.startSlot + offset - 1
    }
}
