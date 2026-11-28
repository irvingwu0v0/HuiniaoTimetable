package com.irvingwu.huiniaotable.ui

import com.irvingwu.huiniaotable.ui.icons.AppIcons

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.tween
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.input.nestedscroll.nestedScroll
import top.yukonga.miuix.kmp.anim.SinOutEasing
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import top.yukonga.miuix.kmp.basic.CircularProgressIndicator
import top.yukonga.miuix.kmp.basic.NavigationBar
import top.yukonga.miuix.kmp.basic.NavigationBarItem
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SnackbarHost
import top.yukonga.miuix.kmp.basic.SnackbarHostState
import top.yukonga.miuix.kmp.basic.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.irvingwu.huiniaotable.t
import com.irvingwu.huiniaotable.data.AppState
import com.irvingwu.huiniaotable.data.CalendarSyncManager
import com.irvingwu.huiniaotable.data.Course
import com.irvingwu.huiniaotable.data.Phase
import com.irvingwu.huiniaotable.data.TimetableRepository
import com.irvingwu.huiniaotable.notify.ClassNotifier
import com.irvingwu.huiniaotable.ui.theme.HuiniaoTimetableTheme
import com.irvingwu.huiniaotable.widget.TimetableWidgetProvider
import kotlinx.coroutines.launch


private enum class SubScreen {
    None, Debug, ClassTimes, Holidays, Import
}

private enum class MainTab(val icon: ImageVector) {
    Schedule(AppIcons.DateRange),
    Courses(AppIcons.List),
    Settings(AppIcons.Settings),
    ;

    val label: String
        get() = when (this) {
            Schedule -> t("课表", "Timetable")
            Courses -> t("课程", "Courses")
            Settings -> t("设置", "Settings")
        }
}

@Composable
fun HuiniaoApp() {
    val state by TimetableRepository.state.collectAsStateWithLifecycle()

    HuiniaoTimetableTheme(dynamicColor = state.dynamicColor) {
        val snackbarHostState = remember { SnackbarHostState() }

        // 每天第一次启动时自动执行教务系统数据同步
        LaunchedEffect(Unit) { TimetableRepository.onLaunch() }

        // 课表 / 周次 / 开学日期 / 调休变化时，同步刷新桌面控件
        val context = LocalContext.current
        LaunchedEffect(
            state.schedule,
            state.currentWeek,
            state.termStart,
            state.holidays,
            state.makeupOverrides,
            state.classNotify,
            state.coursePlacements,
            state.buildingSchemes,
        ) {
            TimetableWidgetProvider.updateAll(context)
            ClassNotifier.refresh(context)
        }

        LaunchedEffect(state.messageSeq) {
            val message = state.message ?: return@LaunchedEffect
            snackbarHostState.showSnackbar(message)
            TimetableRepository.consumeMessage()
        }

        when (state.phase) {
            Phase.Loading -> LoadingScreen()
            Phase.LoggedOut, Phase.Ready -> MainScreen(state, snackbarHostState)
        }
    }
}

@Composable
private fun LoadingScreen() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator()
    }
}

@Composable
private fun MainScreen(
    state: AppState,
    snackbarHostState: SnackbarHostState,
) {
    var tab by rememberSaveable { mutableStateOf(MainTab.Schedule) }
    var detailCourse by remember { mutableStateOf<Course?>(null) }
    var pickTermStart by rememberSaveable { mutableStateOf(false) }
    var showCalendarDialog by remember { mutableStateOf(false) }
    var currentSubScreen by rememberSaveable { mutableStateOf(SubScreen.None) }
    // 调课中的课程 + 源周次（把「哪一周的那一次课」挪走）
    var pendingPlacement by remember { mutableStateOf<Pair<Course, Int>?>(null) }

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val calendarPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
    ) { permissions ->
        val granted = permissions[Manifest.permission.WRITE_CALENDAR] == true
        if (granted) {
            coroutineScope.launch {
                snackbarHostState.showSnackbar(
                    t("正在将全学期课程写入系统日历日程…", "Adding this semester's classes to the system calendar…"),
                )
                val result = CalendarSyncManager.syncToSystemCalendar(context, state)
                result.onSuccess { count ->
                    snackbarHostState.showSnackbar(
                        t("已成功将 $count 节课程同步至手机系统日程！", "Synced $count class sessions to the calendar."),
                    )
                }.onFailure { err ->
                    snackbarHostState.showSnackbar(
                        t("写入失败：${err.message ?: "日历写入异常"}", "Sync failed: ${err.message ?: "Calendar error"}"),
                    )
                }
            }
        } else {
            coroutineScope.launch {
                snackbarHostState.showSnackbar(
                    t("同步失败：未授予日历写入权限", "Sync failed: calendar write permission not granted"),
                )
            }
        }
    }

    val classNotifyPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) {
            TimetableRepository.setClassNotify(true)
            ClassNotifier.refresh(context)
        } else {
            coroutineScope.launch {
                snackbarHostState.showSnackbar(
                    t("未授予通知权限，无法显示上课提醒", "Notification permission is required for class reminders"),
                )
            }
        }
    }

    val requestClassNotify: (Boolean) -> Unit = { enabled ->
        when {
            !enabled -> {
                TimetableRepository.setClassNotify(false)
                ClassNotifier.refresh(context)
            }

            ClassNotifier.canPost(context) -> {
                TimetableRepository.setClassNotify(true)
                ClassNotifier.refresh(context)
            }

            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> {
                classNotifyPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }

            else -> {
                TimetableRepository.setClassNotify(true)
                ClassNotifier.refresh(context)
            }
        }
    }

    val executeCalendarSync: () -> Unit = {
        if (CalendarSyncManager.hasCalendarPermission(context)) {
            coroutineScope.launch {
                snackbarHostState.showSnackbar(
                    t("正在将全学期课程写入系统日历日程…", "Adding this semester's classes to the system calendar…"),
                )
                val result = CalendarSyncManager.syncToSystemCalendar(context, state)
                result.onSuccess { count ->
                    snackbarHostState.showSnackbar(
                        t("已成功将 $count 节课程同步至手机系统日程！", "Synced $count class sessions to the calendar."),
                    )
                }.onFailure { err ->
                    snackbarHostState.showSnackbar(
                        t("写入失败：${err.message ?: "日历写入异常"}", "Sync failed: ${err.message ?: "Calendar error"}"),
                    )
                }
            }
        } else {
            calendarPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.READ_CALENDAR,
                    Manifest.permission.WRITE_CALENDAR,
                ),
            )
        }
    }

    val executeCalendarClear: () -> Unit = {
        if (CalendarSyncManager.hasCalendarPermission(context)) {
            coroutineScope.launch {
                snackbarHostState.showSnackbar(
                    t("正在从系统日历中清理已导入日程…", "Removing imported classes from the system calendar…"),
                )
                val result = CalendarSyncManager.clearAllSyncedEvents(context)
                result.onSuccess { count ->
                    snackbarHostState.showSnackbar(
                        t("已成功从系统日历清除 $count 条课程日程！", "Removed $count class events from the calendar."),
                    )
                }.onFailure { err ->
                    snackbarHostState.showSnackbar(
                        t("清理失败：${err.message ?: "日历清理异常"}", "Cleanup failed: ${err.message ?: "Calendar error"}"),
                    )
                }
            }
        } else {
            calendarPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.READ_CALENDAR,
                    Manifest.permission.WRITE_CALENDAR,
                ),
            )
        }
    }

    val pageState = rememberSaveableStateHolder()
    AnimatedContent(
        targetState = currentSubScreen,
        modifier = Modifier.fillMaxSize(),
        transitionSpec = {
            val direction = if (targetState == SubScreen.None) -1 else 1
            (slideInHorizontally(tween(300, easing = SinOutEasing)) { direction * it / 3 } + fadeIn(tween(220))) togetherWith
                (slideOutHorizontally(tween(250, easing = SinOutEasing)) { -direction * it / 5 } + fadeOut(tween(180)))
        },
        label = "PageTransition",
    ) { page ->
        when (page) {
            SubScreen.Debug -> {
                DebugScreen(state = state, onBack = { currentSubScreen = SubScreen.None })
            }
            SubScreen.ClassTimes -> {
                ClassTimesScreen(
                    state = state,
                    onBack = { currentSubScreen = SubScreen.None },
                    onSelectCampus = { TimetableRepository.setCampus(it) },
                    onSelectBuilding = { TimetableRepository.setBuilding(it) },
                    onSelectBuildingScheme = { key, scheme -> TimetableRepository.setBuildingScheme(key, scheme) },
                    onImportConfig = { TimetableRepository.importScheduleConfig(it) },
                    onResetConfig = { TimetableRepository.resetScheduleConfig() },
                    onUpdatePeriodTime = { id, slot, s, e -> TimetableRepository.updatePeriodTime(id, slot, s, e) },
                    onSelectLunchBreakSlot = { TimetableRepository.setLunchBreakSlot(it) },
                    onSaveScheme = { id, label, campus, periods, isDefault ->
                        TimetableRepository.saveScheme(id, label, campus, periods, isDefault)
                    },
                    onDeleteScheme = { id ->
                        TimetableRepository.deleteScheme(id)
                    },
                )
            }
            SubScreen.Holidays -> {
                HolidaysScreen(
                    state = state,
                    onBack = { currentSubScreen = SubScreen.None },
                    onSelectWeekendMode = { TimetableRepository.setWeekendMode(it) },
                    onSelectHolidayRegion = { TimetableRepository.setHolidayRegion(it) },
                    onSelectHolidaySource = { TimetableRepository.setHolidaySource(it) },
                    onSetHolidayCustomMirror = { TimetableRepository.setHolidayCustomMirror(it) },
                    onRefreshHolidays = { TimetableRepository.refreshHolidays() },
                    onAddHolidayOverride = { d, n, o -> TimetableRepository.addHolidayOverride(d, n, o) },
                    onRemoveHolidayOverride = { TimetableRepository.removeHolidayOverride(it) },
                    onSetMakeup = { d, w -> TimetableRepository.setMakeupMapping(d, w) },
                    onClearMakeup = { TimetableRepository.clearMakeupMapping(it) },
                )
            }
            SubScreen.Import -> WebImportScreen(state, onBack = { currentSubScreen = SubScreen.None }, onSave = TimetableRepository::saveImport)
            SubScreen.None -> pageState.SaveableStateProvider("main") {
                val scrollBehavior = androidx.compose.runtime.key(tab) { MiuixScrollBehavior() }
                Scaffold(
                    snackbarHost = { SnackbarHost(snackbarHostState) },
                    topBar = {
                        if (tab != MainTab.Schedule || state.schedule == null) TopAppBar(title = tab.label, scrollBehavior = scrollBehavior)
                    },
                    bottomBar = {
                        NavigationBar {
                            MainTab.entries.forEach { entry ->
                                NavigationBarItem(
                                    selected = tab == entry,
                                    onClick = { tab = entry },
                                    icon = entry.icon,
                                    label = entry.label,
                                )
                            }
                        }
                    },
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                            .then(if (tab == MainTab.Schedule) Modifier else Modifier.nestedScroll(scrollBehavior.nestedScrollConnection)),
                    ) {
                        // Tab 切换动态平滑过渡
                        AnimatedContent(
                            targetState = tab,
                            transitionSpec = {
                                fadeIn(tween(180, easing = SinOutEasing)) togetherWith fadeOut(tween(120))
                            },
                            label = "MainTabTransition",
                        ) { currentTab ->
                            when (currentTab) {
                                MainTab.Schedule -> ScheduleContent(
                                    state = state,
                                    onRefresh = { currentSubScreen = SubScreen.Import },
                                    onSelectWeek = { TimetableRepository.selectWeek(it) },
                                    onToday = { TimetableRepository.backToCurrentWeek() },
                                    onSetTermStart = { TimetableRepository.setTermStart(it) },
                                    onCourseClick = { detailCourse = it },
                                    placementCourse = pendingPlacement?.first,
                                    onPlaceCourse = { course, week, day, row, fromDay, fromRow ->
                                        // 调课模式：源周从 pendingPlacement 取；拖拽：源周与目标周相同
                                        val sourceWeek = pendingPlacement
                                            ?.takeIf { it.first.key == course.key }
                                            ?.second
                                        val fromWeek = sourceWeek ?: week
                                        TimetableRepository.moveCourse(course, fromWeek, week, day, row, fromDay, fromRow)
                                        if (sourceWeek != null) pendingPlacement = null
                                        coroutineScope.launch {
                                            val tip = if (sourceWeek != null && fromWeek != week) {
                                                t(
                                                    "已把第 $fromWeek 周的《${course.name}》调到第 $week 周",
                                                    "Moved ${course.name} from week $fromWeek to week $week",
                                                )
                                            } else {
                                                t(
                                                    "已把《${course.name}》放到第 $week 周",
                                                    "Placed ${course.name} in week $week",
                                                )
                                            }
                                            snackbarHostState.showSnackbar(tip)
                                        }
                                    },
                                    onCancelPlacement = { pendingPlacement = null },
                                )

                                MainTab.Courses -> CoursesContent(
                                    state = state,
                                    onRefresh = { currentSubScreen = SubScreen.Import },
                                    onCourseClick = { detailCourse = it },
                                    onClearPlacements = { TimetableRepository.clearAllCoursePlacements() },
                                )

                                MainTab.Settings -> SettingsContent(
                                    state = state,
                                    onImport = { currentSubScreen = SubScreen.Import },
                                    onSyncCalendar = { showCalendarDialog = true },
                                    onSetTermStart = { TimetableRepository.setTermStart(it) },
                                    onHideEvening = { TimetableRepository.setHideEvening(it) },
                                    onDynamicColor = { TimetableRepository.setDynamicColor(it) },
                                    onClassNotify = requestClassNotify,
                                    onOpenDebug = { currentSubScreen = SubScreen.Debug },
                                    onClearTimetable = { TimetableRepository.clearImportedSchedule() },
                                    onOpenClassTimes = { currentSubScreen = SubScreen.ClassTimes },
                                    onOpenHolidays = { currentSubScreen = SubScreen.Holidays },
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    CourseDetailSheet(
        course = detailCourse,
        state = state,
        onDismiss = { detailCourse = null },
        onResetPosition = { TimetableRepository.clearCoursePlacement(it) },
        onStartPlacement = { course, week ->
            val fromWeek = state.selectedWeek
            TimetableRepository.selectWeek(week)
            pendingPlacement = course to fromWeek
            detailCourse = null
            tab = MainTab.Schedule
        },
    )

    CalendarSyncDialog(
        show = showCalendarDialog,
        state = state,
        onDismissRequest = { showCalendarDialog = false },
        onConfirmSync = executeCalendarSync,
        onClearSync = executeCalendarClear,
        onNavigateToSetTermStart = { pickTermStart = true },
    )
    if (pickTermStart) PickDateDialog(
        title = t("开学日期", "Term start"), current = state.termStart,
        onConfirm = { TimetableRepository.setTermStart(it); pickTermStart = false },
        onDismiss = { pickTermStart = false },
    )
}
