package io.lerk.lrkFM.tasks

import android.app.ProgressDialog

import io.lerk.lrkFM.activities.file.FileActivity

/**
 * [AsyncTask] which has void params, void progress and generic return value.
 * This class also shows a progress spinner while the task is running.
 *
 * @author Lukas Fülling (lukas@k40s.net)
 */
abstract class ProgressDialogCallbackTask<T>(context: FileActivity?, callback: (T) -> Unit) :
    CallbackTask<T>(callback) {
    /**
     * The [ProgressDialog].
     */
    protected val dialog: ProgressDialog

    /**
     * Constructor.
     *
     * @param context  the current [FileActivity] instance.
     * @param callback the callback to use.
     */
    init {
        dialog = ProgressDialog(context)
    }

    override fun onPreExecute() {
        super.onPreExecute()
        dialog.show()
    }

    /**
     * Hides the progress spinner and triggers the callback.
     *
     * @param t the return value.
     */
    @Deprecated("Deprecated in Java")
    override fun onPostExecute(t: T) {
        super.onPostExecute(t)
        if (dialog.isShowing) {
            dialog.dismiss()
        }
    }
}