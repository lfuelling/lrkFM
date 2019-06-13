package io.lerk.lrkFM.consts;

import io.lerk.lrkFM.Pref;

/**
 * Enum to differentiate between storage types.
 *
 * @author Lukas Fülling (lukas@k40s.net)
 */
public enum PreferenceStore {

    /**
     * Only stored locally.
     */
    LOCAL("local"),

    /**
     * Stored in the cloud
     */
    CLOUD_BACKED("cloud_backed");

    /**
     * Name of the preferences.
     *
     * @see Pref
     */
    private final String name;

    /**
     * Constructor.
     * @param name name of the preferences
     * @see #name
     */
    PreferenceStore(String name) {
        this.name = name;
    }

    /**
     * Getter for name.
     * @return the name of the preferences
     * @see #name
     */
    public String getName() {
        return name;
    }
}
