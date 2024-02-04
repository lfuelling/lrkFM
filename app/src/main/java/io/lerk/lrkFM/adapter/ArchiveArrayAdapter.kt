package io.lerk.lrkFM.adapter

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import android.view.ContextMenu
import android.view.ContextMenu.ContextMenuInfo
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.content.res.AppCompatResources
import io.lerk.lrkFM.Pref
import io.lerk.lrkFM.R
import io.lerk.lrkFM.activities.file.ContextMenuUtil
import io.lerk.lrkFM.consts.PreferenceEntity
import io.lerk.lrkFM.entities.FMArchive
import io.lerk.lrkFM.entities.FMFile

/**
 * @author Lukas Fülling (lukas@k40s.net)
 */
class ArchiveArrayAdapter(
    context: Context,
    resource: Int,
    items: List<FMFile?>?,
    private val archive: FMArchive?
) : BaseArrayAdapter(context, resource, items) {
    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        return initUI(getItem(position))
    }

    /**
     * Opens a file using the default app.
     *
     * @param f the file
     */
    override fun openFile(f: FMFile) {
        if (f.isDirectory) {
            activity!!.loadArchivePath(f.absolutePath, archive)
        } else {
            Toast.makeText(activity, R.string.only_browsing_in_archives, Toast.LENGTH_LONG).show()
        }
    }

    /**
     * Initializes the UI for each file.
     *
     * @param f the file
     * @return the initialized UI
     */
    override fun initUI(f: FMFile?): View {
        assert(activity != null)
        @SuppressLint("InflateParams") val v =
            LayoutInflater.from(activity).inflate(R.layout.layout_archive_file, null) // works.
        if (f != null) {
            val fileNameView = v.findViewById<TextView>(R.id.fileTitle)
            val fileImage = v.findViewById<ImageView>(R.id.fileIcon)
            val fileName = f.name
            if (fileNameView != null) {
                val maxLength = Pref<String>(PreferenceEntity.FILENAME_LENGTH).value?.toInt() ?: 27
                if (fileName.length >= maxLength) {
                    @SuppressLint("SetTextI18n") val output =
                        fileName.substring(0, maxLength - 3) + "..."
                    fileNameView.text = output //shorten long names
                } else {
                    fileNameView.text = fileName
                }
            } else {
                Log.e(TAG, "TextView fileName is null!")
            }
            if (fileImage != null) {
                if (!f.isDirectory) {
                    if (f.isArchive) {
                        fileImage.setImageDrawable(AppCompatResources.getDrawable(context, R.drawable.ic_perm_media_black_24dp))
                    } else {
                        fileImage.setImageDrawable(AppCompatResources.getDrawable(context, R.drawable.ic_insert_drive_file_black_24dp))
                    }
                }
            } else {
                Log.e(TAG, "ImageView fileImage is null!")
            }
            v.setOnClickListener { _: View? -> openFile(f) }
            v.setOnCreateContextMenuListener { menu: ContextMenu, _: View?, _: ContextMenuInfo? ->
                ContextMenuUtil(
                    activity!!, this
                ).initializeContextMenu(f, fileName, menu)
            }
            val contextButton = v.findViewById<ImageButton>(R.id.contextMenuButton)
            contextButton.setOnClickListener { _: View? ->
                activity?.fileListView?.showContextMenuForChild(
                    v
                )
            }
            for (contextFile in activity!!.fileOpContext.second) {
                if (contextFile?.file?.absolutePath == f.file.absolutePath) {
                    v.setBackgroundColor(activity!!.getColor(R.color.default_primary))
                }
            }
        } else {
            Log.e(TAG, "File is null!")
        }
        return v
    }

    companion object {
        private val TAG = ArchiveArrayAdapter::class.java.canonicalName
    }
}
