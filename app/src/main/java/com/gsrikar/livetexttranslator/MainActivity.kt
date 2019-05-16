package com.gsrikar.livetexttranslator

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main_activity)

        // Hide the toolbar text
        supportActionBar?.setDisplayShowTitleEnabled(false)
    }

}
