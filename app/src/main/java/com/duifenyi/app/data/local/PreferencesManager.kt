package com.duifenyi.app.data.local

import android.content.Context
import android.util.Log
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File

/**
 * 简易 SharedPreferences 封装 + 文件存储
 * 用于存储 Cookie、定位配置等
 */
class PreferencesManager(context: Context) {

    companion object {
        private const val TAG = "PreferencesManager"
        private const val PREFS_NAME = "duifenyi_prefs"
        private const val KEY_COOKIE = "cookie"
        private const val KEY_LOCATIONS_FILE = "locations.json"
    }

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val locationsFile = File(context.filesDir, KEY_LOCATIONS_FILE)

    // ─── Cookie 管理 ───────────────────────────────────

    fun saveCookie(cookie: String) {
        prefs.edit().putString(KEY_COOKIE, cookie).apply()
    }

    fun loadCookie(): String {
        return prefs.getString(KEY_COOKIE, "") ?: ""
    }

    fun hasCookie(): Boolean {
        val cookie = loadCookie()
        return cookie.isNotEmpty() && cookie != "1=1"
    }

    fun clearCookie() {
        prefs.edit().remove(KEY_COOKIE).apply()
    }

    // ─── 定位配置 (文件存储) ────────────────────────────

    fun saveLocations(json: String) {
        try {
            locationsFile.writeText(json, Charsets.UTF_8)
            Log.d(TAG, "Locations saved: ${locationsFile.absolutePath}")
        } catch (e: Exception) {
            Log.e(TAG, "saveLocations failed", e)
        }
    }

    fun loadLocations(): String {
        return try {
            if (locationsFile.exists()) {
                locationsFile.readText(Charsets.UTF_8)
            } else {
                "{}"
            }
        } catch (e: Exception) {
            Log.e(TAG, "loadLocations failed", e)
            "{}"
        }
    }

    // ─── 签到延迟 ──────────────────────────────────────

    fun saveDelay(delaySeconds: Int) {
        prefs.edit().putInt("checkin_delay", delaySeconds).apply()
    }

    fun loadDelay(): Int {
        return prefs.getInt("checkin_delay", 0)
    }

    // ─── 上次选中课程 ──────────────────────────────────

    fun saveLastCourseName(name: String) {
        prefs.edit().putString("last_course", name).apply()
    }

    fun loadLastCourseName(): String {
        return prefs.getString("last_course", "") ?: ""
    }
}
