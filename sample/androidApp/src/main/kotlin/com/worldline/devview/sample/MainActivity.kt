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
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.tooling.preview.Preview
import com.worldline.devview.DevView
import com.worldline.devview.core.rememberModules
import com.worldline.devview.featureflip.FeatureFlip
import com.worldline.devview.featureflip.model.Feature
import com.worldline.devview.featureflip.model.LocalFeatureHandler
import com.worldline.devview.featureflip.model.rememberFeatureHandler
import kotlinx.coroutines.flow.first

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        setContent {
            val darkTheme = isSystemInDarkTheme()

            val featureHandler = rememberFeatureHandler(
                features = listOf(
                    Feature.LocalFeature(
                        name = AppFeatures.DARK_MODE.featureName,
                        description = "Enable or disable dark mode",
                        isEnabled = darkTheme
                    )
                )
            )

            CompositionLocalProvider(
                 LocalFeatureHandler provides featureHandler
            ) {
                val localFeatureHandler = LocalFeatureHandler.current

                val darkMode by localFeatureHandler.isFeatureEnabled(AppFeatures.DARK_MODE.featureName)

                val colorScheme = if (darkMode) {
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

                    val modules = rememberModules {
                        module(FeatureFlip)
                        module(TestModule)
                    }

                    DevView(
                        devViewIsOpen = devViewOpen,
                        closeDevView = {
                            devViewOpen = false
                        },
                        modules = modules
                    )
                }
            }
        }
    }
}

private enum class AppFeatures(val featureName: String) {
    DARK_MODE("Dark Mode")
}

@Preview
@Composable
fun AppAndroidPreview() {
    App(
        openDevView = {}
    )
}