package io.lerk.lrkFM.adapter

import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.ImageView
import androidx.annotation.DrawableRes
import androidx.annotation.IdRes
import androidx.annotation.LayoutRes
import androidx.annotation.StringRes
import androidx.appcompat.content.res.AppCompatResources

import io.lerk.lrkFM.R
import io.lerk.lrkFM.activities.file.FileActivity
import io.lerk.lrkFM.entities.FMFile

/**
 * @author Lukas Fülling (lukas@k40s.net)
 */
abstract class BaseArrayAdapter internal constructor(
    context: Context,
    resource: Int,
    items: List<FMFile?>?
) : ArrayAdapter<FMFile?>(context, resource, items!!) {
    var activity: FileActivity? = null

    init {
        if (context is FileActivity) {
            activity = context
        } else {
            Log.d(TAG, "Context is no FileActivity: " + context.javaClass.name)
        }
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        return initUI(getItem(position))
    }

    protected abstract fun initUI(f: FMFile?): View
    protected abstract fun openFile(f: FMFile)

    /**
     * Sets file name in the text field of a dialog.
     *
     * @param alertDialog     the dialog
     * @param destinationName the id of the EditText
     * @param name            the name
     */
    fun presetNameForDialog(alertDialog: AlertDialog?, @IdRes destinationName: Int, name: String?) {
        val editText = alertDialog!!.findViewById<EditText>(destinationName)
        editText?.setText(name)
            ?: Log.w(
                TAG,
                "Unable to find view, can not set file title."
            )
    }

    /**
     * Adds the path of a file to a dialog.
     *
     * @param f           the file
     * @param alertDialog the dialog
     * @see .presetNameForDialog
     */
    @Suppress("unused")
    fun presetPathForDialog(f: FMFile, alertDialog: AlertDialog?) {
        presetNameForDialog(alertDialog, R.id.destinationPath, f.file.absolutePath)
    }

    /**
     * Utility method to create an AlertDialog.
     *
     * @param positiveBtnText  the text of the positive button
     * @param title            the title
     * @param icon             the icon
     * @param view             the content view
     * @param positiveCallBack the positive callback
     * @param negativeCallBack the negative callback
     * @return the dialog
     */
    fun getGenericFileOpDialog(
        @StringRes positiveBtnText: Int,
        @StringRes title: Int,
        @DrawableRes icon: Int,
        @LayoutRes view: Int,
        positiveCallBack: (AlertDialog?) -> Unit,
        negativeCallBack: (AlertDialog?) -> Unit
    ): AlertDialog {
        val dialog = AlertDialog.Builder(activity)
            .setView(view)
            .setTitle(title)
            .setCancelable(true).create()
        dialog.setButton(
            DialogInterface.BUTTON_POSITIVE,
            activity!!.getString(positiveBtnText)
        ) { _: DialogInterface?, _: Int -> positiveCallBack(dialog) }
        dialog.setButton(
            DialogInterface.BUTTON_NEGATIVE,
            activity!!.getString(R.string.cancel)
        ) { _: DialogInterface?, _: Int -> negativeCallBack(dialog) }
        dialog.setOnShowListener { _: DialogInterface? ->
            val dialogIcon = dialog.findViewById<ImageView>(R.id.dialogIcon)
            dialogIcon.setImageDrawable(AppCompatResources.getDrawable(context, icon))
            val inputField: EditText?
            if (view == R.layout.layout_name_prompt) {
                inputField = dialog.findViewById(R.id.destinationName)
                if (inputField != null) {
                    val name = activity!!.getTitleFromPath(activity!!.currentDirectory)
                    inputField.setText(name)
                    Log.d(TAG, "Destination set to: $name")
                } else {
                    Log.w(TAG, "Unable to preset current name, text field is null!")
                }
            } else if (view == R.layout.layout_path_prompt) {
                inputField = dialog.findViewById(R.id.destinationPath)
                if (inputField != null) {
                    val directory = activity?.currentDirectory
                    inputField.setText(directory)
                    Log.d(TAG, "Destination set to: $directory")
                } else {
                    Log.w(TAG, "Unable to preset current path, text field is null!")
                }
            }
        }
        return dialog
    }

    companion object {
        val TAG = BaseArrayAdapter::class.java.canonicalName
    }
}
