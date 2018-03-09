package io.lerk.lrkFM.consts;

public enum Preference {
    FIRST_START("first_start"),
    HOME_DIR("home_dir"),
    BOOKMARKS("bookmarks"),
    BOOKMARK_CURRENT_FOLDER("bookmark_current_folder"),
    BOOKMARK_EDIT_MODE("bookmark_deletion_on"),
    SHOW_TOAST("show_toast_on_cd"),
    FILENAME_LENGTH("filename_length"),
    HEADER_PATH_LENGTH("header_path_length"),
    SORT_FILES_BY("sort_files_by"),
    USE_CONTEXT_FOR_OPS("context_for_ops"),
    USE_CONTEXT_FOR_OPS_TOAST("context_for_ops_toast"),
    ALWAYS_EXTRACT_IN_CURRENT_DIR("always_extract_in_current_folder");

    private final String key;

    Preference(String key) {
        this.key = key;
    }

    public String getKey() {
        return key;
    }
}
