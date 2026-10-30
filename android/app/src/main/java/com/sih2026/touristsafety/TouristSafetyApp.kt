package com.sih2026.touristsafety

import android.app.Application
import android.webkit.WebView
import dagger.hilt.android.HiltAndroidApp
import org.osmdroid.config.Configuration

@HiltAndroidApp
class TouristSafetyApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // Enable whole document drawing for WebView so off-screen rendering
        // captures all pages without tile clipping
        try {
            WebView.enableSlowWholeDocumentDraw()
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Initialize OSMDroid configuration early so map tiles load properly
        Configuration.getInstance().apply {
            userAgentValue = packageName
            load(this@TouristSafetyApp, getSharedPreferences("osmdroid", MODE_PRIVATE))
        }
    }
}
