package com.worldline.devview.sample

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge

/**
 * Android application entry point.
 *
 * This is kept as vanilla as possible - all DevView integration and
 * business logic is in the shared Compose Multiplatform module.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        setContent {
            // All logic is in the shared module
            DevViewApp()
        }
    }
}

