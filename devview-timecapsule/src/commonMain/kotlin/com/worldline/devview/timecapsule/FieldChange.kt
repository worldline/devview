package com.worldline.devview.timecapsule

/**
 * One `key=value` field that differs between two labels, with [newValueRange] pointing at
 * where [new] sits in the *current* label so callers can highlight it in place.
 */
internal data class FieldChange(
    val key: String,
    val old: String,
    val new: String,
    val newValueRange: IntRange
)

// ponytail: 3 fits two lines of bodyMedium for typical short values. It's a display cap,
// not a diff cap — expanding always lists every change. Tune if it reads badly.
private const val COLLAPSED_CHANGE_LIMIT = 3

/**
 * Diffs two `toString()`-shaped labels field by field, e.g. `CounterState(count=3, name=foo)`
 * vs `CounterState(count=4, name=foo)` → `[FieldChange(key="count", old="3", new="4", ...)]`.
 *
 * Only keys present on both sides are compared; a label with no parseable `key=value` pairs
 * (or `previous == null`) yields an empty list — callers fall back to showing the label as-is.
 */
internal fun diffLabels(current: String, previous: String?): List<FieldChange> {
    if (previous == null) return emptyList()

    val currentFields = parseFields(label = current)
    val previousFields = parseFields(label = previous).associateBy { it.key }
    if (currentFields.isEmpty() || previousFields.isEmpty()) return emptyList()

    return currentFields.mapNotNull { field ->
        val old = previousFields[field.key] ?: return@mapNotNull null
        if (old.value == field.value) return@mapNotNull null
        FieldChange(
            key = field.key,
            old = old.value,
            new = field.value,
            newValueRange = field.valueRange
        )
    }
}

/**
 * The changes to actually display: every change when [expanded], otherwise the first
 * [COLLAPSED_CHANGE_LIMIT] — callers show `size - shownChanges(...).size` as a "+N more" suffix.
 *
 * Kept as plain string/list logic (no Compose types) so it stays unit-testable without a
 * Compose runtime; the styled rendering lives in `TimeCapsuleRow.kt`.
 */
internal fun List<FieldChange>.shownChanges(expanded: Boolean): List<FieldChange> =
    if (expanded) this else take(n = COLLAPSED_CHANGE_LIMIT)

private data class ParsedField(val key: String, val value: String, val valueRange: IntRange)

// Parses `key=value` pairs out of a `toString()`-shaped label, splitting on top-level commas
// only (bracket/paren/brace depth tracked) so nested or list-shaped values — `items=[a, b]`,
// `addr=Address(city=Paris)` — survive as a single field instead of being split apart.
private fun parseFields(label: String): List<ParsedField> {
    val openParen = label.indexOf(char = '(')
    val closeParen = label.lastIndexOf(char = ')')
    val contentStart: Int
    val content: String
    if (openParen != -1 && closeParen > openParen) {
        contentStart = openParen + 1
        content = label.substring(startIndex = contentStart, endIndex = closeParen)
    } else {
        contentStart = 0
        content = label
    }

    val fields = mutableListOf<ParsedField>()
    var depth = 0
    var segmentStart = 0

    fun addSegment(endIndex: Int) {
        val segment = content.substring(startIndex = segmentStart, endIndex = endIndex)
        val equalsIndex = segment.indexOf(char = '=')
        if (equalsIndex == -1) return

        val key = segment.substring(startIndex = 0, endIndex = equalsIndex).trim()
        val rawValue = segment.substring(startIndex = equalsIndex + 1)
        val value = rawValue.trim()
        if (key.isEmpty() || value.isEmpty()) return

        val valueOffsetInSegment = equalsIndex + 1 + (rawValue.length - rawValue.trimStart().length)
        val valueStart = contentStart + segmentStart + valueOffsetInSegment
        fields += ParsedField(
            key = key,
            value = value,
            valueRange = valueStart until valueStart + value.length
        )
    }

    for (index in content.indices) {
        when (content[index]) {
            '(', '[', '{' -> depth++
            ')', ']', '}' -> depth--
            ',' -> if (depth == 0) {
                addSegment(endIndex = index)
                segmentStart = index + 1
            }
        }
    }
    addSegment(endIndex = content.length)

    return fields
}
