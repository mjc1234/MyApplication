package com.mjc

import android.content.Context
import com.mjc.processor.ImageLabelingProcessor
import com.mjc.processor.TextRecognitionProcessor
import com.mjc.processor.VisionImageProcessor

object VisionProcessorFactory {
    fun create(mode: AnalysisMode, context: Context): VisionImageProcessor {
        return when (mode) {
            AnalysisMode.IMAGE_LABELING -> ImageLabelingProcessor(context)
            // 后续扩展：
            AnalysisMode.TEXT_RECOGNITION -> TextRecognitionProcessor(context)
            // AnalysisMode.FACE_DETECTION -> FaceDetectionProcessor(context)
            // AnalysisMode.BARCODE_SCANNING -> BarcodeScanningProcessor(context)
            // AnalysisMode.OBJECT_DETECTION -> ObjectDetectionProcessor(context)
            else -> throw NotImplementedError("Processor for mode $mode is not implemented.")
        }
    }
}
