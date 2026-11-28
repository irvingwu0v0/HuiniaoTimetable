package com.irvingwu.huiniaotable.data

import com.irvingwu.huiniaotable.t
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import org.jsoup.parser.Parser
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.util.zip.ZipInputStream

internal object TimetableFile {
    private const val FILE_LIMIT = 4 * 1024 * 1024
    private const val XML_LIMIT = 8 * 1024 * 1024

    fun parse(stream: InputStream): KbParser.Parsed {
        val bytes = stream.readBounded(FILE_LIMIT)
        val html = if (bytes.take(2) == listOf(0x50.toByte(), 0x4b.toByte())) workbook(bytes)
        else Jsoup.parse(bytes.inputStream(), null, "").outerHtml()
        return KbParser.parse(html)
    }

    private fun InputStream.readBounded(limit: Int): ByteArray {
        val result = ByteArrayOutputStream()
        val buffer = ByteArray(8192)
        while (true) {
            val count = read(buffer)
            if (count < 0) break
            if (result.size() + count > limit) throw ParseException(t("课表文件过大", "Timetable file is too large"))
            result.write(buffer, 0, count)
        }
        return result.toByteArray()
    }

    private data class Cell(val col: Int, val row: Int)
    private val address = Regex("([A-Z]{1,3})([1-9]\\d{0,6})")
    private val sheetPath = Regex("xl/worksheets/sheet\\d+\\.xml")

    private fun cell(value: String): Cell? {
        val match = address.matchEntire(value) ?: return null
        val column = match.groupValues[1].fold(0) { total, letter -> total * 26 + (letter - 'A' + 1) }
        return Cell(column, match.groupValues[2].toInt()).takeIf { it.col <= 64 && it.row <= 2048 }
    }

    private fun workbook(bytes: ByteArray): String {
        var remaining = XML_LIMIT
        val sheets = mutableListOf<String>()
        var strings = emptyList<String>()
        ZipInputStream(bytes.inputStream()).use { zip ->
            var count = 0
            while (true) {
                val entry = zip.nextEntry ?: break
                if (++count > 512) throw ParseException(t("工作簿内容过多", "Workbook contains too many entries"))
                val data = zip.readBounded(remaining)
                remaining -= data.size
                when {
                    sheetPath.matches(entry.name) -> sheets += data.toString(Charsets.UTF_8)
                    entry.name == "xl/sharedStrings.xml" -> strings = Jsoup.parse(data.toString(Charsets.UTF_8), "", Parser.xmlParser())
                        .select("si").map { item -> item.select("t").joinToString("") { it.wholeText() } }
                }
            }
        }
        return sheets.joinToString("") { xml ->
            val document = Jsoup.parse(xml, "", Parser.xmlParser())
            val merged = document.select("mergeCell").mapNotNull { merge ->
                val ends = merge.attr("ref").split(':')
                val start = cell(ends.first()) ?: return@mapNotNull null
                val end = cell(ends.last()) ?: return@mapNotNull null
                start to end
            }.toMap()
            val table = Element("table")
            val occupiedUntil = IntArray(65)
            for (row in document.select("sheetData > row")) {
                val cells = row.select("c").mapNotNull { value -> cell(value.attr("r"))?.let { it to value } }.toMap()
                val number = cells.keys.firstOrNull()?.row ?: continue
                val tr = table.appendElement("tr")
                for (column in 1..cells.keys.maxOf { it.col }) {
                    if (occupiedUntil[column] >= number) continue
                    val start = Cell(column, number)
                    val td = tr.appendElement("td")
                    merged[start]?.let { end ->
                        td.attr("colspan", (end.col - column + 1).toString()).attr("rowspan", (end.row - number + 1).toString())
                        for (index in column..end.col) occupiedUntil[index] = end.row
                    }
                    val value = cells[start] ?: continue
                    val raw = value.selectFirst("v")?.text().orEmpty()
                    val text = when (value.attr("t")) {
                        "s" -> raw.toIntOrNull()?.let(strings::getOrNull).orEmpty()
                        "inlineStr" -> value.select("is t").joinToString("") { it.wholeText() }
                        else -> raw
                    }
                    text.lines().forEachIndexed { index, line ->
                        if (index > 0) td.appendElement("br")
                        td.appendText(line)
                    }
                }
            }
            table.outerHtml()
        }
    }
}
