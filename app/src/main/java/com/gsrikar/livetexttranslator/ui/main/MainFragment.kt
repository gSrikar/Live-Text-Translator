package com.gsrikar.livetexttranslator.ui.main

import android.Manifest
import android.app.Activity.RESULT_OK
import android.content.Intent
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.HandlerThread
import android.provider.Settings
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.camera.core.*
import androidx.core.content.ContextCompat.checkSelfPermission
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProviders
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.snackbar.Snackbar.*
import com.google.firebase.analytics.FirebaseAnalytics
import com.gsrikar.livetexttranslator.BuildConfig
import com.gsrikar.livetexttranslator.R
import com.gsrikar.livetexttranslator.analytics.BUNDLE_EXTRA_CAMERA_PERMISSION_GRANTED
import com.gsrikar.livetexttranslator.analytics.BUNDLE_EXTRA_STORAGE_PERMISSION_GRANTED
import com.gsrikar.livetexttranslator.analytics.FIREBASE_ANALYTICS_EVENT_CAPTURE_IMAGE
import com.gsrikar.livetexttranslator.analytics.FIREBASE_ANALYTICS_EVENT_RUNTIME_PERMISSION
import com.gsrikar.livetexttranslator.analyze.LiveTextAnalyzer
import kotlinx.android.synthetic.main.main_fragment.*
import java.io.File
import java.io.IOException


// Request code for runtime permissions
private const val REQUEST_CODE_REQUEST_RUNTIME_PERMISSION = 8912
// Request code for settings activity
private const val REQUEST_CODE_REQUEST_CAMERA_PERMISSION_FROM_SETTINGS = 8913

// Run time permissions
private const val CAMERA_PERMISSION = Manifest.permission.CAMERA
private const val STORAGE_PERMISSION = Manifest.permission.WRITE_EXTERNAL_STORAGE

private val RUNTIME_PERMISSION = arrayOf(CAMERA_PERMISSION, STORAGE_PERMISSION)

// Log cat tag
private val TAG = MainFragment::class.java.simpleName
// True for debug builds and false otherwise
private val DBG = BuildConfig.DEBUG

// Handler thread name
private const val THREAD_NAME_LIVE_TEXT_IMAGE_ANALYSIS = "thread-name-live-text-image-analysis"

class MainFragment : Fragment() {

    private val firebaseAnalytics = context?.let { FirebaseAnalytics.getInstance(it) }

    private lateinit var viewModel: MainViewModel

    private lateinit var preview: Preview

    private lateinit var imageCapture: ImageCapture
    private lateinit var analyzerUseCase: ImageAnalysis

    private val imageSavedListener =
        object : ImageCapture.OnImageSavedListener {
            override fun onImageSaved(file: File) {
                Snackbar.make(
                    main, "Image saved successfully at ${file.path}",
                    LENGTH_SHORT
                ).show()
            }

            override fun onError(useCaseError: ImageCapture.UseCaseError, message: String, cause: Throwable?) {
                Snackbar.make(
                    main, "Image capture failed: $message",
                    LENGTH_LONG
                ).show()
                cause?.printStackTrace()
            }

        }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.main_fragment, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewModel = ViewModelProviders.of(this).get(MainViewModel::class.java)

        setPreview()
        setImageAnalysisConfig()
        setListeners()
        checkPermission()
    }

    private fun setListeners() {
        captureImageButton.setOnClickListener {
            trackCapture()
            captureImage()
        }

        // Listen to the preview output updates
        preview.setOnPreviewOutputUpdateListener {
            cameraTextureView.surfaceTexture = it.surfaceTexture
        }
    }

    private fun trackCapture() {
        firebaseAnalytics?.logEvent(FIREBASE_ANALYTICS_EVENT_CAPTURE_IMAGE, null)
    }

    private fun trackPermissionGranted(isCameraGranted: Boolean, isStorageGranted: Boolean) {
        val bundle = Bundle().apply {
            putString(BUNDLE_EXTRA_CAMERA_PERMISSION_GRANTED, isCameraGranted.toString())
            putString(BUNDLE_EXTRA_STORAGE_PERMISSION_GRANTED, isStorageGranted.toString())
        }
        firebaseAnalytics?.logEvent(FIREBASE_ANALYTICS_EVENT_RUNTIME_PERMISSION, bundle)
    }

    private fun setPreview() {
        val rotation = activity?.windowManager?.defaultDisplay?.rotation ?: 0
        if (DBG) Log.d(TAG, "Rotation $rotation")
        val previewConfig = PreviewConfig.Builder()
            .setLensFacing(CameraX.LensFacing.BACK)
            .setTargetRotation(rotation)
            .build()
        preview = Preview(previewConfig)

        val imageCaptureConfig = ImageCaptureConfig.Builder()
            .setCaptureMode(ImageCapture.CaptureMode.MAX_QUALITY)
            .setFlashMode(FlashMode.AUTO)
            .build()

        // Initialize the capture
        imageCapture = ImageCapture(imageCaptureConfig)
    }

    private fun setImageAnalysisConfig() {
        val config = ImageAnalysisConfig.Builder().apply {
            // Create a handler thread and start it
            val analyzerThread = HandlerThread(THREAD_NAME_LIVE_TEXT_IMAGE_ANALYSIS).apply { start() }
            // Receive the callbacks on the background thread
            setCallbackHandler(Handler(analyzerThread.looper))

            // Use the latest images for the analysis
            setImageReaderMode(ImageAnalysis.ImageReaderMode.ACQUIRE_LATEST_IMAGE)
        }.build()

        // Create an image analyser
        analyzerUseCase = ImageAnalysis(config).apply {
            analyzer = LiveTextAnalyzer()
        }
    }

    private fun checkPermission() {
        if (isPermissionGranted()) {
            prepareToStartCamera()
        } else {
            requestPermission()
        }
    }

    private fun requestPermission() {
        requestPermissions(
            RUNTIME_PERMISSION,
            REQUEST_CODE_REQUEST_RUNTIME_PERMISSION
        )
    }

    private fun isPermissionGranted(): Boolean {
        return context?.let {
            checkSelfPermission(it, CAMERA_PERMISSION) == PERMISSION_GRANTED &&
                    checkSelfPermission(it, STORAGE_PERMISSION) == PERMISSION_GRANTED
        } ?: false
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            REQUEST_CODE_REQUEST_CAMERA_PERMISSION_FROM_SETTINGS ->
                checkPermissionGrantedSettings(resultCode)
        }
    }

    private fun checkPermissionGrantedSettings(resultCode: Int) {
        if (resultCode == RESULT_OK) {
            prepareToStartCamera()
        } else {
            showRationaleText()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            REQUEST_CODE_REQUEST_RUNTIME_PERMISSION -> checkPermissionGranted()
        }
    }

    private fun checkPermissionGranted() {
        when {
            isPermissionGranted() -> prepareToStartCamera()
            shouldShowRational() -> checkPermission()
            else -> showRationaleText()
        }
    }

    private fun shouldShowRational(): Boolean {
        return shouldShowRequestPermissionRationale(CAMERA_PERMISSION) ||
                shouldShowRequestPermissionRationale(STORAGE_PERMISSION)
    }

    private fun showRationaleText() {
        Snackbar.make(
            main,
            R.string.text_permission_rationale,
            LENGTH_INDEFINITE
        ).setAction(getString(R.string.action_snackbar_settings)) {
            openSettings()
        }.show()
    }

    private fun openSettings() {
        val intent = Intent().apply {
            action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
            data = Uri.fromParts("package", context?.packageName, null)
        }
        if (isSettingsAppExists(intent)) {
            Snackbar.make(
                main,
                getString(R.string.no_settings_exists),
                LENGTH_INDEFINITE
            ).show()
        } else {
            startActivityForResult(intent, REQUEST_CODE_REQUEST_CAMERA_PERMISSION_FROM_SETTINGS)
        }
    }

    private fun isSettingsAppExists(intent: Intent): Boolean {
        return context?.let { intent.resolveActivity(it.packageManager) } == null
    }

    /**
     * Make sure the view is inflated before camera starts
     */
    private fun prepareToStartCamera() {
        cameraTextureView.post { startCamera() }
    }

    private fun startCamera() {
        // Bind the preview a lifecycle
        // Based on the live data lifecycle changes, CameraX decides
        // when to start the camera preview and when to stop
        CameraX.bindToLifecycle(this, preview, imageCapture, analyzerUseCase)
    }

    private fun captureImage() {
        // Decide the location of the picture to be saved to
        val file = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM),
            "IMG_${System.currentTimeMillis()}.jpg"
        )
        if (DBG) Log.d(TAG, "File Path: ${file.path}")
        // Create the file
        if (!file.exists()) {
            try {
                val isCreated = file.createNewFile()
                if (DBG) Log.d(TAG, "File Created: $isCreated")
            } catch (e: IOException) {
                Snackbar.make(
                    main, "Failed to create the file",
                    LENGTH_LONG
                ).show()
                e.printStackTrace()
            }
        }
        // Take a picture and save it
        imageCapture.takePicture(file, imageSavedListener)
    }
}
