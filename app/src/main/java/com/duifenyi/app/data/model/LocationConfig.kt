package com.duifenyi.app.data.model

import com.google.gson.annotations.SerializedName

/**
 * 定位坐标配置
 */
data class LocationConfig(
    @SerializedName("lng") val lng: String = "",
    @SerializedName("lat") val lat: String = "",
    @SerializedName("note") val note: String = ""
)
