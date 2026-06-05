package com.duifenyi.app.data.model

/**
 * 签到类型
 */
enum class CheckInType(val code: String, val label: String) {
    DIGITAL("1", "数字码签到"),
    QR("2", "二维码签到"),
    LOCATION("3", "定位签到");

    companion object {
        fun fromCode(code: String): CheckInType = entries.find { it.code == code } ?: DIGITAL
    }
}
