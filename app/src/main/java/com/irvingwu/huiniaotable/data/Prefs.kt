package com.irvingwu.huiniaotable.data

import android.content.Context
import java.time.LocalDate

/** 本地偏好与缓存。全部保存在应用私有目录，不对外暴露。 */
class Prefs(context: Context) {

    private val sp = context.applicationContext.getSharedPreferences("huiniao_timetable", Context.MODE_PRIVATE)

    var studentId: String
        get() = sp.getString(KEY_STUDENT_ID, "").orEmpty()
        set(value) = sp.edit().putString(KEY_STUDENT_ID, value).apply()

    var passwordEncrypted: String?
        get() = sp.getString(KEY_PASSWORD, null)
        set(value) = sp.edit().putString(KEY_PASSWORD, value).apply()

    var scheduleJson: String?
        get() = sp.getString(KEY_SCHEDULE, null)
        set(value) = sp.edit().putString(KEY_SCHEDULE, value).apply()

    /** 上次自动刷新的日期（本地时区 yyyy-MM-dd），用来判断「今天是否已经刷新过」。 */
    var lastRefreshDay: String?
        get() = sp.getString(KEY_LAST_REFRESH_DAY, null)
        set(value) = sp.edit().putString(KEY_LAST_REFRESH_DAY, value).apply()

    var fetchedAt: Long
        get() = sp.getLong(KEY_FETCHED_AT, 0L)
        set(value) = sp.edit().putLong(KEY_FETCHED_AT, value).apply()

    var term: String?
        get() = sp.getString(KEY_TERM, null)
        set(value) = sp.edit().putString(KEY_TERM, value).apply()

    /** 开学第一周的周一。当前周次由它和今天自动算出。 */
    var termStart: LocalDate?
        get() = sp.getString(KEY_TERM_START, null)?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
        set(value) = sp.edit().putString(KEY_TERM_START, value?.toString()).apply()

    var jwxtBaseUrl: String
        get() = sp.getString(KEY_JWXT_BASE_URL, DEFAULT_JWXT_BASE_URL) ?: DEFAULT_JWXT_BASE_URL
        set(value) = sp.edit().putString(KEY_JWXT_BASE_URL, value.trim()).apply()

    var scheduleConfigJson: String?
        get() = sp.getString(KEY_SCHEDULE_CONFIG, null)
        set(value) = sp.edit().putString(KEY_SCHEDULE_CONFIG, value).apply()

    var campus: Campus
        get() = sp.getString(KEY_CAMPUS, null)?.let { label -> Campus(label) } ?: Campus.Default
        set(value) = sp.edit().putString(KEY_CAMPUS, value.label).apply()

    /** 上课时间方案（哪栋楼）。为空表示用校区的默认方案。 */
    var building: CampusBuilding?
        get() = sp.getString(KEY_BUILDING, null)?.let { id -> ClassTimes.findBuilding(id) }
        set(value) = sp.edit().putString(KEY_BUILDING, value?.id).apply()

    /** 用户手动校准的「楼栋 -> 上课时间方案」。 */
    var buildingSchemesJson: String?
        get() = sp.getString(KEY_BUILDING_SCHEMES, null)
        set(value) = sp.edit().putString(KEY_BUILDING_SCHEMES, value).apply()

    /** 用户手动指定的「调休上班日 -> 补周几」。0 表示不补课。 */
    var makeupOverridesJson: String?
        get() = sp.getString(KEY_MAKEUP_OVERRIDES, null)
        set(value) = sp.edit().putString(KEY_MAKEUP_OVERRIDES, value).apply()

    var weekendMode: WeekendMode
        get() = sp.getString(KEY_WEEKEND_MODE, null)
            ?.let { name -> WeekendMode.entries.firstOrNull { it.name == name } }
            ?: WeekendMode.Auto
        set(value) = sp.edit().putString(KEY_WEEKEND_MODE, value.name).apply()

    var holidayJson: String?
        get() = sp.getString(KEY_HOLIDAY_JSON, null)
        set(value) = sp.edit().putString(KEY_HOLIDAY_JSON, value).apply()

    var holidayFetchedAt: Long
        get() = sp.getLong(KEY_HOLIDAY_FETCHED_AT, 0L)
        set(value) = sp.edit().putLong(KEY_HOLIDAY_FETCHED_AT, value).apply()

    /** 用户手动添加的本地假期/调休覆盖（校庆、校运会调休等）。 */
    var holidayOverridesJson: String?
        get() = sp.getString(KEY_HOLIDAY_OVERRIDES, null)
        set(value) = sp.edit().putString(KEY_HOLIDAY_OVERRIDES, value).apply()

    var holidayRegion: HolidayRegion
        get() = HolidayRegion.fromCode(sp.getString(KEY_HOLIDAY_REGION, null))
        set(value) = sp.edit().putString(KEY_HOLIDAY_REGION, value.code).apply()

    var holidaySource: HolidaySource
        get() = HolidaySource.fromCode(sp.getString(KEY_HOLIDAY_SOURCE, null))
        set(value) = sp.edit().putString(KEY_HOLIDAY_SOURCE, value.code).apply()

    var holidayCustomMirror: String
        get() = sp.getString(KEY_HOLIDAY_CUSTOM_MIRROR, "") ?: ""
        set(value) = sp.edit().putString(KEY_HOLIDAY_CUSTOM_MIRROR, value).apply()

    var dynamicColor: Boolean
        get() = sp.getBoolean(KEY_DYNAMIC_COLOR, true)
        set(value) = sp.edit().putBoolean(KEY_DYNAMIC_COLOR, value).apply()

    /** 是否开启上课实时通知（课前 15 分钟提醒 + 上课中的实时进度）。 */
    var classNotifyEnabled: Boolean
        get() = sp.getBoolean(KEY_CLASS_NOTIFY, false)
        set(value) = sp.edit().putBoolean(KEY_CLASS_NOTIFY, value).apply()

    /** 中午时段 / 午休开始节次（0 表示自动跟随相差最大的两节）。 */
    var lunchBreakSlot: Int
        get() = sp.getInt(KEY_LUNCH_BREAK_SLOT, 0)
        set(value) = sp.edit().putInt(KEY_LUNCH_BREAK_SLOT, value).apply()

    /** 是否在课表里隐藏晚自习（第 9-10 节）。 */
    var hideEvening: Boolean
        get() = sp.getBoolean(KEY_HIDE_EVENING, false)
        set(value) = sp.edit().putBoolean(KEY_HIDE_EVENING, value).apply()

    /** 用户手动拖拽调整的课程位置：courseKey -> 打包后的「星期*100+行号」。 */
    var coursePlacementsJson: String?
        get() = sp.getString(KEY_COURSE_PLACEMENTS, null)
        set(value) = sp.edit().putString(KEY_COURSE_PLACEMENTS, value).apply()

    fun credentials(): Pair<String, String>? {
        val id = studentId
        val password = SecureStore.decrypt(passwordEncrypted)
        return if (id.isNotBlank() && !password.isNullOrBlank()) id to password else null
    }

    fun saveCredentials(studentId: String, password: String) {
        this.studentId = studentId
        passwordEncrypted = SecureStore.encrypt(password)
    }

    fun clearCredentials() {
        sp.edit()
            .remove(KEY_PASSWORD)
            .remove(KEY_SCHEDULE)
            .remove(KEY_LAST_REFRESH_DAY)
            .remove(KEY_FETCHED_AT)
            .remove(KEY_TERM)
            .apply()
    }

    companion object {
        const val DEFAULT_JWXT_BASE_URL = ""
        private const val KEY_JWXT_BASE_URL = "jwxt_base_url"
        private const val KEY_SCHEDULE_CONFIG = "schedule_config_json"

        const val KEY_STUDENT_ID = "student_id"
        const val KEY_PASSWORD = "password_encrypted"
        const val KEY_SCHEDULE = "schedule_json"
        const val KEY_LAST_REFRESH_DAY = "last_refresh_day"
        const val KEY_FETCHED_AT = "fetched_at"
        const val KEY_TERM = "term"
        const val KEY_TERM_START = "term_start"
        const val KEY_CAMPUS = "campus"
        const val KEY_BUILDING = "building"
        const val KEY_BUILDING_SCHEMES = "building_schemes"
        const val KEY_WEEKEND_MODE = "weekend_mode"
        const val KEY_HOLIDAY_JSON = "holiday_json"
        const val KEY_HOLIDAY_FETCHED_AT = "holiday_fetched_at"
        const val KEY_HOLIDAY_OVERRIDES = "holiday_overrides"
        const val KEY_HOLIDAY_REGION = "holiday_region"
        const val KEY_HOLIDAY_SOURCE = "holiday_source"
        const val KEY_HOLIDAY_CUSTOM_MIRROR = "holiday_custom_mirror"
        const val KEY_MAKEUP_OVERRIDES = "makeup_overrides"
        const val KEY_DYNAMIC_COLOR = "dynamic_color"
        const val KEY_CLASS_NOTIFY = "class_notify"
        const val KEY_LUNCH_BREAK_SLOT = "lunch_break_slot"
        const val KEY_HIDE_EVENING = "hide_evening"
        const val KEY_COURSE_PLACEMENTS = "course_placements"
    }
}
