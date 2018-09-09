package io.lerk.lrkFM.tasks.archive;

import android.util.Log;

import io.lerk.lrkFM.Handler;
import io.lerk.lrkFM.R;
import io.lerk.lrkFM.activities.FileActivity;
import io.lerk.lrkFM.entities.FMFile;
import io.lerk.lrkFM.exceptions.BlockingStuffOnMainThreadException;
import io.lerk.lrkFM.tasks.ProgressDialogBooleanCallbackTask;
import io.lerk.lrkFM.tasks.archive.ArchiveUtil;

/**
 * {@link android.os.AsyncTask} that is called when there's an archive to extract.
 */
public class ArchiveExtractionTask extends ProgressDialogBooleanCallbackTask {

    private static final String TAG = ArchiveExtractionTask.class.getCanonicalName();
    /**
     * The destination path to extract to.
     */
    private final String destinationPath;

    /**
     * The archive to extract.
     */
    private final FMFile archive;

    /**
     * Constructor.
     * @param context the current {@link FileActivity} instance
     * @param destinationPath the destination path to extract to
     * @param archive the archive to extract
     * @param callback the callback {@link Handler}
     */
    public ArchiveExtractionTask(FileActivity context, String destinationPath, FMFile archive, Handler<Boolean> callback) {
        super(context, callback);
        this.dialog.setTitle(R.string.extracting);
        this.dialog.setMessage(context.getString(R.string.extracting_detail) + FileActivity.WHITESPACE + archive.getName());
        this.dialog.setCancelable(false);

        this.destinationPath = destinationPath;
        this.archive = archive;
    }

    /**
     * Extracts the archive.
     * @param args the void args (unused)
     * @return true if successful.
     * @see ArchiveUtil#doExtractArchive(String, FMFile)
     */
    @Override
    protected Boolean doInBackground(final Void... args) {
        try {
            return new ArchiveUtil().doExtractArchive(destinationPath, archive);
        } catch (BlockingStuffOnMainThreadException e) {
            Log.wtf(TAG, "This should not happen here!", e);
            return false;
        }
    }
}