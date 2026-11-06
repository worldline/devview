package com.worldline.devview.consolelogger

internal actual class ConsoleLock {
    private val monitor = Any()

    actual fun <T> withLock(block: () -> T): T = synchronized(lock = monitor, block = block)
}
