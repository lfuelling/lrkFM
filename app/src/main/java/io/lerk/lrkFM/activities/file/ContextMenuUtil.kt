package io.lerk.lrkFM.activities.file

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.view.ContextMenu
import android.view.MenuItem
import android.widget.EditText
import android.widget.Toast
import io.lerk.lrkFM.Pref
import io.lerk.lrkFM.R
import io.lerk.lrkFM.adapter.ArchiveArrayAdapter
import io.lerk.lrkFM.adapter.BaseArrayAdapter
import io.lerk.lrkFM.consts.Operation
import io.lerk.lrkFM.consts.PreferenceEntity
import io.lerk.lrkFM.entities.FMFile
import io.lerk.lrkFM.tasks.archive.ArchiveCreationTask
import io.lerk.lrkFM.tasks.archive.ArchiveParentFinderTask
import io.lerk.lrkFM.tasks.operation.FileDeleteTask
import io.lerk.lrkFM.tasks.operation.FileMoveTask
import io.lerk.lrkFM.tasks.operation.FileOperationTask
import java.io.File
import java.util.Objects

/**
 * @author Lukas Fülling (lukas@k40s.net)
 */
class ContextMenuUtil(
    private val activity: FileActivity,
    private val arrayAdapter: BaseArrayAdapter
) {
    /**
     * Adds menu buttons to context menu.
     *
     * @param f        the file
     * @param fileName the file name for the title
     * @param menu     the context menu to fill
     */
    fun initializeContextMenu(f: FMFile, fileName: String?, menu: ContextMenu) {
        menu.setHeaderTitle(fileName)
        addCopyPathToMenu(f, menu)
        if (arrayAdapter !is ArchiveArrayAdapter) { // those actions are not available while inside archives
            addExtractToMenu(f, menu)
            addDeleteToMenu(f, menu)
            addShareToMenu(f, menu)
            addCreateZipToMenu(f, menu)
            addExploreToMenu(f, menu)
            addOpenWithToMenu(f, menu)
            addCopyToMenu(f, menu)
            addMoveToMenu(f, menu)
            addRenameToMenu(f, menu)
        }
    }

    /**
     * Adds delete button to menu.
     *
     * @param f    the file
     * @param menu the menu
     */
    private fun addDeleteToMenu(f: FMFile, menu: ContextMenu) {
        menu.add(0, ID_DELETE, 0, activity.getString(R.string.delete))
            .setOnMenuItemClickListener { _: MenuItem? ->
                AlertDialog.Builder(activity)
                    .setTitle(R.string.delete)
                    .setMessage(activity.getString(R.string.warn_delete_msg) + FileActivity.Companion.WHITESPACE + f.name + "?")
                    .setNegativeButton(R.string.cancel) { dialogInterface: DialogInterface, i: Int -> dialogInterface.cancel() }
                    .setPositiveButton(R.string.yes) { dialogInterface: DialogInterface, _: Int ->
                        FileDeleteTask(activity, { b: Boolean? ->
                            if (!b!!) {
                                Toast.makeText(
                                    activity,
                                    R.string.err_deleting_element,
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }, f).execute()
                        activity.reloadCurrentDirectory()
                        dialogInterface.dismiss()
                    }
                    .show()
                true
            }
    }

    /**
     * Adds extract to menu.
     *
     * @param file file file
     * @param menu menu
     */
    private fun addExtractToMenu(file: FMFile, menu: ContextMenu) {
        ArchiveParentFinderTask(
            file
        ) { parentFinder: ArchiveParentFinder ->
            menu.add(0, ID_EXTRACT, 0, activity.getString(R.string.extract))
                .setOnMenuItemClickListener { _: MenuItem? ->
                    var archiveToExtract: FMFile? = file
                    if (!file.isArchive && parentFinder.isArchive) {
                        archiveToExtract = parentFinder.archiveFile
                    }
                    activity.addFileToOpContext(Operation.EXTRACT, archiveToExtract)
                    if (Pref<Boolean>(PreferenceEntity.USE_CONTEXT_FOR_OPS_TOAST).value == true) {
                        Toast.makeText(
                            activity,
                            activity.getString(R.string.file_added_to_context) + (archiveToExtract?.name
                                ?: "<ERROR>"),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    if (Pref<Boolean>(PreferenceEntity.ALWAYS_EXTRACT_IN_CURRENT_DIR).value == true) {
                        activity.finishFileOperation()
                    } else {
                        AlertDialog.Builder(activity)
                            .setView(R.layout.layout_extract_now_prompt)
                            .setPositiveButton(R.string.yes) { _: DialogInterface?, _: Int -> activity.finishFileOperation() }
                            .setNeutralButton(R.string.yes_and_remember) { _: DialogInterface?, _: Int ->
                                Pref<Boolean>(PreferenceEntity.ALWAYS_EXTRACT_IN_CURRENT_DIR).value = true
                                activity.finishFileOperation()
                            }
                            .setNegativeButton(R.string.no) { _: DialogInterface?, _: Int ->
                                Log.d(
                                    TAG,
                                    "noop"
                                )
                            }
                            .create().show()
                    }
                    activity.reloadCurrentDirectory()
                    true
                }
                .setVisible(file.isArchive || parentFinder.isArchive)
        }.execute()
    }

    /**
     * Adds share to menu.
     *
     * @param f    the file
     * @param menu the menu
     */
    private fun addShareToMenu(f: FMFile, menu: ContextMenu) {
        menu.add(0, ID_SHARE, 0, activity.getString(R.string.share))
            .setOnMenuItemClickListener { _: MenuItem? ->
                val intent = Intent(Intent.ACTION_SEND)
                intent.setType(FMFile.Companion.getMimeTypeFromFile(f))
                intent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(f.file))
                activity.startActivity(
                    Intent.createChooser(
                        intent,
                        activity.getString(R.string.share_file)
                    )
                )
                true
            }
    }

    /**
     * Adds rename to menu.
     *
     * @param f    the file
     * @param menu the menu
     */
    private fun addRenameToMenu(f: FMFile, menu: ContextMenu) {
        menu.add(0, ID_RENAME, 0, activity.getString(R.string.rename))
            .setOnMenuItemClickListener { _: MenuItem? ->
                val alertDialog = arrayAdapter.getGenericFileOpDialog(
                    R.string.rename,
                    R.string.rename,
                    R.drawable.ic_mode_edit_black_24dp,
                    R.layout.layout_name_prompt,
                    { d: AlertDialog? ->
                        FileMoveTask(
                            activity, { b: Boolean? -> }, f, d
                        )
                    }
                ) { _: AlertDialog? -> Log.i(TAG, "Cancelled.") }
                alertDialog.setOnShowListener { d: DialogInterface? ->
                    arrayAdapter.presetNameForDialog(
                        alertDialog,
                        R.id.destinationName,
                        f.name
                    )
                }
                alertDialog.show()
                activity.reloadCurrentDirectory()
                true
            }
    }

    /**
     * Adds move to menu.
     *
     * @param f    the file
     * @param menu the menu
     */
    private fun addMoveToMenu(f: FMFile, menu: ContextMenu) {
        menu.add(0, ID_MOVE, 0, activity.getString(R.string.move))
            .setOnMenuItemClickListener { _: MenuItem? ->
                activity.addFileToOpContext(Operation.MOVE, f)
                if (Pref<Boolean>(PreferenceEntity.USE_CONTEXT_FOR_OPS_TOAST).value == true) {
                    Toast.makeText(
                        activity,
                        activity.getString(R.string.file_added_to_context) + f.name,
                        Toast.LENGTH_SHORT
                    ).show()
                }
                activity.reloadCurrentDirectory()
                true
            }
    }

    /**
     * Adds copy to menu.
     *
     * @param f    the file
     * @param menu the menu
     */
    private fun addCopyToMenu(f: FMFile, menu: ContextMenu) {
        menu.add(0, ID_COPY, 0, activity.getString(R.string.copy))
            .setOnMenuItemClickListener { _: MenuItem? ->
                activity.addFileToOpContext(Operation.COPY, f)
                if (Pref<Boolean>(PreferenceEntity.USE_CONTEXT_FOR_OPS_TOAST).value == true) {
                    Toast.makeText(
                        activity,
                        activity.getString(R.string.file_added_to_context) + f.name,
                        Toast.LENGTH_SHORT
                    ).show()
                }
                activity.reloadCurrentDirectory()
                true
            }
    }

    /**
     * Adds "copy path" to menu.
     *
     * @param f    the file
     * @param menu the menu
     */
    private fun addCopyPathToMenu(f: FMFile, menu: ContextMenu) {
        menu.add(0, ID_COPY_PATH, 0, R.string.copy_path)
            .setOnMenuItemClickListener { _: MenuItem? ->
                (Objects.requireNonNull(activity.getSystemService(Context.CLIPBOARD_SERVICE)) as ClipboardManager).setPrimaryClip(
                    ClipData.newPlainText(
                        activity.getString(R.string.file_location), f.file.absolutePath
                    )
                )
                true
            }
    }

    /**
     * Adds "Create zip" and "add to zip" to menu.
     *
     * @param f    the file
     * @param menu the menu
     */
    private fun addCreateZipToMenu(f: FMFile, menu: ContextMenu) {
        val fileOpContext = activity.fileOpContext
        val zipFileReady =
            fileOpContext.first == Operation.CREATE_ZIP && fileOpContext.second.size >= 1
        menu.add(
            0,
            ID_ADD_TO_ZIP,
            0,
            if (zipFileReady) activity.getString(R.string.add_to_zip) else activity.getString(R.string.new_zip_file)
        ).setOnMenuItemClickListener { _: MenuItem? ->
            activity.addFileToOpContext(Operation.CREATE_ZIP, f)
            if (Pref<Boolean>(PreferenceEntity.USE_CONTEXT_FOR_OPS_TOAST).value == true) {
                Toast.makeText(
                    activity,
                    activity.getString(R.string.file_added_to_context) + f.name,
                    Toast.LENGTH_SHORT
                ).show()
            }
            activity.reloadCurrentDirectory()
            true
        }
        if (zipFileReady) {
            menu.add(0, ID_CREATE_ZIP, 0, activity.getString(R.string.create_zip_file))
                .setOnMenuItemClickListener { _: MenuItem? ->
                    if (fileOpContext.first == Operation.CREATE_ZIP) {
                        val alertDialog = arrayAdapter.getGenericFileOpDialog(
                            R.string.create_zip_file,
                            R.string.op_destination,
                            R.drawable.ic_archive_black_24dp,
                            R.layout.layout_name_prompt,
                            { d: AlertDialog? ->
                                var tmpName: String?
                                val editText = d?.findViewById<EditText>(R.id.destinationName)
                                tmpName = editText?.text.toString()
                                if (tmpName.isEmpty() || tmpName.startsWith("/")) {
                                    Toast.makeText(
                                        activity,
                                        R.string.err_invalid_input_zip,
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    tmpName = null
                                } else if (!tmpName.endsWith(".zip")) {
                                    tmpName = "$tmpName.zip"
                                }
                                val destination = File(activity.currentDirectory + "/" + tmpName)
                                if (destination.exists()) {
                                    val builder: AlertDialog.Builder =
                                        FileOperationTask.Companion.getFileExistsDialogBuilder(
                                            activity
                                        )
                                    builder.setOnDismissListener { _: DialogInterface? ->
                                        ArchiveCreationTask(
                                            activity, fileOpContext.second, destination
                                        ) { success: Boolean? ->
                                            activity.clearFileOpCache()
                                            activity.reloadCurrentDirectory()
                                            if (!success!!) {
                                                Toast.makeText(
                                                    activity,
                                                    R.string.unable_to_create_zip_file,
                                                    Toast.LENGTH_LONG
                                                ).show()
                                            }
                                        }.execute()
                                    }.show()
                                } else {
                                    ArchiveCreationTask(
                                        activity,
                                        fileOpContext.second,
                                        destination
                                    ) { success: Boolean? ->
                                        activity.clearFileOpCache()
                                        activity.reloadCurrentDirectory()
                                        if (!success!!) {
                                            Toast.makeText(
                                                activity,
                                                R.string.unable_to_create_zip_file,
                                                Toast.LENGTH_LONG
                                            ).show()
                                        }
                                    }.execute()
                                }
                            }
                        ) { _: AlertDialog? -> Log.i(TAG, "Cancelled.") }
                        alertDialog.show()
                        activity.reloadCurrentDirectory()
                        return@setOnMenuItemClickListener true
                    } else {
                        Log.e(
                            TAG,
                            "Illegal operation mode. Expected " + Operation.CREATE_ZIP + " but was: " + fileOpContext.first
                        )
                    }
                    false
                }.setVisible(fileOpContext.first == Operation.CREATE_ZIP)
        }
    }

    private fun addExploreToMenu(f: FMFile, menu: ContextMenu) {
        menu.add(0, ID_EXPLORE, 0, activity.getString(R.string.explore))
            .setOnMenuItemClickListener { _: MenuItem? ->
                activity.loadPath(f.absolutePath)
                true
            }.setVisible(f.isArchive)
    }

    @SuppressLint("QueryPermissionsNeeded") // apparently my code should work regardless
    private fun addOpenWithToMenu(f: FMFile, menu: ContextMenu) {
        menu.add(0, ID_OPEN_WITH, 0, activity.getString(R.string.open_with))
            .setOnMenuItemClickListener { _: MenuItem? ->
                val i = Intent(Intent.ACTION_VIEW)
                val mimeType: String = FMFile.Companion.getMimeTypeFromFile(f) ?: "application/octet-stream"
                i.setDataAndType(Uri.fromFile(f.file), mimeType)
                i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                val chooser =
                    Intent.createChooser(i, activity.getString(R.string.choose_application))
                if (i.resolveActivity(activity.packageManager) != null) {
                    if (activity.packageManager.queryIntentActivities(i, 0).size == 1) {
                        Toast.makeText(
                            activity,
                            R.string.only_one_app_to_handle_file,
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    activity.startActivity(chooser)
                } else {
                    Toast.makeText(activity, R.string.no_app_to_handle_file, Toast.LENGTH_SHORT)
                        .show()
                }
                true
            }.setVisible(!f.isDirectory)
    }

    companion object {
        private const val ID_COPY = 0
        private const val ID_MOVE = 1
        private const val ID_RENAME = 2
        private const val ID_DELETE = 3
        private const val ID_EXTRACT = 4
        private const val ID_SHARE = 5
        private const val ID_COPY_PATH = 6
        private const val ID_ADD_TO_ZIP = 7
        private const val ID_CREATE_ZIP = 8
        private const val ID_EXPLORE = 9
        private const val ID_OPEN_WITH = 10
        private val TAG = ContextMenuUtil::class.java.canonicalName
    }
}
