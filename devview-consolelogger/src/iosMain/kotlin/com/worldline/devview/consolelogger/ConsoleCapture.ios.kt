package com.worldline.devview.consolelogger

import com.worldline.devview.consolelogger.model.ConsoleLog
import com.worldline.devview.consolelogger.model.LogLevel
import kotlin.time.Clock
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.IntVar
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.allocArray
import kotlinx.cinterop.convert
import kotlinx.cinterop.get
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.usePinned
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import platform.posix.EAGAIN
import platform.posix.EINTR
import platform.posix.EWOULDBLOCK
import platform.posix.F_GETFL
import platform.posix.F_SETFL
import platform.posix.O_NONBLOCK
import platform.posix.STDERR_FILENO
import platform.posix.STDOUT_FILENO
import platform.posix._IOLBF
import platform.posix.close
import platform.posix.dup
import platform.posix.dup2
import platform.posix.errno
import platform.posix.fcntl
import platform.posix.pipe
import platform.posix.read
import platform.posix.setvbuf
import platform.posix.stdout
import platform.posix.write

private const val READ_BUFFER_SIZE = 4096

/**
 * Redirects `stdout` and `stderr` through in-process pipes so Kotlin `println`, Swift
 * `print`, and C `printf` output reaches [ConsoleLogger] even when no debugger is
 * attached. Bytes are teed back to the original file descriptor so Xcode's console still
 * shows everything when the device is tethered.
 *
 * This cannot see `NSLog`/`os_log`/Swift `Logger` output on a device with no debugger
 * attached — those bypass the file descriptor entirely. See [DevViewLogWriter] to close
 * that gap for logging routed through Kermit.
 */
@OptIn(ExperimentalForeignApi::class)
internal actual suspend fun tailNativeConsole(): Unit = coroutineScope {
    launch { redirectStream(fd = STDOUT_FILENO, level = LogLevel.INFO) }
    launch { redirectStream(fd = STDERR_FILENO, level = LogLevel.ERROR) }
}

@OptIn(ExperimentalForeignApi::class)
internal suspend fun redirectStream(fd: Int, level: LogLevel): Unit = withContext(Dispatchers.IO) {
    val saved = dup(fd)
    check(value = saved >= 0) { "Console dup failed: errno=$errno" }
    var readFd = -1
    var writeFd = -1
    var redirected = false
    try {
        memScoped {
            val fds = allocArray<IntVar>(2)
            check(value = pipe(fds) == 0) { "Console pipe failed: errno=$errno" }
            readFd = fds[0]
            writeFd = fds[1]
        }
        makeNonBlocking(fd = readFd)
        check(value = duplicateDescriptor(source = writeFd, target = fd) >= 0) {
            "Console redirect failed: errno=$errno"
        }
        redirected = true
        close(writeFd)
        writeFd = -1
        // A full capture pipe must not block the host app’s writers.
        makeNonBlocking(fd = fd)
        if (fd == STDOUT_FILENO) {
            check(value = setvbuf(stdout, null, _IOLBF, 0.convert()) == 0) {
                "Console stdout buffering failed: errno=$errno"
            }
        }
        drainConsolePipe(readFd = readFd, saved = saved, level = level)
    } finally {
        val restored = if (redirected) duplicateDescriptor(source = saved, target = fd) else 0
        val restoreFailure = errno
        if (readFd >= 0) close(readFd)
        if (writeFd >= 0) close(writeFd)
        close(saved)
        check(value = restored >= 0) { "Console restore failed: errno=$restoreFailure" }
    }
}

@OptIn(ExperimentalForeignApi::class)
private suspend fun drainConsolePipe(readFd: Int, saved: Int, level: LogLevel) {
    val buffer = ByteArray(size = READ_BUFFER_SIZE)
    var pending = ""
    while (true) {
        val bytesRead = readConsolePipe(readFd = readFd, buffer = buffer)
        if (bytesRead == 0L) break
        buffer.usePinned { pinned ->
            write(saved, pinned.addressOf(index = 0), bytesRead.convert())
        }
        pending += buffer.decodeToString(endIndex = bytesRead.toInt())
        val lines = pending.split("\n")
        pending = lines.last()
        lines.dropLast(n = 1).forEach { line ->
            if (line.isNotEmpty()) {
                ConsoleLogger.log(
                    log = ConsoleLog(
                        level = level,
                        tag = "",
                        message = line,
                        timestamp = Clock.System.now().toEpochMilliseconds()
                    )
                )
            }
        }
    }
}

@OptIn(ExperimentalForeignApi::class)
private suspend fun readConsolePipe(readFd: Int, buffer: ByteArray): Long {
    var bytesRead: Long
    do {
        currentCoroutineContext().ensureActive()
        bytesRead = buffer.usePinned { pinned ->
            read(readFd, pinned.addressOf(index = 0), buffer.size.convert())
        }
        if (bytesRead < 0L) {
            val failure = errno
            when {
                failure == EINTR -> Unit
                failure == EAGAIN || failure == EWOULDBLOCK -> {
                    // Idle reads suspend so cancellation can restore both output streams.
                    delay(timeMillis = 10L)
                }
                else -> error(message = "Console pipe read failed: errno=$failure")
            }
        }
    } while (bytesRead < 0L)
    return bytesRead
}

@OptIn(ExperimentalForeignApi::class)
private fun makeNonBlocking(fd: Int) {
    val flags = fcntl(fd, F_GETFL)
    check(value = flags >= 0) { "Console read flags failed: errno=$errno" }
    check(value = fcntl(fd, F_SETFL, flags or O_NONBLOCK) == 0) {
        "Console nonblocking setup failed: errno=$errno"
    }
}

@OptIn(ExperimentalForeignApi::class)
private fun duplicateDescriptor(source: Int, target: Int): Int {
    var result: Int
    do {
        result = dup2(source, target)
    } while (result < 0 && errno == EINTR)
    return result
}
