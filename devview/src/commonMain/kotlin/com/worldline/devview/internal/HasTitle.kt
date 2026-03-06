package com.worldline.devview.internal

import androidx.navigation3.runtime.NavKey

/**
 * Internal marker interface for navigation keys that carry a title for the DevView top app bar.
 *
 * This interface is intentionally **internal** to the `devview` module. It is not part of
 * the public API and must not be implemented by integrators or by module destination types.
 *
 * ## Purpose
 *
 * [HasTitle] exists to handle the title of navigation destinations that live inside the
 * DevView framework itself (e.g. [com.worldline.devview.Home], future settings screens)
 * and that therefore cannot be represented in any [com.worldline.devview.core.Module]'s
 * [com.worldline.devview.core.DestinationMetadata] map.
 *
 * For module destinations, titles are declared via
 * [com.worldline.devview.core.DestinationMetadata.title] instead.
 *
 * ## Title resolution order in DevView
 *
 * When resolving the top app bar title, DevView checks in the following order:
 * 1. The active backstack entry implements [HasTitle] → use [title] directly.
 * 2. The active backstack entry belongs to a registered module and its
 *    [com.worldline.devview.core.DestinationMetadata.title] is non-null → use that.
 * 3. Fall back to [com.worldline.devview.core.Module.moduleName].
 *
 * ## Maintainer note
 *
 * If you add a new framework-level screen (i.e. a [NavKey] that is not part of any module),
 * implement this interface on its destination object to ensure the top app bar shows a
 * meaningful title. Do **not** expose this interface publicly.
 *
 * @property title The title string to display in the top app bar.
 *
 * @see com.worldline.devview.Home
 * @see com.worldline.devview.core.DestinationMetadata
 */
internal interface HasTitle : NavKey {
    val title: String
}
