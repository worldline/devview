package com.worldline.devview.consolelogger

import com.worldline.devview.consolelogger.model.ConsoleLog
import com.worldline.devview.consolelogger.model.LogLevel

internal fun filterConsoleLogs(
    logs: List<ConsoleLog>,
    query: String,
    levels: Set<LogLevel>,
    excludedPatterns: List<String> = emptyList()
): List<ConsoleLog> = logs.filter { log ->
    log.level in levels && !log.matchesConsoleNoise(patterns = excludedPatterns) &&
        (
            query.isBlank() || log.tag.contains(other = query, ignoreCase = true) ||
                log.message.contains(other = query, ignoreCase = true)
            )
}

internal fun ConsoleLog.matchesConsoleNoise(patterns: List<String>): Boolean =
    patterns.any { pattern ->
        pattern.isNotBlank() &&
            (
                tag.contains(
                    other = pattern,
                    ignoreCase = true
                ) || message.contains(other = pattern, ignoreCase = true)
                )
    }

internal fun consoleRowKeys(logs: List<ConsoleLog>): List<String> {
    val occurrences = mutableMapOf<String, Int>()
    return logs.map { log ->
        val rank = occurrences[log.id] ?: 0
        occurrences[log.id] = rank + 1
        "${log.id}:$rank"
    }
}

internal val LogLevel.displayName: String
    get() = name.lowercase().replaceFirstChar { it.uppercase() }

internal fun ConsoleLog.copyText(): String =
    "$formattedTimestamp ${level.displayName} $tag\n$message"
