package com.worldline.devview

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.worldline.devview.core.Module
import com.worldline.devview.core.Section
import com.worldline.devview.core.previewModule
import com.worldline.devview.internal.HasTitle
import com.worldline.devview.internal.components.ModuleItem
import com.worldline.devview.internal.components.ModulePosition
import kotlinx.serialization.Serializable
import org.jetbrains.compose.resources.painterResource

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

    val watermarkAlpha = if (MaterialTheme.colorScheme.background.luminance() <
        0.5f
    ) {
        0.38f
    } else {
        0.07f
    }

    Scaffold(
        modifier = modifier
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize()) {
            Image(
                painter = painterResource(resource = Res.drawable.devview_chameleon),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize()
                    .alpha(alpha = watermarkAlpha),
                contentScale = ContentScale.Fit
            )
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

                mappedModules.entries.forEachIndexed {
                        mappedModulesIndex,
                        (section, modulesPerSection)
                    ->
                    stickyHeader(
                        key = section
                    ) {
                        Text(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 16.dp + 16.dp)
                                .padding(vertical = 8.dp)
                                .testTag(tag = "section_header_${section.name}"),
                            text = section.name.replace(oldChar = '_', newChar = ' '),
                            style = MaterialTheme.typography.bodySmallEmphasized.copy(
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Medium
                            )
                        )
                    }
                    itemsIndexed(
                        items = modulesPerSection,
                        key = { _, module -> module.moduleName },
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
public data object Home : HasTitle {
    override val title: String
        get() = "DevView"
}

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
