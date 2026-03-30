# 相机模块指南

## 模块概述

此模块提供了基于CameraX架构的相机功能实现。CameraX是Jetpack组件的一部分，提供了简化且一致的API来处理相机功能。

**模块信息：**
- **包名**: `com.mjc.feature.camera`
- **依赖**: CameraX, Compose, Core模块
- **架构**: MVVM with CameraX UseCases

## CameraX 架构

### 核心组件

1. **ProcessCameraProvider** - 相机生命周期管理
2. **Preview UseCase** - 相机预览
3. **ImageAnalysis UseCase** - 图像分析
4. **VideoCapture UseCase** - 视频录制

### 架构图
```
CameraScreen (UI层)
    ↓
CameraViewModel (ViewModel层)
    ↓
CameraController (控制器层)
    ↓
CameraX UseCases (相机操作层)
    ↓
CameraProvider (相机服务层)
```

## CameraX 1.6 新特性与架构更新 (2026)

CameraX 1.6 引入了多项重要改进和新的架构模式，特别是对 Jetpack Compose 的原生支持。

### 1. Compose 原生集成
- **CameraXViewfinder**：新的 Compose-native 组件，取代了传统的 `AndroidView(PreviewView)` 模式
- **两种集成模式**：
  - **EXTERNAL 模式**：高性能模式，预览与 Compose 树分离
  - **EMBEDDED 模式**：完全 Compose 集成，支持 ContentScale 和 Alignment 修饰符
- **生命周期管理**：自动与 Compose 生命周期同步，无需手动生命周期管理

### 2. SessionConfig API
- **集中式配置**：统一管理所有用例绑定和功能组
- **Feature Groups**：允许同时配置多个功能（如 HDR、60 FPS、预览稳定化）
- **动态配置**：运行时更改相机参数而无需重新绑定

### 3. 增强的视频功能
- **高速录制**：支持 120/240 FPS 慢动作视频
- **HDR 视频**：改进的 HDR 视频支持，与 Camera Extensions 集成
- **CameraPipe 集成**：基于 Pixel 相机堆栈的统一相机管道，提供更好的性能和一致性

### 4. 高级相机控制
- **物理镜头选择**：直接访问多相机设备上的单个物理镜头
- **RAW/DNG 捕获**：原生 RAW 格式支持
- **零快门延迟改进**：更快的拍摄响应时间

### 5. 架构推荐 (2026)
```kotlin
// 推荐的 2026 架构模式
CameraScreen (Compose UI)
    ↓
CameraViewModel (状态管理)
    ↓
CameraSession (SessionConfig 管理)
    ↓
CameraX UseCases (Preview, Capture, Analysis, Video)
    ↓
CameraProvider (CameraPipe 后端)
```

## 代码结构

```
feature/camera/src/main/java/com/mjc/feature/camera/
├── CameraScreen.kt           # 主相机UI界面
├── CameraViewModel.kt        # 相机状态管理
├── controller/              # 相机控制器
│   ├── CameraController.kt  # 相机控制逻辑
│   ├── PermissionController.kt # 权限处理
│   └── CameraState.kt       # 相机状态定义
├── usercase/               # CameraX用例
│   ├── CameraPreviewUseCase.kt
│   ├── ImageAnalysisUseCase.kt
│   └── VideoCaptureUseCase.kt
├── ui/                     # 相机UI组件
│   ├── CameraPreview.kt    # 预览组件
│   ├── CameraControls.kt   # 控制按钮
│   ├── CameraOverlay.kt    # 叠加层(网格、比例等)
│   └── CaptureResult.kt    # 拍摄结果展示
└── utils/                  # 工具类
    ├── CameraUtils.kt      # 相机工具
    ├── ImageProcessor.kt   # 图像处理
    └── FileSaver.kt        # 文件保存
```

## 实现指南

### 1. 相机权限配置

**AndroidManifest.xml 权限声明：**
```xml
<uses-permission android:name="android.permission.CAMERA" />
<uses-permission android:name="android.permission.RECORD_AUDIO" />
<uses-feature android:name="android.hardware.camera" />
<uses-feature android:name="android.hardware.camera.autofocus" />
```

**权限请求流程：**
```kotlin
// 检查并请求权限
val permissions = arrayOf(
    Manifest.permission.CAMERA,
    Manifest.permission.RECORD_AUDIO
)

if (hasPermissions(permissions)) {
    startCamera()
} else {
    requestPermissions(permissions)
}
```

### 2. CameraX 配置

**build.gradle.kts 依赖：**
```kotlin
dependencies {
    // CameraX核心库
    implementation(libs.camerax.core)
    implementation(libs.camerax.camera2)
    implementation(libs.camerax.lifecycle)

    // CameraX用例
    implementation(libs.camerax.view)
    implementation(libs.camerax.video)

    // Compose集成
    implementation(libs.camerax.compose)
}
```

**版本目录配置 (gradle/libs.versions.toml)：**
```toml
[versions]
camerax = "1.6.0"

[libraries]
camerax-core = { group = "androidx.camera", name = "camera-core", version.ref = "camerax" }
camerax-camera2 = { group = "androidx.camera", name = "camera-camera2", version.ref = "camerax" }
camerax-lifecycle = { group = "androidx.camera", name = "camera-lifecycle", version.ref = "camerax" }
camerax-view = { group = "androidx.camera", name = "camera-view", version.ref = "camerax" }
camerax-video = { group = "androidx.camera", name = "camera-video", version.ref = "camerax" }
camerax-compose = { group = "androidx.camera", name = "camera-compose", version.ref = "camerax" }
```

### 3. 相机初始化

**CameraController 示例：**
```kotlin
class CameraController(
    private val context: Context,
    private val lifecycleOwner: LifecycleOwner
) {
    private lateinit var cameraProvider: ProcessCameraProvider
    private lateinit var previewUseCase: Preview
    private lateinit var imageCapture: ImageCapture
    private lateinit var imageAnalyzer: ImageAnalysis

    suspend fun initializeCamera() {
        cameraProvider = ProcessCameraProvider.getInstance(context).await()

        // 配置预览用例
        previewUseCase = Preview.Builder()
            .setTargetResolution(Size(1080, 1920))
            .build()

        // 配置拍照用例
        imageCapture = ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
            .setTargetResolution(Size(4032, 3024))
            .build()

        // 绑定相机到生命周期
        bindCameraToLifecycle()
    }

    private fun bindCameraToLifecycle() {
        val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

        cameraProvider.unbindAll()

        cameraProvider.bindToLifecycle(
            lifecycleOwner,
            cameraSelector,
            previewUseCase,
            imageCapture,
            imageAnalyzer
        )
    }
}
```

### 4. Compose UI 集成

**CameraScreen 示例：**
```kotlin
@Composable
fun CameraScreen(
    viewModel: CameraViewModel = viewModel(),
    onResult: (Uri) -> Unit = {}
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    CameraTheme {
        Box(modifier = Modifier.fillMaxSize()) {
            // 相机预览
            CameraPreview(
                controller = viewModel.cameraController,
                modifier = Modifier.fillMaxSize()
            )

            // 控制按钮
            CameraControls(
                onCapture = { viewModel.captureImage(onResult) },
                onToggleFlash = viewModel::toggleFlash,
                onSwitchCamera = viewModel::switchCamera,
                modifier = Modifier.align(Alignment.BottomCenter)
            )

            // 叠加层
            CameraOverlay(
                showGrid = viewModel.showGrid,
                aspectRatio = viewModel.aspectRatio,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}
```

### 4.1 使用 CameraXViewfinder (2026 推荐)

CameraX 1.6 引入了 `CameraXViewfinder` 组件，提供了原生的 Compose 集成：

```kotlin
@Composable
fun ModernCameraScreen(
    viewModel: CameraViewModel = viewModel(),
    onResult: (Uri) -> Unit = {}
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // 获取 CameraController（来自 viewModel）
    val cameraController = viewModel.cameraController

    CameraTheme {
        Box(modifier = Modifier.fillMaxSize()) {
            // 使用 CameraXViewfinder 替代传统的 PreviewView
            CameraXViewfinder(
                controller = cameraController,
                modifier = Modifier.fillMaxSize(),
                // 可选：配置 ContentScale 和 Alignment
                contentScale = ContentScale.Crop,
                alignment = Alignment.Center
            )

            // 控制按钮（与之前相同）
            CameraControls(
                onCapture = { viewModel.captureImage(onResult) },
                onToggleFlash = viewModel::toggleFlash,
                onSwitchCamera = viewModel::switchCamera,
                modifier = Modifier.align(Alignment.BottomCenter)
            )

            // 叠加层
            CameraOverlay(
                showGrid = viewModel.showGrid,
                aspectRatio = viewModel.aspectRatio,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}
```

**优点：**
- **完全 Compose-native**：无需 `AndroidView` 互操作
- **自动生命周期管理**：与 Compose 生命周期同步
- **更好的性能**：支持 EXTERNAL 和 EMBEDDED 模式
- **标准修饰符支持**：支持 `ContentScale`、`Alignment` 等标准 Compose 修饰符

### 5. 拍照功能

**图像捕获实现（MediaStore集成）：**

本应用使用Android MediaStore API将照片保存到公共的Pictures目录，使照片在图库中可见，并遵循Android 10+的分区存储最佳实践。

**关键特性：**
- 保存到 `Pictures/MyApplication/` 目录
- 使用 `RELATIVE_PATH` 和 `IS_PENDING` 标志（Android 10+）
- 返回内容URI（Content URI）而非文件URI
- 自动处理捕获失败清理

**MediaStore集成实现：**
```kotlin
suspend fun captureImage(): Uri? = withContext(Dispatchers.IO) {
    val imageCapture = _imageCapture ?: return@withContext null

    // 生成文件名
    val fileName = MediaStoreHelper.generateImageFileName()
    val contentValues = MediaStoreHelper.createImageContentValues(fileName)

    // 插入MediaStore获取URI
    val imageUri = try {
        context.contentResolver.insert(
            MediaStoreHelper.getImagesContentUri(),
            contentValues
        )
    } catch (e: Exception) {
        // 插入失败（如存储空间不足、权限问题等）
        return@withContext null
    }

    if (imageUri == null) {
        return@withContext null
    }

    // 创建CameraX输出选项
    val outputOptions = ImageCapture.OutputFileOptions.Builder(
        context.contentResolver,
        imageUri,
        contentValues
    ).build()

    // 使用CompletableDeferred等待异步结果
    val deferred = CompletableDeferred<Uri?>()

    imageCapture.takePicture(
        outputOptions,
        executor,
        object : ImageCapture.OnImageSavedCallback {
            override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                // 成功：标记图片为已完成，使其在图库中可见
                MediaStoreHelper.finalizePendingImage(context.contentResolver, imageUri)
                deferred.complete(imageUri)
            }

            override fun onError(exception: ImageCaptureException) {
                // 失败：清理MediaStore中的待处理记录
                MediaStoreHelper.cleanupFailedImage(context.contentResolver, imageUri)
                deferred.complete(null)
            }
        }
    )

    return@withContext deferred.await()
}
```

**MediaStoreHelper工具类：**
```kotlin
object MediaStoreHelper {
    fun generateImageFileName(): String {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        return "IMG_${timeStamp}.jpg"
    }

    fun createImageContentValues(fileName: String, dateTaken: Long? = null): ContentValues {
        return ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            put(MediaStore.Images.Media.DATE_ADDED, System.currentTimeMillis() / 1000)
            put(MediaStore.Images.Media.DATE_MODIFIED, System.currentTimeMillis() / 1000)
            dateTaken?.let { put(MediaStore.Images.Media.DATE_TAKEN, it) }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/MyApplication/")
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
        }
    }

    fun getImagesContentUri(): Uri {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        } else {
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        }
    }

    fun finalizePendingImage(contentResolver: ContentResolver, uri: Uri) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val values = ContentValues().apply {
                put(MediaStore.Images.Media.IS_PENDING, 0)
            }
            contentResolver.update(uri, values, null, null)
        }
    }

    fun cleanupFailedImage(contentResolver: ContentResolver, uri: Uri) {
        contentResolver.delete(uri, null, null)
    }
}
```

**注意事项：**
1. **Android版本**：最低SDK版本为29（Android 10），支持所有Android 10+特性
2. **权限**：Android 10+无需 `WRITE_EXTERNAL_STORAGE` 权限
3. **内容URI**：返回的是内容URI，需要使用 `ContentResolver.openInputStream()` 访问
4. **图库可见性**：成功保存后照片会出现在系统图库中
5. **存储位置**：照片保存在 `Pictures/MyApplication/` 目录下

### 6. 视频录制

**视频录制实现：**
```kotlin
class VideoCaptureUseCase(
    private val context: Context
) {
    private lateinit var videoCapture: VideoCapture<Recorder>
    private var recording: Recording? = null

    fun startRecording(): File? {
        val outputDir = context.getExternalFilesDir(Environment.DIRECTORY_MOVIES)
        val videoFile = File.createTempFile(
            "VID_${System.currentTimeMillis()}",
            ".mp4",
            outputDir
        )

        val outputOptions = FileOutputOptions.Builder(videoFile).build()

        recording = videoCapture.output
            .prepareRecording(context, outputOptions)
            .withAudioEnabled()
            .start(executor) { event ->
                when (event) {
                    is VideoRecordEvent.Start -> {
                        // 录制开始
                    }
                    is VideoRecordEvent.Finalize -> {
                        // 录制完成
                        if (!event.hasError()) {
                            _recordingResult.value = RecordingResult.Success(videoFile.toUri())
                        }
                    }
                }
            }

        return videoFile
    }

    fun stopRecording() {
        recording?.stop()
        recording = null
    }
}
```

### 7. 图像分析 (集成 ML Kit)

**与ML Kit集成的图像分析：**
```kotlin
class MlKitImageAnalyzer(
    private val mlKitProcessor: MlKitProcessor
) : ImageAnalysis.Analyzer {
    override fun analyze(image: ImageProxy) {
        // 转换为ML Kit可处理的格式
        val inputImage = InputImage.fromMediaImage(
            image.image!!,
            image.imageInfo.rotationDegrees
        )

        // 使用ML Kit处理图像
        mlKitProcessor.processImage(inputImage) { result ->
            // 处理分析结果
            _analysisResult.value = result
        }

        image.close()
    }
}
```

## 最佳实践

### 1. 性能优化

1. **分辨率选择**：
   ```kotlin
   // 根据设备能力选择最佳分辨率
   val cameraInfo = cameraProvider.availableCameraInfos
   val resolutionSelector = ResolutionSelector.Builder()
       .setAllowedResolutions(listOf(Size(1920, 1080), Size(1280, 720)))
       .build()
   ```

2. **资源管理**：
   ```kotlin
   // 及时释放资源
   override fun onCleared() {
       super.onCleared()
       cameraController.release()
       imageAnalyzer.shutdown()
   }
   ```

### 2. 错误处理

```kotlin
sealed class CameraError {
    data class PermissionDenied(val permissions: List<String>) : CameraError()
    data class CameraUnavailable(val exception: CameraUnavailableException) : CameraError()
    data class CaptureFailed(val exception: ImageCaptureException) : CameraError()
    data class RecordingFailed(val exception: VideoCaptureException) : CameraError()
}

fun handleCameraError(error: CameraError) {
    when (error) {
        is CameraError.PermissionDenied -> {
            // 显示权限请求对话框
            showPermissionRationale(error.permissions)
        }
        is CameraError.CameraUnavailable -> {
            // 显示相机不可用提示
            showCameraUnavailableMessage()
        }
        // ... 其他错误处理
    }
}
```

### 3. 测试策略

1. **单元测试**：测试业务逻辑和状态管理
2. **仪器测试**：测试相机权限和基本功能
3. **UI测试**：测试Compose UI组件交互
4. **集成测试**：测试与ML Kit模块的集成

**测试示例：**
```kotlin
@Test
fun `拍照应该保存文件并返回URI`() = runTest {
    // Given
    val mockFile = mockk<File>()
    val mockUri = mockk<Uri>()
    every { mockFile.toUri() } returns mockUri

    // When
    val result = cameraController.captureImage()

    // Then
    assertThat(result).isEqualTo(mockUri)
}
```

## 功能路线图

### 阶段1：基础功能 (当前)
- [ ] 相机预览
- [ ] 拍照功能
- [ ] 基本权限处理
- [ ] 前后摄像头切换

### 阶段2：增强功能
- [ ] 视频录制
- [ ] 闪光灯控制
- [ ] 变焦功能
- [ ] 网格叠加
- [ ] 比例选择 (1:1, 4:3, 16:9)

### 阶段3：高级功能
- [ ] HDR模式
- [ ] 夜景模式
- [ ] 人像模式
- [ ] RAW格式支持
- [ ] 手动控制 (ISO, 快门速度, 白平衡)

### 阶段4：AI集成
- [ ] 与ML Kit集成进行物体识别
- [ ] 智能场景检测
- [ ] 自动构图建议
- [ ] AR叠加效果

## 常见问题

### Q1: CameraX与旧版Camera API的区别？
A: CameraX提供了简化的API、自动生命周期管理、更好的设备兼容性，并且与Jetpack组件深度集成。

### Q2: 如何处理不同设备的兼容性问题？
A: CameraX自动处理大多数兼容性问题，但仍需测试不同Android版本和设备型号。

### Q3: 如何优化相机应用的性能？
A: 选择合适的分辨率、及时释放资源、使用适当的图像处理管道、避免主线程阻塞。

### Q4: 如何测试相机功能？
A: 使用Espresso进行UI测试，Robolectric进行单元测试，在真实设备上进行仪器测试。

## 参考资料

1. [CameraX官方文档](https://developer.android.com/training/camerax)
2. [CameraX Codelab](https://developer.android.com/codelabs/camerax-getting-started)
3. [CameraX GitHub示例](https://github.com/android/camera-samples)
4. [ML Kit相机集成指南](https://developers.google.com/ml-kit/vision)
5. [Compose-Native CameraX in 2026: The Complete Guide](https://proandroiddev.com/compose-native-camerax-in-2026-the-complete-guide-bf36c76a78e9)
6. [CameraX Architecture | Android Developers (2026)](https://scriptagc.wasmer.app/https_developer_android_com/media/camera/camerax/architecture)
7. [CameraX Release Notes (2026)](https://android-dot-devsite-v2-prod.appspot.com/jetpack/androidx/releases/camera?hl=zh-cn)

---
**文档版本**: 1.1
**更新日期**: 2026-03-27
**适用模块**: feature/camera