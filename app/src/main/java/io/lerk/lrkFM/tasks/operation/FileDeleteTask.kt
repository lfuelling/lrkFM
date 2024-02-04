package io.lerk.lrkFM.tasks.operation

import android.util.Log

import io.lerk.lrkFM.R
import io.lerk.lrkFM.activities.file.FileActivity
import io.lerk.lrkFM.entities.FMFile
import io.lerk.lrkFM.exceptions.BlockingStuffOnMainThreadException

/**
 * Task to delete a file.
 *
 * @author Lukas Fülling (lukas@k40s.net)
 */
class FileDeleteTask(context: FileActivity, callback: (Boolean) -> Unit, private val f: FMFile?) :
    FileOperationTask(context, callback) {
    init {
        dialog.setTitle(R.string.deleting)
        dialog.setMessage(context.getString(R.string.deleting_detail) + FileActivity.Companion.WHITESPACE + f?.name)
    }

    @Deprecated("Deprecated in Java")
    override fun doInBackground(vararg voids: Void?): Boolean? {
        return try {
            OperationUtil.doDeleteNoValidation(f)
        } catch (e: BlockingStuffOnMainThreadException) {
            Log.wtf(TAG, "This should not happen here!", e)
            false
        }
    }

    companion object {
        private val TAG = FileDeleteTask::class.java.canonicalName
    }
}
