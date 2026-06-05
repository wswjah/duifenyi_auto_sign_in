package com.duifenyi.app.service

import android.util.Log
import com.duifenyi.app.data.model.LogLevel
import com.duifenyi.app.data.repository.DuifenyiRepository
import kotlinx.coroutines.*

/**
 * 签到监控轮询器 — 每2秒轮询一次签到API
 * 与 Python Loop._poll() 行为对齐
 */
class CheckInMonitor(
    private val repository: DuifenyiRepository
) {
    companion object {
        private const val TAG = "CheckInMonitor"
        private const val POLL_INTERVAL_MS = 2000L
    }

    private var job: Job? = null
    private var tickCount = 0

    fun start() {
        stop()
        tickCount = 0
        job = CoroutineScope(Dispatchers.IO + SupervisorJob()).launch {
            while (isActive) {
                try {
                    tickCount++
                    val result = repository.pollCheckIn()

                    when (result) {
                        is DuifenyiRepository.PollResult.Stop -> {
                            repository.addLog(LogLevel.WARN, "监控停止: ${result.reason}")
                            repository.stopMonitoring()
                            break
                        }
                        is DuifenyiRepository.PollResult.Continue -> {
                            if (result.result == null && tickCount % 10 == 1) {
                                repository.addLog(LogLevel.INFO, "持续监控中...")
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Poll error", e)
                    repository.addLog(LogLevel.ERROR, "监控异常: ${e.message}")
                }
                delay(POLL_INTERVAL_MS)
            }
        }
    }

    fun stop() {
        job?.cancel()
        job = null
        tickCount = 0
    }
}
