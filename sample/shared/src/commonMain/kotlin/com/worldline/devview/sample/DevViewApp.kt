package com.worldline.devview.sample

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.worldline.devview.DevView
import com.worldline.devview.analytics.Analytics
import com.worldline.devview.analytics.AnalyticsLogger
import com.worldline.devview.analytics.LocalAnalytics
import com.worldline.devview.core.rememberModules
import com.worldline.devview.featureflip.FeatureFlip
import com.worldline.devview.featureflip.model.Feature
import com.worldline.devview.featureflip.model.LocalFeatureHandler
import com.worldline.devview.featureflip.model.rememberFeatureHandler
import com.worldline.devview.networkmock.NetworkMock
import devview_root.sample.network.generated.resources.Res
import org.jetbrains.compose.resources.ExperimentalResourceApi

/**
 * Main DevView-integrated app composable.
 *
 * This is the entry point for the sample application with DevView integration.
 * It sets up:
 * - Feature flags (FeatureFlip module)
 * - Analytics logging (Analytics module)
 * - Network mocking (NetworkMock module)
 * - Theme management
 * - DevView overlay
 */
@Composable
public fun DevViewApp() {
    val darkTheme = isSystemInDarkTheme()

    // Initialize feature handler with dark mode feature
    val featureHandler = rememberFeatureHandler(
        features = listOf(
            element = Feature.LocalFeature(
                name = AppFeatures.DARK_MODE.featureName,
                description = "Enable or disable dark mode",
                isEnabled = darkTheme
            )
        )
    )

    // Initialize analytics logger
    val analytics = remember { AnalyticsLogger.logs }

    CompositionLocalProvider(
        LocalFeatureHandler provides featureHandler,
        LocalAnalytics provides analytics
    ) {
        val localFeatureHandler = LocalFeatureHandler.current

        // Observe dark mode feature state
        val darkMode by localFeatureHandler.isFeatureEnabled(
            featureName = AppFeatures.DARK_MODE.featureName
        )

        val colorScheme = if (darkMode) {
            darkColorScheme()
        } else {
            lightColorScheme()
        }

        MaterialTheme(
            colorScheme = colorScheme
        ) {
            // DevView open/close state
            var devViewOpen by remember { mutableStateOf(value = false) }

            @OptIn(ExperimentalResourceApi::class)
            val networkMock = remember {
                NetworkMock(
                    resourceLoader = { path -> Res.readBytes(path = path) }
                )
            }

            // Main app content
            App(openDevView = { devViewOpen = it })

            // DevView modules configuration
            val modules = rememberModules {
                module(module = FeatureFlip)
                module(module = Analytics)
                module(module = networkMock)
                module(module = TestModule)
            }

            // DevView overlay
            DevView(
                devViewIsOpen = devViewOpen,
                closeDevView = { devViewOpen = false },
                modules = modules
            )
        }
    }
}

/**
 * Application feature flags enumeration.
 */
private enum class AppFeatures(val featureName: String) {
    DARK_MODE("Dark Mode")
}
