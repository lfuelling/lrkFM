package io.lerk.lrkFM.consts;

import java.util.HashSet;

import io.lerk.lrkFM.LrkFMApp;
import io.lerk.lrkFM.R;

/**
 * Preferences.
 */
public enum Preference {

    // Locally stored

    /**
     * If it's the first start of the application.
     */
    FIRST_START(Store.LOCAL, "first_start", true),

    /**
     * The home directory of the application.
     */
    HOME_DIR(Store.LOCAL, "home_dir", ""),

    /**
     * If the bookmarks can currently be edited.
     */
    BOOKMARK_EDIT_MODE(Store.LOCAL, "bookmark_deletion_on", false),

    /**
     * The parameter to sort files by.
     */
    SORT_FILES_BY(Store.LOCAL, "sort_files_by", (LrkFMApp.getContext() != null) ? LrkFMApp.getContext().getString(R.string.pref_sortby_value_default) : "name"),

    /**
     * If the backup quota is exceeded.
     */
    BACKUP_QUOTA_EXCEEDED(Store.LOCAL, "backup_quota_exceeded", false),

    // Backed up to cloud

    /**
     * The bookmarks.
     */
    BOOKMARKS(Store.CLOUD_BACKED, "bookmarks", new HashSet<>()),

    /**
     * If always the current folder should be added as a bookmark.
     */
    BOOKMARK_CURRENT_FOLDER(Store.CLOUD_BACKED, "bookmark_current_folder", false),

    /**
     * If a toast should be shown on directory change.
     */
    SHOW_TOAST(Store.CLOUD_BACKED, "show_toast_on_cd", false),

    /**
     * The maximum display length of filenames.
     */
    FILENAME_LENGTH(Store.CLOUD_BACKED, "filename_length", "27"),

    /**
     * Maximum length of the header path.
     */
    HEADER_PATH_LENGTH(Store.CLOUD_BACKED, "header_path_length", (LrkFMApp.getContext() != null) ? LrkFMApp.getContext().getString(R.string.pref_header_path_length_default) : "27"),

    /**
     * If the context should be used for file ops. (Choose target by navigating)
     */
    USE_CONTEXT_FOR_OPS(Store.CLOUD_BACKED, "context_for_ops", true),

    /**
     * If a Toast should be shown on file operation context change.
     */
    USE_CONTEXT_FOR_OPS_TOAST(Store.CLOUD_BACKED, "context_for_ops_toast", true),

    /**
     * If archives should always be extracted in the current directory.
     */
    ALWAYS_EXTRACT_IN_CURRENT_DIR(Store.CLOUD_BACKED, "always_extract_in_current_folder", false);

    /**
     * The preference's key.
     */
    private final String key;

    /**
     * The preference's storage type.
     * @see Store
     */
    private final Store store;

    /**
     * The type of the preference.
     */
    private final Class type;

    /**
     * The default value of the preference.
     */
    private final Object defaultValue;

    /**
     * Constructor.
     * @param store teh storage type
     * @param key the key
     * @param defaultValue the default value
     */
    Preference(Store store, String key, Object defaultValue) {
        this.key = key;
        this.store = store;
        this.defaultValue = defaultValue;
        this.type = defaultValue.getClass();
    }

    /**
     * Getter.
     * @return the key.
     */
    public String getKey() {
        return key;
    }

    /**
     * Getter.
     * @return the storage type.
     * @see Store
     */
    public Store getStore() {
        return store;
    }

    /**
     * Getter.
     * @return the type.
     */
    public Class getType() {
        return type;
    }

    /**
     * Getter.
     * @return the default value.
     */
    public Object getDefaultValue() {
        return defaultValue;
    }

    /**
     * Enum to differentiate between storage types.
     */
    public enum Store {

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
         * @see io.lerk.lrkFM.util.LrkFMBackupAgent
         * @see io.lerk.lrkFM.util.PrefUtils
         */
        private final String name;

        /**
         * Constructor.
         * @param name name of the preferences
         * @see #name
         */
        Store(String name) {
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
}
