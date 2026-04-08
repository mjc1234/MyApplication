package com.mjc.feature.download.di

import com.mjc.feature.download.controller.DownloadController
import com.mjc.feature.download.controller.DownloadControllerImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * feature:download 模块 Hilt 绑定
 * 将 DownloadController 接口绑定到 DownloadControllerImpl 实现
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class DownloadControllerModule {

    @Binds
    @Singleton
    abstract fun bindDownloadController(impl: DownloadControllerImpl): DownloadController
}
