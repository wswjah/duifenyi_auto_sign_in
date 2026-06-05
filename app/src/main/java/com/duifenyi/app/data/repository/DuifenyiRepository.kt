package com.duifenyi.app.data.repository

import android.util.Log
import com.duifenyi.app.data.local.PreferencesManager
import com.duifenyi.app.data.model.*
import com.duifenyi.app.data.network.DuifenyiApi
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlin.random.Random

/**
 * 对分易业务逻辑仓库 — 连接 API、本地存储、签到监控
 */
class DuifenyiRepository(private val prefs: PreferencesManager) {

    companion object {
        private const val TAG = "DuifenyiRepository"
        private val DATE_FMT = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss")
    }

    private val gson = Gson()

    // 登录状态
    private val _loginState = MutableStateFlow<LoginState>(LoginState.Idle)
    val loginState: StateFlow<LoginState> = _loginState.asStateFlow()

    // 课程列表
    private val _courses = MutableStateFlow<List<Course>>(emptyList())
    val courses: StateFlow<List<Course>> = _courses.asStateFlow()

    // 签到日志
    private val _logs = MutableStateFlow<List<LogEntry>>(emptyList())
    val logs: StateFlow<List<LogEntry>> = _logs.asStateFlow()

    // 监控状态
    private val _isMonitoring = MutableStateFlow(false)
    val isMonitoring: StateFlow<Boolean> = _isMonitoring.asStateFlow()

    // ─── 日志 ──────────────────────────────────────────

    fun addLog(level: LogLevel, message: String) {
        val now = LocalDateTime.now()
        val ts = now.format(DateTimeFormatter.ofPattern("HH:mm:ss"))
        val entry = LogEntry(ts, level, message)
        _logs.value = _logs.value + entry
        Log.d(TAG, "[${ts}] [${level.name}] $message")
    }

    fun clearLogs() {
        _logs.value = emptyList()
    }

    // ─── 初始化 ────────────────────────────────────────

    suspend fun init() {
        addLog(LogLevel.DEBUG, "init: 开始初始化，尝试加载课程...")

        // 对齐 Python 逻辑：不调用 checkLogin() 做前置验证
        // 直接尝试 loadCourses() — 成功即登录有效，失败则尝试 prefs 回退
        loadCourses()
        if (_courses.value.isNotEmpty()) {
            _loginState.value = LoginState.LoggedIn
            addLog(LogLevel.INFO, "Cookie有效，已加载 ${_courses.value.size} 门课程")
            return
        }

        // 内存 CookieJar 无效 → 从 prefs 恢复
        if (prefs.hasCookie()) {
            val cookie = prefs.loadCookie()
            addLog(LogLevel.DEBUG, "init: 从prefs加载Cookie len=${cookie.length}")
            DuifenyiApi.cookieJar.importCookies(cookie)
            addLog(LogLevel.INFO, "已加载本地Cookie")

            loadCourses()
            if (_courses.value.isNotEmpty()) {
                _loginState.value = LoginState.LoggedIn
                addLog(LogLevel.INFO, "已加载 ${_courses.value.size} 门课程")
            } else {
                addLog(LogLevel.WARN, "Cookie已过期，请重新登录")
                _loginState.value = LoginState.LoggedOut
            }
        } else {
            addLog(LogLevel.INFO, "未找到Cookie，请登录")
            _loginState.value = LoginState.LoggedOut
        }
    }

    // ─── 微信登录 ──────────────────────────────────────

    suspend fun loginWithWeChatCode(code: String): Boolean {
        _loginState.value = LoginState.LoggingIn
        addLog(LogLevel.INFO, "正在微信登录...")

        return try {
            val success = DuifenyiApi.loginByWeChatCode(code)
            if (success) {
                // 对齐 Python Auth.wx(): P.aspx 返回 200 即视为登录成功
                // 直接保存 Cookie 并进入已登录状态，不额外调用 checkLogin
                val cookie = DuifenyiApi.cookieJar.exportCookies()
                prefs.saveCookie(cookie)
                addLog(LogLevel.OK, "Cookie已保存")
                _loginState.value = LoginState.LoggedIn
                loadCourses()
                true
            } else {
                addLog(LogLevel.ERROR, "微信登录失败")
                _loginState.value = LoginState.Error("微信登录失败")
                false
            }
        } catch (e: Exception) {
            addLog(LogLevel.ERROR, "登录异常: ${e.message}")
            _loginState.value = LoginState.Error(e.message ?: "登录异常")
            false
        }
    }

    /** 从微信授权链接中提取 code */
    fun extractWeChatCode(url: String): String? {
        val regex = Regex("(?<=code=)[A-Za-z0-9]{32}")
        return regex.find(url)?.value
    }

    // ─── 课程 ──────────────────────────────────────────

    suspend fun loadCourses() {
        addLog(LogLevel.INFO, "正在加载课程列表...")
        val list = DuifenyiApi.getCourseList()
        if (list != null && list.isNotEmpty()) {
            _courses.value = list
            addLog(LogLevel.OK, "已加载 ${list.size} 门课程")
        } else {
            addLog(LogLevel.ERROR, "获取课程列表失败")
        }
    }

    fun getCourseByName(name: String): Course? {
        return _courses.value.find { it.courseName == name }
    }

    // ─── 定位坐标 ──────────────────────────────────────

    private val _selectedLocationName = MutableStateFlow("")
    val selectedLocationName: StateFlow<String> = _selectedLocationName.asStateFlow()

    /** 获取所有已配置的地址名称列表 */
    fun getAllLocationNames(): List<String> {
        val locationsJson = prefs.loadLocations()
        if (locationsJson == "{}") return emptyList()
        try {
            val type = object : TypeToken<Map<String, LocationConfig>>() {}.type
            val map: Map<String, LocationConfig> = gson.fromJson(locationsJson, type)
            return map.keys.toList()
        } catch (_: Exception) { }
        return emptyList()
    }

    fun selectLocation(name: String) {
        _selectedLocationName.value = name
    }

    private fun loadAllLocationConfigs(): Map<String, LocationConfig> {
        val locationsJson = prefs.loadLocations()
        if (locationsJson == "{}") return emptyMap()
        try {
            val type = object : TypeToken<Map<String, LocationConfig>>() {}.type
            return gson.fromJson(locationsJson, type)
        } catch (_: Exception) { }
        return emptyMap()
    }

    private fun getLocationCoord(name: String): Pair<String, String> {
        val config = loadAllLocationConfigs()[name]
        if (config != null && config.lng.isNotEmpty() && config.lat.isNotEmpty()) {
            return Pair(config.lng, config.lat)
        }
        return Pair("", "")
    }

    fun getCoordinates(courseName: String): Pair<String, String> {
        // 优先使用用户手动选择的地址
        val selectedName = _selectedLocationName.value
        if (selectedName.isNotEmpty()) {
            return getLocationCoord(selectedName)
        }
        // 回退到按课程名匹配
        return getLocationCoord(courseName)
    }

    /** 随机偏移坐标 (≈10米范围) */
    fun addRandomOffset(lng: Double, lat: Double): Pair<Double, Double> {
        val offsetLng = lng + Random.nextDouble(-0.000089, 0.000089)
        val offsetLat = lat + Random.nextDouble(-0.000089, 0.000089)
        return Pair(
            Math.round(offsetLng * 1e8) / 1e8,
            Math.round(offsetLat * 1e8) / 1e8
        )
    }

    // ─── 监控 ──────────────────────────────────────────

    private val doneIds = mutableSetOf<String>()
    private var currentCourse: Course? = null
    private var delaySeconds: Int = 0
    private var isRunning = false

    fun startMonitoring(course: Course, delay: Int) {
        currentCourse = course
        delaySeconds = delay
        doneIds.clear()
        isRunning = true
        _isMonitoring.value = true
    }

    fun stopMonitoring() {
        isRunning = false
        _isMonitoring.value = false
        currentCourse = null
        doneIds.clear()
    }

    /**
     * 单次轮询签到 — 由 CheckInMonitor 定期调用
     * @return true 表示发现了待签到并已处理
     */
    suspend fun pollCheckIn(): PollResult {
        val course = currentCourse ?: return PollResult.Stop("未设置课程")

        // 对齐 Python Loop._poll(): 不自检 login，由实际 API 调用成败决定

        val studentUid = DuifenyiApi.getStudentUidFromCheckInPage(course.tclassId)
            ?: DuifenyiApi.getStudentUid()
            ?: return PollResult.Continue(null)

        val records = DuifenyiApi.fetchCheckInRecords(course.tclassId, studentUid)

        if (records == null) {
            return PollResult.Continue(null)
        }

        val pending = findPendingRecord(records)
            ?: return PollResult.Continue(null)

        val rid = pending.id
        val ct = pending.checkInType
        val typeName = when (ct) {
            "1" -> "数字码"
            "2" -> "二维码"
            "3" -> "定位"
            else -> "未知"
        }

        doneIds.add(rid)
        addLog(LogLevel.INFO, "发现待签到! ID:$rid 类型:$typeName 签到码:${pending.checkInCode.ifEmpty { "无" }}")

        if (delaySeconds > 0) {
            addLog(LogLevel.INFO, "将在 $delaySeconds 秒后自动签到...")
            kotlinx.coroutines.delay(delaySeconds * 1000L)
        }

        // 对于数字码签到，使用记录中的 CheckInCode
        val result = when (ct) {
            "1" -> {
                val code = pending.checkInCode
                if (code.isNotEmpty()) {
                    DuifenyiApi.doCodeCheckIn(studentUid, code)
                } else {
                    "无签到码"
                }
            }
            "2" -> DuifenyiApi.doQRCheckIn(rid)
            "3" -> {
                var (lng, lat) = getCoordinates(course.courseName)
                if (lng.isEmpty()) {
                    lng = pending.longitude
                    lat = pending.latitude
                }
                if (lng.isEmpty()) {
                    val parsed = DuifenyiApi.parseRoomCoordinates(course.tclassId, rid)
                    if (parsed != null) {
                        lng = parsed.first
                        lat = parsed.second
                    }
                }
                if (lng.isEmpty()) {
                    val default = DuifenyiApi.getDefaultCoordinates()
                    lng = default.lng
                    lat = default.lat
                    addLog(LogLevel.WARN, "未配置【${course.courseName}】坐标, 使用C5教学楼默认")
                }
                val (offsetLng, offsetLat) = addRandomOffset(
                    lng.toDoubleOrNull() ?: 114.39437,
                    lat.toDoubleOrNull() ?: 22.70462
                )
                DuifenyiApi.doLocationCheckIn(studentUid, offsetLng, offsetLat, course.courseId, course.tclassId)
            }
            else -> "未知签到类型: $ct"
        }

        if (result.contains("成功")) {
            addLog(LogLevel.OK, "签到成功! ID:$rid")
        } else {
            addLog(LogLevel.WARN, "签到失败! ID:$rid error: $result")
        }

        return PollResult.Continue(result)
    }

    private fun findPendingRecord(records: List<CheckInRecord>): CheckInRecord? {
        val now = LocalDateTime.now()
        for (r in records) {
            if (r.id in doneIds) continue
            if (r.checkInStatus.isNotEmpty() || r.checkInDate.isNotEmpty() || r.studentID.isNotEmpty()) continue
            if (r.statusID == "1" || r.statusName == "出勤") continue
            if (r.canApply != "1") continue

            // 检查是否过期
            if (r.applyLimitDate.isNotEmpty()) {
                try {
                    val limit = LocalDateTime.parse(r.applyLimitDate, DATE_FMT)
                    if (now.isAfter(limit)) continue
                } catch (_: Exception) { }
            }

            // 检查是否超过30分钟
            if (r.createrDate.isNotEmpty()) {
                try {
                    val created = LocalDateTime.parse(r.createrDate, DATE_FMT)
                    if (java.time.Duration.between(created, now).seconds > 1800) continue
                } catch (_: Exception) { }
            }

            return r
        }
        return null
    }

    sealed class PollResult {
        data class Continue(val result: String?) : PollResult()
        data class Stop(val reason: String) : PollResult()
    }
}
