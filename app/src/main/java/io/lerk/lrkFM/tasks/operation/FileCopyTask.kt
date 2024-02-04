package io.lerk.lrkFM.tasks.operation

import android.app.AlertDialog
import android.content.DialogInterface
import android.util.Log
import android.widget.EditText
import android.widget.Toast

import io.lerk.lrkFM.R
import io.lerk.lrkFM.activities.file.FileActivity
import io.lerk.lrkFM.entities.FMFile
import io.lerk.lrkFM.exceptions.BlockingStuffOnMainThreadException
import java.io.File
import java.lang.ref.WeakReference

/**
 * Task used to copy a file.
 *
 * @author Lukas Fülling (lukas@k40s.net)
 */
class FileCopyTask(context: FileActivity, callback: (Boolean) -> Unit, f: FMFile?, d: AlertDialog?) :
    FileOperationTask(context, callback) {
    private val contextRef: WeakReference<FileActivity>
    private val f: FMFile?
    private var destination: File? = null

    init {
        setDestination(context, f, d)
        contextRef = WeakReference(context)
        this.f = f
        dialog.setTitle(R.string.copying)
        dialog.setMessage(context.getString(R.string.copying_detail) + FileActivity.Companion.WHITESPACE + destination!!.name)
    }

    @Deprecated("Deprecated in Java")
    override fun doInBackground(vararg voids: Void?): Boolean {
        return try {
            OperationUtil.doCopy(f, destination)
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

    private fun setDestination(context: FileActivity, f: FMFile?, d: AlertDialog?) {
        val pathname: String?
        if (d != null) {
            val editText = d.findViewById<EditText>(R.id.destinationPath)
            pathname = editText.text.toString()
            if (pathname.isEmpty()) {
                Toast.makeText(context, R.string.err_empty_input, Toast.LENGTH_SHORT).show()
                cancel(true)
            }
        } else {
            pathname = context.currentDirectory
        }
        destination = File(pathname!!)
        if (destination!!.isDirectory && !f!!.isDirectory) {
            destination = File(destination!!.absolutePath + File.separator + f.name)
        }
        if (destination!!.exists()) {
            FileOperationTask.Companion.getFileExistsDialogBuilder(context)
                .setOnDismissListener(DialogInterface.OnDismissListener { _: DialogInterface? -> this.execute() })
                .setOnCancelListener(DialogInterface.OnCancelListener { _: DialogInterface? ->
                    cancel(
                        true
                    )
                })
                .create().show()
        }
    }

    companion object {
        private val TAG = FileCopyTask::class.java.canonicalName
    }
}
