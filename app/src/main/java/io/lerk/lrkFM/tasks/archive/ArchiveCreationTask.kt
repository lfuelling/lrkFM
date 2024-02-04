package io.lerk.lrkFM.tasks.archive

import android.util.Log

import io.lerk.lrkFM.R
import io.lerk.lrkFM.activities.file.FileActivity
import io.lerk.lrkFM.entities.FMFile
import io.lerk.lrkFM.exceptions.BlockingStuffOnMainThreadException
import io.lerk.lrkFM.tasks.ProgressDialogCallbackTask
import java.io.File
import java.util.concurrent.CopyOnWriteArrayList

/**
 * [android.os.AsyncTask] to be run when creating an archive.
 *
 * @author Lukas Fülling (lukas@k40s.net)
 */
class ArchiveCreationTask(
    context: FileActivity,
    targets: CopyOnWriteArrayList<FMFile?>?,
    destination: File,
    callback: (Boolean) -> Unit
) : ProgressDialogCallbackTask<Boolean>(context, callback) {
    /**
     * The files to be added to the archive.
     */
    private val targets: CopyOnWriteArrayList<FMFile?>?

    /**
     * The destination archive file.
     */
    private val destination: File

    /**
     * Constructor.
     * @param context the current [FileActivity] instance.
     * @param targets the target files
     * @param destination the destination archive file
     * @param callback the callback handler
     */
    init {
        dialog.setTitle(R.string.creating)
        dialog.setMessage(context.getString(R.string.creating_detail) + FileActivity.Companion.WHITESPACE + destination.name)
        dialog.setCancelable(false)
        this.targets = targets
        this.destination = destination
    }

    /**
     * Creates the archive.
     * @param voids the unused params
     * @return true if successful
     * @see ArchiveUtil.doCreateZip
     */
    @Deprecated("Deprecated in Java")
    override fun doInBackground(vararg voids: Void?): Boolean {
        return try {
            ArchiveUtil().doCreateZip(targets, destination)
        } catch (e: BlockingStuffOnMainThreadException) {
            Log.wtf(TAG, "This should not happen here!", e)
            false
        }
    }

    companion object {
        private val TAG = ArchiveCreationTask::class.java.canonicalName
    }
}