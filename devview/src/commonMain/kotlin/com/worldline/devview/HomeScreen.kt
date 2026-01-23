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
import com.worldline.devview.internal.components.ModuleItem
import com.worldline.devview.internal.components.ModulePosition
import kotlinx.serialization.Serializable

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

@Serializable
public data object Home : NavKey

@Preview
@Composable
private fun HomeScreenPreview() {
    HomeScreen(
        modules = listOf(
            Module.AppInfo,
            Module.FeatureFlip,
            Module.Console,
            Module.Analytics,
            Module.AppSpecific
        ),
        openModule = {}
    )
}
