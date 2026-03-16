package com.worldline.devview.networkmock.model

import androidx.compose.runtime.Immutable

/**
 * Represents a single line in a computed diff between two mock response bodies.
 *
 * - [Unchanged] — the line is identical in both responses; carries 1-based line numbers
 *   for each side
 * - [Different] — the line differs; either side may be null when one response has more
 *   lines than the other. Line numbers are null on the side that has no content for that
 *   position.
 */
@Immutable
internal sealed interface DiffLine {
    @Immutable
    data class Unchanged(val text: String, val lineLeft: Int, val lineRight: Int) : DiffLine

    @Immutable
    data class Different(
        val textLeft: String?,
        val lineLeft: Int?,
        val textRight: String?,
        val lineRight: Int?
    ) : DiffLine
}
