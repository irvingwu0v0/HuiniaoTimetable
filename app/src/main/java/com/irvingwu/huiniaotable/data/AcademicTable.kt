package com.irvingwu.huiniaotable.data

import org.jsoup.nodes.Element

internal class AcademicTable(table: Element) {
    val rows: List<Map<Int, Element>> = buildList {
        val carried = mutableMapOf<Int, Pair<Int, Element>>()
        for (row in table.select("tr").filter { it.closest("table") === table }) {
            carried.entries.removeAll { it.value.first <= size }
            val cells = carried.mapValues { it.value.second }.toMutableMap()
            var column = 0
            for (cell in row.children().filter { it.normalName() in setOf("th", "td") }) {
                while (column in cells) column++
                val width = cell.span("colspan")
                val height = cell.span("rowspan")
                for (index in column until column + width) {
                    cells[index] = cell
                    if (height > 1) carried[index] = size + height to cell
                }
                column += width
            }
            add(cells.toSortedMap())
        }
    }

    private fun Element.span(attribute: String) = (attr(attribute).toIntOrNull() ?: 1).coerceIn(1, 64)
}
