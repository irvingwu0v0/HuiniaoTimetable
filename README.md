# 灰鸟课表 · HuiniaoTimetable

灰鸟课表是一款遵循 **Google Material You (Material 3)** 规范设计的现代高校 Android 课表客户端。支持通用高校教务系统端点自由配置、统一身份认证登录、自动抓取与本地缓存、多校区/多楼宇作息时间管理、全学期系统日历同步、以及 Android 16 胶囊锁屏上课实时更新通知（Live Updates）。

[![Platform](https://img.shields.io/badge/Platform-Android%2010%2B-brightgreen.svg)](https://developer.android.com/)
[![License](https://img.shields.io/badge/License-GPL%20v3-blue.svg)](LICENSE)
[![Design](https://img.shields.io/badge/Design-Material%20You%20(M3)-purple.svg)](https://m3.material.io/)
[![Build & Release](https://github.com/irvingwu0v0/HuiniaoTimetable/actions/workflows/release.yml/badge.svg)](https://github.com/irvingwu0v0/HuiniaoTimetable/actions/workflows/release.yml)

---

## 核心特性

### 1. Material You 动态设计与课程配色
- **动态取色与自适应**：原生适配 Android 12+ Monet 动态色彩引擎，提供亮色与深色主题无缝切换；同时支持关闭动态色彩，采用灰鸟专属基准色盘。
- **一课一色 (Per-Course Hue)**：同一门课程在课表、桌面小组件、日程同步与通知中保持全局统一的色相；基于 Material 3 色调板，亮色下容器色调约为 Tone 90 / 文字 Tone 10，深色下为 Tone 30 / 文字 Tone 90，确保通过 WCAG AA 对比度标准。

### 2. 上课实时通知（Live Updates）与课前提醒
- **智能课前提醒**：开课前 15 分钟触发通知提醒，展示课程名称、授课教师、教室与上课时间。
- **上课进度常驻条**：上课期间常驻一条进度卡片，显示当前剩余时长与进度条；在支持的设备（Android 16+）接入系统 Promoted Notifications 状态栏胶囊与圆形跟踪球（Tracker Icon）。
- **权限与节假日感知**：实时通知默认关闭，开启时动态申请通知权限；法定放假停课日自动静音不提醒，调休补课日按补课计划准时推送。

### 3. 直观流畅的课表视图
- **全屏网格与手势翻周**：单屏网格完整铺满，无需纵向滚动；支持左右横向滑动手势无缝切换教学周。
- **日期带月份**：星期表头下方清晰标注具体日期（如 `10/9`）。
- **隐藏晚自习**：设置页提供一键开关，支持隐藏第 9-10 节（最后两节时段）。
- **长按拖动与调课排布**：
  - **当周拖动**：长按任意课程卡片即可浮起拖拽，放置到当周的空白节次格子中（附带缩放与回弹动画）。
  - **跨周调课**：课程详情页点击调课图标，唤起课表周次选择弹窗；选择目标周后点击任意空格放置，源周自动腾出原位置（换时间上课），目标周原有排课完整保留。
  - **一键恢复**：在课程列表页支持「恢复所有调课」，一键还原教务初始排课。

### 4. 灵活的作息时间与多校区楼宇方案
- **标准作息内置**：默认内置高校通用标准作息（08:00 开课，两节一连堂，共 10 节课）。
- **图形化方案编辑**：支持新建、修改各校区与特定楼宇的作息方案，每节课起止时间均配备直观的时间选择器。
- **自动识别与匹配**：导入/导出 JSON 配置不再硬编码楼栋别名，系统依据课表中解析到的真实建筑名称与校区关键词动态进行最佳方案打分与匹配。
- **午休时段个性化**：主设置行直观显示「第 X 节后」，支持自动推算相邻两节课最大间隔，亦可手动指定哪节课后切入午休。

### 5. 放假安排与调休编排
- **多源节假日同步**：支持国务院公布方案，并独家支持五个少数民族自治区（新疆、西藏、宁夏、广西、内蒙古）特色放假方案。
- **源切换与网络加速**：支持快速切换放假数据源，并内置加速镜像代理选择。
- **调休补周几校准**：点击调休日期可弹窗选择所补星期、设为休息或恢复系统智能推算。

### 6. 系统日程同步与桌面小组件
- **日历一键导入与撤销**：根据全学期排课、校区作息与调休安排，全量写入系统日历（Calendar Provider），并附带课前 15 分钟系统闹钟提醒；支持在同步弹窗内「一键清除已导入日程」。
- **桌面今日课表组件**：采用 Material You 动态取色规范，支持横竖自由缩放拉伸，已结课程灰色弱化、正在进行或下一节高亮强调，跨节次自动刷新。

### 7. 隐私安全与开发者调试
- **本地硬件安全隔离**：用户学号与密码仅存储在本地设备私有沙盒中，密码通过 **Android KeyStore** 硬件密钥（AES-GCM-256）加密保护。
- **端点自由配置**：支持配置您所在高校的强智/通用教务系统端点 URL，无任何第三方中心服务器或隐私收集。
- **开发者调试模式**：在设置页面连续点击应用图标 10 次，解锁开发者面板，可立即触发 Live Updates 实时通知预览，便于离线测试与调试。

---

## 兼容性与运行环境

- **最低支持**：Android 10 (API 29)
- **推荐环境**：Android 12+ (支持 Monet 动态主题) / Android 16+ (完整支持 Live Updates 状态栏胶囊)
- **构建环境**：JDK 17+、Android SDK 36

---

## 构建与测试

```bash
# 1. 配置 JDK 路径（示例）
export JAVA_HOME=/opt/android-studio/jbr

# 2. 运行本地单元测试（课表解析、调课排布、作息匹配、节假日客户端等）
./gradlew :app:testDebugUnitTest

# 3. 编译 Debug APK
./gradlew :app:assembleDebug

# 4. 安装到连接的设备
./gradlew :app:installDebug
```

产出 APK 文件位于：`app/build/outputs/apk/debug/app-debug.apk`。

### 自动化构建发布 (GitHub Actions CI/CD)
本项目已配置 GitHub Actions 自动化流水线 (`.github/workflows/release.yml`)。每次推送（`push`）至 `main` 分支时，将自动执行编译、测试、签名打包并创建 GitHub Release 发布：
- **安装包命名规范**：`Huiniao_1.X_XXXXXX(commit号)_(时间).apk`（例如：`Huiniao_1.1_d3b81cd_20261010_165030.apk`）
- **发布页面**：前往 [Releases 页面](https://github.com/irvingwu0v0/HuiniaoTimetable/releases) 即可直接下载各版本最新签名的 APK。

---

## 目录结构

```
app/src/main/java/com/irvingwu/huiniaotable/
├── MainActivity.kt               # 顶层入口与 Compose 导航容器
├── data/                         # 数据层
│   ├── CalendarSyncManager.kt   # 系统日历 Calendar Provider 读写与全量撤销
│   ├── ClassTimes.kt            # 作息方案管理、楼栋智能匹配与 JSON 导入导出
│   ├── HolidayClient.kt         # 节假日与调休远端检索（全国/自治区）
│   ├── HttpClient.kt            # 网络通信基础层与会话 Cookie 隔离
│   ├── JwxtClient.kt            # 高校教务系统接口协议对接
│   ├── KbParser.kt              # 课表 HTML 结构解析器
│   ├── MakeupGuesser.kt         # 调休补周几推算逻辑
│   ├── Models.kt                # 核心数据模型与状态封装
│   ├── Prefs.kt                 # 用户偏好与本地存储
│   ├── SecureStore.kt           # Android KeyStore 硬件加密存储
│   ├── TeacherNames.kt          # 教师姓名清洗与职称剔除
│   └── TimetableRepository.kt   # 统一仓库状态流与生命周期管理
├── notify/                       # 通知层
│   ├── ClassNotifier.kt         # 课前提醒与 Live Updates 实时进度通知构建
│   └── ClassNotifierReceiver.kt # 定时闹钟与通知广播接收器
├── ui/                           # UI 表现层 (Jetpack Compose)
│   ├── AppRoot.kt               # 界面主路由与手势骨架
│   ├── CalendarSyncDialog.kt    # 日历同步与一键撤销交互对话框
│   ├── ClassTimesScreen.kt      # 作息方案与多校区楼宇独立管理页面
│   ├── CoursesScreen.kt         # 课程全景列表与无排课记录展示
│   ├── DatePickers.kt           # 日期与时间选择器组件
│   ├── DebugScreen.kt           # 开发者调试入口与通知预览界面
│   ├── HolidaysScreen.kt        # 节假日方案与调休编排页面
│   ├── LoginScreen.kt           # 统一教务认证登入界面
│   ├── ScheduleScreen.kt        # 核心网格课表、长按拖动与调课视图
│   ├── SelectionDialog.kt       # 规范化单选/分组选择对话框
│   ├── SettingsScreen.kt        # 系统全局设置视图
│   └── theme/                   # Material You 主题定义与课程调色板
└── widget/                       # 桌面小组件
    └── TimetableWidgetProvider.kt # 今日课表 RemoteViews 桌面控件
```

---

## 开源协议

本项目基于 [GNU General Public License v3.0 (GPLv3)](LICENSE) 许可协议开源。
