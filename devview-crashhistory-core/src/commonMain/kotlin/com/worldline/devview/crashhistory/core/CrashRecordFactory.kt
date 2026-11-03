package com.worldline.devview.crashhistory.core

import kotlin.random.Random

private const val HEX_RADIX = 16

/**
 * Builds a [CrashRecord], truncating [message] and [stackTrace] to the documented limits.
 *
 * Single place for truncation and id generation so every capture path (in-process handler,
 * `record()`, Android exit reasons, MetricKit) behaves the same.
 */
@Suppress("LongParameterList")
internal fun buildCrashRecord(
    kind: CrashRecordKind,
    metadata: CrashMetadata,
    timestampMillis: Long,
    exceptionType: String?,
    message: String?,
    stackTrace: String?,
    threadName: String? = null,
    processId: Int? = null,
    source: String? = null,
    isCrash: Boolean = true
): CrashRecord {
    val shortMessage = message?.take(n = CrashRecorder.MAX_MESSAGE_LENGTH)
    val shortTrace = stackTrace?.take(n = CrashRecorder.MAX_STACK_TRACE_LENGTH)
    val randomSuffix = Random.nextLong().toULong().toString(radix = HEX_RADIX)
    return CrashRecord(
        id = "${processId ?: 0}-$randomSuffix",
        timestampMillis = timestampMillis,
        kind = kind,
        exceptionType = exceptionType,
        message = shortMessage,
        stackTrace = shortTrace,
        threadName = threadName,
        processId = processId,
        source = source,
        isCrash = isCrash,
        metadata = metadata,
        truncated =
            shortMessage?.length != message?.length || shortTrace?.length != stackTrace?.length
    )
}
