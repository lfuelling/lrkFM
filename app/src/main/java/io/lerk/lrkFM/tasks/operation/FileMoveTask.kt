package io.lerk.lrkFM.tasks.operation

import android.app.AlertDialog
import android.content.DialogInterface
import android.util.Log
import android.view.View
import android.widget.EditText
import android.widget.Toast

import io.lerk.lrkFM.R
import io.lerk.lrkFM.activities.file.FileActivity
import io.lerk.lrkFM.entities.FMFile
import io.lerk.lrkFM.exceptions.BlockingStuffOnMainThreadException
import java.io.File
import java.lang.ref.WeakReference

/**
 * Task to move a file.
 *
 * @author Lukas Fülling (lukas@k40s.net)
 */
class FileMoveTask private constructor(
    context: FileActivity,
    callback: (Boolean) -> Unit,
    private val f: FMFile?,
    newName: String
) : FileOperationTask(context, callback) {
    private val contextRef: WeakReference<FileActivity>
    private val destination: File

    constructor(
        context: FileActivity,
        callback: (Boolean) -> Unit,
        f: FMFile?,
        d: AlertDialog?
    ) : this(
        context,
        callback,
        f,
        (if (d != null) (d.findViewById<View>(R.id.destinationName) as EditText).text.toString() else context.currentDirectory)!!
    )

    init {
        contextRef = WeakReference(context)
        if (newName.isEmpty()) {
            Toast.makeText(context, R.string.err_empty_input, Toast.LENGTH_SHORT).show()
            cancel(true)
        }
        destination = File(newName + "/" + f!!.file.name)
        if (destination.exists()) {
            FileOperationTask.Companion.getFileExistsDialogBuilder(context)
                .setOnDismissListener { _: DialogInterface? -> this.execute() }
                .setOnCancelListener { _: DialogInterface? ->
                    cancel(
                        true
                    )
                } //cancel task on "no"
                .create().show()
        }
        dialog.setTitle(R.string.moving)
        dialog.setMessage(context.getString(R.string.moving_detail) + FileActivity.Companion.WHITESPACE + f.name)
    }

    @Deprecated("Deprecated in Java")
    override fun doInBackground(vararg voids: Void?): Boolean {
        return try {
            OperationUtil.doMove(f, destination)
        } catch (e: BlockingStuffOnMainThreadException) {
            Log.wtf(TAG, "This should not happen here!", e)
            false
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onPostExecute(t: Boolean) {
        super.onPostExecute(t)
        contextRef.get()!!.clearFileOpCache()
        contextRef.get()!!.reloadCurrentDirectory()
    }

    companion object {
        private val TAG = FileMoveTask::class.java.canonicalName
    }
}
