package com.duifenyi.app

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build

class DuifenyiApp : Application() {
    companion object {
        const val CHANNEL_ID = "checkin_channel"
    }

    override fun onCreate() {
        super.onCreate()

        // 创建通知渠道 (监控服务可能需要)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "签到监控",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "签到监控状态通知"
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }
}
