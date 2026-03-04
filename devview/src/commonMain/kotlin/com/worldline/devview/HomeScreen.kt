package com.worldline.devview

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavKey
import com.worldline.devview.core.Module
import com.worldline.devview.core.Section
import com.worldline.devview.core.previewModule
import com.worldline.devview.internal.components.ModuleItem
import com.worldline.devview.internal.components.ModulePosition
import kotlinx.serialization.Serializable

/**
 * Internal composable that displays the DevView home screen with all modules.
 *
 * This screen serves as the main menu for DevView, displaying all registered
 * modules organized by their sections. Modules within the same section are
 * visually grouped together with connected card shapes.
 *
 * ## Features
 * - **Section Grouping**: Modules automatically grouped by [Section]
 * - **Visual Hierarchy**: Connected card shapes for modules in same section
 * - **Spacing**: Automatic spacing between section groups
 * - **Interactive**: Tappable module cards that navigate to module content
 *
 * ## Layout Structure
 * ```
 * LazyColumn
 * ├── Top Padding
 * ├── Section 1 (e.g., SETTINGS)
 * │   ├── Module 1 (rounded top, squared bottom)
 * │   ├── Module 2 (squared all sides, with divider)
 * │   └── Module 3 (squared top, rounded bottom)
 * ├── Spacing
 * ├── Section 2 (e.g., FEATURES)
 * │   └── Single Module (fully rounded)
 * └── Bottom Padding
 * ```
 *
 * ## Module Positioning
 * Modules are styled based on their position within a section:
 * - **Single**: Fully rounded corners (only module in section)
 * - **First**: Rounded top, squared bottom corners
 * - **Middle**: All corners squared, top divider
 * - **Last**: Squared top, rounded bottom corners, top divider
 *
 * ## Usage
 * This composable is used internally by [DevView] and is not intended for
 * direct use outside the framework.
 *
 * @param modules The list of modules to display, typically from rememberModules.
 * @param openModule Callback invoked when a user taps a module card. Receives
 *        the [Module] that was tapped.
 * @param modifier Optional [Modifier] to apply to the root Scaffold.
 *
 * @see com.worldline.devview.DevView
 * @see Module
 * @see Section
 * @see com.worldline.devview.internal.components.ModuleItem
 */
@Composable
internal fun HomeScreen(
    modules: List<Module>,
    openModule: (Module) -> Unit,
    modifier: Modifier = Modifier
) {
    val mappedModules by remember(key1 = modules) {
        derivedStateOf {
            modules
                .groupBy { module ->
                    module.section
                }
        }
    }

    Scaffold(
        modifier = modifier
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
        ) {
            item {
                Spacer(
                    modifier = Modifier.height(
                        height = paddingValues.calculateTopPadding()
                    )
                )
            }
            mappedModules.values.forEachIndexed { mappedModulesIndex, modulesPerSection ->
                itemsIndexed(
                    items = modulesPerSection,
                    key = { _, module -> module.hashCode() },
                    contentType = { _, _ -> "Module" }
                ) { index, module ->
                    ModuleItem(
                        module = module,
                        position = when {
                            modulesPerSection.size == 1 -> ModulePosition.SINGLE
                            index == 0 -> ModulePosition.FIRST
                            index == modulesPerSection.lastIndex -> ModulePosition.LAST
                            else -> ModulePosition.MIDDLE
                        },
                        openModule = openModule
                    )
                }
                if (mappedModulesIndex != mappedModules.values.toList().lastIndex) {
                    item {
                        Spacer(
                            modifier = Modifier.height(height = 16.dp)
                        )
                    }
                }
            }
            item {
                Spacer(
                    modifier = Modifier.height(
                        height = paddingValues.calculateBottomPadding()
                    )
                )
            }
        }
    }
}

/**
 * Navigation destination representing the DevView home screen.
 *
 * This is the root destination of the DevView navigation graph. It displays
 * a list of all registered modules grouped by their sections.
 *
 * ## Navigation
 * - This is always the first item in the DevView backstack
 * - Navigating back from this screen closes DevView
 * - Tapping a module navigates to that module's first destination
 *
 * ## Serialization
 * This object is serializable to support navigation state persistence
 * across process death and configuration changes.
 *
 * @see com.worldline.devview.DevView
 * @see HomeScreen
 * @see Module
 */
@Serializable
public data object Home : NavKey

@Preview(locale = "en")
@Composable
private fun HomeScreenPreview() {
    HomeScreen(
        modules = listOf(
            previewModule(section = Section.SETTINGS, name = "AppInfo"),
            previewModule(section = Section.FEATURES, name = "FeatureFlip"),
            previewModule(section = Section.NETWORK, name = "Mocks"),
            previewModule(section = Section.LOGGING, name = "Console"),
            previewModule(section = Section.LOGGING, name = "Analytics"),
            previewModule(section = Section.CUSTOM, name = "AppSpecific")
        ),
        openModule = {}
    )
}
