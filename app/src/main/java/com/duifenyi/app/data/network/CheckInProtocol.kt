package com.duifenyi.app.data.network

import com.duifenyi.app.data.model.CheckInRecord
import com.duifenyi.app.data.model.HanziOption
import com.google.gson.Gson
import com.google.gson.JsonParser
import com.google.gson.reflect.TypeToken
import okhttp3.FormBody

/** 保留 JSON 数字原样，避免 Map<Double> 中转把类型 4 变成字符串 "4.0"。 */
internal object CheckInProtocol {
    private val gson = Gson()

    fun codeCheckInBody(studentUid: String, code: String): FormBody = FormBody.Builder()
        .add("action", "studentcheckin")
        .add("studentid", studentUid)
        .add("checkincode", code)
        .build()

    fun parseRecords(body: String): List<CheckInRecord> {
        val json = JsonParser.parseString(body).asJsonObject
        require(json.get("msg")?.asString == "1") { "获取签到列表失败，请检查登录状态" }
        val rows = json.getAsJsonArray("rows") ?: error("签到列表缺少 rows")
        val type = object : TypeToken<List<CheckInRecord>>() {}.type
        return gson.fromJson(rows, type)
    }

    fun parseHanziGrid(body: String, expectedId: String): List<HanziOption> {
        val json = JsonParser.parseString(body).asJsonObject
        require(json.get("msg")?.asString == "1") { "获取候选字失败，请使用微信签到或稍后重试" }
        require(json.get("checkintype")?.asString == "4") { "当前活动不是汉字签到" }
        require(json.get("checkinid")?.asString == expectedId) {
            "候选字不属于当前课程的签到活动，请使用微信签到"
        }
        require(json.get("ischeckin")?.asString == "0") { "该活动已签到，请刷新签到状态" }
        val grid = json.getAsJsonArray("grid") ?: error("未返回候选字，请使用微信签到")
        val options = grid.map {
            val option = it.asJsonObject
            HanziOption(option.get("h")?.asString.orEmpty(), option.get("p")?.asString.orEmpty())
        }
        require(options.size == 9 && options.all { it.h.codePointCount(0, it.h.length) == 1 } &&
            options.map { it.h }.distinct().size == 9) { "候选字格式已变化，请使用微信签到" }
        return options
    }
}
