package com.mjc.core.download.di

import javax.inject.Qualifier

/**
 * 限定符：下载专用 OkHttpClient
 * 区别于 core:network 提供的通用 OkHttpClient
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class DownloadOkHttpClient

/**
 * 限定符：下载专用 Retrofit
 * 区别于 core:network 提供的通用 Retrofit
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class DownloadRetrofit
