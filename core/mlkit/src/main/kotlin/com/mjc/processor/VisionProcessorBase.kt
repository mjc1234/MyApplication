package com.mjc.processor

import android.annotation.SuppressLint
import android.app.ActivityManager
import android.content.Context
import android.graphics.Bitmap
import android.os.SystemClock
import android.util.Log
import androidx.annotation.GuardedBy
import com.google.android.gms.tasks.OnFailureListener
import com.google.android.gms.tasks.OnSuccessListener
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.TaskExecutors
import com.google.android.gms.tasks.Tasks
import com.google.android.odml.image.MlImage
import com.mjc.core.mlkit.util.FrameMetadata
import com.mjc.core.mlkit.util.ScopedExecutor
import com.google.mlkit.common.MlKitException
import com.google.mlkit.vision.common.InputImage
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import java.nio.ByteBuffer
import java.util.Timer
import java.util.TimerTask

/**
 * Abstract base class for ML Kit frame processors. Subclasses need to implement {@link
 * #onSuccess(T, FrameMetadata, GraphicOverlay)} to define what they want to with the detection
 * results and {@link #detectInImage(VisionImage)} to specify the detector object.
 *
 * @param <T> The type of the detected feature.
 */
@SuppressLint("DiscouragedApi")
abstract class VisionProcessorBase<T>(context: Context) : VisionImageProcessor {

    companion object {
        const val MANUAL_TESTING_LOG = "LogTagForTest"
        private const val TAG = "VisionProcessorBase"
    }

    private var activityManager: ActivityManager =
        context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
    private val fpsTimer = Timer("VisionProcessorBase-FpsTimer", true)
    private val executor = ScopedExecutor(TaskExecutors.MAIN_THREAD)

    // Whether this processor is already shut down
    @Volatile
    private var isShutdown = false

    // Used to calculate latency. Accessed from executor callbacks and Timer thread.
    @Volatile private var numRuns = 0
    @Volatile private var totalFrameMs = 0L
    @Volatile private var maxFrameMs = 0L
    @Volatile private var minFrameMs = Long.MAX_VALUE
    @Volatile private var totalDetectorMs = 0L
    @Volatile private var maxDetectorMs = 0L
    @Volatile private var minDetectorMs = Long.MAX_VALUE

    // Frame count that have been processed so far in an one second interval to calculate FPS.
    @Volatile private var frameProcessedInOneSecondInterval = 0
    @Volatile private var framesPerSecond = 0

    // To keep the latest images and its metadata.
    @GuardedBy("frameLock") private var latestImage: ByteBuffer? = null
    @GuardedBy("frameLock") private var latestImageMetaData: FrameMetadata? = null
    // To keep the images and metadata in process.
    @GuardedBy("frameLock") private var processingImage: ByteBuffer? = null
    @GuardedBy("frameLock") private var processingMetaData: FrameMetadata? = null

    private val frameLock = Any()

    // Flow-based result delivery
    private val _resultFlow = MutableSharedFlow<T>(
        replay = 0,
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val resultFlow: SharedFlow<T> = _resultFlow.asSharedFlow()

    init {
        fpsTimer.scheduleAtFixedRate(
            object : TimerTask() {
                override fun run() {
                    framesPerSecond = frameProcessedInOneSecondInterval
                    frameProcessedInOneSecondInterval = 0
                }
            },
            0,
            1000
        )
    }

    // -----------------Code for processing single still image----------------------------------------
    override fun processBitmap(bitmap: Bitmap) {
        val frameStartMs = SystemClock.elapsedRealtime()

        requestDetectInImage(
            InputImage.fromBitmap(bitmap, 0),
            /* shouldShowFps= */ false,
            frameStartMs
        )
    }

    // -----------------Code for processing live preview frame from Camera1 API-----------------------
    override fun processInputImage(
        data: ByteBuffer?,
        frameMetadata: FrameMetadata?,
    ) {
        synchronized(frameLock) {
            latestImage = data
            latestImageMetaData = frameMetadata
            if (processingImage == null && processingMetaData == null) {
                processLatestImage()
            }
        }
    }

    override fun processInputImage(inputImage: InputImage, onComplete: () -> Unit) {
        val frameStartMs = SystemClock.elapsedRealtime()
        requestDetectInImage(
            inputImage,
            true,
            frameStartMs
        )
            // When the image is from CameraX analysis use case, must call image.close() on received
            // images when finished using them. Otherwise, new images may not be received or the camera
            // may stall.
            .addOnCompleteListener { onComplete() }
    }

    private fun processLatestImage() {
        val (image, metadata) = synchronized(frameLock) {
            val img = latestImage
            val meta = latestImageMetaData
            latestImage = null
            latestImageMetaData = null
            if (img != null && meta != null) {
                processingImage = img
                processingMetaData = meta
                Pair(img, meta)
            } else {
                null
            }
        } ?: return

        if (!isShutdown) {
            processImage(image, metadata)
        }
    }

    private fun processImage(
        data: ByteBuffer,
        frameMetadata: FrameMetadata
    ) {
        val frameStartMs = SystemClock.elapsedRealtime()

        requestDetectInImage(
            InputImage.fromByteBuffer(
                data,
                frameMetadata.width,
                frameMetadata.height,
                frameMetadata.rotation,
                InputImage.IMAGE_FORMAT_NV21
            ),
            /* shouldShowFps= */ true,
            frameStartMs
        )
            .addOnSuccessListener(executor) { processLatestImage() }
    }

    // -----------------Common processing logic-------------------------------------------------------
    private fun requestDetectInImage(
        image: InputImage,
        shouldShowFps: Boolean,
        frameStartMs: Long
    ): Task<T> {
        return setUpListener(
            detectInImage(image),
            shouldShowFps,
            frameStartMs
        )
    }

    private fun setUpListener(
        task: Task<T>,
        shouldShowFps: Boolean,
        frameStartMs: Long
    ): Task<T> {
        val detectorStartMs = SystemClock.elapsedRealtime()
        return task
            .addOnSuccessListener(
                executor,
                OnSuccessListener { results: T ->
                    val endMs = SystemClock.elapsedRealtime()
                    val currentFrameLatencyMs = endMs - frameStartMs
                    val currentDetectorLatencyMs = endMs - detectorStartMs
                    if (numRuns >= 500) {
                        resetLatencyStats()
                    }
                    numRuns++
                    frameProcessedInOneSecondInterval++
                    totalFrameMs += currentFrameLatencyMs
                    maxFrameMs = currentFrameLatencyMs.coerceAtLeast(maxFrameMs)
                    minFrameMs = currentFrameLatencyMs.coerceAtMost(minFrameMs)
                    totalDetectorMs += currentDetectorLatencyMs
                    maxDetectorMs = currentDetectorLatencyMs.coerceAtLeast(maxDetectorMs)
                    minDetectorMs = currentDetectorLatencyMs.coerceAtMost(minDetectorMs)

                    // Only log inference info once per second. When frameProcessedInOneSecondInterval is
                    // equal to 1, it means this is the first frame processed during the current second.
                    if (frameProcessedInOneSecondInterval == 1) {
                        Log.d(TAG, "Num of Runs: $numRuns")
                        Log.d(
                            TAG,
                            "Frame latency: max=" +
                                    maxFrameMs +
                                    ", min=" +
                                    minFrameMs +
                                    ", avg=" +
                                    totalFrameMs / numRuns
                        )
                        Log.d(
                            TAG,
                            "Detector latency: max=" +
                                    maxDetectorMs +
                                    ", min=" +
                                    minDetectorMs +
                                    ", avg=" +
                                    totalDetectorMs / numRuns
                        )
                        val mi = ActivityManager.MemoryInfo()
                        activityManager.getMemoryInfo(mi)
                        val availableMegs: Long = mi.availMem / 0x100000L
                        Log.d(TAG, "Memory available in system: $availableMegs MB")
                        if (shouldShowFps) {
                            Log.d(TAG, "Vision Process FPS: $framesPerSecond")
                        }
                    }
                    this@VisionProcessorBase.onSuccess(results)
                    _resultFlow.tryEmit(results)
                }
            )
            .addOnFailureListener(
                executor,
                OnFailureListener { e: Exception ->
                    val error = "Failed to process. Error: " + e.localizedMessage
                    Log.d(TAG, error)
                    e.printStackTrace()
                    this@VisionProcessorBase.onFailure(e)
                }
            )
    }

    override fun stop() {
        executor.shutdown()
        isShutdown = true
        resetLatencyStats()
        fpsTimer.cancel()
    }

    private fun resetLatencyStats() {
        numRuns = 0
        totalFrameMs = 0
        maxFrameMs = 0
        minFrameMs = Long.MAX_VALUE
        totalDetectorMs = 0
        maxDetectorMs = 0
        minDetectorMs = Long.MAX_VALUE
    }

    protected abstract fun detectInImage(image: InputImage): Task<T>

    protected open fun detectInImage(image: MlImage): Task<T> {
        return Tasks.forException(
            MlKitException(
                "MlImage is currently not demonstrated for this feature",
                MlKitException.INVALID_ARGUMENT
            )
        )
    }

    protected abstract fun onSuccess(results: T)

    protected abstract fun onFailure(e: Exception)

    protected open fun isMlImageEnabled(context: Context?): Boolean {
        return false
    }
}