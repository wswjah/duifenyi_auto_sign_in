package com.duifenyi.app.data.network

import android.util.Log
import com.duifenyi.app.data.model.CheckInRecord
import com.duifenyi.app.data.model.Course
import com.duifenyi.app.data.model.LocationConfig
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.jsoup.Jsoup
import java.security.SecureRandom
import java.security.cert.X509Certificate
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

/**
 * 对分易 API 网络层 — 与 Python requests.Session 行为对齐
 */
object DuifenyiApi {

    private const val TAG = "DuifenyiApi"
    const val HOST = "https://www.duifene.com"

    // 桌面 UA (MBCount.ashx 必须使用桌面 UA)
    private const val DESKTOP_UA =
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/148.0.0.0 Safari/537.36 Edg/148.0.0.0"

    // 移动微信 UA (定位签到需要)
    const val MOBILE_WX_UA =
        "Mozilla/5.0 (Linux; Android 16; PLK110 Build/BP2A.250605.015; wv) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/146.0.7680.178 Mobile Safari/537.36 XWEB/1460093 MMWEBSDK/20250904 MMWEBID/2685 MicroMessenger/8.0.65.2960(0x28004137) WeChat/arm64 Weixin NetType/WIFI Language/zh_CN ABI/arm64"

    val cookieJar = PersistentCookieJar()
    private val gson = Gson()

    private val trustAllManager = object : X509TrustManager {
        override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
        override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
        override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
    }

    private val sslContext = SSLContext.getInstance("TLS").apply {
        init(null, arrayOf<TrustManager>(trustAllManager), SecureRandom())
    }

    val client: OkHttpClient = OkHttpClient.Builder()
        .cookieJar(cookieJar)
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .writeTimeout(10, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .sslSocketFactory(sslContext.socketFactory, trustAllManager as X509TrustManager)
        .hostnameVerifier { _, _ -> true }
        .addInterceptor { chain ->
            val request = chain.request().newBuilder()
                .header("User-Agent", DESKTOP_UA)
                .build()
            chain.proceed(request)
        }
        .build()

    // ─── 认证 ──────────────────────────────────────────

    /**
     * 检查登录状态
     * POST HOST/AppCode/LoginInfo.ashx → Action=checklogin
     */
    suspend fun checkLogin(): Boolean = withContext(Dispatchers.IO) {
        try {
            val body = "Action=checklogin".toRequestBody(MEDIA_TYPE_FORM)
            val request = Request.Builder()
                .url("$HOST/AppCode/LoginInfo.ashx")
                .header("Referer", "$HOST/_UserCenter/PC/CenterStudent.aspx")
                .post(body)
                .build()
            val response = client.newCall(request).execute()
            val result = response.body?.string() ?: ""
            val ok = response.code == 200 && result.contains("\"msg\":\"1\"")
            Log.d(TAG, "checkLogin: code=${response.code}, ok=$ok, body=${result.take(200)}")
            ok
        } catch (e: Exception) {
            Log.e(TAG, "checkLogin failed", e)
            false
        }
    }

    /**
     * 微信授权码登录
     * GET HOST/P.aspx?authtype=1&code={code}&state=1
     */
    suspend fun loginByWeChatCode(code: String): Boolean = withContext(Dispatchers.IO) {
        try {
            cookieJar.clear()
            val request = Request.Builder()
                .url("$HOST/P.aspx?authtype=1&code=$code&state=1")
                .get()
                .build()
            val response = client.newCall(request).execute()
            response.code == 200
        } catch (e: Exception) {
            Log.e(TAG, "loginByWeChatCode failed", e)
            false
        }
    }

    // ─── 课程 ──────────────────────────────────────────

    /**
     * 获取课程列表
     * POST HOST/_UserCenter/CourseInfo.ashx → action=getstudentcourse&classtypeid=2
     */
    suspend fun getCourseList(): List<Course>? = withContext(Dispatchers.IO) {
        try {
            val body = "action=getstudentcourse&classtypeid=2".toRequestBody(MEDIA_TYPE_FORM)
            val request = Request.Builder()
                .url("$HOST/_UserCenter/CourseInfo.ashx")
                .header("Referer", "$HOST/_UserCenter/PC/CenterStudent.aspx")
                .post(body)
                .build()
            val response = client.newCall(request).execute()
            val bodyStr = response.body?.string() ?: return@withContext null

            Log.d(TAG, "getCourseList HTTP ${response.code}, bodyLen=${bodyStr.length}, preview=${bodyStr.take(300)}")

            if (response.code != 200) {
                Log.w(TAG, "getCourseList: non-200 response code=${response.code}")
                return@withContext null
            }

            // 使用 lenient Gson 处理可能的字段类型不匹配 (如 int→String)
            val gsonLenient = GsonBuilder().setLenient().create()

            // 尝试直接解析为 List
            try {
                val type = object : TypeToken<List<Course>>() {}.type
                val result: List<Course> = gsonLenient.fromJson(bodyStr, type)
                Log.d(TAG, "getCourseList: direct parse OK, size=${result.size}")
                return@withContext result
            } catch (e: Exception) {
                Log.d(TAG, "getCourseList: direct parse failed: ${e.message}")
            }

            // 通过封装对象解析
            return@withContext tryParseCourseList(bodyStr, gsonLenient)
        } catch (e: Exception) {
            Log.e(TAG, "getCourseList failed", e)
            null
        }
    }

    private fun tryParseCourseList(bodyStr: String, gson: Gson): List<Course>? {
        try {
            val json = gson.fromJson(bodyStr, Map::class.java) ?: return null
            val keys = arrayOf("data", "list", "courses", "items")
            for (key in keys) {
                val raw = json[key]
                if (raw is List<*>) {
                    Log.d(TAG, "getCourseList: found key='$key', listSize=${raw.size}")
                    val type = object : TypeToken<List<Course>>() {}.type
                    return gson.fromJson(gson.toJson(raw), type)
                }
            }
            Log.w(TAG, "getCourseList: no known key found, top-level keys=${json.keys}")
        } catch (e: Exception) {
            Log.w(TAG, "getCourseList: tryParseCourseList failed: ${e.message}")
        }
        return null
    }

    // ═══════════════════════════════════════════════════════
    //  以下所有方法都在 object DuifenyiApi 内部
    // ═══════════════════════════════════════════════════════

    /**
     * 获取学生 UID (从页面 HTML 解析)
     */
    suspend fun getStudentUid(): String? = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("$HOST/_UserCenter/MB/index.aspx")
                .get()
                .build()
            val response = client.newCall(request).execute()
            if (response.code != 200) return@withContext null
            val html = response.body?.string() ?: return@withContext null
            return@withContext parseUidFromHtml(html)
        } catch (e: Exception) {
            Log.e(TAG, "getStudentUid failed", e)
            null
        }
    }

    /**
     * 从签到页面解析学生 UID
     */
    suspend fun getStudentUidFromCheckInPage(classId: String): String? = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("$HOST/_CheckIn/PC/StudentNoCheckCount.aspx?classid=$classId")
                .get()
                .build()
            val response = client.newCall(request).execute()
            if (response.code != 200) return@withContext null
            val html = response.body?.string() ?: return@withContext null
            return@withContext parseUidFromHtml(html)
        } catch (e: Exception) {
            Log.e(TAG, "getStudentUidFromCheckInPage failed", e)
            null
        }
    }

    private fun parseUidFromHtml(html: String): String? {
        val doc = Jsoup.parse(html)
        val uidIds = arrayOf("hidUID", "hidUid", "hidUserId", "hidSID", "studentId", "HidStudentID")
        for (eid in uidIds) {
            val el = doc.getElementById(eid)
            if (el != null && el.`val`().isNotEmpty()) return el.`val`()
        }
        for (input in doc.select("input[type=hidden]")) {
            val name = input.attr("name").lowercase()
            if (name.contains("uid") || name.contains("studentid")) {
                val value = input.`val`()
                if (value.isNotEmpty()) return value
            }
        }
        return null
    }

    // ─── 签到 API 探测 ─────────────────────────────────

    /**
     * 探测签到 API — POST MBCount.ashx 验证接口可用性
     */
    suspend fun discoverCheckInApi(classId: String, studentUid: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val body = "action=getstudentinlogbyday&classid=$classId&studentid=$studentUid"
                .toRequestBody(MEDIA_TYPE_FORM)
            val request = Request.Builder()
                .url("$HOST/_CheckIn/MBCount.ashx")
                .header("Referer", "https://www.duifene.com/_CheckIn/PC/StudentNoCheckCount.aspx")
                .header("X-Requested-With", "XMLHttpRequest")
                .header("Origin", "https://www.duifene.com")
                .header("Accept", "application/json, text/javascript, */*; q=0.01")
                .post(body)
                .build()
            val response = client.newCall(request).execute()
            if (response.code == 200) {
                val respBody = response.body?.string() ?: ""
                return@withContext respBody.contains("\"rows\"") || respBody.contains("\"msg\"")
            }
            false
        } catch (e: Exception) {
            Log.e(TAG, "discoverCheckInApi failed", e)
            false
        }
    }

    /**
     * 获取签到记录列表
     */
    suspend fun fetchCheckInRecords(classId: String, studentUid: String): List<CheckInRecord>? =
        withContext(Dispatchers.IO) {
            try {
                val body = "action=getstudentinlogbyday&classid=$classId&studentid=$studentUid"
                    .toRequestBody(MEDIA_TYPE_FORM)
                val request = Request.Builder()
                    .url("$HOST/_CheckIn/MBCount.ashx")
                    .header("Referer", "$HOST/_UserCenter/MB/index.aspx")
                    .post(body)
                    .build()
                val response = client.newCall(request).execute()
                if (response.code != 200) return@withContext null
                val respBody = response.body?.string() ?: return@withContext null

                val json = gson.fromJson(respBody, Map::class.java) ?: return@withContext null
                if (json["msg"]?.toString() != "1") return@withContext null
                val rows = json["rows"]
                if (rows !is List<*>) return@withContext null

                val type = object : TypeToken<List<CheckInRecord>>() {}.type
                return@withContext gson.fromJson(gson.toJson(rows), type)
            } catch (e: Exception) {
                Log.e(TAG, "fetchCheckInRecords failed", e)
                null
            }
        }

    // ─── 签到执行 ──────────────────────────────────────

    /** 数字码签到 */
    suspend fun doCodeCheckIn(studentUid: String, code: String): String = withContext(Dispatchers.IO) {
        try {
            val body = "action=studentcheckin&studentid=$studentUid&checkincode=$code"
                .toRequestBody(MEDIA_TYPE_FORM)
            val request = Request.Builder()
                .url("$HOST/_CheckIn/CheckIn.ashx")
                .header("Referer", "$HOST/_CheckIn/MB/CheckInStudent.aspx?moduleid=16&pasd=")
                .post(body)
                .build()
            val response = client.newCall(request).execute()
            if (response.code == 200) {
                val respBody = response.body?.string() ?: ""
                try {
                    val json = gson.fromJson(respBody, Map::class.java)
                    json["msgbox"]?.toString() ?: "签到返回: $respBody"
                } catch (_: Exception) {
                    "签到返回: $respBody"
                }
            } else {
                "签到请求失败, 状态码: ${response.code}"
            }
        } catch (e: Exception) {
            Log.e(TAG, "doCodeCheckIn failed", e)
            "签到请求异常: ${e.message}"
        }
    }

    /** 二维码签到 */
    suspend fun doQRCheckIn(checkInId: String): String = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("$HOST/_CheckIn/MB/QrCodeCheckOK.aspx?state=$checkInId")
                .get()
                .build()
            val response = client.newCall(request).execute()
            if (response.code == 200) {
                val html = response.body?.string() ?: ""
                val doc = Jsoup.parse(html)
                val okIds = arrayOf("DivOK", "divok", "divOK")
                for (eid in okIds) {
                    val el = doc.getElementById(eid)
                    if (el != null && el.text().contains("签到成功")) return@withContext el.text()
                }
                doc.text()
            } else {
                "签到请求失败, 状态码: ${response.code}"
            }
        } catch (e: Exception) {
            Log.e(TAG, "doQRCheckIn failed", e)
            "签到请求异常: ${e.message}"
        }
    }

    /** 定位签到 (使用移动微信 UA) */
    suspend fun doLocationCheckIn(
        studentUid: String,
        lng: Double,
        lat: Double,
        courseId: String,
        tclassId: String
    ): String = withContext(Dispatchers.IO) {
        try {
            val body = "action=signin&cid=$courseId&tcid=$tclassId&sid=$studentUid&latitude=$lat&longitude=$lng"
                .toRequestBody(MEDIA_TYPE_FORM)
            val request = Request.Builder()
                .url("$HOST/_CheckIn/CheckInRoomHandler.ashx")
                .header("User-Agent", MOBILE_WX_UA)
                .header("Referer", "$HOST/_CheckIn/MB/CheckInStudent.aspx?moduleid=16&pasd=")
                .header("X-Requested-With", "XMLHttpRequest")
                .post(body)
                .build()
            val response = client.newCall(request).execute()
            if (response.code == 200) {
                val respBody = response.body?.string() ?: ""
                try {
                    val json = gson.fromJson(respBody, Map::class.java)
                    json["msgbox"]?.toString() ?: "签到返回: $respBody"
                } catch (_: Exception) {
                    "签到返回: $respBody"
                }
            } else {
                "签到请求失败, 状态码: ${response.code}"
            }
        } catch (e: Exception) {
            Log.e(TAG, "doLocationCheckIn failed", e)
            "签到请求异常: ${e.message}"
        }
    }

    /** 从签到页面解析教室坐标 (兜底方案) */
    suspend fun parseRoomCoordinates(classId: String, checkInId: String): Pair<String, String>? =
        withContext(Dispatchers.IO) {
            val urls = listOf(
                "$HOST/_CheckIn/MB/TeachCheckIn.aspx?classid=$classId&temps=0&checktype=3&isrefresh=0&timeinterval=0&roomid=0&match=",
                "$HOST/_CheckIn/MB/CheckInStudent.aspx?moduleid=16&pasd=&checkinid=$checkInId"
            )
            for (url in urls) {
                try {
                    val request = Request.Builder().url(url).get().build()
                    val response = client.newCall(request).execute()
                    if (response.code == 200) {
                        val html = response.body?.string() ?: return@withContext null
                        val doc = Jsoup.parse(html)
                        var lng: String? = null
                        var lat: String? = null
                        for (eid in listOf("HFRoomLongitude", "hfroomlongitude")) {
                            val el = doc.getElementById(eid)
                            if (el != null && el.`val`().isNotEmpty()) { lng = el.`val`(); break }
                        }
                        for (eid in listOf("HFRoomLatitude", "hfroomlatitude")) {
                            val el = doc.getElementById(eid)
                            if (el != null && el.`val`().isNotEmpty()) { lat = el.`val`(); break }
                        }
                        if (lng != null && lat != null) return@withContext Pair(lng, lat)
                    }
                } catch (_: Exception) { }
            }
            null
        }

    /** 获取默认坐标 (C5教学楼) */
    fun getDefaultCoordinates(): LocationConfig = LocationConfig(lng = "114.39437", lat = "22.70462", note = "C5教学楼(默认)")

    private val MEDIA_TYPE_FORM = "application/x-www-form-urlencoded; charset=UTF-8".toMediaType()
}
