package com.worldline.devview.sample

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
import androidx.compose.ui.graphics.Color
import co.touchlab.kermit.Logger
import com.worldline.devview.DevView
import com.worldline.devview.analytics.Analytics
import com.worldline.devview.analytics.AnalyticsLogger
import com.worldline.devview.analytics.model.AnalyticsLog
import com.worldline.devview.analytics.model.AnalyticsLogType
import com.worldline.devview.consolelogger.Console
import com.worldline.devview.consolelogger.DevViewLogWriter
import com.worldline.devview.consolelogger.theme.LocalLogColorScheme
import com.worldline.devview.consolelogger.theme.LogColorScheme
import com.worldline.devview.core.rememberModules
import com.worldline.devview.featureflip.FeatureFlip
import com.worldline.devview.featureflip.model.Feature
import com.worldline.devview.featureflip.model.FeatureState
import com.worldline.devview.featureflip.model.LocalFeatureHandler
import com.worldline.devview.featureflip.model.rememberFeatureHandler
import com.worldline.devview.networkmock.NetworkMock
import com.worldline.devview.timecapsule.TimeCapsule
import devview_root.sample.network.generated.resources.Res
import kotlin.time.Clock

/**
 * Main DevView-integrated app composable.
 *
 * This is the entry point for the sample application with DevView integration.
 * It sets up:
 * - Feature flags (FeatureFlip module)
 * - Analytics logging (Analytics module)
 * - State history recording and restore (TimeCapsule module)
 * - Network mocking (NetworkMock module)
 * - Theme management
 * - DevView overlay
 */
@Composable
public fun DevViewApp() {
    // DevView modules configuration
    val modules = rememberModules {
        module(module = FeatureFlip)
        module(module = Analytics())
        module(module = Console())
        module(module = TimeCapsule)
        module(
            module = NetworkMock(
                resourceLoader = { path -> Res.readBytes(path = path) },
                specPaths = listOf(
                    "files/networkmocks/specs/jsonplaceholder.json",
                    "files/networkmocks/specs/sample-api.json"
                )
            )
        )
        module(module = TestModule)
    }

    val darkTheme = isSystemInDarkTheme()

    // Initialize feature handler with dark mode feature
    val featureHandler = rememberFeatureHandler(
        features = listOf(
            Feature.LocalFeature(
                name = AppFeatures.DARK_MODE.featureName,
                description = "Enable or disable dark mode",
                isEnabled = darkTheme
            ),
            Feature.RemoteFeature(
                name = "Remote Feature",
                description = "This is a remote feature for demonstration purposes.",
                defaultRemoteValue = false,
                state = FeatureState.REMOTE
            )
        )
    )

    LaunchedEffect(key1 = Unit) {
        AnalyticsLogType.allTypes().forEachIndexed { index, type ->
            AnalyticsLogger.log(
                log = AnalyticsLog(
                    tag = "Sample Event $index",
                    screenClass = "MainScreen $index",
                    timestamp = Clock.System.now().toEpochMilliseconds(),
                    type = type,
                    params = if (index == 0) {
                        mapOf(
                            "source" to "sample",
                            "screen" to "main"
                        )
                    } else {
                        emptyMap()
                    }
                )
            )
        }
    }

    // Route Kermit-based logging (e.g. sample:network's HTTP client logging) into the
    // Console module, closing the gap native capture can't reach on iOS (NSLog/os_log
    // with no debugger attached). Registered once — Logger.addLogWriter is not
    // idempotent, so this must not re-run on every recomposition.
    LaunchedEffect(key1 = Unit) {
        Logger.addLogWriter(DevViewLogWriter)
    }

    CompositionLocalProvider(values = arrayOf(LocalFeatureHandler provides featureHandler)) {
        val localFeatureHandler = LocalFeatureHandler.current

        // Observe dark mode feature state
        val darkMode by localFeatureHandler.isFeatureEnabled(
            featureName = AppFeatures.DARK_MODE.featureName
        )

        val colorScheme = if (darkMode) {
            darkColorScheme(
                primary = Color(color = 0xFFCDB4FF),
                onPrimary = Color(color = 0xFF38008F),
                primaryContainer = Color(color = 0xFF4F00BE),
                onPrimaryContainer = Color(color = 0xFFE8DDFF),
                secondary = Color(color = 0xFFBFC2FF),
                onSecondary = Color(color = 0xFF212A6C),
                secondaryContainer = Color(color = 0xFF38418A),
                onSecondaryContainer = Color(color = 0xFFDEE0FF),
                tertiary = Color(color = 0xFFF0B4FF),
                onTertiary = Color(color = 0xFF5B0068),
                tertiaryContainer = Color(color = 0xFF7A1F90),
                onTertiaryContainer = Color(color = 0xFFF9D8FF),
                background = Color(color = 0xFF141218),
                onBackground = Color(color = 0xFFE6E1E9),
                surface = Color(color = 0xFF141218),
                onSurface = Color(color = 0xFFE6E1E9),
                surfaceVariant = Color(color = 0xFF49454F),
                onSurfaceVariant = Color(color = 0xFFCAC4D0),
                surfaceTint = Color(color = 0xFFCDB4FF),
                surfaceDim = Color(color = 0xFF141218),
                surfaceBright = Color(color = 0xFF3B3740),
                surfaceContainerLowest = Color(color = 0xFF0F0D12),
                surfaceContainerLow = Color(color = 0xFF1C1A20),
                surfaceContainer = Color(color = 0xFF201E24),
                surfaceContainerHigh = Color(color = 0xFF2B282F),
                surfaceContainerHighest = Color(color = 0xFF36323A),
                outline = Color(color = 0xFF948F99),
                outlineVariant = Color(color = 0xFF49454F)
            )
        } else {
            lightColorScheme(
                primary = Color(color = 0xFF602BD4),
                onPrimary = Color(color = 0xFFFFFFFF),
                primaryContainer = Color(color = 0xFFE8DDFF),
                onPrimaryContainer = Color(color = 0xFF21005E),
                secondary = Color(color = 0xFF545AAE),
                onSecondary = Color(color = 0xFFFFFFFF),
                secondaryContainer = Color(color = 0xFFDEE0FF),
                onSecondaryContainer = Color(color = 0xFF0D1650),
                tertiary = Color(color = 0xFFA03CBC),
                onTertiary = Color(color = 0xFFFFFFFF),
                tertiaryContainer = Color(color = 0xFFF9D8FF),
                onTertiaryContainer = Color(color = 0xFF380040),
                background = Color(color = 0xFFFDF7FF),
                onBackground = Color(color = 0xFF1C1B20),
                surface = Color(color = 0xFFFDF7FF),
                onSurface = Color(color = 0xFF1C1B20),
                surfaceVariant = Color(color = 0xFFE7E0F0),
                onSurfaceVariant = Color(color = 0xFF49454F),
                surfaceTint = Color(color = 0xFF602BD4),
                surfaceDim = Color(color = 0xFFDED7E2),
                surfaceBright = Color(color = 0xFFFDF7FF),
                surfaceContainerLowest = Color(color = 0xFFFFFFFF),
                surfaceContainerLow = Color(color = 0xFFF8F1FC),
                surfaceContainer = Color(color = 0xFFF2ECF6),
                surfaceContainerHigh = Color(color = 0xFFECE6F0),
                surfaceContainerHighest = Color(color = 0xFFE6E0EB),
                outline = Color(color = 0xFF7A757F),
                outlineVariant = Color(color = 0xFFCAC4D0)
            )
        }

        MaterialTheme(
            colorScheme = colorScheme
        ) {
            SystemBarsAppearance(darkTheme = darkMode)

            // Chosen here, alongside the MaterialTheme colorScheme above, rather than as
            // a Console constructor param: this stays reactive to the darkMode toggle,
            // and rememberModules() (where Console is constructed) runs before darkMode
            // is readable.
            CompositionLocalProvider(
                value = LocalLogColorScheme provides
                    if (darkMode) LogColorScheme.Dark else LogColorScheme.Light
            ) {
                // DevView open/close state
                var devViewOpen by remember { mutableStateOf(value = false) }

                // Main app content
                App(openDevView = { devViewOpen = it })

                // DevView overlay
                DevView(
                    devViewIsOpen = devViewOpen,
                    closeDevView = { devViewOpen = false },
                    modules = modules
                )
            }
        }
    }
}

/**
 * Application feature flags enumeration.
 */
private enum class AppFeatures(val featureName: String) {
    DARK_MODE("Dark Mode")
}
