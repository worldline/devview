package com.worldline.devview.crashhistory.core

import kotlin.random.Random
import kotlinx.io.buffered
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import kotlinx.io.readString
import kotlinx.io.writeString
import kotlinx.serialization.json.Json

private const val RECORD_EXTENSION = ".json"
private const val TEMP_EXTENSION = ".tmp"
private const val TIMESTAMP_DIGITS = 15

private val storeJson = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
}

/**
 * File-backed store: one JSON file per [CrashRecord] plus a few tiny state files.
 *
 * Every write goes to a temp file first and is then atomically moved into place, so a process
 * killed mid-write never leaves a half-written record behind. File names start with the
 * zero-padded timestamp, so lexical order is chronological order.
 *
 * All methods are synchronous on purpose: [write] is called from inside crash handlers, where
 * suspending is not an option.
 */
internal class CrashStore(private val directory: Path) {
    fun write(record: CrashRecord): Boolean = writeAtomically(
        target = child(name = recordFileName(record = record)),
        text = storeJson.encodeToString(serializer = CrashRecord.serializer(), value = record)
    )

    /** Returns every readable record, newest first. Unreadable files are deleted. */
    fun loadAll(): List<CrashRecord> = recordFiles()
        .mapNotNull { readRecord(file = it) }
        .sortedWith(
            comparator = compareByDescending<CrashRecord> {
                it.timestampMillis
            }.thenByDescending { it.id }
        )

    /** Deletes the oldest records so at most [max] remain. */
    fun prune(max: Int) {
        recordFiles().drop(n = max).forEach { deleteQuietly(file = it) }
    }

    /** Deletes all records. State files are kept so cleared entries are not imported again. */
    fun clear() {
        recordFiles().forEach { deleteQuietly(file = it) }
    }

    fun readState(name: String): String? {
        val file = child(name = name)
        return if (SystemFileSystem.exists(path = file)) readText(file = file) else null
    }

    fun writeState(name: String, value: String): Boolean =
        writeAtomically(target = child(name = name), text = value)

    private fun child(name: String): Path = Path(base = "$directory/$name")

    private fun recordFiles(): List<Path> {
        if (!SystemFileSystem.exists(path = directory)) return emptyList()
        return SystemFileSystem
            .list(directory = directory)
            .filter { it.name.endsWith(suffix = RECORD_EXTENSION) }
            .sortedByDescending { it.name }
    }

    private fun readRecord(file: Path): CrashRecord? = try {
        storeJson.decodeFromString(
            deserializer = CrashRecord.serializer(),
            string = readText(file = file)
        )
    } catch (ignored: Exception) {
        deleteQuietly(file = file)
        null
    }

    private fun writeAtomically(target: Path, text: String): Boolean {
        val temp = child(
            name = "${target.name}.${Random.nextLong().toULong().toString(
                radix = 16
            )}$TEMP_EXTENSION"
        )
        return try {
            SystemFileSystem.createDirectories(path = directory)
            SystemFileSystem.sink(path = temp).buffered().use { it.writeString(string = text) }
            SystemFileSystem.atomicMove(source = temp, destination = target)
            true
        } catch (ignored: Exception) {
            false
        } finally {
            deleteQuietly(file = temp)
        }
    }

    internal companion object {
        const val ENABLED = "enabled"
        const val EXIT_INFO_CURSOR = "exitinfo-cursor"
        const val METRICKIT_SEEN = "metrickit-seen"
    }
}

private fun readText(file: Path): String = SystemFileSystem
    .source(path = file)
    .buffered()
    .use { it.readString() }

private fun deleteQuietly(file: Path) {
    try {
        SystemFileSystem.delete(path = file, mustExist = false)
    } catch (ignored: Exception) {
        // Best effort: a file we cannot delete is retried on the next prune.
    }
}

private fun recordFileName(record: CrashRecord): String {
    val timestamp = record.timestampMillis.toString().padStart(
        length = TIMESTAMP_DIGITS,
        padChar = '0'
    )
    return "$timestamp-${record.id}$RECORD_EXTENSION"
}
