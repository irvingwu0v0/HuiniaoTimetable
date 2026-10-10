package com.irvingwu.huiniaotable.data

import org.json.JSONArray
import org.json.JSONObject

/** 校区。 */
data class Campus(val label: String) {
    val buildings: List<CampusBuilding>
        get() = ClassTimes.buildingsForCampus(label)

    val defaultBuilding: CampusBuilding
        get() = buildings.firstOrNull() ?: ClassTimes.defaultBuilding

    companion object {
        val Default: Campus get() = Campus(ClassTimes.defaultBuilding.campus)
        val entries: List<Campus> get() = ClassTimes.campuses.map { Campus(it) }
    }
}

/** 一节课的起止时间，格式 HH:mm。 */
data class ClockTime(val start: String, val end: String) {
    override fun toString(): String = "$start-$end"
}

/**
 * 校区与楼宇时间方案。
 */
data class CampusBuilding(
    val id: String,
    val label: String,
    val campus: String,
) {
    val name: String get() = id

    companion object {
        val entries: List<CampusBuilding> get() = ClassTimes.allBuildings
    }
    val fullLabel: String
        get() = if (campus.isNotBlank() && campus != label) "$campus · $label" else label

    val timeSummary: String
        get() = "1-2节 ${ClassTimes.timeTextFor(1, 2, this)} · 3-4节 ${ClassTimes.timeTextFor(3, 4, this)}"
}

/**
 * 上课时间表 + 楼栋自动匹配识别管理器。
 *
 * 默认内置大学标准作息（第 1 节 08:00 开始）；
 * 导入导出 JSON 作息配置仅包含校区与方案节次定义，不包含固化的 buildingHints，
 * 系统依据课表中出现的教学楼/教室建筑名称与校区自动匹配最佳作息方案。
 */
object ClassTimes {

    /** 高校标准作息（默认 10 节课）。 */
    val DEFAULT_MAINLAND_PERIODS: List<ClockTime> = listOf(
        ClockTime("08:00", "08:45"), // 第 1 节
        ClockTime("08:55", "09:40"), // 第 2 节
        ClockTime("10:00", "10:45"), // 第 3 节 (大课间 20 分钟后)
        ClockTime("10:55", "11:40"), // 第 4 节
        ClockTime("14:00", "14:45"), // 第 5 节 (午休后)
        ClockTime("14:55", "15:40"), // 第 6 节
        ClockTime("16:00", "16:45"), // 第 7 节
        ClockTime("16:55", "17:40"), // 第 8 节
        ClockTime("19:00", "19:45"), // 第 9 节 (晚自习)
        ClockTime("19:55", "20:40"), // 第 10 节
    )

    val DEFAULT_BUILDING = CampusBuilding(
        id = "default",
        label = "标准教学楼作息",
        campus = "主校区",
    )

    var configName: String = "标准作息表"
        private set

    var defaultBuilding: CampusBuilding = DEFAULT_BUILDING
        private set

    private val buildingsMap = LinkedHashMap<String, CampusBuilding>().apply {
        put(DEFAULT_BUILDING.id, DEFAULT_BUILDING)
    }

    private val periodsMap = LinkedHashMap<String, List<ClockTime>>().apply {
        put(DEFAULT_BUILDING.id, DEFAULT_MAINLAND_PERIODS)
    }

    /** 当前所有已注册的楼宇/方案。 */
    val allBuildings: List<CampusBuilding>
        get() = synchronized(this) { buildingsMap.values.toList() }

    /** 当前所有校区名称列表。 */
    val campuses: List<String>
        get() = synchronized(this) { buildingsMap.values.map { it.campus }.distinct() }

    fun buildingsForCampus(campusName: String): List<CampusBuilding> =
        synchronized(this) { buildingsMap.values.filter { it.campus == campusName } }

    fun findBuilding(id: String): CampusBuilding? =
        synchronized(this) { buildingsMap[id] }

    /** 第 1 到 10 节课的时间。 */
    fun periods(building: CampusBuilding): List<ClockTime> = synchronized(this) {
        periodsMap[building.id] ?: periodsMap[defaultBuilding.id] ?: DEFAULT_MAINLAND_PERIODS
    }

    fun periodsForSchemeId(schemeId: String): List<ClockTime> = synchronized(this) {
        periodsMap[schemeId] ?: periods(defaultBuilding)
    }

    /** 节次区间的上课时间，例如第 1-2 节 -> 08:00-09:40。 */
    fun timeTextFor(startSlot: Int, endSlot: Int, building: CampusBuilding): String {
        val table = periods(building)
        val start = table.getOrNull((startSlot - 1).coerceIn(0, table.size - 1))?.start ?: return ""
        val end = table.getOrNull((endSlot - 1).coerceIn(0, table.size - 1))?.end ?: return start
        return "$start-$end"
    }

    /** 某个大节里每一节的起止时间，用于左侧节次列竖排展示。 */
    fun rowsFor(building: CampusBuilding, row: SlotRow): List<ClockTime> {
        val table = periods(building)
        return (row.startSlot..row.endSlot).mapNotNull { table.getOrNull(it - 1) }
    }

    /**
     * 计算某套方案中相差最大的相邻两节课（即默认的午休切分点）。
     * 返回前一节课的节次（1-indexed），例如 4 代表第 4 节课后开始午休（即第 4 节与第 5 节之间）。
     */
    fun defaultLunchBreakSlot(building: CampusBuilding = defaultBuilding): Int {
        val table = periods(building)
        return findLargestGapSlot(table)
    }

    /**
     * 找到相邻两节课起止时间间隔（以分钟计）最大的那一节（1-indexed）。
     */
    fun findLargestGapSlot(table: List<ClockTime>): Int {
        if (table.size < 2) return 4
        var maxGap = -1
        var bestSlot = 4
        for (i in 0 until table.size - 1) {
            val endMin = parseTimeToMinutes(table[i].end)
            val nextStartMin = parseTimeToMinutes(table[i + 1].start)
            if (endMin != null && nextStartMin != null) {
                val gap = nextStartMin - endMin
                if (gap > maxGap) {
                    maxGap = gap
                    bestSlot = i + 1
                }
            }
        }
        return bestSlot
    }

    /** 计算两节课之间的间隔分钟数。 */
    fun gapMinutesBetween(p1: ClockTime, p2: ClockTime): Int {
        val endMin = parseTimeToMinutes(p1.end) ?: return 0
        val startMin = parseTimeToMinutes(p2.start) ?: return 0
        return (startMin - endMin).coerceAtLeast(0)
    }

    fun parseTimeToMinutes(timeStr: String): Int? {
        val parts = timeStr.trim().split(":")
        if (parts.size != 2) return null
        val h = parts[0].toIntOrNull() ?: return null
        val m = parts[1].toIntOrNull() ?: return null
        return h * 60 + m
    }

    /**
     * 检测文本中是否包含当前作息表里配置的某个校区名称。
     */
    fun detectCampus(text: String?): String? {
        if (text.isNullOrBlank()) return null
        return synchronized(this) {
            campuses.firstOrNull { text.contains(it) }
        }
    }

    /**
     * 从教学楼或教室字段中提取纯粹的建筑名称（如 "第一教学楼101" -> "第一教学楼"，"实验楼203" -> "实验楼"）。
     * 若该字段仅为校区名称（如 "东校区"、"南校区"），则返回 null。
     */
    fun extractBuildingName(raw: String): String? {
        var trimmed = raw.trim().trim('【', '】')
        if (trimmed.isBlank()) return null

        val campus = detectCampus(trimmed)
        if (campus != null) {
            val stripped = trimmed.replace(campus, "").trim('·', '-', ' ', '、')
            if (stripped.isBlank()) {
                // 该字段纯粹只是校区名称
                return null
            }
            trimmed = stripped
        }

        // 匹配常见建筑名词后缀
        val keywordMatch = Regex("""^(.*?)(教学楼|实验楼|实训楼|综合楼|信息楼|科技楼|办公楼|大厦|中心|楼|馆|堂|院|栋|所|场)(?:[-_#\s]*[A-Za-z]?[0-9]+[A-Za-z]?室?|.*)?$""")
            .find(trimmed)
        if (keywordMatch != null) {
            val name = (keywordMatch.groupValues[1] + keywordMatch.groupValues[2]).trim()
            if (name.isNotBlank()) return name
        }
        // 去除末尾房间号纯数字
        val noDigitsMatch = Regex("""^([^\d]+?)(?=\d)""").find(trimmed)
        if (noDigitsMatch != null) {
            val name = noDigitsMatch.groupValues[1].trim()
            if (name.isNotBlank()) return name
        }
        return trimmed.takeIf { it.isNotBlank() }
    }

    /**
     * 识别课表里出现的建筑标识。优先从教学楼字段提取，若教学楼字段为校区或空则从教室字段提取。
     */
    fun buildingKeyOf(teachingBuilding: String?, room: String?): String? {
        val fromTb = teachingBuilding?.let { extractBuildingName(it) }
        if (fromTb != null) return fromTb
        return room?.let { extractBuildingName(it) }
    }

    /**
     * 从文本中提取建筑编号（如 "1号教学楼" -> 1，"第二教学楼" -> 2）。
     */
    private fun extractBuildingNumber(text: String): Int? {
        val cnMap = mapOf(
            '一' to 1, '二' to 2, '三' to 3, '四' to 4, '五' to 5,
            '六' to 6, '七' to 7, '八' to 8, '九' to 9, '十' to 10,
        )
        for ((ch, n) in cnMap) {
            if (text.contains("第$ch") || text.contains("${ch}号") || text.contains("${ch}教")) return n
        }
        val digitMatch = Regex("""(\d+)""").find(text)
        return digitMatch?.groupValues?.get(1)?.toIntOrNull()
    }

    /**
     * 根据课表中出现的建筑名称（及可选校区提示）在已注册的方案中自动匹配最佳方案。
     */
    fun matchBuilding(buildingName: String, campusHint: String? = null): CampusBuilding? = synchronized(this) {
        val cleanName = extractBuildingName(buildingName) ?: buildingName
        val campus = campusHint ?: detectCampus(buildingName)

        val candidateSchemes = if (campus != null) {
            val byCampus = buildingsMap.values.filter { it.campus == campus }
            byCampus.ifEmpty { buildingsMap.values.toList() }
        } else {
            buildingsMap.values.toList()
        }

        if (candidateSchemes.isEmpty()) return@synchronized defaultBuilding

        val bNum = extractBuildingNumber(cleanName)

        var bestScore = -1
        var bestScheme: CampusBuilding? = null

        for (s in candidateSchemes) {
            var score = 0
            val sLabel = s.label
            val sId = s.id
            val sNum = extractBuildingNumber(sLabel) ?: extractBuildingNumber(sId)

            when {
                // 完全一致
                cleanName == sLabel -> score = 100
                // 方案名包含建筑名或建筑名包含方案名
                cleanName.contains(sLabel) || sLabel.contains(cleanName) -> score = 85
                // 编号一致（如 1号楼 / 1号教学楼 / 一教）
                bNum != null && sNum != null && bNum == sNum -> score = 80
                // 特征字符重合（去除泛化词）
                cleanName.filterNot { "教学楼及其他宇方案作息".contains(it) }.let { kw ->
                    kw.isNotBlank() && sLabel.contains(kw)
                } -> score = 65
                // 校区内「其他楼宇」或默认通用方案作为托底
                sLabel.contains("其他") || sId == "default" -> score = 15
            }

            if (score > bestScore) {
                bestScore = score
                bestScheme = s
            }
        }

        bestScheme ?: candidateSchemes.firstOrNull() ?: defaultBuilding
    }

    /** 某个楼栋的对应方案（优先用户手动校准的 [overrides]，否则基于建筑名称自动匹配）。 */
    fun schemeForKey(key: String, overrides: Map<String, CampusBuilding> = emptyMap()): CampusBuilding? {
        overrides[key]?.let { return it }
        return matchBuilding(key)
    }

    /** 一门课实际用哪套时间：先查用户校准，再根据课表建筑名称与校区自动匹配，兜底使用默认方案。 */
    fun resolveScheme(
        teachingBuilding: String?,
        room: String?,
        overrides: Map<String, CampusBuilding>,
        default: CampusBuilding,
    ): CampusBuilding {
        val key = buildingKeyOf(teachingBuilding, room)
        if (key != null && overrides.containsKey(key)) {
            return overrides[key]!!
        }
        val campus = detectCampus(teachingBuilding) ?: detectCampus(room)
        if (key != null) {
            matchBuilding(key, campus)?.let { return it }
        }
        return default
    }

    /**
     * 保存或新增一套楼宇/校区作息方案。
     */
    fun saveScheme(
        id: String,
        label: String,
        campus: String,
        periods: List<ClockTime>,
        isDefault: Boolean = false,
    ) {
        synchronized(this) {
            val building = CampusBuilding(id, label.trim().ifBlank { "未命名方案" }, campus.trim().ifBlank { "主校区" })
            buildingsMap[id] = building
            periodsMap[id] = periods
            if (isDefault || buildingsMap.size == 1) {
                defaultBuilding = building
            }
        }
    }

    /**
     * 删除指定的作息方案（保留至少一个默认方案）。
     */
    fun deleteScheme(id: String) {
        synchronized(this) {
            if (buildingsMap.size <= 1) return
            buildingsMap.remove(id)
            periodsMap.remove(id)
            if (defaultBuilding.id == id) {
                defaultBuilding = buildingsMap.values.first()
            }
        }
    }

    /**
     * 生成唯一的新方案 ID。
     */
    fun generateNewSchemeId(): String = "scheme_" + System.currentTimeMillis().toString().takeLast(6)

    /**
     * 图形化修改某节课的起止时间。
     */
    fun updatePeriod(schemeId: String, slotIndex: Int, newStart: String, newEnd: String) {
        synchronized(this) {
            val oldList = periodsMap[schemeId] ?: DEFAULT_MAINLAND_PERIODS
            val mutable = oldList.toMutableList()
            if (slotIndex in 0 until mutable.size) {
                mutable[slotIndex] = ClockTime(newStart.trim(), newEnd.trim())
                periodsMap[schemeId] = mutable
            }
        }
    }

    /**
     * 重置回默认内地高校标准作息。
     */
    fun resetToDefault() {
        synchronized(this) {
            configName = "标准作息表"
            defaultBuilding = DEFAULT_BUILDING
            buildingsMap.clear()
            buildingsMap[DEFAULT_BUILDING.id] = DEFAULT_BUILDING
            periodsMap.clear()
            periodsMap[DEFAULT_BUILDING.id] = DEFAULT_MAINLAND_PERIODS
        }
    }

    /**
     * 从 JSON 字符串解析并应用作息配置（纯方案列表，无需且不依赖 buildingHints）。
     * @return 成功返回 true，解析失败返回 false。
     */
    fun importJson(jsonStr: String): Boolean = runCatching {
        val root = JSONObject(jsonStr)
        val name = root.optString("name", "自定义作息表")
        val defaultId = root.optString("defaultSchemeId", "default")
        val schemesArr = root.optJSONArray("schemes") ?: return false

        val newBuildings = LinkedHashMap<String, CampusBuilding>()
        val newPeriods = LinkedHashMap<String, List<ClockTime>>()

        for (i in 0 until schemesArr.length()) {
            val sObj = schemesArr.getJSONObject(i)
            val id = sObj.getString("id")
            val campus = sObj.optString("campus", "主校区")
            val sName = sObj.optString("name", "教学楼作息")
            val b = CampusBuilding(id, sName, campus)
            newBuildings[id] = b

            val pArr = sObj.getJSONArray("periods")
            val pList = ArrayList<ClockTime>()
            for (p in 0 until pArr.length()) {
                val pObj = pArr.getJSONObject(p)
                pList.add(ClockTime(pObj.getString("start"), pObj.getString("end")))
            }
            newPeriods[id] = pList
        }

        synchronized(this) {
            configName = name
            buildingsMap.clear()
            buildingsMap.putAll(newBuildings)
            periodsMap.clear()
            periodsMap.putAll(newPeriods)
            defaultBuilding = buildingsMap[defaultId] ?: buildingsMap.values.firstOrNull() ?: DEFAULT_BUILDING
        }
        true
    }.getOrDefault(false)

    /**
     * 导出当前作息配置为格式化 JSON 字符串（不再包含 buildingHints）。
     */
    fun exportJson(): String = synchronized(this) {
        val root = JSONObject()
        root.put("name", configName)
        root.put("version", 1)
        root.put("defaultSchemeId", defaultBuilding.id)

        val schemesArr = JSONArray()
        for (b in buildingsMap.values) {
            val sObj = JSONObject()
            sObj.put("id", b.id)
            sObj.put("campus", b.campus)
            sObj.put("name", b.label)

            val pArr = JSONArray()
            val list = periodsMap[b.id] ?: DEFAULT_MAINLAND_PERIODS
            list.forEachIndexed { idx, p ->
                val pObj = JSONObject()
                pObj.put("slot", idx + 1)
                pObj.put("start", p.start)
                pObj.put("end", p.end)
                pArr.put(pObj)
            }
            sObj.put("periods", pArr)
            schemesArr.put(sObj)
        }
        root.put("schemes", schemesArr)

        root.toString(2)
    }
}
