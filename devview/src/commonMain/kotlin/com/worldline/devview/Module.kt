package com.worldline.devview

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AppSettingsAlt
import androidx.compose.material.icons.rounded.DeveloperMode
import androidx.compose.material.icons.rounded.FormatListNumbered
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable
import kotlinx.serialization.modules.PolymorphicModuleBuilder

public sealed interface Module : NavKey {
    public val section: Section

    public val icon: ImageVector
        get() = section.icon
    public val containerColor: Color
        get() = Color(color = 0xFF326EE6)
    public val contentColor: Color
        get() = Color(color = 0xFFE6E6E6)

    public val subtitle: String?
        get() = null

    public val asSubclass: PolymorphicModuleBuilder<NavKey>.() -> Unit

    @Serializable
    public data object AppInfo : Module {
        override val section: Section
            get() = Section.SETTINGS

        override val asSubclass: PolymorphicModuleBuilder<NavKey>.() -> Unit
            get() = {
                subclass(subclass = AppInfo::class, serializer = serializer())
            }
    }

    @Serializable
    public data object FeatureFlip : Module {
        override val section: Section
            get() = Section.FEATURES

        override val asSubclass: PolymorphicModuleBuilder<NavKey>.() -> Unit
            get() = {
                subclass(subclass = FeatureFlip::class, serializer = serializer())
            }
    }

    @Serializable
    public data object Console : Module {
        override val section: Section
            get() = Section.LOGGING

        override val subtitle: String
            get() = "Logcat"

        override val asSubclass: PolymorphicModuleBuilder<NavKey>.() -> Unit
            get() = {
                subclass(subclass = Console::class, serializer = serializer())
            }
    }

    @Serializable
    public data object Analytics : Module {
        override val section: Section
            get() = Section.LOGGING

        override val subtitle: String
            get() = "Firebase"

        override val asSubclass: PolymorphicModuleBuilder<NavKey>.() -> Unit
            get() = {
                subclass(subclass = Analytics::class, serializer = serializer())
            }
    }

    @Serializable
    public data object AppSpecific : Module {
        override val section: Section
            get() = Section.APP_SPECIFIC

        override val asSubclass: PolymorphicModuleBuilder<NavKey>.() -> Unit
            get() = {
                subclass(subclass = AppSpecific::class, serializer = serializer())
            }
    }
}

public enum class Section {
    SETTINGS,
    FEATURES,
    LOGGING,
    APP_SPECIFIC
}

public val Section.icon: ImageVector
    get() = when (this) {
        Section.SETTINGS -> Icons.Rounded.Settings
        Section.FEATURES -> Icons.Rounded.DeveloperMode
        Section.LOGGING -> Icons.Rounded.FormatListNumbered
        Section.APP_SPECIFIC -> Icons.Rounded.AppSettingsAlt
    }
