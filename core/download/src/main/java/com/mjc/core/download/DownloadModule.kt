package com.mjc.core.download

import android.content.Context
import com.mjc.core.download.api.DownloadApi
import com.mjc.core.download.config.DownloadConfig
import com.mjc.core.download.di.DownloadOkHttpClient
import com.mjc.core.download.di.DownloadRetrofit
import com.mjc.core.download.interceptor.RangeInterceptor
import com.mjc.core.download.interceptor.RetryInterceptor
import com.mjc.core.download.service.DownloadService
import com.mjc.core.download.service.RetrofitDownloadService
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.Cache
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.io.File
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

/**
 * 下载模块 Hilt 依赖注入
 * 提供下载专用的 OkHttpClient、Retrofit、ApiService 和 DownloadService
 *
 * 下载客户端与通用网络客户端隔离：
 * - 更长的超时时间（大文件下载）
 * - 内置 Range 拦截器（断点续传）
 * - 内置重试拦截器（指数退避）
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class DownloadModule {

    /** 绑定 DownloadService 接口到 RetrofitDownloadService 实现 */
    @Binds
    @Singleton
    abstract fun bindDownloadService(impl: RetrofitDownloadService): DownloadService

    companion object {

        /** 下载Retrofit的默认基础URL（虚拟URL，实际使用@Url完整URL） */
        private const val DEFAULT_DOWNLOAD_BASE_URL = "https://download.invalid/"
        private const val DOWNLOAD_CACHE_DIR = "download-cache"
        private const val DOWNLOAD_CACHE_SIZE = 20L * 1024 * 1024 // 20MB

        /**
         * 提供下载专用 OkHttpClient（单例）
         * 针对文件下载优化：
         * - 60秒连接超时，300秒读取超时（大文件）
         * - Range 拦截器（断点续传）
         * - 重试拦截器（指数退避）
         * - 日志拦截器（BASIC 级别）
         * - 20MB 磁盘缓存
         */
        @Provides
        @Singleton
        @DownloadOkHttpClient
        fun provideDownloadOkHttpClient(
            @ApplicationContext context: Context
        ): OkHttpClient {
            val cacheDir = File(context.cacheDir, DOWNLOAD_CACHE_DIR)
            return OkHttpClient.Builder()
                .connectTimeout(60, TimeUnit.SECONDS)
                .readTimeout(300, TimeUnit.SECONDS)
                .writeTimeout(60, TimeUnit.SECONDS)
                .followRedirects(true)
                .followSslRedirects(true)
                .addInterceptor(RangeInterceptor())
                .addInterceptor(RetryInterceptor())
                .addInterceptor(
                    HttpLoggingInterceptor().apply {
                        level = HttpLoggingInterceptor.Level.BASIC
                    }
                )
                .cache(Cache(cacheDir, DOWNLOAD_CACHE_SIZE))
                .build()
        }

        /**
         * 提供下载专用 Retrofit 实例（单例）
         * baseUrl 为虚拟 URL，DownloadApiService 使用 @Url 传入完整 URL
         */
        @Provides
        @Singleton
        @DownloadRetrofit
        fun provideDownloadRetrofit(
            @DownloadOkHttpClient client: OkHttpClient
        ): Retrofit {
            return Retrofit.Builder()
                .baseUrl(DEFAULT_DOWNLOAD_BASE_URL)
                .client(client)
                .addConverterFactory(MoshiConverterFactory.create())
                .build()
        }

        /**
         * 提供 DownloadApiService 实例（单例）
         */
        @Provides
        @Singleton
        fun provideDownloadApiService(
            @DownloadRetrofit retrofit: Retrofit
        ): DownloadApi {
            return retrofit.create(DownloadApi::class.java)
        }

        /**
         * 提供默认下载配置（单例）
         */
        @Provides
        @Singleton
        fun provideDownloadConfig(): DownloadConfig = DownloadConfig.default()
    }
}
