package io.lerk.lrkFM.tasks.operation

import android.app.AlertDialog
import android.content.DialogInterface

import io.lerk.lrkFM.R
import io.lerk.lrkFM.activities.file.FileActivity
import io.lerk.lrkFM.tasks.ProgressDialogCallbackTask

/**
 * Abstract operation task.
 *
 * @author Lukas Fülling (lukas@k40s.net)
 */
abstract class FileOperationTask internal constructor(
    context: FileActivity?,
    callback: (Boolean) -> Unit
) : ProgressDialogCallbackTask<Boolean>(context, callback) {
    init {
        dialog.setCancelable(false)
    }

    companion object {
        fun getFileExistsDialogBuilder(context: FileActivity?): AlertDialog.Builder {
            return AlertDialog.Builder(context)
                .setCancelable(true)
                .setTitle(R.string.warn_file_exists_title)
                .setMessage(R.string.warn_file_exists_msg)
                .setPositiveButton(R.string.yes) { dialogInterface: DialogInterface, _: Int -> dialogInterface.dismiss() }
                .setNegativeButton(R.string.cancel) { dialogInterface: DialogInterface, _: Int -> dialogInterface.cancel() }
        }
    }
}
