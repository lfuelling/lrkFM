package io.lerk.lrkFM.tasks;

import android.app.ProgressDialog;
import android.os.AsyncTask;

import io.lerk.lrkFM.Handler;
import io.lerk.lrkFM.activities.FileActivity;

public abstract class ProgressDialogBooleanCallbackTask extends AsyncTask<Void, Void, Boolean> {

    protected final ProgressDialog dialog;
    private final Handler<Boolean> callback;

    ProgressDialogBooleanCallbackTask(FileActivity context, Handler<Boolean> callback) {
        this.dialog = new ProgressDialog(context);
        this.callback = callback;
    }

    @Override
    protected void onPreExecute() {
        this.dialog.show();
    }

    @Override
    protected void onPostExecute(final Boolean success) {
        if (dialog.isShowing()) {
            dialog.dismiss();
        }
        callback.handle(success);
    }
}