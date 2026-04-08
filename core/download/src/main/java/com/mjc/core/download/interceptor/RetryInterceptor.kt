package com.mjc.core.download.interceptor

import okhttp3.Interceptor
import okhttp3.Response

/**
 * 重试拦截器（专门针对下载场景优化）
 * 支持指数退避重试策略
 */
class RetryInterceptor(
    private val maxRetries: Int = 3,
    private val retryDelayMs: Long = 1000 // 初始重试延迟1秒
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        var response: Response
        var retryCount = 0

        while (true) {
            try {
                response = chain.proceed(request)

                when {
                    // 成功响应（包括部分内容206）
                    response.isSuccessful || response.code == 206 -> {
                        return response
                    }
                    // 服务器错误（5xx）可以重试
                    response.code in 500..599 && retryCount < maxRetries -> {
                        retryCount++
                        response.close()
                        Thread.sleep(calculateExponentialBackoff(retryCount))
                        continue
                    }
                    // 客户端错误（4xx）通常不应该重试（除非是429 Too Many Requests）
                    response.code == 429 && retryCount < maxRetries -> {
                        retryCount++
                        response.close()
                        val retryAfter = response.header("Retry-After")?.toIntOrNull() ?: 5
                        Thread.sleep(retryAfter * 1000L)
                        continue
                    }
                    else -> {
                        return response
                    }
                }
            } catch (e: Exception) {
                if (retryCount < maxRetries && isRetryableException(e)) {
                    retryCount++
                    Thread.sleep(calculateExponentialBackoff(retryCount))
                    continue
                }
                throw e
            }
        }
    }

    private fun isRetryableException(e: Exception): Boolean {
        return when (e) {
            is java.net.SocketTimeoutException -> true
            is java.net.ConnectException -> true
            is java.net.NoRouteToHostException -> true
            is java.net.PortUnreachableException -> true
            is java.net.UnknownHostException -> true
            is java.net.UnknownServiceException -> true
            is java.net.SocketException -> true
            is java.net.HttpRetryException -> true
            is java.net.ProtocolException -> false
            is javax.net.ssl.SSLException -> false
            is javax.net.ssl.SSLHandshakeException -> false
            else -> e is java.io.IOException
        }
    }

    private fun calculateExponentialBackoff(retryCount: Int): Long {
        return retryDelayMs * (1L shl (retryCount - 1))
    }
}
