@file:Suppress("MatchingDeclarationName")

package com.worldline.devview.sample

import androidx.compose.runtime.Composable
import platform.UIKit.UIDevice

public class IOSPlatform : Platform {
    override val name: String =
        UIDevice.currentDevice.systemName() + " " + UIDevice.currentDevice.systemVersion
}

public actual fun getPlatform(): Platform = IOSPlatform()

@Composable
internal actual fun SystemBarsAppearance(darkTheme: Boolean) {
    // shortcut: iOS bars stay host-owned; wire appearance when the sample controller handles themes.
}
