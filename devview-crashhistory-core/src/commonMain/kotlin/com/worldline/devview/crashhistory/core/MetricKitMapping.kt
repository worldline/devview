package com.worldline.devview.crashhistory.core

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject

private const val SIGILL = 4
private const val SIGTRAP = 5
private const val SIGABRT = 6
private const val SIGBUS = 7
private const val SIGFPE = 8
private const val SIGKILL = 9
private const val SIGSEGV = 11

private val prettyJson = Json { prettyPrint = true }

private fun signalName(number: Int): String = when (number) {
    SIGILL -> "SIGILL"
    SIGTRAP -> "SIGTRAP"
    SIGABRT -> "SIGABRT"
    SIGBUS -> "SIGBUS"
    SIGFPE -> "SIGFPE"
    SIGKILL -> "SIGKILL"
    SIGSEGV -> "SIGSEGV"
    else -> "signal $number"
}

/**
 * Maps the JSON of one iOS `MXCrashDiagnostic` to a [CrashRecord], or `null` if it cannot be
 * parsed. Lives in common code (it only touches JSON) so it is tested on every host, not just on
 * macOS.
 *
 * The call stack tree is kept as pretty-printed JSON and is **not symbolicated**. Kotlin
 * exceptions show up here as `SIGABRT`: the Kotlin stack trace is only in the in-process record.
 */
internal fun crashDiagnosticToRecord(
    json: String,
    timestampMillis: Long,
    metadata: CrashMetadata
): CrashRecord? {
    val root: JsonObject = try {
        Json.parseToJsonElement(string = json).jsonObject
    } catch (ignored: Exception) {
        return null
    }
    val meta = root["diagnosticMetaData"] as? JsonObject
    val field = { name: String -> (meta?.get(key = name) as? JsonPrimitive)?.contentOrNull }
    val signal = field("signal")?.toIntOrNull()?.let { signalName(number = it) }
    val message = field("terminationReason")
        ?: "Mach exception ${field("exceptionType") ?: "?"}, code ${field("exceptionCode") ?: "?"}"
    return buildCrashRecord(
        kind = CrashRecordKind.SYSTEM_REPORTED,
        metadata = metadata,
        timestampMillis = timestampMillis,
        exceptionType = signal?.let { "Crash ($it)" } ?: "Crash",
        message = message,
        stackTrace = root["callStackTree"]?.let {
            prettyJson.encodeToString(serializer = JsonElement.serializer(), value = it)
        },
        source = "metrickit"
    )
}
