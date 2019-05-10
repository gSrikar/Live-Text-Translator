package com.gsrikar.livetexttranslator.ui.main

import android.Manifest
import android.app.Activity.RESULT_OK
import android.content.Intent
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat.checkSelfPermission
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProviders
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.snackbar.Snackbar.LENGTH_INDEFINITE
import com.gsrikar.livetexttranslator.R
import kotlinx.android.synthetic.main.main_fragment.*


// Request for camera permissions
private const val REQUEST_CODE_REQUEST_CAMERA_PERMISSION = 8912
private const val REQUEST_CODE_REQUEST_CAMERA_PERMISSION_FROM_SETTINGS = 8913
private const val CAMERA_PERMISSION = Manifest.permission.CAMERA

class MainFragment : Fragment() {

    private lateinit var viewModel: MainViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.main_fragment, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewModel = ViewModelProviders.of(this).get(MainViewModel::class.java)

        checkPermission()
    }

    private fun checkPermission() {
        if (isPermissionGranted()) {
            // TODO: Show camera preview
        } else {
            requestPermission()
        }
    }

    private fun requestPermission() {
        requestPermissions(
            arrayOf(CAMERA_PERMISSION),
            REQUEST_CODE_REQUEST_CAMERA_PERMISSION
        )
    }

    private fun isPermissionGranted(): Boolean {
        return context?.let {
            checkSelfPermission(it, CAMERA_PERMISSION)
        } == PERMISSION_GRANTED
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
            // TODO: Show camera preview
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
            REQUEST_CODE_REQUEST_CAMERA_PERMISSION -> checkPermissionGranted(grantResults)
        }
    }

    private fun checkPermissionGranted(grantResults: IntArray) {
        if (grantResults.isNotEmpty() && grantResults[0] == PERMISSION_GRANTED) {
            // TODO: Show camera preview
        } else if (shouldShowRational()) {
            checkPermission()
        } else {
            showRationaleText()
        }
    }

    private fun shouldShowRational(): Boolean {
        return shouldShowRequestPermissionRationale(CAMERA_PERMISSION)
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

}
