package com.worldline.devview.consolelogger

internal expect class ConsoleLock() {
    fun <T> withLock(block: () -> T): T
}
