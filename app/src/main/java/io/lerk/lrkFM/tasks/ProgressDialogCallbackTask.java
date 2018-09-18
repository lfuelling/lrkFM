package io.lerk.lrkFM.tasks;

import android.app.ProgressDialog;
import android.os.AsyncTask;

import io.lerk.lrkFM.Handler;
import io.lerk.lrkFM.activities.file.FileActivity;

/**
 * {@link AsyncTask} which has void params, void progress and generic return value.
 * This class also shows a progress spinner while the task is running.
 */
public abstract class ProgressDialogCallbackTask<T> extends CallbackTask<T> {

    /**
     * The {@link ProgressDialog}.
     */
    protected final ProgressDialog dialog;

    /**
     * Constructor.
     * @param context the current {@link FileActivity} instance.
     * @param callback the callback to use.
     */
    public ProgressDialogCallbackTask(FileActivity context, Handler<T> callback) {
        super(callback);
        this.dialog = new ProgressDialog(context);
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        this.dialog.show();
    }

    /**
     * Hides the progress spinner and triggers the callback.
     * @param t the return value.
     */
    @Override
    protected void onPostExecute(final T t) {
        super.onPostExecute(t);
        if (dialog.isShowing()) {
            dialog.dismiss();
        }
    }
}