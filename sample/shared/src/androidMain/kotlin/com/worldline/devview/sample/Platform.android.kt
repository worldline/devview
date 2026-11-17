@file:Suppress("MatchingDeclarationName")

package com.worldline.devview.sample

import android.app.Activity
import android.content.ContextWrapper
import android.os.Build
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

public class AndroidPlatform : Platform {
    override val name: String = "Android ${Build.VERSION.SDK_INT}"
}

public actual fun getPlatform(): Platform = AndroidPlatform()

@Composable
internal actual fun SystemBarsAppearance(darkTheme: Boolean) {
    val view = LocalView.current
    val background = MaterialTheme.colorScheme.background.toArgb()
    val activity = generateSequence(seed = view.context) { context ->
        (context as? ContextWrapper)?.baseContext
    }.filterIsInstance<Activity>().firstOrNull()
    SideEffect {
        if (activity != null) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                // Legacy edge-to-edge keeps an OS-themed scrim after the app theme changes.
                @Suppress("DEPRECATION")
                activity.window.navigationBarColor = background
            }
            WindowCompat.getInsetsController(activity.window, view).apply {
                isAppearanceLightStatusBars = !darkTheme
                isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }
}
