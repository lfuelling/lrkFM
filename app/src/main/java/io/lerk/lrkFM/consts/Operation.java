package io.lerk.lrkFM.consts;

import android.support.annotation.StringRes;

import io.lerk.lrkFM.R;
import io.lerk.lrkFM.activities.FileActivity;

/**
 * @author Lukas Fülling (lukas@k40s.net)
 */
public enum Operation {
    COPY(R.string.copy),
    MOVE(R.string.move),
    EXTRACT(R.string.extract),
    CREATE_ZIP(R.string.new_zip_file),
    NONE(R.string.todo);

    private final int title;

    /**
     * File Operation.
     *
     * @param title May not contain <pre>(</pre>
     * @see FileActivity#onPrepareOptionsMenu
     */
    Operation(@StringRes int title) {
        this.title = title;
    }

    public int getTitle() {
        return title;
    }
}