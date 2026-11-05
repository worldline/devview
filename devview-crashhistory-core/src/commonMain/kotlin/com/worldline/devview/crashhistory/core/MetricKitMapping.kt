package com.worldline.devview.crashhistory.core

private const val SIGILL = 4
private const val SIGTRAP = 5
private const val SIGABRT = 6
private const val SIGEMT = 7
private const val SIGFPE = 8
private const val SIGKILL = 9
private const val SIGBUS = 10
private const val SIGSEGV = 11
private const val SIGSYS = 12
private const val UNKNOWN = "unknown"
private val occurrenceIdPattern = Regex(pattern = "[0-9a-f]{64}")

internal data class SystemCrashDiagnostic(
    val occurrenceId: String,
    val periodEndMillis: Long,
    val signal: Int? = null,
    val exceptionType: Int? = null,
    val exceptionCode: String? = null,
    val terminationReason: String? = null,
    val callStackTreeJson: String? = null,
    val appVersion: String? = null,
    val appBuildVersion: String? = null,
    val osVersion: String? = null,
    val deviceType: String? = null
)

private fun signalName(number: Int): String = when (number) {
    SIGILL -> "SIGILL"
    SIGTRAP -> "SIGTRAP"
    SIGABRT -> "SIGABRT"
    SIGEMT -> "SIGEMT"
    SIGFPE -> "SIGFPE"
    SIGKILL -> "SIGKILL"
    SIGBUS -> "SIGBUS"
    SIGSEGV -> "SIGSEGV"
    SIGSYS -> "SIGSYS"
    else -> "signal $number"
}

/** Maps either Swift MetricKit path without pretending the reporting period is a crash time. */
internal fun crashDiagnosticToRecord(
    diagnostic: SystemCrashDiagnostic,
    metadata: CrashMetadata
): CrashRecord? {
    // The digest is also a filename component; never accept a path from the native boundary.
    if (!occurrenceIdPattern.matches(input = diagnostic.occurrenceId)) return null
    return buildCrashRecord(
        id = "metrickit-${diagnostic.occurrenceId}",
        kind = CrashRecordKind.SYSTEM_REPORTED,
        metadata = metadata.copy(
            appVersion = diagnostic.appVersion ?: UNKNOWN,
            buildNumber = diagnostic.appBuildVersion ?: UNKNOWN,
            osVersion = diagnostic.osVersion ?: UNKNOWN,
            deviceModel = diagnostic.deviceType ?: UNKNOWN
        ),
        timestampMillis = diagnostic.periodEndMillis,
        exceptionType = diagnostic.signal?.let { "Crash (${signalName(number = it)})" } ?: "Crash",
        message = diagnostic.terminationReason
            ?: "Mach exception ${diagnostic.exceptionType ?: "?"}, code ${diagnostic.exceptionCode ?: "?"}",
        stackTrace = diagnostic.callStackTreeJson,
        source = "metrickit"
    )
}
