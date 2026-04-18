package com.mjc.processor

import android.content.Context
import android.util.Log
import com.google.android.gms.tasks.Task
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.label.ImageLabel
import com.google.mlkit.vision.label.ImageLabeler
import com.google.mlkit.vision.label.ImageLabeling
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions
import java.io.IOException

class ImageLabelingProcessor(context: Context): VisionProcessorBase<List<ImageLabel>>(context) {
    private val imageLabeler: ImageLabeler = ImageLabeling.getClient(ImageLabelerOptions.DEFAULT_OPTIONS)

    override fun stop() {
        super.stop()
        try {
            imageLabeler.close()
        } catch (e: IOException) {
            Log.e(
                TAG,
                "Exception thrown while trying to close ImageLabelerClient: $e"
            )
        }
    }

    override fun detectInImage(image: InputImage): Task<List<ImageLabel>> {
        return imageLabeler.process(image)
    }

    override fun onSuccess(results: List<ImageLabel>) {
        logExtrasForTesting(results)
    }

    override fun onFailure(e: Exception) {
        Log.w(TAG, "Label detection failed.$e")
    }

    companion object {
        private const val TAG = "LabelDetectorProcessor"

        private fun logExtrasForTesting(labels: List<ImageLabel>?) {
            if (labels == null) {
                Log.v(MANUAL_TESTING_LOG, "No labels detected")
            } else {
                for (label in labels) {
                    Log.v(
                        MANUAL_TESTING_LOG,
                        String.format("Label %s, confidence %f", label.text, label.confidence)
                    )
                }
            }
        }
    }
}