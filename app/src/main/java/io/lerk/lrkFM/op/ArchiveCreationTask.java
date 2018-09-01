package io.lerk.lrkFM.op;

import android.app.ProgressDialog;
import android.os.AsyncTask;

import java.io.File;
import java.util.ArrayList;

import io.lerk.lrkFM.Handler;
import io.lerk.lrkFM.R;
import io.lerk.lrkFM.activities.FileActivity;
import io.lerk.lrkFM.entities.FMFile;

public class ArchiveCreationTask extends AsyncTask<String, Void, Boolean> {

    private final ProgressDialog dialog;
    private final ArchiveUtil archiveUtil;
    private final ArrayList<FMFile> targets;
    private final File destination;
    private final Handler<Boolean> callback;

    public ArchiveCreationTask(FileActivity context, ArrayList<FMFile> targets, File destination, Handler<Boolean> callback) {
        this.dialog = new ProgressDialog(context);
        this.archiveUtil = new ArchiveUtil();
        this.targets = targets;
        this.dialog.setTitle(R.string.creating);
        this.dialog.setMessage(context.getString(R.string.creating_detail) + FileActivity.WHITESPACE + destination.getName());
        this.dialog.setCancelable(false);
        this.destination = destination;
        this.callback = callback;
    }

    @Override
    protected void onPreExecute() {
        this.dialog.show();
    }

    @Override
    protected Boolean doInBackground(final String... args) {
        return archiveUtil.doCreateZip(targets, destination);
    }

    @Override
    protected void onPostExecute(final Boolean success) {
        if (dialog.isShowing()) {
            dialog.dismiss();
        }
        callback.handle(success);
    }
}