package com.duifenyi.app.data.model

data class HanziOption(val h: String, val p: String)

data class ManualCheckInState(
    val requestId: String,
    val recordId: String,
    val courseName: String,
    val isHanzi: Boolean,
    val grid: List<HanziOption> = emptyList(),
    val gridVersion: Int = 0,
    val isBusy: Boolean = true,
    val message: String = "正在准备签到…"
)

sealed interface ManualCheckInAction {
    data class Submit(val code: String) : ManualCheckInAction
    data object RefreshGrid : ManualCheckInAction
    data object Skip : ManualCheckInAction
}

enum class ManualCheckInResult { CONFIRMED, SKIPPED, EXPIRED }
