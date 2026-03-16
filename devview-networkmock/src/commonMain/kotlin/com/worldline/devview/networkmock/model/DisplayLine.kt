package com.worldline.devview.networkmock.model

import androidx.compose.runtime.Immutable

/**
 * A flattened display entry used by [InlineDiffContent].
 * Long runs of [DiffLine.Unchanged] are collapsed into [DisplayLine.Collapsed].
 */
@Immutable
internal sealed interface DisplayLine {
    @Immutable
    data class Unchanged(val text: String, val lineLeft: Int, val lineRight: Int) : DisplayLine

    @Immutable
    data class Left(val text: String, val line: Int) : DisplayLine

    @Immutable
    data class Right(val text: String, val line: Int) : DisplayLine

    /** Placeholder for a collapsed run of unchanged lines. */
    @Immutable
    data class Collapsed(val count: Int) : DisplayLine
}
