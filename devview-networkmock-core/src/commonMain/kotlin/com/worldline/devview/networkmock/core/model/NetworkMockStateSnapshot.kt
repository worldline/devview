package com.worldline.devview.networkmock.core.model

/**
 * Persisted network mock state and its repository-wide revision, read atomically.
 *
 * Pass [revision] and the selected sequence to
 * [com.worldline.devview.networkmock.core.repository.MockStateRepository.compareAndAdvanceSequence]
 * before returning a prepared sequence response. Explicit state writes increment the revision
 * even when their value is unchanged; timestamps and state equality cannot detect those resets.
 *
 * @property state State read from the same DataStore snapshot as [revision]
 * @property revision Persisted revision shared by all writers; zero for existing unversioned state
 */
@Suppress("UseDataClass")
public class NetworkMockStateSnapshot internal constructor(
    public val state: NetworkMockState,
    public val revision: Long
)
