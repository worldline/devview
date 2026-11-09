package com.worldline.devview.sample

import androidx.compose.runtime.Composable

public interface Platform {
    public val name: String
}

public expect fun getPlatform(): Platform

@Composable
internal expect fun SystemBarsAppearance(darkTheme: Boolean)
