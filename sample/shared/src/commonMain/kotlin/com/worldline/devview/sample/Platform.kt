package com.worldline.devview.sample

public interface Platform {
    public val name: String
}

public expect fun getPlatform(): Platform
