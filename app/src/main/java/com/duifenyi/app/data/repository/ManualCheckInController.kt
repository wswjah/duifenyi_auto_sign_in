package com.duifenyi.app.data.repository

import com.duifenyi.app.data.model.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withTimeoutOrNull
import java.time.Duration
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID

/** 一个活动只等待一次人工操作；错误保留弹窗，停止监控则取消等待。 */
internal class ManualCheckInController(
    private val fetchGrid: suspend (String, String) -> List<HanziOption>,
    private val fetchRecord: suspend (String, String, String) -> CheckInRecord?,
    private val submit: suspend (String, String, String) -> String
) {
    private val _state = MutableStateFlow<ManualCheckInState?>(null)
    val state = _state.asStateFlow()
    @Volatile private var answer: CompletableDeferred<ManualCheckInAction>? = null

    fun respond(requestId: String, action: ManualCheckInAction) {
        val current = _state.value ?: return
        if (current.requestId == requestId && !current.isBusy) answer?.complete(action)
    }

    fun cancel() {
        answer?.cancel()
        _state.value = null
    }

    suspend fun run(course: Course, record: CheckInRecord, uid: String): ManualCheckInResult {
        val requestId = UUID.randomUUID().toString()
        var current = ManualCheckInState(requestId, record.id, course.courseName, record.checkInType == "4")
        val now = LocalDateTime.now()
        val deadline = deadline(record, now)
        val timeout = Duration.between(now, deadline).toMillis()
        if (timeout <= 0) return ManualCheckInResult.EXPIRED

        fun publish(message: String) {
            current = current.copy(isBusy = true, message = message)
            _state.value = current
        }

        suspend fun loadGrid() {
            publish("正在加载九宫格…")
            // 刷新后清空旧选择，不用其他活动的候选字兜底。
            current = current.copy(grid = emptyList(), gridVersion = current.gridVersion + 1)
            try {
                val grid = fetchGrid(uid, record.id)
                currentCoroutineContext().ensureActive()
                current = current.copy(grid = grid, message = "请按老师给出的顺序选择 4 个汉字")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                current = current.copy(message = e.message ?: "候选字加载失败，请重试或使用微信签到")
            }
        }

        try {
            return withTimeoutOrNull(timeout) {
                if (current.isHanzi) loadGrid()
                else current = current.copy(message = "请输入老师提供的 4 位数字码")
                while (true) {
                    currentCoroutineContext().ensureActive()
                    val waiting = CompletableDeferred<ManualCheckInAction>()
                    answer = waiting
                    _state.value = current.copy(isBusy = false)
                    val action = try { waiting.await() } finally {
                        if (answer === waiting) answer = null
                    }
                    if (action == ManualCheckInAction.Skip) return@withTimeoutOrNull ManualCheckInResult.SKIPPED
                    if (action == ManualCheckInAction.RefreshGrid) {
                        if (current.isHanzi) loadGrid()
                        continue
                    }
                    val code = (action as ManualCheckInAction.Submit).code.trim()
                    if (!isValidCode(code, current.isHanzi, current.grid)) {
                        current = current.copy(message = if (current.isHanzi) "请从九宫格中依次选择 4 个不同的汉字" else "请输入完整的 4 位数字码")
                        continue
                    }
                    try {
                        publish("正在核实活动是否有效…")
                        val latest = fetchRecord(course.tclassId, uid, record.id)
                        currentCoroutineContext().ensureActive()
                        if (latest?.isConfirmed() == true) return@withTimeoutOrNull ManualCheckInResult.CONFIRMED
                        if (latest == null || latest.checkInType != record.checkInType ||
                            latest.canApply != "1" || !LocalDateTime.now().isBefore(deadline(latest, now))) {
                            return@withTimeoutOrNull ManualCheckInResult.EXPIRED
                        }
                        publish("正在提交签到…")
                        val response = submit(record.checkInType, uid, code)
                        // 不从“成功”等页面文字推断结果，重新读取当前活动的出勤记录。
                        val confirmed = fetchRecord(course.tclassId, uid, record.id)
                        currentCoroutineContext().ensureActive()
                        if (confirmed?.isConfirmed() == true) return@withTimeoutOrNull ManualCheckInResult.CONFIRMED
                        current = current.copy(message = "尚未确认签到成功，请核对后重试或使用微信。返回：${response.take(160)}")
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        current = current.copy(message = e.message ?: "请求失败，请重试或使用微信签到")
                    }
                }
                @Suppress("UNREACHABLE_CODE")
                ManualCheckInResult.EXPIRED
            } ?: ManualCheckInResult.EXPIRED
        } finally {
            if (_state.value?.requestId == requestId) _state.value = null
        }
    }

    companion object {
        private val dateFormat = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss")

        private fun deadline(record: CheckInRecord, fallback: LocalDateTime): LocalDateTime {
            fun parse(value: String) = runCatching { LocalDateTime.parse(value, dateFormat) }.getOrNull()
            val ageLimit = (parse(record.createrDate) ?: fallback).plusMinutes(30)
            return minOf(ageLimit, parse(record.applyLimitDate) ?: ageLimit)
        }

        fun isValidCode(code: String, isHanzi: Boolean, grid: List<HanziOption>): Boolean {
            if (!isHanzi) return code.matches(Regex("[0-9]{4}"))
            val chars = code.codePoints().toArray().map { String(Character.toChars(it)) }
            return grid.size == 9 && chars.size == 4 && chars.distinct().size == 4 &&
                chars.all { char -> grid.any { it.h == char } }
        }
    }
}

internal fun CheckInRecord.isConfirmed(): Boolean =
    statusID == "1" || statusName == "出勤" || checkInDate.isNotBlank()
