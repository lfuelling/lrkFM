package io.lerk.lrkFM.tasks;

import io.lerk.lrkFM.Handler;
import io.lerk.lrkFM.R;
import io.lerk.lrkFM.activities.FileActivity;
import io.lerk.lrkFM.entities.FMFile;
import io.lerk.lrkFM.op.ArchiveUtil;

public class ArchiveExtractionTask extends ProgressDialogBooleanCallbackTask {

    private final String destinationPath;
    private final FMFile archive;

    public ArchiveExtractionTask(FileActivity context, String destinationPath, FMFile archive, Handler<Boolean> callback) {
        super(context, callback);
        this.dialog.setTitle(R.string.extracting);
        this.dialog.setMessage(context.getString(R.string.extracting_detail) + FileActivity.WHITESPACE + archive.getName());
        this.dialog.setCancelable(false);

        this.destinationPath = destinationPath;
        this.archive = archive;
    }

    @Override
    protected Boolean doInBackground(final Void... args) {
        return new ArchiveUtil().doExtractArchive(destinationPath, archive);
    }
}