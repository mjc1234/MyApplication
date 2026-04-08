package com.mjc.core.download.config

import java.io.File

/**
 * 下载配置类
 * 用于配置下载相关的参数，如超时时间、重试策略、缓存等
 *
 * 通过 Hilt 注入 DownloadExecutor 时作为默认配置，或手动创建时传入自定义参数
 */
data class DownloadConfig(
    /** 连接超时时间（毫秒），默认60秒 */
    val connectTimeoutMs: Long = 60_000L,
    /** 读取超时时间（毫秒），默认300秒（5分钟） */
    val readTimeoutMs: Long = 300_000L,
    /** 写入超时时间（毫秒），默认60秒 */
    val writeTimeoutMs: Long = 60_000L,
    /** 是否跟随重定向，默认true */
    val followRedirects: Boolean = true,
    /** 是否跟随SSL重定向，默认true */
    val followSslRedirects: Boolean = true,
    /** 是否启用Range请求支持（断点续传），默认true */
    val enableRangeSupport: Boolean = true,
    /** 是否启用重试拦截器，默认true */
    val enableRetry: Boolean = true,
    /** 最大重试次数，默认3次 */
    val maxRetries: Int = 3,
    /** 重试初始延迟（毫秒），默认1000ms */
    val retryDelayMs: Long = 1000L,
    /** 是否启用缓存，默认false */
    val enableCache: Boolean = false,
    /** 缓存目录（如果启用缓存） */
    val cacheDir: File? = null,
    /** 缓存大小（字节），默认20MB */
    val cacheSize: Long = 20 * 1024 * 1024,
    /** 是否启用日志拦截器（调试用），默认false */
    val enableLogging: Boolean = false,
    /** 日志级别，默认BASIC */
    val loggingLevel: LoggingLevel = LoggingLevel.BASIC,
    /** 缓冲区大小（字节），默认8192字节（8KB） */
    val bufferSize: Int = 8192,
    /** 是否验证SSL证书，默认true */
    val sslVerification: Boolean = true
) {
    companion object {
        fun default() = DownloadConfig()
    }
}

/**
 * 日志级别枚举
 */
enum class LoggingLevel {
    /** 不记录日志 */
    NONE,
    /** 记录请求和响应行 */
    BASIC,
    /** 记录请求和响应行及头部 */
    HEADERS,
    /** 记录请求和响应行、头部及正文 */
    BODY
}

/**
 * 重试策略配置
 */
data class RetryPolicy(
    val maxRetries: Int = 3,
    val retryDelayMs: Long = 1000L,
    val exponentialBackoff: Boolean = true,
    val retryableStatusCodes: Set<Int> = setOf(500, 502, 503, 504, 429),
    val retryableExceptions: Set<Class<out Exception>> = setOf(
        java.net.SocketTimeoutException::class.java,
        java.net.ConnectException::class.java,
        java.net.UnknownHostException::class.java,
        java.net.SocketException::class.java
    )
)
