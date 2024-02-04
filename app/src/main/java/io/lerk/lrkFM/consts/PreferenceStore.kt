package io.lerk.lrkFM.consts

import io.lerk.lrkFM.Pref

/**
 * Enum to differentiate between storage types.
 *
 * @author Lukas Fülling (lukas@k40s.net)
 */
enum class PreferenceStore
/**
 * Constructor.
 * @param storeName name of the preferences_general
 * @see .name
 */(
    /**
     * Name of the preferences_general.
     *
     * @see Pref
     */
    val storeName: String
) {
    /**
     * Only stored locally.
     */
    LOCAL("local"),

    /**
     * Stored in the cloud
     */
    CLOUD_BACKED("cloud_backed")
    /**
     * Getter for name.
     * @return the name of the preferences_general
     * @see .name
     */

}
