package com.mjc.core.download.api

import com.mjc.core.download.utils.DownloadUtils
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.HEAD
import retrofit2.http.Header
import retrofit2.http.Url

/**
 * 下载专用API服务接口
 * 支持断点续传（Range请求）和文件信息获取
 */
interface DownloadApi {

    /**
     * 下载文件（支持断点续传）
     * @param url 文件URL
     * @param range Range请求头（格式：bytes=start-end，如bytes=1024-）
     * @param ifRange 条件请求头，确保资源未改变（ETag或HTTP-date）
     * @param ifMatch 条件请求头，仅当资源匹配给定ETag时返回
     * @param ifModifiedSince 条件请求头，仅当资源在给定时间后修改时返回
     * @return 响应体（支持流式读取）
     */
    @GET
    suspend fun downloadFile(
        @Url url: String,
        @Header("Range") range: String? = null,
        @Header("If-Range") ifRange: String? = null,
        @Header("If-Match") ifMatch: String? = null,
        @Header("If-Modified-Since") ifModifiedSince: String? = null
    ): Response<ResponseBody>

    /**
     * 获取文件信息（HEAD请求）
     * @param url 文件URL
     * @return 响应头（包含Content-Length、Content-Type等信息）
     */
    @HEAD
    suspend fun getFileInfo(@Url url: String): Response<Void>

    /**
     * 获取详细的文件信息（HEAD请求）
     * @param url 文件URL
     * @return 包含文件详细信息的对象
     */
    suspend fun getFileInfoWithDetails(@Url url: String): FileInfoResponse {
        val response = getFileInfo(url)
        return response.toFileInfoResponse()
    }
}

/**
 * 文件信息响应
 * 包含常见的文件元数据
 */
data class FileInfoResponse(
    /** 文件大小（字节） */
    val contentLength: Long?,
    /** 内容类型 */
    val contentType: String?,
    /** 最后修改时间（HTTP-date格式） */
    val lastModified: String?,
    /** ETag（实体标签） */
    val eTag: String?,
    /** 是否支持Range请求 */
    val acceptRanges: String?,
    /** 内容编码 */
    val contentEncoding: String?,
    /** 文件名（从Content-Disposition头提取） */
    val fileName: String?
)

/**
 * 范围请求数据类
 */
data class Range(
    val start: Long,
    val end: Long?
) {
    /**
     * 转换为Range头字符串
     */
    fun toHeaderValue(): String = buildString {
        append("bytes=")
        append(start)
        append("-")
        end?.let { append(it) }
    }
}

/**
 * 多范围请求辅助函数
 */
fun List<Range>.toRangeHeader(): String = buildString {
    append("bytes=")
    this@toRangeHeader.forEachIndexed { index, range ->
        if (index > 0) append(",")
        append(range.start)
        append("-")
        range.end?.let { append(it) }
    }
}

/**
 * 将HEAD响应转换为文件信息对象
 */
fun Response<Void>.toFileInfoResponse(): FileInfoResponse {
    val headers = headers()
    return FileInfoResponse(
        contentLength = headers["Content-Length"]?.toLongOrNull(),
        contentType = headers["Content-Type"],
        lastModified = headers["Last-Modified"],
        eTag = headers["ETag"],
        acceptRanges = headers["Accept-Ranges"],
        contentEncoding = headers["Content-Encoding"],
        fileName = headers["Content-Disposition"]?.let { DownloadUtils.extractFileName(it) }
    )
}
