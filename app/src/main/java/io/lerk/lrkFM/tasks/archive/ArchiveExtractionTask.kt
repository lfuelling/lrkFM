package io.lerk.lrkFM.tasks.archive

import android.util.Log

import io.lerk.lrkFM.R
import io.lerk.lrkFM.activities.file.FileActivity
import io.lerk.lrkFM.entities.FMFile
import io.lerk.lrkFM.exceptions.BlockingStuffOnMainThreadException
import io.lerk.lrkFM.tasks.ProgressDialogCallbackTask

/**
 * [android.os.AsyncTask] that is called when there's an archive to extract.
 *
 * @author Lukas Fülling (lukas@k40s.net)
 */
class ArchiveExtractionTask(
    context: FileActivity,
    destinationPath: String?,
    archive: FMFile?,
    callback: (Boolean) -> Unit
) : ProgressDialogCallbackTask<Boolean>(context, callback) {
    /**
     * The destination path to extract to.
     */
    private val destinationPath: String?

    /**
     * The archive to extract.
     */
    private val archive: FMFile?

    /**
     * Constructor.
     * @param context the current [FileActivity] instance
     * @param destinationPath the destination path to extract to
     * @param archive the archive to extract
     * @param callback the callback handler
     */
    init {
        dialog.setTitle(R.string.extracting)
        dialog.setMessage(context.getString(R.string.extracting_detail) + FileActivity.Companion.WHITESPACE + archive!!.name)
        dialog.setCancelable(false)
        this.destinationPath = destinationPath
        this.archive = archive
    }

    /**
     * Extracts the archive.
     * @param args the void args (unused)
     * @return true if successful.
     * @see ArchiveUtil.doExtractArchive
     */
    @Deprecated("Deprecated in Java")
    override fun doInBackground(vararg args: Void?): Boolean {
        return try {
            ArchiveUtil().doExtractArchive(destinationPath, archive)
        } catch (e: BlockingStuffOnMainThreadException) {
            Log.wtf(TAG, "This should not happen here!", e)
            false
        }
    }

    companion object {
        private val TAG = ArchiveExtractionTask::class.java.canonicalName
    }
}