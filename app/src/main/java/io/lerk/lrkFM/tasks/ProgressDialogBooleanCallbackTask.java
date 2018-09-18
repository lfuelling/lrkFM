package io.lerk.lrkFM.tasks;

import android.os.AsyncTask;

import io.lerk.lrkFM.Handler;
import io.lerk.lrkFM.activities.file.FileActivity;

/**
 * {@link AsyncTask} which has void params, void progress and boolean return value.
 * This class also shows a progress spinner while the task is running.
 */
public abstract class ProgressDialogBooleanCallbackTask extends ProgressDialogTask<Boolean> {

    /**
     * The callback {@link Handler}.
     */
    private final Handler<Boolean> callback;

    /**
     * Constructor.
     * @param context the current {@link FileActivity} instance.
     * @param callback the callback to use.
     */
    public ProgressDialogBooleanCallbackTask(FileActivity context, Handler<Boolean> callback) {
        super(context);
        this.callback = callback;
    }

    /**
     * Hides the progress spinner and triggers the callback.
     * @param success the boolean return value.
     */
    @Override
    protected void onPostExecute(final Boolean success) {
        super.onPostExecute(success);
        callback.handle(success);
    }
}