package com.worldline.devview.sample

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.tooling.preview.Preview
import com.worldline.devview.DevView
import com.worldline.devview.Module
import kotlinx.collections.immutable.persistentListOf

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        setContent {
            val colorScheme = if (isSystemInDarkTheme()) {
                darkColorScheme()
            } else {
                lightColorScheme()
            }

            MaterialTheme(
                colorScheme = colorScheme
            ) {
                var devViewOpen by remember { mutableStateOf(false) }
                App(
                    openDevView = {
                        devViewOpen = it
                    }
                )
                DevView(
                    openDevView = { devViewOpen },
                    closeDevView = {
                        devViewOpen = false
                    },
                    modules = persistentListOf(
                        Module.AppInfo,
                        Module.FeatureFlip,
                        Module.Console,
                        Module.Analytics,
                        Module.AppSpecific
                    )
                )
            }
        }
    }
}

@Preview
@Composable
fun AppAndroidPreview() {
    App(
        openDevView = {}
    )
}