package com.worldline.devview.networkmock

import com.worldline.devview.utils.DataStoreDelegate

/**
 * The process-level [DataStoreDelegate] for the network mock module.
 *
 * This single top-level instance is shared by both `devview-networkmock` (UI)
 * and `devview-networkmock-ktor` (Ktor plugin), ensuring that both modules
 * operate on the same underlying DataStore instance regardless of which one
 * accesses it first.
 *
 * ## Ownership
 * - **Initialised by**: `devview-networkmock` — the UI module drives initialisation
 *   via `Module.initModule` after `rememberModules` has called
 *   [com.worldline.devview.utils.RequiresDataStore.initDataStore]
 * - **Read by**: `devview-networkmock-ktor` — the Ktor plugin reads the DataStore
 *   instance via [DataStoreDelegate.get] at request interception time, which is
 *   always after Composition has run
 *
 * ## Why a top-level val
 * Both `devview-networkmock` and `devview-networkmock-ktor` depend on
 * `devview-networkmock-core` but do not depend on each other. A top-level val
 * in `core` is the only location both modules can reference without introducing
 * a circular dependency.
 *
 * @see DataStoreDelegate
 * @see NetworkMockInitializer
 */
public val NetworkMockDataStoreDelegate: DataStoreDelegate = DataStoreDelegate()

/**
 * The filename used for the network mock DataStore preferences file.
 *
 * Internal to `devview-networkmock-core` — integrators never see or set this value.
 */
public const val NETWORK_MOCK_DATASTORE_NAME: String = "network_mock_datastore.preferences_pb"
