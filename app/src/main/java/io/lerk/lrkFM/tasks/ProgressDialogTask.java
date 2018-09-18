package io.lerk.lrkFM.tasks;

import android.app.ProgressDialog;
import android.os.AsyncTask;

import io.lerk.lrkFM.activities.file.FileActivity;

/**
 * {@link AsyncTask} which shows a progress spinner while the task is running.
 */
public abstract class ProgressDialogTask<T> extends AsyncTask<Void, Void, T> {

    /**
     * The {@link ProgressDialog}.
     */
    protected final ProgressDialog dialog;

    /**
     * Constructor.
     * @param context the current {@link FileActivity} instance
     */
    ProgressDialogTask(FileActivity context) {
        this.dialog = new ProgressDialog(context);
    }

    /**
     * Shows the progress spinner.
     */
    @Override
    protected void onPreExecute() {
        this.dialog.show();
    }

    /**
     * Hides the progress spinner.
     * @param t return value
     */
    @Override
    protected void onPostExecute(T t) {
        super.onPostExecute(t);
        if (dialog.isShowing()) {
            dialog.dismiss();
        }
    }
}
