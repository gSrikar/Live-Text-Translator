package com.gsrikar.livetexttranslator.analyze

import android.util.Log
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.gsrikar.livetexttranslator.BuildConfig
import java.nio.ByteBuffer
import java.util.concurrent.TimeUnit


// Log cat tag
private val TAG = LiveTextAnalyzer::class.java.simpleName
// True for debug builds and false otherwise
private val DBG = BuildConfig.DEBUG

/**
 * Analyze the image to find text in the frame
 */
class LiveTextAnalyzer : ImageAnalysis.Analyzer {

    /**
     * Keep track of the last analyzed time
     */
    private var lastAnalyzedTime = 0L

    override fun analyze(image: ImageProxy?, rotationDegrees: Int) {
        if (DBG) Log.d(TAG, "Rotation in degrees: $rotationDegrees")
        if (DBG) Log.d(TAG, "Image Height: ${image?.image?.height}")
        if (DBG) Log.d(TAG, "Image Width: ${image?.image?.width}")
        val currentTimeMillis = System.currentTimeMillis()
        // Find the text every second instead of every frame
        if (currentTimeMillis - lastAnalyzedTime > TimeUnit.SECONDS.toMillis(1)) {
            // Get the plane 0 byte array
            val data = image?.planes?.get(0)?.buffer?.toByteArray()

            // Update the last analyzed time
            lastAnalyzedTime = System.currentTimeMillis()
        }
    }

}

/**
 * Extension to convert the byte buffer to byte array
 */
private fun ByteBuffer.toByteArray(): ByteArray {
    // Rewind the buffer to zero
    rewind()
    val data = ByteArray(remaining())
    // Copy the buffer into a byte array
    get(data)
    // Return the byte array
    return data
}
