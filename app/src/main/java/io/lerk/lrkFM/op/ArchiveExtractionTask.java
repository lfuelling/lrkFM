package io.lerk.lrkFM.op;


import android.app.ProgressDialog;
import android.os.AsyncTask;

import io.lerk.lrkFM.Handler;
import io.lerk.lrkFM.R;
import io.lerk.lrkFM.activities.FileActivity;
import io.lerk.lrkFM.entities.FMFile;

public class ArchiveExtractionTask extends AsyncTask<String, Void, Boolean> {

    private final ProgressDialog dialog;
    private final ArchiveUtil archiveUtil;
    private final String destinationPath;
    private final FMFile archive;
    private final Handler<Boolean> callback;

    public ArchiveExtractionTask(FileActivity context, String destinationPath, FMFile archive, Handler<Boolean> callback) {
        this.dialog = new ProgressDialog(context);
        this.dialog.setTitle(R.string.extracting);
        this.dialog.setMessage(context.getString(R.string.extracting_detail) + FileActivity.WHITESPACE + archive.getName());
        this.dialog.setCancelable(false);

        this.archiveUtil = new ArchiveUtil();
        this.destinationPath = destinationPath;
        this.archive = archive;
        this.callback = callback;
    }

    @Override
    protected void onPreExecute() {
        this.dialog.show();
    }

    @Override
    protected Boolean doInBackground(final String... args) {
        return archiveUtil.doExtractArchive(destinationPath, archive);
    }

    @Override
    protected void onPostExecute(final Boolean success) {
        if (dialog.isShowing()) {
            dialog.dismiss();
        }
        callback.handle(success);
    }
}