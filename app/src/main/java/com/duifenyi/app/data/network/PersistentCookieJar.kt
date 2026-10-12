package com.duifenyi.app.data.network

import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl

/**
 * 持久化 CookieJar — 使用内存存储 + 对外提供序列化/反序列化接口
 */
class PersistentCookieJar : CookieJar {

    private val cookieStore = LinkedHashMap<String, MutableList<Cookie>>()

    override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
        val key = url.host
        cookieStore.getOrPut(key) { mutableListOf() }.apply {
            cookies.forEach { newCookie ->
                removeAll { it.name == newCookie.name }
                add(newCookie)
            }
        }
    }

    override fun loadForRequest(url: HttpUrl): List<Cookie> {
        // 遍历所有存储的 Cookie（而非仅按 host 查找），
        // 因为登录过程中可能有重定向到不同子域，Cookie 可能分散在不同 host key 下
        return cookieStore.values.flatten().filter { it.matches(url) }
    }

    /** 导出所有 Cookie 为字符串 (用于持久化) */
    fun exportCookies(): String {
        val map = mutableMapOf<String, String>()
        cookieStore.values.flatten().forEach { cookie ->
            if (cookie.name.isNotEmpty()) {
                map[cookie.name] = cookie.value
            }
        }
        return map.entries.joinToString("; ") { "${it.key}=${it.value}" }
    }

    /** 从持久化字符串恢复 Cookie */
    fun importCookies(cookieString: String) {
        cookieStore.clear()
        cookieString.split(";").forEach { pair ->
            val trimmed = pair.trim()
            val eqIndex = trimmed.indexOf('=')
            if (eqIndex > 0) {
                val name = trimmed.substring(0, eqIndex).trim()
                val value = trimmed.substring(eqIndex + 1).trim()
                if (name.isNotEmpty() && value != "1") {
                    val cookie = Cookie.Builder()
                        .name(name)
                        .value(value)
                        .domain("www.duifene.com")
                        .path("/")
                        .build()
                    cookieStore.getOrPut("www.duifene.com") { mutableListOf() }.add(cookie)
                }
            }
        }
    }

    /** 清空所有 Cookie */
    fun clear() {
        cookieStore.clear()
    }
}
