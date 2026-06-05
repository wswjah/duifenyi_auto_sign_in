package com.duifenyi.app.data.model

/**
 * 日志条目
 */
data class LogEntry(
    val timestamp: String,
    val level: LogLevel,
    val message: String
)

enum class LogLevel { INFO, OK, WARN, ERROR, DEBUG }
