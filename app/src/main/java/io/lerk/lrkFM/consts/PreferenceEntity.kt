package io.lerk.lrkFM.consts

import android.os.Build
import io.lerk.lrkFM.LrkFMApp
import io.lerk.lrkFM.R

/**
 * Preferences.
 *
 * @author Lukas Fülling (lukas@k40s.net)
 */
enum class PreferenceEntity(
    /**
     * The preference's storage type.
     *
     * @see PreferenceStore
     */
    val store: PreferenceStore,
    /**
     * The preference's key.
     */
    val key: String,
    /**
     * The default value of the preference.
     */
    val defaultValue: Any
) {
    // Locally stored
    /**
     * If it's the first start of the application.
     */
    FIRST_START(PreferenceStore.LOCAL, "first_start", true),

    /**
     * The home directory of the application.
     */
    HOME_DIR(
        PreferenceStore.LOCAL,
        "home_dir",
        if (Build.VERSION.SDK_INT <= 28) "/storage/emulated/0" else "/storage"
    ),

    /**
     * If the bookmarks can currently be edited.
     */
    BOOKMARK_EDIT_MODE(PreferenceStore.LOCAL, "bookmark_deletion_on", false),

    /**
     * The parameter to sort files by.
     */
    SORT_FILES_BY(
        PreferenceStore.LOCAL,
        "sort_files_by",
        if (LrkFMApp.Companion.getContext() != null) LrkFMApp.Companion.getContext()!!
            .getString(R.string.pref_sortby_value_default) else "namea"
    ),

    /**
     * If the backup quota is exceeded.
     */
    BACKUP_QUOTA_EXCEEDED(PreferenceStore.LOCAL, "backup_quota_exceeded", false),

    /**
     * If zip files should be explorable
     */
    ZIPS_EXPLORABLE(PreferenceStore.LOCAL, "zips_explorable", false),

    /**
     * If there should be a notification when there is a new version available.
     */
    UPDATE_NOTIFICATION(PreferenceStore.LOCAL, "update_notification", false),

    /**
     * Cache for "multitasking".
     */
    CURRENT_DIR_CACHE(PreferenceStore.LOCAL, "current_dir_cached", ""),

    /**
     * Always show [io.lerk.lrkFM.activities.IntroActivity].
     */
    ALWAYS_SHOW_INTRO(PreferenceStore.LOCAL, "always_show_intro", false),
    // Backed up to cloud
    /**
     * The bookmarks.
     */
    BOOKMARKS(PreferenceStore.CLOUD_BACKED, "bookmarks", HashSet<Any>()),

    /**
     * If always the current folder should be added as a bookmark.
     */
    BOOKMARK_CURRENT_FOLDER(PreferenceStore.CLOUD_BACKED, "bookmark_current_folder", false),

    /**
     * If a toast should be shown on directory change.
     */
    SHOW_TOAST(PreferenceStore.CLOUD_BACKED, "show_toast_on_cd", false),

    /**
     * The maximum display length of filenames.
     */
    FILENAME_LENGTH(PreferenceStore.CLOUD_BACKED, "filename_length", "27"),

    /**
     * Maximum length of the header path.
     */
    HEADER_PATH_LENGTH(
        PreferenceStore.CLOUD_BACKED,
        "header_path_length",
        if (LrkFMApp.Companion.getContext() != null) LrkFMApp.Companion.getContext()!!
            .getString(R.string.pref_header_path_length_default) else "27"
    ),

    /**
     * If a Toast should be shown on file operation context change.
     */
    USE_CONTEXT_FOR_OPS_TOAST(PreferenceStore.CLOUD_BACKED, "context_for_ops_toast", true),

    /**
     * If archives should always be extracted in the current directory.
     */
    ALWAYS_EXTRACT_IN_CURRENT_DIR(
        PreferenceStore.CLOUD_BACKED,
        "always_extract_in_current_folder",
        false
    ),

    /**
     * Unit to use in the nav drawer header.
     */
    NAV_HEADER_UNIT(PreferenceStore.CLOUD_BACKED, "nav_header_unit", "m"),

    /**
     * App Theme.
     */
    THEME(PreferenceStore.CLOUD_BACKED, "theme", "default"),

    /**
     * Vibration on some Toasts.
     */
    VIBRATING_TOASTS(PreferenceStore.CLOUD_BACKED, "context_toasts_vibrating", true),

    /**
     * Vibration length of vibrating Toasts.
     */
    VIBRATION_LENGTH(PreferenceStore.CLOUD_BACKED, "vibration_length", 120),

    /**
     * Replace checkboxes with menu buttons.
     */
    SHOW_MENU_BUTTON_INSTEAD_OF_CHECKBOX(
        PreferenceStore.CLOUD_BACKED,
        "show_menu_button_instead_of_checkbox",
        false
    );
    /**
     * Getter.
     *
     * @return the key.
     */
    /**
     * Getter.
     *
     * @return the storage type.
     * @see PreferenceStore
     */
    /**
     * Getter.
     *
     * @return the type.
     */
    /**
     * The type of the preference.
     */
    val type: Class<*>
    /**
     * Getter.
     *
     * @return the default value.
     */

    /**
     * Constructor.
     *
     * @param store        teh storage type
     * @param key          the key
     * @param defaultValue the default value
     */
    init {
        type = defaultValue.javaClass
    }

    companion object {
        fun determineByKey(key: String): PreferenceEntity? {
            for (preference in entries) {
                if (preference.key == key) {
                    return preference
                }
            }
            return null
        }
    }
}
