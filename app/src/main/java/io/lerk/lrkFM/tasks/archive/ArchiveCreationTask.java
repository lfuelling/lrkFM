package io.lerk.lrkFM.tasks.archive;

import android.util.Log;

import java.io.File;
import java.util.ArrayList;

import io.lerk.lrkFM.Handler;
import io.lerk.lrkFM.R;
import io.lerk.lrkFM.activities.FileActivity;
import io.lerk.lrkFM.entities.FMFile;
import io.lerk.lrkFM.exceptions.BlockingStuffOnMainThreadException;
import io.lerk.lrkFM.tasks.ProgressDialogBooleanCallbackTask;
import io.lerk.lrkFM.tasks.archive.ArchiveUtil;

/**
 * {@link android.os.AsyncTask} to be run when creating an archive.
 */
public class ArchiveCreationTask extends ProgressDialogBooleanCallbackTask {

    private static final String TAG = ArchiveCreationTask.class.getCanonicalName();
    /**
     * The files to be added to the archive.
     */
    private final ArrayList<FMFile> targets;

    /**
     * The destination archive file.
     */
    private final File destination;

    /**
     * Constructor.
     * @param context the current {@link FileActivity} instance.
     * @param targets the target files
     * @param destination the destination archive file
     * @param callback the callback {@link Handler}
     */
    public ArchiveCreationTask(FileActivity context, ArrayList<FMFile> targets, File destination, Handler<Boolean> callback) {
        super(context, callback);
        this.dialog.setTitle(R.string.creating);
        this.dialog.setMessage(context.getString(R.string.creating_detail) + FileActivity.WHITESPACE + destination.getName());
        this.dialog.setCancelable(false);

        this.targets = targets;
        this.destination = destination;
    }

    /**
     * Creates the archive.
     * @param voids the unused params
     * @return true if successful
     * @see ArchiveUtil#doCreateZip(ArrayList, File)
     */
    @Override
    protected Boolean doInBackground(Void... voids) {
        try {
            return new ArchiveUtil().doCreateZip(targets, destination);
        } catch (BlockingStuffOnMainThreadException e) {
            Log.wtf(TAG, "This should not happen here!", e);
            return false;
        }
    }
}