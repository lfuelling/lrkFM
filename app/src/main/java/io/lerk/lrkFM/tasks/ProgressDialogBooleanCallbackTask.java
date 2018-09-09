package io.lerk.lrkFM.tasks;

import android.app.ProgressDialog;
import android.os.AsyncTask;

import io.lerk.lrkFM.Handler;
import io.lerk.lrkFM.activities.FileActivity;

/**
 * {@link AsyncTask} which has void params, void progress and boolean return value.
 * This class also shows a progress spinner while the task is running.
 */
public abstract class ProgressDialogBooleanCallbackTask extends AsyncTask<Void, Void, Boolean> {

    /**
     * The {@link ProgressDialog}.
     */
    protected final ProgressDialog dialog;

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
        this.dialog = new ProgressDialog(context);
        this.callback = callback;
    }

    /**
     * Shows the progress spinner.
     */
    @Override
    protected void onPreExecute() {
        this.dialog.show();
    }

    /**
     * Hides the progress spinner and triggers the callback.
     * @param success the boolean return value.
     */
    @Override
    protected void onPostExecute(final Boolean success) {
        if (dialog.isShowing()) {
            dialog.dismiss();
        }
        callback.handle(success);
    }
}