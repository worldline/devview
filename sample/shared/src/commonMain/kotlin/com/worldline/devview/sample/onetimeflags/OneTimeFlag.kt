package com.worldline.devview.sample.onetimeflags

import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey

/** App-owned one-shot UI flags. Keep existing persisted key names when adopting this enum. */
public enum class OneTimeFlag(public val description: String) {
    /** Onboarding has been completed. */
    ONBOARDING_SEEN(description = "Onboarding carousel"),

    /** The sample rating dialog has been dismissed. */
    RATING_PROMPT_SHOWN(description = "App rating dialog"),

    /** Release notes have been seen. */
    WHATS_NEW_SEEN(description = "What's new sheet"),

    /** A contextual tip has been seen. */
    COACH_MARK_SEEN(description = "Filter coach mark");

    /** Key used by both the app and its developer module. */
    public val key: Preferences.Key<Boolean> = booleanPreferencesKey(name = name)
}
