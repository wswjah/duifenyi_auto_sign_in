package com.duifenyi.app.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.duifenyi.app.data.local.PreferencesManager
import com.duifenyi.app.data.model.Course
import com.duifenyi.app.data.model.LogLevel
import com.duifenyi.app.data.network.DuifenyiApi
import com.duifenyi.app.data.repository.DuifenyiRepository
import com.duifenyi.app.service.CheckInMonitor
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = PreferencesManager(application)
    val repository = DuifenyiRepository(prefs)
    private val monitor = CheckInMonitor(repository)

    private val _selectedCourse = MutableStateFlow<Course?>(null)
    val selectedCourse: StateFlow<Course?> = _selectedCourse.asStateFlow()

    private val _delaySeconds = MutableStateFlow("0")
    val delaySeconds: StateFlow<String> = _delaySeconds.asStateFlow()

    init {
        _delaySeconds.value = prefs.loadDelay().toString()

        // 初始化仓库：加载本地 Cookie → 验证登录 → 拉取课程列表
        viewModelScope.launch {
            repository.init()
            val lastCourse = prefs.loadLastCourseName()
            if (lastCourse.isNotEmpty() && repository.courses.value.isNotEmpty()) {
                val course = repository.courses.value.find { it.courseName == lastCourse }
                if (course != null) {
                    _selectedCourse.value = course
                }
            } else if (repository.courses.value.isNotEmpty()) {
                _selectedCourse.value = repository.courses.value.first()
            }
        }
    }

    fun selectCourse(course: Course) {
        _selectedCourse.value = course
        viewModelScope.launch {
            prefs.saveLastCourseName(course.courseName)
        }
    }

    fun setDelay(delay: String) {
        _delaySeconds.value = delay
        val d = delay.toIntOrNull() ?: 0
        prefs.saveDelay(d)
    }

    fun startMonitoring() {
        val course = _selectedCourse.value ?: return
        val delay = _delaySeconds.value.toIntOrNull() ?: 0

        repository.clearLogs()
        repository.startMonitoring(course, delay)
        monitor.start()
    }

    fun stopMonitoring() {
        monitor.stop()
        repository.stopMonitoring()
    }

    fun getCoordinateForCourse(courseName: String): Pair<String, String> {
        return repository.getCoordinates(courseName)
    }

    fun getAllLocationNames(): List<String> = repository.getAllLocationNames()

    fun selectLocation(name: String) = repository.selectLocation(name)

    fun logout() {
        stopMonitoring()
        DuifenyiApi.cookieJar.clear()
        prefs.clearCookie()
        repository.addLog(LogLevel.INFO, "已退出登录，Cookie已清除")
    }

    override fun onCleared() {
        super.onCleared()
        monitor.stop()
        repository.stopMonitoring()
    }
}
