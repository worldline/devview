package com.worldline.devview.consolelogger

import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSRecursiveLock

@OptIn(ExperimentalForeignApi::class)
internal actual class ConsoleLock {
    private val lock = NSRecursiveLock()

    actual fun <T> withLock(block: () -> T): T {
        lock.lock()
        try {
            return block()
        } finally {
            lock.unlock()
        }
    }
}
