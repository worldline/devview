package com.worldline.devview.crashhistory.core

/**
 * Starts crash recording from the iOS app entry point, earlier than `CrashHistory()` would.
 *
 * Optional: adding `CrashHistory()` to `rememberModules` already starts recording once DevView is
 * composed. Call this from `iOSApp.init()` to also catch crashes during app startup:
 *
 * ```swift
 * @main struct iOSApp: App {
 *     init() { CrashHistoryIos.shared.start() }
 *     ...
 * }
 * ```
 *
 * This requires the Kotlin framework to export `devview-crashhistory-core`.
 */
public object CrashHistoryIos {
    /** Starts recording and keeps [CrashRecorder.DEFAULT_MAX_RECORDS] records. */
    public fun start() {
        start(maxRecords = CrashRecorder.DEFAULT_MAX_RECORDS)
    }

    /**
     * Starts recording.
     *
     * An overload rather than a default argument, because Swift cannot see Kotlin defaults.
     *
     * The latest call updates retention. Zero is unsupported.
     *
     * @param maxRecords Positive number of records to keep.
     * @throws IllegalArgumentException if [maxRecords] is not positive.
     */
    public fun start(maxRecords: Int) {
        require(value = maxRecords > 0) { "maxRecords must be positive" }
        CrashRecorder.start(IosCrashPlatform(), maxRecords)
    }
}
