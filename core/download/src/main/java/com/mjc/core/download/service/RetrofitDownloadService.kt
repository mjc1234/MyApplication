package com.mjc.core.download.service

import android.util.Log
import com.mjc.core.download.api.DownloadApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.ResponseBody
import retrofit2.Response
import javax.inject.Inject

/**
 * Retrofit实现的下载服务
 * 将Retrofit响应转换为不依赖Retrofit的抽象类型
 */
class RetrofitDownloadService @Inject constructor(
    private val downloadApi: DownloadApi
) : DownloadService {

    override suspend fun downloadFile(
        url: String,
        range: String?,
        ifRange: String?,
        ifMatch: String?,
        ifModifiedSince: String?
    ): DownloadResponse = withContext(Dispatchers.IO) {
        Log.d("Debug", "download file url $url range $range, ifRange $ifRange")
        val response = downloadApi.downloadFile(
            url = url,
            range = range,
            ifRange = ifRange,
            ifMatch = ifMatch,
            ifModifiedSince = ifModifiedSince
        )
        convertResponse(response)
    }

    override suspend fun getFileInfo(url: String): FileInfoResponse = withContext(Dispatchers.IO) {
        val response = downloadApi.getFileInfo(url)
        convertFileInfoResponse(response)
    }

    /**
     * 将Retrofit响应转换为DownloadResponse
     */
    private fun convertResponse(response: Response<ResponseBody>): DownloadResponse {
        val responseBody = response.body()
        val headers = response.headers().toMap()

        return DownloadResponse(
            statusCode = response.code(),
            isSuccessful = response.isSuccessful,
            headers = headers,
            bodyStream = responseBody?.byteStream(),
            contentLength = responseBody?.contentLength() ?: -1L
        )
    }

    /**
     * 将Retrofit HEAD响应转换为FileInfoResponse
     */
    private fun convertFileInfoResponse(response: Response<Void>): FileInfoResponse {
        val headers = response.headers().toMap()

        return FileInfoResponse(
            statusCode = response.code(),
            isSuccessful = response.isSuccessful,
            headers = headers
        )
    }

    /**
     * 将okhttp3.Headers转换为Map<String, String>
     */
    private fun okhttp3.Headers.toMap(): Map<String, String> {
        val map = mutableMapOf<String, String>()
        for (i in 0 until size) {
            val name = name(i)
            val value = value(i)
            map[name] = value
        }
        return map
    }
}
