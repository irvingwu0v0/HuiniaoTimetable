package com.irvingwu.huiniaotable.ui

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.automirrored.outlined.List
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material.icons.outlined.EditCalendar
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.irvingwu.huiniaotable.data.AppState
import com.irvingwu.huiniaotable.data.HolidayRegion
import com.irvingwu.huiniaotable.data.CalendarSyncManager
import com.irvingwu.huiniaotable.data.Course
import com.irvingwu.huiniaotable.data.Phase
import com.irvingwu.huiniaotable.data.TimetableRepository
import com.irvingwu.huiniaotable.notify.ClassNotifier
import com.irvingwu.huiniaotable.ui.theme.HuiniaoTimetableTheme
import com.irvingwu.huiniaotable.widget.TimetableWidgetProvider
import kotlinx.coroutines.launch


private enum class SubScreen {
    None, Debug, ClassTimes, Holidays
}

private enum class MainTab(
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
) {
    Schedule("课表", Icons.Filled.DateRange, Icons.Outlined.DateRange),
    Courses("课程", Icons.AutoMirrored.Filled.List, Icons.AutoMirrored.Outlined.List),
    Settings("设置", Icons.Filled.Settings, Icons.Outlined.Settings),
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
            Phase.LoggedOut -> LoginScreen(
                state = state,
                onLogin = { studentId, password -> TimetableRepository.login(studentId, password) },
            )

            Phase.Ready -> MainScreen(state = state, snackbarHostState = snackbarHostState)
        }
    }
}

@Composable
private fun LoadingScreen() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator(
            color = MaterialTheme.colorScheme.primary,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MainScreen(state: AppState, snackbarHostState: SnackbarHostState) {
    var tab by rememberSaveable { mutableStateOf(MainTab.Schedule) }
    var detailCourse by remember { mutableStateOf<Course?>(null) }
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
                snackbarHostState.showSnackbar("正在将全学期课程写入系统日历日程…")
                val result = CalendarSyncManager.syncToSystemCalendar(context, state)
                result.onSuccess { count ->
                    snackbarHostState.showSnackbar("已成功将 $count 节课程同步至手机系统日程！")
                }.onFailure { err ->
                    snackbarHostState.showSnackbar("写入失败：${err.message ?: "日历写入异常"}")
                }
            }
        } else {
            coroutineScope.launch {
                snackbarHostState.showSnackbar("同步失败：未授予日历写入权限")
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
                snackbarHostState.showSnackbar("未授予通知权限，无法显示上课提醒")
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
                snackbarHostState.showSnackbar("正在将全学期课程写入系统日历日程…")
                val result = CalendarSyncManager.syncToSystemCalendar(context, state)
                result.onSuccess { count ->
                    snackbarHostState.showSnackbar("已成功将 $count 节课程同步至手机系统日程！")
                }.onFailure { err ->
                    snackbarHostState.showSnackbar("写入失败：${err.message ?: "日历写入异常"}")
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
                snackbarHostState.showSnackbar("正在从系统日历中清理已导入日程…")
                val result = CalendarSyncManager.clearAllSyncedEvents(context)
                result.onSuccess { count ->
                    snackbarHostState.showSnackbar("已成功从系统日历清除 $count 条课程日程！")
                }.onFailure { err ->
                    snackbarHostState.showSnackbar("清理失败：${err.message ?: "日历清理异常"}")
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

    when (currentSubScreen) {
        SubScreen.Debug -> {
            DebugScreen(state = state, onBack = { currentSubScreen = SubScreen.None })
            return
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
            return
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
            return
        }
        SubScreen.None -> Unit
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                navigationIcon = {
                    if (tab == MainTab.Schedule) {
                        IconButton(
                            onClick = { showCalendarDialog = true },
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.EditCalendar,
                                contentDescription = "一键全部添加到日程",
                                tint = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                },
                title = {
                    when (tab) {
                        MainTab.Schedule -> {
                            Column {
                                Text(
                                    text = "课程作息表",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.SemiBold,
                                )
                                Text(
                                    text = buildString {
                                        if (state.term.isNotBlank()) append("${state.term} · ")
                                        append("第 ${state.selectedWeek} 教学周")
                                        if (state.selectedWeek == state.currentWeek) append(" (当前周)")
                                    },
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }

                        MainTab.Courses -> {
                            Column {
                                Text(
                                    text = "学期课程总览",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.SemiBold,
                                )
                                Text(
                                    text = "共检索到 ${state.schedule?.distinctCourses?.size ?: 0} 门教学课程",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }

                        MainTab.Settings -> {
                            Text(
                                text = "系统设置",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                    }
                },
                actions = {
                    if (tab != MainTab.Settings) {
                        if (state.refreshing) {
                            Box(
                                modifier = Modifier.size(48.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                            }
                        } else {
                            IconButton(onClick = { TimetableRepository.refresh() }) {
                                Icon(
                                    imageVector = Icons.Outlined.Refresh,
                                    contentDescription = "同步教务数据",
                                )
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surfaceContainer,
            ) {
                MainTab.entries.forEach { entry ->
                    val selected = (tab == entry)
                    NavigationBarItem(
                        selected = selected,
                        onClick = { tab = entry },
                        icon = {
                            Icon(
                                imageVector = if (selected) entry.selectedIcon else entry.unselectedIcon,
                                contentDescription = entry.label,
                            )
                        },
                        label = { Text(entry.label) },
                    )
                }
            }
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            // Material You Tab 切换动态平滑过渡
            AnimatedContent(
                targetState = tab,
                transitionSpec = {
                    if (targetState.ordinal > initialState.ordinal) {
                        (slideInHorizontally { width -> width / 4 } + fadeIn())
                            .togetherWith(slideOutHorizontally { width -> -width / 4 } + fadeOut())
                    } else {
                        (slideInHorizontally { width -> -width / 4 } + fadeIn())
                            .togetherWith(slideOutHorizontally { width -> width / 4 } + fadeOut())
                    }
                },
                label = "MainTabTransition",
            ) { currentTab ->
                when (currentTab) {
                    MainTab.Schedule -> ScheduleContent(
                        state = state,
                        onRefresh = { TimetableRepository.refresh() },
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
                                    "已把第 $fromWeek 周的《${course.name}》调到第 $week 周"
                                } else {
                                    "已把《${course.name}》放到第 $week 周"
                                }
                                snackbarHostState.showSnackbar(tip)
                            }
                        },
                        onCancelPlacement = { pendingPlacement = null },
                    )

                    MainTab.Courses -> CoursesContent(
                        state = state,
                        onRefresh = { TimetableRepository.refresh() },
                        onCourseClick = { detailCourse = it },
                        onClearPlacements = { TimetableRepository.clearAllCoursePlacements() },
                    )

                    MainTab.Settings -> SettingsContent(
                        state = state,
                        onRefresh = { TimetableRepository.refresh() },
                        onLogin = { TimetableRepository.requestLogin() },
                        onSyncCalendar = { showCalendarDialog = true },
                        onSelectTerm = { TimetableRepository.selectTerm(it) },
                        onSetTermStart = { TimetableRepository.setTermStart(it) },
                        onSelectCampus = { TimetableRepository.setCampus(it) },
                        onSelectBuilding = { TimetableRepository.setBuilding(it) },
                        onSelectBuildingScheme = { key, scheme ->
                            TimetableRepository.setBuildingScheme(key, scheme)
                        },
                        onSelectWeekendMode = { TimetableRepository.setWeekendMode(it) },
                        onHideEvening = { TimetableRepository.setHideEvening(it) },
                        onRefreshHolidays = { TimetableRepository.refreshHolidays() },
                        onAddHolidayOverride = { date, name, off ->
                            TimetableRepository.addHolidayOverride(date, name, off)
                        },
                        onRemoveHolidayOverride = { TimetableRepository.removeHolidayOverride(it) },
                        onSetMakeup = { date, weekday -> TimetableRepository.setMakeupMapping(date, weekday) },
                        onClearMakeup = { TimetableRepository.clearMakeupMapping(it) },
                        onDynamicColor = { TimetableRepository.setDynamicColor(it) },
                        onClassNotify = requestClassNotify,
                        onOpenDebug = { currentSubScreen = SubScreen.Debug },
                        onLogout = { TimetableRepository.logout() },
                        onSetJwxtBaseUrl = { TimetableRepository.setJwxtBaseUrl(it) },
                        onOpenClassTimes = { currentSubScreen = SubScreen.ClassTimes },
                        onOpenHolidays = { currentSubScreen = SubScreen.Holidays },
                    )
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

    if (showCalendarDialog) {
        CalendarSyncDialog(
            state = state,
            onDismissRequest = { showCalendarDialog = false },
            onConfirmSync = executeCalendarSync,
            onClearSync = executeCalendarClear,
            onNavigateToSetTermStart = {
                tab = MainTab.Settings
            },
        )
    }
}
