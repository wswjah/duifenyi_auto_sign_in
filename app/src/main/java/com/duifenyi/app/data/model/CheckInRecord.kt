package com.duifenyi.app.data.model

import com.google.gson.annotations.SerializedName

/**
 * 签到记录 (来自 MBCount.ashx?action=getstudentinlogbyday 的 rows)
 */
data class CheckInRecord(
    @SerializedName("ID") val id: String = "",
    @SerializedName("CheckInType") val checkInType: String = "",
    @SerializedName("CheckInCode") val checkInCode: String = "",
    @SerializedName("CheckInStatus") val checkInStatus: String = "",
    @SerializedName("CheckInDate") val checkInDate: String = "",
    @SerializedName("StudentID") val studentID: String = "",
    @SerializedName("CanApply") val canApply: String = "0",
    @SerializedName("ApplyLimitDate") val applyLimitDate: String = "",
    @SerializedName("StatusID") val statusID: String = "",
    @SerializedName("StatusName") val statusName: String = "",
    @SerializedName("CreaterDate") val createrDate: String = "",
    @SerializedName("Longitude") val longitude: String = "",
    @SerializedName("Latitude") val latitude: String = ""
)
