package com.worldline.devview.sample

import androidx.compose.ui.window.ComposeUIViewController
import platform.UIKit.UIViewController

@Suppress("FunctionNaming")
public fun MainViewController(): UIViewController = ComposeUIViewController { DevViewApp() }
