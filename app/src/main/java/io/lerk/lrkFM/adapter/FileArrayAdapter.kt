package io.lerk.lrkFM.adapter

import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Vibrator
import android.view.ContextMenu
import android.view.ContextMenu.ContextMenuInfo
import android.view.LayoutInflater
import android.view.View
import android.widget.CheckBox
import android.widget.CompoundButton
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.content.ContextCompat
import io.lerk.lrkFM.Pref
import io.lerk.lrkFM.R
import io.lerk.lrkFM.activities.file.ContextMenuUtil
import io.lerk.lrkFM.consts.PreferenceEntity
import io.lerk.lrkFM.entities.FMArchive
import io.lerk.lrkFM.entities.FMFile
import java.text.SimpleDateFormat
import kotlin.math.floor
import kotlin.math.ln
import kotlin.math.pow

/**
 * Heavily abused ArrayAdapter that also adds menus and listeners.
 *
 * @author Lukas Fülling (lukas@k40s.net)
 */
class FileArrayAdapter(context: Context, resource: Int, items: List<FMFile?>?) :
    BaseArrayAdapter(context, resource, items) {
    /**
     * Opens a file using the default app.
     *
     * @param f the file
     */
    override fun openFile(f: FMFile) {
        if (Pref<Boolean>(PreferenceEntity.ZIPS_EXPLORABLE).value == true
            && f.isArchive && f !is FMArchive) {
            activity!!.loadPath(f.absolutePath)
        } else {
            val i = Intent(Intent.ACTION_VIEW)
            i.setDataAndType(Uri.fromFile(f.file), FMFile.Companion.getMimeTypeFromFile(f))
            i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            try {
                context.startActivity(i)
            } catch (e: ActivityNotFoundException) {
                Toast.makeText(context, R.string.no_app_to_handle_file, Toast.LENGTH_SHORT).show()
            }
        }
    }

    /**
     * Gets the size of a file as formatted string.
     *
     * @param f the file
     * @return the size, formatted.
     */
    private fun getSizeFormatted(f: FMFile): String {
        val units = arrayOf("B", "KiB", "MiB", "GiB", "TiB", "PiB")
        val length = f.file.length()
        if (length == 0L) {
            return "0"
        }
        val number = floor(ln(length.toDouble()) / ln(1024.0))
        val pow = 1024.0.pow(floor(number))
        val d = length / pow
        val formattedString = d.toString().substring(0, d.toString().indexOf(".") + 2)
        return formattedString + ' ' + units[number.toInt()]
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
            LayoutInflater.from(activity).inflate(R.layout.layout_file, null) // works.
        if (f != null) {
            val fileNameView = v.findViewById<TextView>(R.id.fileTitle)
            val filePermissions = v.findViewById<TextView>(R.id.filePermissions)
            val fileDate = v.findViewById<TextView>(R.id.fileDate)
            val fileSize = v.findViewById<TextView>(R.id.fileSize)
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
            }
            if (filePermissions != null) {
                filePermissions.text = f.permissions
            }
            if (fileDate != null) {
                fileDate.text = SimpleDateFormat.getDateTimeInstance().format(f.lastModified)
            }
            if (fileSize != null) {
                if (f.isDirectory) {
                    fileSize.visibility = View.GONE
                } else {
                    fileSize.text = getSizeFormatted(f)
                }
            }
            if (fileImage != null) {
                if (!f.isDirectory) {
                    if (f.isArchive) {
                        fileImage.setImageDrawable(AppCompatResources.getDrawable(context, R.drawable.ic_perm_media_black_24dp))
                    } else {
                        fileImage.setImageDrawable(AppCompatResources.getDrawable(context, R.drawable.ic_insert_drive_file_black_24dp))
                    }
                }
            }
            if (f.isDirectory || f.isExplorableArchive && Pref<Boolean>(PreferenceEntity.ZIPS_EXPLORABLE).value == true) {
                v.setOnClickListener { _: View? -> activity!!.loadPath(f.file.absolutePath) }
            } else {
                v.setOnClickListener { _: View? -> openFile(f) }
            }
            v.setOnCreateContextMenuListener { menu: ContextMenu, _: View?, _: ContextMenuInfo? ->
                ContextMenuUtil(
                    activity!!, this
                ).initializeContextMenu(f, fileName, menu)
            }
            val contextButton = v.findViewById<ImageButton>(R.id.contextMenuButton)
            contextButton.setOnClickListener { _: View? ->
                (activity!!.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator).vibrate(8)
                activity?.fileListView?.showContextMenuForChild(v)
            }
            val checkBox = v.findViewById<CheckBox>(R.id.fileCheckBox)
            checkBox.setOnCheckedChangeListener { _: CompoundButton?, isChecked: Boolean ->
                if (isChecked && !isInContext(f)) {
                    activity?.fileOpContext?.second?.add(f)
                } else if (!isChecked && isInContext(f)) {
                    removeFromContext(f)
                }
            }
            if (Pref<Boolean>(PreferenceEntity.SHOW_MENU_BUTTON_INSTEAD_OF_CHECKBOX).value == true) {
                checkBox.visibility = View.INVISIBLE
                contextButton.visibility = View.VISIBLE
            } else {
                checkBox.visibility = View.VISIBLE
                contextButton.visibility = View.GONE
            }
            for (contextFile in activity!!.fileOpContext.second) {
                if (contextFile?.file?.absolutePath == f.file.absolutePath) {
                    v.setBackgroundColor(ContextCompat.getColor(context, R.color.primary_light))
                    checkBox.isChecked = true
                } else {
                    checkBox.isChecked = false
                }
            }
        }
        return v
    }

    private fun isInContext(f: FMFile): Boolean {
        var res = false
        val iterator: Iterator<FMFile?> = activity!!.fileOpContext.second.iterator()
        while (iterator.hasNext()) {
            val contextFile = iterator.next()
            if (contextFile?.file?.absolutePath == f.file.absolutePath) {
                res = true
            }
        }
        return res
    }

    private fun removeFromContext(f: FMFile) {
        val iterator: Iterator<FMFile?> = activity!!.fileOpContext.second.iterator()
        while (iterator.hasNext()) {
            val contextFile = iterator.next()
            if (contextFile?.file?.absolutePath == f.file.absolutePath) {
                activity!!.fileOpContext.second.remove(contextFile)
            }
        }
    }
}
