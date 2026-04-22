# core:download 模块开发指南

## 模块概述

`core:download` 是一个基于 OkHttp/Retrofit 的文件下载核心模块，提供断点续传、进度上报、自动重试等功能。通过 `DownloadService` 接口实现网络层解耦，功能模块不直接依赖 Retrofit。下载进度和结果通过 Kotlin Flow 以 `DownloadEvent` 事件流形式上报。

- **命名空间**: `com.mjc.core.download`
- **类型**: Android Library
- **Min SDK**: 29

## 源码结构

```
core/download/src/main/java/com/mjc/core/download/
├── DownloadModule.kt              # 依赖注入入口，创建 HttpClient/Service
├── api/
│   └── DownloadApiService.kt      # Retrofit API 接口定义
├── client/
│   └── DownloadHttpClient.kt      # OkHttp 客户端工厂（含重试拦截器）
├── config/
│   └── DownloadConfig.kt          # 下载配置（超时、重试、缓存等）
├── executor/
│   └── DownloadExecutor.kt        # 下载执行器（核心下载逻辑+Flow事件流）
├── ext/
│   └── DownloadResponseExt.kt     # ResponseBody/Response 扩展函数
├── interceptor/
│   └── RangeInterceptor.kt        # Range 请求头验证拦截器
├── model/
│   └── DownloadTask.kt            # 数据模型（DownloadTask, DownloadStatus, DownloadError 等）
├── service/
│   ├── DownloadService.kt         # 下载服务抽象接口（解耦 Retrofit）
│   └── RetrofitDownloadService.kt # DownloadService 的 Retrofit 实现
└── utils/
    └── DownloadUtils.kt           # 工具类（文件大小格式化、Range解析等）
```

## 核心架构

### 分层设计

```
功能模块 (feature:download)
       │
       ▼
DownloadExecutor          ← 下载执行器，封装通用下载逻辑
       │
       ▼
DownloadService (接口)    ← 抽象层，解耦 HTTP 客户端
       │
       ▼
RetrofitDownloadService   ← Retrofit 实现
       │
       ▼
DownloadApiService        ← Retrofit API 接口
```

### 关键组件说明

**DownloadModule** - 模块入口
- 单例对象，负责创建和缓存 OkHttpClient、Retrofit、Service 实例
- 所有外部依赖通过此模块获取

**DownloadService** - 服务抽象接口
- `downloadFile(url, range, ifRange, ...)` - 下载文件，支持断点续传
- `getFileInfo(url)` - HEAD 请求获取文件元信息
- `getFileInfoWithDetails(url)` - 获取详细的文件信息

**DownloadExecutor** - 下载执行器
- `execute(url, targetFile, resumeFromBytes, etag, ...)` - 执行下载，返回 `Flow<DownloadEvent>`
- `getFileInfo(url)` - 获取文件信息，返回 `FileInfoResult`
- 进度事件发射间隔：250ms

**DownloadConfig** - 配置项
- 连接超时：60s，读取超时：300s，写入超时：60s
- 默认启用 Range 支持（断点续传）
- 默认启用重试（最多3次，1s初始延迟）
- 缓冲区大小：8KB

## 使用方式

### 基本用法

```kotlin
// 1. 创建 Service
val downloadService = DownloadModule.createDownloadService()

// 2. 创建 Executor
val executor = DownloadExecutor(downloadService)

// 3. 执行下载，收集 Flow 事件流
executor.execute(
    url = "https://example.com/file.zip",
    targetFile = File("/sdcard/Download/file.zip")
).collect { event ->
    when (event) {
        is DownloadEvent.Progress -> {
            Log.d("Download", "进度: ${(event.progress.progress * 100).toInt()}%")
        }
        is DownloadEvent.Success -> {
            Log.d("Download", "下载完成: ${event.result.totalBytes} bytes")
        }
        is DownloadEvent.Failure -> {
            Log.e("Download", "下载失败: ${event.result.error?.message}")
        }
    }
}
```

### 断点续传

```kotlin
// 获取已下载字节数，从该位置继续
val existingBytes = targetFile.length()
executor.execute(
    url = url,
    targetFile = targetFile,
    resumeFromBytes = existingBytes,
    etag = fileInfo.eTag,  // 用于验证资源未改变
    lastModified = fileInfo.lastModified
).collect { event ->
    // 处理下载事件
}
```

### 自定义配置

```kotlin
val config = DownloadConfig(
    connectTimeoutMs = 30_000L,
    maxRetries = 5,
    enableLogging = true,
    loggingLevel = LoggingLevel.HEADERS
)
val client = DownloadModule.createHttpClient(config)
val service = DownloadModule.createDownloadService(client = client)
```

## 数据模型

| 类型 | 说明 |
|------|------|
| `DownloadTask` | 下载任务状态（id, url, status, progress, speed 等） |
| `DownloadStatus` | 枚举：PENDING, DOWNLOADING, PAUSED, COMPLETED, FAILED, CANCELLED |
| `DownloadError` | 密封类：NetworkError, StorageError, PermissionError, FileError, Cancelled, UnknownError |
| `DownloadResult` | 下载结果（success, downloadedBytes, totalBytes, error） |
| `DownloadProgress` | 进度数据（downloadedBytes, totalBytes, speedBytesPerSecond, progress） |
| `DownloadEvent` | 下载事件流（Progress/Success/Failure），通过 Flow 发射 |
| `ResumeData` | 断点续传数据（tempFilePath, etag, lastModified） |

## 依赖关系

```kotlin
// 本模块依赖
api(project(":core:network"))   // 传递网络层依赖
implementation(libs.okhttp)
implementation(libs.okhttp.logging.interceptor)
implementation(libs.retrofit)
implementation(libs.retrofit.converter.moshi)
implementation(libs.moshi)
implementation(libs.moshi.kotlin)
implementation(libs.kotlinx.coroutines.android)
```

## 注意事项

- DownloadExecutor 使用 `Dispatchers.IO` 执行下载操作
- 进度事件通过 `Flow<DownloadEvent>` 发射，频率限制为每 250ms 一次，避免 UI 线程压力
- 取消下载通过协程取消机制（`CancellationException`）实现
- OkHttp 客户端实例有内部缓存，相同配置会复用同一实例
- Range 请求头格式为 `bytes=start-end`（如 `bytes=1024-`）
- 206 状态码表示部分内容响应（断点续传成功）
- `DownloadModule.DEFAULT_DOWNLOAD_BASE_URL = "https://download.invalid/"` 为虚拟 URL，实际通过 `@Url` 参数传入完整 URL
