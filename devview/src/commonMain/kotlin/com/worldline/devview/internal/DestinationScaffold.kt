package com.worldline.devview.internal

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavEntryDecorator
import androidx.navigation3.runtime.NavKey
import com.worldline.devview.Home
import com.worldline.devview.core.Module
import com.worldline.devview.core.ModuleDestinationAction
import kotlinx.collections.immutable.ImmutableList

internal const val DESTINATION_METADATA_KEY = "com.worldline.devview.internal.destination"
internal val LocalEntryBottomPadding = staticCompositionLocalOf(defaultFactory = { 0.dp })

internal fun destinationScaffoldDecorator(
    modules: ImmutableList<Module>,
    activeDestination: NavKey,
    onNavigateBack: () -> Unit,
    closeDevView: () -> Unit
): NavEntryDecorator<NavKey> = NavEntryDecorator { entry ->
    val destination = entry.metadata.getValue(key = DESTINATION_METADATA_KEY) as NavKey
    val module = modules.find { destination::class in it.destinations }
    val metadata = module?.destinations?.get(key = destination::class)
    DestinationScaffold(
        destination = destination,
        isActive = destination == activeDestination,
        title = if (destination is HasTitle) {
            destination.title
        } else {
            metadata?.title
                ?: module?.moduleName.orEmpty()
        },
        actions = metadata?.actions.orEmpty(),
        onNavigateBack = onNavigateBack,
        closeDevView = closeDevView,
        content = { entry.Content() }
    )
}

@Composable
private fun DestinationScaffold(
    destination: NavKey,
    isActive: Boolean,
    title: String,
    actions: List<ModuleDestinationAction>,
    onNavigateBack: () -> Unit,
    closeDevView: () -> Unit,
    content: @Composable () -> Unit
) {
    var activePopupIndex by rememberSaveable { mutableStateOf<Int?>(value = null) }
    var expandedMenuIndex by rememberSaveable { mutableStateOf<Int?>(value = null) }

    LaunchedEffect(key1 = isActive) {
        if (!isActive) {
            activePopupIndex = null
            expandedMenuIndex = null
        }
    }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = { Text(text = title, fontWeight = FontWeight.SemiBold) },
                    navigationIcon = {
                        IconButton(
                            onClick = if (destination == Home) closeDevView else onNavigateBack
                        ) {
                            Icon(
                                imageVector = if (destination == Home) {
                                    Icons.Rounded.Close
                                } else {
                                    Icons.AutoMirrored.Rounded.ArrowBack
                                },
                                contentDescription = if (destination ==
                                    Home
                                ) {
                                    "Close DevView"
                                } else {
                                    "Back"
                                }
                            )
                        }
                    },
                    actions = {
                        actions.forEachIndexed { index, action ->
                            Box {
                                TooltipBox(
                                    positionProvider = TooltipDefaults
                                        .rememberTooltipPositionProvider(
                                            positioning = TooltipAnchorPosition.Above
                                        ),
                                    tooltip = { PlainTooltip { Text(text = action.label) } },
                                    state = rememberTooltipState()
                                ) {
                                    IconButton(
                                        modifier = Modifier.testTag(tag = "top_bar_action_$index"),
                                        onClick = {
                                            when {
                                                action.menuItems != null ->
                                                    expandedMenuIndex =
                                                        index
                                                action.popup != null -> activePopupIndex = index
                                                else -> action.action(destination)
                                            }
                                        }
                                    ) {
                                        Icon(
                                            imageVector = action.icon,
                                            contentDescription = action.label
                                        )
                                    }
                                }
                                action.menuItems?.let { items ->
                                    DropdownMenu(
                                        expanded = isActive && expandedMenuIndex == index,
                                        onDismissRequest = { expandedMenuIndex = null }
                                    ) {
                                        items.forEach { item ->
                                            DropdownMenuItem(
                                                text = { Text(text = item.label) },
                                                onClick = {
                                                    expandedMenuIndex = null
                                                    item.onClick()
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                )
                HorizontalDivider()
            }
        }
    ) { padding ->
        val direction = LocalLayoutDirection.current
        val contentPadding = PaddingValues(
            start = padding.calculateStartPadding(layoutDirection = direction),
            top = padding.calculateTopPadding(),
            end = padding.calculateEndPadding(layoutDirection = direction)
        )
        Box(
            modifier = Modifier
                .padding(paddingValues = contentPadding)
                .consumeWindowInsets(paddingValues = contentPadding)
        ) {
            CompositionLocalProvider(
                value = LocalEntryBottomPadding provides padding.calculateBottomPadding()
            ) {
                content()
            }
        }
        activePopupIndex?.takeIf { isActive }?.let { index ->
            actions.getOrNull(index = index)?.let { action ->
                action.popup?.let { popup ->
                    AlertDialog(
                        onDismissRequest = { activePopupIndex = null },
                        title = { Text(text = popup.title) },
                        text = popup.subtitle?.let { { Text(text = it) } },
                        confirmButton = {
                            TextButton(onClick = {
                                activePopupIndex = null
                                action.action(destination)
                            }) {
                                Text(text = popup.confirmButton)
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { activePopupIndex = null }) {
                                Text(text = popup.dismissButton)
                            }
                        }
                    )
                }
            }
        }
    }
}
