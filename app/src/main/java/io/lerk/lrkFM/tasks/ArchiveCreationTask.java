package io.lerk.lrkFM.tasks;

import java.io.File;
import java.util.ArrayList;

import io.lerk.lrkFM.Handler;
import io.lerk.lrkFM.R;
import io.lerk.lrkFM.activities.FileActivity;
import io.lerk.lrkFM.entities.FMFile;
import io.lerk.lrkFM.op.ArchiveUtil;

public class ArchiveCreationTask extends ProgressDialogBooleanCallbackTask {

    private final ArrayList<FMFile> targets;
    private final File destination;

    public ArchiveCreationTask(FileActivity context, ArrayList<FMFile> targets, File destination, Handler<Boolean> callback) {
        super(context, callback);
        this.dialog.setTitle(R.string.creating);
        this.dialog.setMessage(context.getString(R.string.creating_detail) + FileActivity.WHITESPACE + destination.getName());
        this.dialog.setCancelable(false);

        this.targets = targets;
        this.destination = destination;
    }

    @Override
    protected Boolean doInBackground(Void... voids) {
        return new ArchiveUtil().doCreateZip(targets, destination);
    }
}