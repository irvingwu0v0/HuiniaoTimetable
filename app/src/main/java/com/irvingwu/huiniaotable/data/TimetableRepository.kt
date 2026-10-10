package com.irvingwu.huiniaotable.data

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.json.JSONObject
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.time.LocalDate
import java.time.temporal.ChronoUnit

enum class Phase { Loading, LoggedOut, Ready }

/**
 * 某门课某次上课的具体排布（对应一个原始教学周 [originWeek]）。
 * 调课后该次课实际落在 [week] 周的星期 [day] 第 [row] 大节。
 */
data class CourseOccurrence(
    val originWeek: Int,
    val week: Int,
    val day: Int,
    val row: Int,
)

/** 表示某次课被调走、原位不再显示的哨兵位置。 */
private val REMOVED_POSITION = -1 to -1

data class AppState(
    val phase: Phase = Phase.Loading,
    val loggedIn: Boolean = false,
    val studentId: String = "",
    val jwxtBaseUrl: String = Prefs.DEFAULT_JWXT_BASE_URL,
    val scheduleConfigName: String = "标准高校作息表",
    val schedule: Schedule? = null,
    val terms: List<KbParser.Term> = emptyList(),
    val term: String = "",
    val currentWeek: Int = 1,
    val selectedWeek: Int = 1,
    val selectedWeekTouched: Boolean = false,
    val refreshing: Boolean = false,
    val loggingIn: Boolean = false,
    val loginError: String? = null,
    val fetchedAt: Long = 0L,
    val lastRefreshDay: String? = null,
    val termStart: LocalDate? = null,
    val campus: Campus = Campus.Default,
    val building: CampusBuilding? = null,
    val weekendMode: WeekendMode = WeekendMode.Auto,
    val buildingSchemes: Map<String, CampusBuilding> = emptyMap(),
    val holidays: HolidayYear? = null,
    val holidayOverrides: List<HolidayOverride> = emptyList(),
    val holidayFetchedAt: Long = 0L,
    val holidayRegion: HolidayRegion = HolidayRegion.NONE,
    val holidaySource: HolidaySource = HolidaySource.AUTO,
    val holidayCustomMirror: String = "",
    /** 自动推断的「调休上班日 -> 补周几」。 */
    val makeupGuesses: Map<LocalDate, Int> = emptyMap(),
    /** 用户手动改过的调休补课设置（0 表示不补课）。 */
    val makeupOverrides: Map<LocalDate, Int> = emptyMap(),
    val dynamicColor: Boolean = true,
    val classNotify: Boolean = false,
    /** 中午时段 / 午休开始节次设置（0 表示自动跟随相差最大的两节）。 */
    val lunchBreakSlotOverride: Int = 0,
    /** 课表是否隐藏晚自习（第 9-10 节）。 */
    val hideEvening: Boolean = false,
    /** 用户手动拖拽调整过的课程位置：courseKey -> 打包后的「星期*100+行号」。 */
    val coursePlacements: Map<String, Int> = emptyMap(),
    val message: String? = null,
    val messageSeq: Long = 0L,
) {
    /**
     * 课表的最大周次：严格以实际有排课的教学周（[Schedule.lastTeachingWeek]）为准，
     * 杜绝教务网页端下拉框预设的 30 周占位导致手势可以滑到 20 多周空白周。
     * 若当前周已推进超过排课周（如期末考试周），则自适应允许显示至当前周。
     */
    val maxWeek: Int
        get() = (schedule?.lastTeachingWeek ?: schedule?.maxWeek ?: 18)
            .coerceAtLeast(currentWeek)

    val lastTeachingWeek: Int get() = maxWeek

    /** 还没设置开学日期。 */
    val needsTermStart: Boolean get() = termStart == null

    /** 当前生效的午休开始节次（即第几节课后开始午休，默认是相邻节次相差最大的那两节）。 */
    val effectiveLunchBreakSlot: Int
        get() {
            if (lunchBreakSlotOverride > 0) return lunchBreakSlotOverride
            return ClassTimes.defaultLunchBreakSlot(timeTable)
        }

    /** 认不出楼栋时用的默认方案。 */
    val timeTable: CampusBuilding
        get() {
            val preferred = building
            return if (preferred != null && preferred.campus == campus.label) preferred else campus.defaultBuilding
        }

    /** 某门课实际用哪套上课时间：按教室所在楼栋判断。 */
    fun schemeFor(course: Course): CampusBuilding =
        ClassTimes.resolveScheme(course.campus, course.room, buildingSchemes, timeTable)

    fun timeTextFor(course: Course): String =
        ClassTimes.timeTextFor(course.startSlot, course.endSlot, schemeFor(course))

    /** 课表里出现过的楼栋，用于在设置页逐个校准。 */
    val buildingKeys: List<String>
        get() = schedule?.courses
            ?.mapNotNull { ClassTimes.buildingKeyOf(it.campus, it.room) }
            ?.distinct()
            ?.sorted()
            ?: emptyList()

    fun schemeForKey(key: String): CampusBuilding =
        ClassTimes.schemeForKey(key, buildingSchemes) ?: timeTable

    /** 排布键：第几周 + 课程 key，实现「按周」调课。 */
    fun placementKeyOf(course: Course, week: Int): String = "$week#${course.key}"

    /**
     * 计算某门课在整个学期各个教学周的全部实际排布情况（含手动调课）。
     * 课程的每个原始教学周次（[CourseOccurrence.originWeek]）对应一次课。
     */
    fun occurrencesOf(course: Course): List<CourseOccurrence> {
        val totalTeachingWeeks = (1..maxWeek).toList()
        val baseWeeks = if (course.weeks.isNotEmpty()) course.weeks else totalTeachingWeeks
        val prefix = "#${course.key}"
        val extraOriginWeeks = coursePlacements.keys
            .filter { it.endsWith(prefix) }
            .mapNotNull { it.substringBefore('#').toIntOrNull() }
            .filter { it !in baseWeeks }
        val allOriginWeeks = (baseWeeks + extraOriginWeeks).distinct()
        return allOriginWeeks.mapNotNull { originWeek ->
            val packed = coursePlacements[placementKeyOf(course, originWeek)]
            when {
                packed == null -> {
                    CourseOccurrence(
                        originWeek = originWeek,
                        week = originWeek,
                        day = course.dayOfWeek,
                        row = course.rowIndex,
                    )
                }
                packed <= 0 -> null
                packed < 1000 -> {
                    // 兼容旧格式（同周内移动）：week 仍为 originWeek，packed 为 day * 100 + row
                    CourseOccurrence(
                        originWeek = originWeek,
                        week = originWeek,
                        day = packed / 100,
                        row = packed % 100,
                    )
                }
                else -> {
                    // 新格式：targetWeek * 10000 + day * 100 + row
                    val targetWeek = packed / 10000
                    val day = (packed % 10000) / 100
                    val row = packed % 100
                    CourseOccurrence(
                        originWeek = originWeek,
                        week = targetWeek,
                        day = day,
                        row = row,
                    )
                }
            }
        }
    }

    /** 某门课在第 [week] 周的全部实际上课位置。 */
    fun positionsOf(course: Course, week: Int): List<Pair<Int, Int>> {
        val prefix = "#${course.key}"
        val hasAnyPlacement = coursePlacements.keys.any { it.endsWith(prefix) }
        val baseWeeks = if (course.weeks.isNotEmpty()) course.weeks else (1..maxWeek).toList()
        if (!hasAnyPlacement) {
            return if (baseWeeks.contains(week)) {
                listOf(course.dayOfWeek to course.rowIndex)
            } else {
                emptyList()
            }
        }

        val result = mutableListOf<Pair<Int, Int>>()

        // 1. 本周如果原本有此课，查看本周的记录是否还在 / 改变了同周位置 / 调往它周
        val thisWeekPacked = coursePlacements[placementKeyOf(course, week)]
        if (thisWeekPacked == null) {
            if (baseWeeks.contains(week)) {
                result.add(course.dayOfWeek to course.rowIndex)
            }
        } else if (thisWeekPacked in 1..999) {
            result.add(thisWeekPacked / 100 to thisWeekPacked % 100)
        } else if (thisWeekPacked >= 1000 && thisWeekPacked / 10000 == week) {
            result.add((thisWeekPacked % 10000) / 100 to thisWeekPacked % 100)
        }

        // 2. 从其他周次调入本周的课程
        coursePlacements.forEach { (key, packed) ->
            if (key.endsWith(prefix) && packed >= 1000 && packed / 10000 == week) {
                val originWeek = key.substringBefore('#').toIntOrNull()
                if (originWeek != week) {
                    result.add((packed % 10000) / 100 to packed % 100)
                }
            }
        }

        return result
    }

    /** 这门课在第 [week] 周是否有排布记录。 */
    fun hasPlacement(course: Course, week: Int): Boolean =
        coursePlacements.containsKey(placementKeyOf(course, week))

    /**
     * 课表里某门课在第 [week] 周显示的位置（星期, 行号）。
     * 若该周无此课程排布则返回 [REMOVED_POSITION]（不显示）。
     */
    fun positionOf(course: Course, week: Int): Pair<Int, Int> =
        positionsOf(course, week).firstOrNull() ?: REMOVED_POSITION

    /** 这门课是否在任意教学周被手动调过。 */
    fun isCourseMoved(course: Course): Boolean =
        coursePlacements.keys.any { it.endsWith("#${course.key}") }

    /** 这门课被手动调到的目标周（升序）。 */
    fun placementWeeksOf(course: Course): List<Int> =
        occurrencesOf(course)
            .filter { occ ->
                val packed = coursePlacements[placementKeyOf(course, occ.originWeek)]
                packed != null && (occ.week != occ.originWeek || occ.day != course.dayOfWeek || occ.row != course.rowIndex)
            }
            .map { it.week }
            .distinct()
            .sorted()

    /** 这门课被调走（源周原位消失）的周（升序）。 */
    fun removedWeeksOf(course: Course): List<Int> =
        coursePlacements.entries
            .filter { it.key.endsWith("#${course.key}") }
            .mapNotNull { entry ->
                val originWeek = entry.key.substringBefore('#').toIntOrNull() ?: return@mapNotNull null
                val packed = entry.value
                val targetWeek = when {
                    packed <= 0 -> 0
                    packed < 1000 -> originWeek
                    else -> packed / 10000
                }
                if (targetWeek != originWeek) originWeek else null
            }
            .distinct()
            .sorted()

    /** 被手动调过课的课程门数（去重）。 */
    val movedCourseCount: Int
        get() = coursePlacements.keys.map { it.substringAfter('#') }.distinct().size

    /**
     * 按「调课生效后」的位置取某格课程（用于课表网格渲染）。
     */
    fun displayCoursesAt(day: Int, row: Int, week: Int): List<Course> =
        schedule?.courses?.filter { course ->
            positionsOf(course, week).any { it.first == day && it.second == row }
        } ?: emptyList()

    /** 某个格子这一周是不是空的（可放置）。 */
    fun isSlotEmpty(day: Int, row: Int, week: Int): Boolean =
        displayCoursesAt(day, row, week).isEmpty()

    /** 某一周的 7 个日期。 */
    fun datesOf(week: Int): List<LocalDate> {
        val start = termStart ?: return emptyList()
        val monday = start.minusDays((start.dayOfWeek.value - 1).toLong()).plusWeeks((week - 1).toLong())
        return (0..6).map { monday.plusDays(it.toLong()) }
    }

    fun holidayOn(date: LocalDate): HolidayDay? = holidays?.dayOn(date)

    fun isOffDay(date: LocalDate): Boolean = holidays?.isOffDay(date) == true

    fun isMakeupWorkday(date: LocalDate): Boolean = holidays?.isMakeupWorkday(date) == true

    /** 调休补课推断结果。外部（仓库）没预先算的话，这里按放假数据自己算一份。 */
    private val effectiveGuesses: Map<LocalDate, Int> by lazy {
        if (makeupGuesses.isNotEmpty()) makeupGuesses else MakeupGuesser.guess(holidays?.days.orEmpty())
    }

    /** 调休上班日按哪一天的课来上。用户设置优先，其次自动推断。 */
    val makeupMap: Map<LocalDate, Int> by lazy {
        val merged = LinkedHashMap(effectiveGuesses)
        makeupOverrides.forEach { (date, day) ->
            if (day in 1..7) merged[date] = day else merged.remove(date)
        }
        merged
    }

    fun effectiveDayOfWeek(date: LocalDate): Int =
        makeupMap[date]?.takeIf { it in 1..7 } ?: date.dayOfWeek.value

    /** 这一周里某个星期几是否有课。 */
    fun hasCoursesOn(dayOfWeek: Int, week: Int): Boolean =
        schedule?.courses?.any { it.dayOfWeek == dayOfWeek && it.activeIn(week) } == true

    /**
     * 这一列实际按星期几的课显示。调休上班日优先按「补的那天」，
     * 补的那天这一周没课时，退回当天本来的星期（避免出现空白列）。
     */
    fun displayDayOfWeek(date: LocalDate, week: Int): Int {
        val effective = effectiveDayOfWeek(date)
        if (effective == date.dayOfWeek.value) return effective
        if (hasCoursesOn(effective, week)) return effective
        return if (hasCoursesOn(date.dayOfWeek.value, week)) date.dayOfWeek.value else effective
    }

    /** 这一列是不是在「补别的星期」，返回「补周三」这样的标签。 */
    fun makeupLabelFor(date: LocalDate, week: Int): String? {
        if (holidays?.isMakeupWorkday(date) != true) return null
        val display = displayDayOfWeek(date, week)
        if (display == date.dayOfWeek.value) return null
        return "补${MakeupGuesser.weekdayLabel(display)}"
    }

    /**
     * 某一周实际显示哪几列。
     *
     * 周一到周五固定显示；周六周日**逐列判断**：那一列有课就显示（不管是调休补课还是本来就有），
     * 没课就隐藏；两天都有就都显示。
     */
    fun visibleDays(week: Int): List<Int> = when (weekendMode) {
        WeekendMode.Always -> (1..7).toList()
        WeekendMode.Hide -> (1..5).toList()
        WeekendMode.Auto -> {
            val weekDates = datesOf(week)
            buildList {
                addAll(1..5)
                for (day in 6..7) {
                    val date = weekDates.getOrNull(day - 1)
                    val displayDay = date?.let { displayDayOfWeek(it, week) } ?: day
                    if (hasCoursesOn(displayDay, week)) add(day)
                }
            }
        }
    }

    /** 这一周是否会显示任何一列周末。 */
    fun showWeekendFor(week: Int): Boolean = visibleDays(week).any { it >= 6 }

    /** 当前查看的那一周是否显示周末。 */
    val showWeekend: Boolean get() = showWeekendFor(selectedWeek)

    /** 本年需要处理的调休上班日。 */
    val makeupWorkdays: List<HolidayDay>
        get() = holidays?.days?.values
            ?.filter { !it.isOffDay }
            ?.sortedBy { it.date }
            ?: emptyList()

    fun makeupGuessFor(date: LocalDate): Int? = effectiveGuesses[date]
}

/**
 * 课表仓库：登录、抓取、缓存、当前周次、调休数据都在这里。
 *
 * 刷新策略：
 *  - App 启动时若凭据存在且 `lastRefreshDay != 今天`，自动刷新一次（每天只自动刷一次）；
 *  - 用户随时可以手动刷新（顶栏按钮 / 设置页）。
 */
object TimetableRepository {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val refreshMutex = Mutex()
    private val holidayMutex = Mutex()

    private val _state = MutableStateFlow(AppState())

    val state: StateFlow<AppState> = _state.asStateFlow()

    private var prefsOrNull: Prefs? = null
    private var launchHandled = false

    private val prefs: Prefs
        get() = prefsOrNull ?: error("TimetableRepository.init() 还没有被调用")

    fun init(context: Context) {
        if (prefsOrNull != null) return
        val prefs = Prefs(context)
        prefsOrNull = prefs

        prefs.scheduleConfigJson?.let { ClassTimes.importJson(it) }
        val saved = Schedule.parse(prefs.scheduleJson)
        val hasCredentials = prefs.credentials() != null

        _state.value = AppState(
            jwxtBaseUrl = prefs.jwxtBaseUrl,
            scheduleConfigName = ClassTimes.configName,
            // 有缓存课表就先展示（即使登录凭据丢了，也能看已缓存的课表）。
            phase = if (hasCredentials || saved != null) Phase.Ready else Phase.LoggedOut,
            loggedIn = hasCredentials,
            studentId = prefs.studentId,
            schedule = saved,
            term = prefs.term.orEmpty(),
            fetchedAt = saved?.fetchedAt ?: prefs.fetchedAt,
            lastRefreshDay = prefs.lastRefreshDay,
            termStart = prefs.termStart,
            campus = prefs.campus,
            building = prefs.building,
            weekendMode = prefs.weekendMode,
            buildingSchemes = parseBuildingSchemes(prefs.buildingSchemesJson),
            holidayFetchedAt = prefs.holidayFetchedAt,
            holidayRegion = prefs.holidayRegion,
            holidaySource = prefs.holidaySource,
            holidayCustomMirror = prefs.holidayCustomMirror,
            dynamicColor = prefs.dynamicColor,
            classNotify = prefs.classNotifyEnabled,
            lunchBreakSlotOverride = prefs.lunchBreakSlot,
            hideEvening = prefs.hideEvening,
            coursePlacements = parseCoursePlacements(prefs.coursePlacementsJson),
        )
        reloadHolidays()
        recomputeWeeks()
    }

    /** App 进程启动时调用一次：处理每天第一次启动自动更新课表 + 调休数据。 */
    fun onLaunch() {
        if (launchHandled) return
        launchHandled = true
        scope.launch {
            recomputeWeeks()
            if (prefs.credentials() != null && prefs.lastRefreshDay != LocalDate.now().toString()) {
                doRefresh(manual = false)
            }
            if (needsHolidayRefresh()) {
                fetchHolidays(silent = true)
            }
        }
    }

    fun login(studentId: String, password: String) {
        if (_state.value.loggingIn) return
        _state.update { it.copy(loggingIn = true, loginError = null) }
        scope.launch {
            try {
                when (val result = JwxtClient(baseUrl = _state.value.jwxtBaseUrl).login(studentId, password)) {
                    is LoginResult.Failure ->
                        _state.update { it.copy(loggingIn = false, loginError = result.message) }

                    LoginResult.Success -> {
                        prefs.saveCredentials(studentId, password)
                        _state.update {
                            it.copy(
                                loggingIn = false,
                                loginError = null,
                                phase = Phase.Ready,
                                studentId = studentId,
                            )
                        }
                        doRefresh(manual = false)
                    }
                }
            } catch (t: Throwable) {
                _state.update { it.copy(loggingIn = false, loginError = friendlyError(t)) }
            }
        }
    }

    /** 手动刷新课表。 */
    fun refresh() {
        scope.launch { doRefresh(manual = true) }
    }

    /** 手动刷新调休数据。 */
    fun refreshHolidays() {
        scope.launch { fetchHolidays(silent = false) }
    }

    private suspend fun doRefresh(manual: Boolean) {
        refreshMutex.withLock {
            val credentials = prefs.credentials()
            if (credentials == null) {
                // 只有缓存课表、没有登录凭据：保留课表，提示去登录，不要把人踢回登录页。
                _state.update { it.copy(refreshing = false) }
                pushMessage("登录已失效，请重新登录后再刷新")
                return
            }
            _state.update { it.copy(refreshing = true) }

            var finished = false
            try {
                val client = JwxtClient(baseUrl = _state.value.jwxtBaseUrl)
                when (val login = client.login(credentials.first, credentials.second)) {
                    is LoginResult.Failure -> pushMessage("刷新失败：${login.message}")

                    LoginResult.Success -> {
                        val parsed = client.fetchSchedule(prefs.term)
                        val schedule = parsed.schedule
                        prefs.scheduleJson = schedule.toJson().toString()
                        prefs.fetchedAt = schedule.fetchedAt
                        prefs.lastRefreshDay = LocalDate.now().toString()
                        prefs.term = schedule.term

                        _state.update { current ->
                            current.copy(
                                schedule = schedule,
                                terms = parsed.terms.ifEmpty { current.terms },
                                term = schedule.term.ifBlank { current.term },
                                fetchedAt = schedule.fetchedAt,
                                lastRefreshDay = prefs.lastRefreshDay,
                                refreshing = false,
                                phase = Phase.Ready,
                                loggedIn = true,
                            )
                        }
                        recomputeWeeks()
                        if (manual) pushMessage("课表已更新")
                        finished = true
                    }
                }
            } catch (t: Throwable) {
                pushMessage("刷新失败：${friendlyError(t)}")
            } finally {
                if (!finished) _state.update { it.copy(refreshing = false) }
            }
        }
    }

    private suspend fun fetchHolidays(silent: Boolean) {
        holidayMutex.withLock {
            try {
                val region = prefs.holidayRegion
                val source = prefs.holidaySource
                val customMirror = prefs.holidayCustomMirror
                val termStart = prefs.termStart
                val termName = prefs.term
                val data = HolidayClient().fetchMerged(
                    region = region,
                    source = source,
                    customBaseUrl = customMirror,
                    termStart = termStart,
                    termName = termName,
                    termYear = termStart?.year,
                )
                prefs.holidayJson = data.toJson().toString()
                prefs.holidayFetchedAt = data.fetchedAt
                _state.update { it.copy(holidayFetchedAt = data.fetchedAt) }
                reloadHolidays()
                if (!silent) pushMessage("节假日数据已更新（${data.source}）")
            } catch (t: Throwable) {
                reloadHolidays()
                if (!silent) pushMessage(friendlyError(t))
            }
        }
    }

    private fun needsHolidayRefresh(): Boolean {
        val cached = HolidayYear.parse(prefs.holidayJson) ?: return true
        if (cached.days.isEmpty()) return true
        val today = LocalDate.now()
        // 若缓存中完全没有当前公历年份的任何日期，说明跨年了需要刷新
        if (cached.days.keys.none { it.year == today.year }) return true
        val ageDays = (System.currentTimeMillis() - prefs.holidayFetchedAt) / 86_400_000L
        // 10 月以后国务院陆续发布次年安排，若次年尚未获取，将刷新频次提高到 7 天一次
        val nextYearKnown = cached.days.keys.any { it.year == today.year + 1 }
        if (today.monthValue >= 10 && !nextYearKnown) return ageDays > 7
        return ageDays > 30
    }

    /** 把全国放假数据和用户手动添加的合并进状态，并重算调休补课推断。 */
    private fun reloadHolidays() {
        val overrides = HolidayOverride.parseList(prefs.holidayOverridesJson)
        val base = HolidayYear.parse(prefs.holidayJson)
        val effectiveYear = base?.year ?: prefs.termStart?.year ?: LocalDate.now().year
        val merged = mergeHolidays(base, overrides, effectiveYear)
        val guesses = merged?.let { MakeupGuesser.guess(it.days) } ?: emptyMap()
        val makeup = parseMakeupOverrides(prefs.makeupOverridesJson)
        _state.update {
            it.copy(
                holidays = merged,
                holidayOverrides = overrides,
                makeupGuesses = guesses,
                makeupOverrides = makeup,
            )
        }
    }

    /** 手动指定某个调休上班日补哪天的课；[dayOfWeek] 传 0 表示不补课。 */
    fun setMakeupMapping(date: LocalDate, dayOfWeek: Int) {
        val updated = _state.value.makeupOverrides + (date to dayOfWeek)
        prefs.makeupOverridesJson = serializeMakeupOverrides(updated)
        _state.update { it.copy(makeupOverrides = updated) }
    }

    /** 恢复成自动推断。 */
    fun clearMakeupMapping(date: LocalDate) {
        val updated = _state.value.makeupOverrides - date
        prefs.makeupOverridesJson = serializeMakeupOverrides(updated)
        _state.update { it.copy(makeupOverrides = updated) }
    }

    private fun parseMakeupOverrides(json: String?): Map<LocalDate, Int> = try {
        val obj = json?.takeIf { it.isNotBlank() }?.let { JSONObject(it) }
        val out = linkedMapOf<LocalDate, Int>()
        obj?.keys()?.forEach { key ->
            val date = runCatching { LocalDate.parse(key) }.getOrNull() ?: return@forEach
            out[date] = obj.optInt(key, 0)
        }
        out
    } catch (t: Throwable) {
        emptyMap()
    }

    private fun serializeMakeupOverrides(map: Map<LocalDate, Int>): String =
        JSONObject().apply { map.forEach { (date, day) -> put(date.toString(), day) } }.toString()

    fun addHolidayOverride(date: LocalDate, name: String, isOffDay: Boolean) {
        val list = HolidayOverride.parseList(prefs.holidayOverridesJson)
            .filterNot { it.date == date } + HolidayOverride(date, name, isOffDay)
        prefs.holidayOverridesJson = HolidayOverride.listToJson(list.sortedBy { it.date })
        reloadHolidays()
    }

    fun removeHolidayOverride(date: LocalDate) {
        val list = HolidayOverride.parseList(prefs.holidayOverridesJson).filterNot { it.date == date }
        prefs.holidayOverridesJson = HolidayOverride.listToJson(list)
        reloadHolidays()
    }

    fun setHolidayRegion(region: HolidayRegion) {
        if (prefs.holidayRegion == region && prefs.holidayJson != null) return
        prefs.holidayRegion = region
        prefs.holidayJson = null
        prefs.holidayFetchedAt = 0L
        _state.update {
            it.copy(
                holidayRegion = region,
                holidayFetchedAt = 0L,
            )
        }
        scope.launch { fetchHolidays(silent = false) }
    }

    fun setHolidaySource(source: HolidaySource) {
        if (prefs.holidaySource == source && prefs.holidayJson != null) return
        prefs.holidaySource = source
        _state.update {
            it.copy(holidaySource = source)
        }
        scope.launch { fetchHolidays(silent = false) }
    }

    fun setHolidayCustomMirror(mirror: String) {
        val trimmed = mirror.trim()
        if (prefs.holidayCustomMirror == trimmed && prefs.holidaySource == HolidaySource.CUSTOM && prefs.holidayJson != null) return
        prefs.holidayCustomMirror = trimmed
        prefs.holidaySource = HolidaySource.CUSTOM
        _state.update {
            it.copy(
                holidaySource = HolidaySource.CUSTOM,
                holidayCustomMirror = trimmed,
            )
        }
        scope.launch { fetchHolidays(silent = false) }
    }

    fun logout() {
        prefs.clearCredentials()
        _state.update {
            AppState(
                phase = Phase.LoggedOut,
                loggedIn = false,
                studentId = prefs.studentId,
                termStart = prefs.termStart,
                campus = prefs.campus,
                building = prefs.building,
                weekendMode = prefs.weekendMode,
                buildingSchemes = parseBuildingSchemes(prefs.buildingSchemesJson),
                holidayFetchedAt = prefs.holidayFetchedAt,
                holidayRegion = prefs.holidayRegion,
                holidaySource = prefs.holidaySource,
                holidayCustomMirror = prefs.holidayCustomMirror,
                dynamicColor = prefs.dynamicColor,
                classNotify = prefs.classNotifyEnabled,
                lunchBreakSlotOverride = prefs.lunchBreakSlot,
                hideEvening = prefs.hideEvening,
                coursePlacements = parseCoursePlacements(prefs.coursePlacementsJson),
            )
        }
        reloadHolidays()
    }

    /** 设置页里点「去登录」时用：回到登录页，但保留已缓存的课表数据。 */
    fun requestLogin() {
        _state.update { it.copy(phase = Phase.LoggedOut, loginError = null) }
    }

    fun selectWeek(week: Int) {
        _state.update { it.copy(selectedWeek = week.coerceIn(1, it.maxWeek), selectedWeekTouched = true) }
    }

    fun backToCurrentWeek() {
        _state.update { it.copy(selectedWeek = it.currentWeek, selectedWeekTouched = false) }
    }

    /** 设置开学第一周的周一，之后当前周次自动推算。 */
    
    fun setJwxtBaseUrl(url: String) {
        val trimmed = url.trim()
        prefs.jwxtBaseUrl = trimmed
        _state.update { it.copy(jwxtBaseUrl = trimmed) }
    }

    fun importScheduleConfig(jsonStr: String): Boolean {
        val ok = ClassTimes.importJson(jsonStr)
        if (ok) {
            prefs.scheduleConfigJson = jsonStr
            _state.update {
                it.copy(
                    scheduleConfigName = ClassTimes.configName,
                    campus = Campus.Default,
                    building = ClassTimes.defaultBuilding,
                    message = "已导入作息表：${ClassTimes.configName}",
                    messageSeq = it.messageSeq + 1,
                )
            }
        }
        return ok
    }

    fun resetScheduleConfig() {
        ClassTimes.resetToDefault()
        prefs.scheduleConfigJson = null
        prefs.lunchBreakSlot = 0
        _state.update {
            it.copy(
                scheduleConfigName = ClassTimes.configName,
                campus = Campus.Default,
                building = ClassTimes.defaultBuilding,
                lunchBreakSlotOverride = 0,
                message = "已恢复标准作息",
                messageSeq = it.messageSeq + 1,
            )
        }
    }

    fun updatePeriodTime(schemeId: String, slotIndex: Int, newStart: String, newEnd: String) {
        ClassTimes.updatePeriod(schemeId, slotIndex, newStart, newEnd)
        prefs.scheduleConfigJson = ClassTimes.exportJson()
        _state.update {
            it.copy(
                message = "已更新节次时间",
                messageSeq = it.messageSeq + 1,
            )
        }
    }

    fun saveScheme(
        schemeId: String,
        label: String,
        campus: String,
        periods: List<ClockTime>,
        isDefault: Boolean = false,
    ) {
        ClassTimes.saveScheme(schemeId, label, campus, periods, isDefault)
        prefs.scheduleConfigJson = ClassTimes.exportJson()
        _state.update {
            it.copy(
                scheduleConfigName = ClassTimes.configName,
                campus = Campus.Default,
                building = ClassTimes.defaultBuilding,
                message = "已保存作息方案：$label",
                messageSeq = it.messageSeq + 1,
            )
        }
    }

    fun deleteScheme(schemeId: String) {
        ClassTimes.deleteScheme(schemeId)
        prefs.scheduleConfigJson = ClassTimes.exportJson()
        _state.update {
            it.copy(
                scheduleConfigName = ClassTimes.configName,
                campus = Campus.Default,
                building = ClassTimes.defaultBuilding,
                message = "已删除作息方案",
                messageSeq = it.messageSeq + 1,
            )
        }
    }

    fun setTermStart(date: LocalDate) {
        val monday = mondayOf(date)
        prefs.termStart = monday
        _state.update { it.copy(termStart = monday) }
        recomputeWeeks()
    }

    fun setCampus(campus: Campus) {
        prefs.campus = campus
        val building = prefs.building?.takeIf { it.campus == campus.label } ?: campus.defaultBuilding
        prefs.building = building
        _state.update { it.copy(campus = campus, building = building) }
    }

    fun setBuilding(building: CampusBuilding) {
        prefs.building = building
        _state.update { it.copy(building = building) }
    }

    /** 手动校准某个楼栋用哪套上课时间。 */
    fun setBuildingScheme(key: String, scheme: CampusBuilding) {
        val updated = _state.value.buildingSchemes + (key to scheme)
        prefs.buildingSchemesJson = serializeBuildingSchemes(updated)
        _state.update { it.copy(buildingSchemes = updated) }
    }

    fun clearBuildingScheme(key: String) {
        val updated = _state.value.buildingSchemes - key
        prefs.buildingSchemesJson = serializeBuildingSchemes(updated)
        _state.update { it.copy(buildingSchemes = updated) }
    }

    fun setWeekendMode(mode: WeekendMode) {
        prefs.weekendMode = mode
        _state.update { it.copy(weekendMode = mode) }
    }

    fun selectTerm(term: String) {
        if (term.isBlank() || term == _state.value.term) return
        prefs.term = term
        _state.update { it.copy(term = term) }
        refresh()
    }

    fun setDynamicColor(enabled: Boolean) {
        prefs.dynamicColor = enabled
        _state.update { it.copy(dynamicColor = enabled) }
    }

    /** 开启 / 关闭上课实时通知。 */
    fun setClassNotify(enabled: Boolean) {
        prefs.classNotifyEnabled = enabled
        _state.update { it.copy(classNotify = enabled) }
    }

    /** 设置中午时段 / 午休开始节次（0 表示自动跟随相差最大的两节）。 */
    fun setLunchBreakSlot(slot: Int) {
        prefs.lunchBreakSlot = slot
        _state.update { it.copy(lunchBreakSlotOverride = slot) }
    }

    /** 是否在课表里隐藏晚自习（第 9-10 节）。 */
    fun setHideEvening(enabled: Boolean) {
        prefs.hideEvening = enabled
        _state.update { it.copy(hideEvening = enabled) }
    }

    /**
     * 调课：把某门课在第 [fromWeek] 周的那一次上课，挪到第 [toWeek] 周的星期 [day] 第 [row] 大节。
     * 若指定了 [fromDay] 和 [fromRow]，则精准移动该格子里的那次课（支持同周多节独立拖拽）。
     * 跨周时源周（[fromWeek]）的原位会被移除，而目标周原有的其他课保持完好——即「一次课换个时间/周上」，而不是覆盖那一周的课。
     */
    fun moveCourse(
        course: Course,
        fromWeek: Int,
        toWeek: Int,
        day: Int,
        row: Int,
        fromDay: Int? = null,
        fromRow: Int? = null,
    ) {
        val current = _state.value
        val map = current.coursePlacements.toMutableMap()
        val occs = current.occurrencesOf(course)
        val sourceCandidates = occs.filter { it.week == fromWeek }
        val sourceOcc = if (fromDay != null && fromRow != null) {
            sourceCandidates.firstOrNull { it.day == fromDay && it.row == fromRow }
                ?: sourceCandidates.firstOrNull()
        } else {
            sourceCandidates.firstOrNull { it.originWeek == fromWeek }
                ?: sourceCandidates.firstOrNull()
        }
        val originWeek = sourceOcc?.originWeek ?: fromWeek

        if (toWeek == originWeek && day == course.dayOfWeek && row == course.rowIndex) {
            map.remove(current.placementKeyOf(course, originWeek))
        } else {
            map[current.placementKeyOf(course, originWeek)] = toWeek * 10000 + day * 100 + row
        }
        prefs.coursePlacementsJson = serializeCoursePlacements(map)
        _state.update { it.copy(coursePlacements = map) }
    }

    /** 兼容入口：把某门课调到「第 week 周的星期 day 的第 row 大节」（同一周内移动）。 */
    fun setCoursePlacement(course: Course, week: Int, day: Int, row: Int) =
        moveCourse(course, fromWeek = week, toWeek = week, day = day, row = row)

    /** 恢复某门课在所有教学周的默认位置。 */
    fun clearCoursePlacement(course: Course) {
        val updated = _state.value.coursePlacements
            .filterKeys { !it.endsWith("#${course.key}") }
        prefs.coursePlacementsJson = serializeCoursePlacements(updated)
        _state.update { it.copy(coursePlacements = updated) }
    }

    /** 清空全部手动排布。 */
    fun clearAllCoursePlacements() {
        prefs.coursePlacementsJson = null
        _state.update { it.copy(coursePlacements = emptyMap()) }
    }

    private fun parseCoursePlacements(json: String?): Map<String, Int> = try {
        val obj = json?.takeIf { it.isNotBlank() }?.let { JSONObject(it) }
        val out = linkedMapOf<String, Int>()
        obj?.keys()?.forEach { key -> out[key] = obj.optInt(key, 0) }
        out.filterValues { it >= 0 }
    } catch (t: Throwable) {
        emptyMap()
    }

    private fun serializeCoursePlacements(map: Map<String, Int>): String =
        JSONObject().apply { map.forEach { (key, value) -> put(key, value) } }.toString()

    fun consumeMessage() {
        _state.update { it.copy(message = null) }
    }

    private fun pushMessage(text: String) {
        _state.update { it.copy(message = text, messageSeq = it.messageSeq + 1) }
    }

    private fun recomputeWeeks() {
        val today = LocalDate.now()
        _state.update { current ->
            val start = current.termStart
            val weekNumber = if (start == null) {
                1
            } else {
                ChronoUnit.WEEKS.between(mondayOf(start), mondayOf(today)).toInt() + 1
            }
            val maxWeek = current.maxWeek.coerceAtLeast(1)
            val clamped = weekNumber.coerceIn(1, maxWeek)
            current.copy(
                currentWeek = clamped,
                selectedWeek = if (current.selectedWeekTouched) current.selectedWeek.coerceIn(1, maxWeek) else clamped,
            )
        }
    }

    private fun mondayOf(date: LocalDate): LocalDate =
        date.minusDays((date.dayOfWeek.value - 1).toLong())

    private fun parseBuildingSchemes(json: String?): Map<String, CampusBuilding> = try {
        val obj = json?.takeIf { it.isNotBlank() }?.let { JSONObject(it) }
        val out = linkedMapOf<String, CampusBuilding>()
        obj?.keys()?.forEach { key ->
            val name = obj.optString(key)
            CampusBuilding.entries.firstOrNull { it.name == name }?.let { out[key] = it }
        }
        out
    } catch (t: Throwable) {
        emptyMap()
    }

    private fun serializeBuildingSchemes(map: Map<String, CampusBuilding>): String =
        JSONObject().apply { map.forEach { (key, value) -> put(key, value.name) } }.toString()

    private fun friendlyError(t: Throwable): String = when (t) {
        is SessionExpiredException -> "登录状态已失效，请重新登录"
        is ParseException -> t.message ?: "解析失败"
        is UnknownHostException -> "网络不可用，请检查网络连接"
        is SocketTimeoutException -> "连接超时，请稍后重试"
        else -> t.message?.takeIf { it.isNotBlank() } ?: "网络异常，请稍后重试"
    }
}
