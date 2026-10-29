package com.worldline.devview.timecapsule

import androidx.compose.ui.text.TextRange

/** Detached editing values; never retains a mutable text buffer. */
internal data class TextFieldSnapshot(
    val name: String,
    val text: String,
    val selection: TextRange
) {
    val description: String
        get() {
            val escaped = text
                .replace(oldValue = "\\", newValue = "\\\\")
                .replace(oldValue = "\"", newValue = "\\\"")
                .replace(oldValue = "\n", newValue = "\\n")
                .replace(oldValue = "\r", newValue = "\\r")
                .replace(oldValue = "\t", newValue = "\\t")
            return "\"$escaped\" (selection=${selection.start}..${selection.end})"
        }
}

internal val Recorded<*>.displayLabel: String
    get() = buildString {
        append(label)
        textFields.forEach { field ->
            append("\n${field.name}=${field.description}")
        }
    }

/** Uses captured values, not label parsing, for explicitly registered editing fields. */
internal fun Recorded<*>.changesFrom(previous: Recorded<*>?): List<FieldChange> {
    if (previous == null) return emptyList()

    val names = textFields.map { it.name }.toSet()
    val changes = diffLabels(current = label, previous = previous.label)
        .filterNot { it.key in names }
        .toMutableList()
    val previousFields = previous.textFields.associateBy { it.name }
    var offset = label.length
    textFields.forEach { field ->
        val value = field.description
        val start = offset + 1 + field.name.length + 1
        val old = previousFields[field.name]
        if (old != null && old != field) {
            changes += FieldChange(
                key = field.name,
                old = old.description,
                new = value,
                newValueRange = start until start + value.length
            )
        }
        offset = start + value.length
    }
    return changes
}
