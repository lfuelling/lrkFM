package io.lerk.lrkFM.entities

import android.util.Log
import android.webkit.MimeTypeMap
import io.lerk.lrkFM.Pref
import io.lerk.lrkFM.consts.FileType
import io.lerk.lrkFM.consts.PreferenceEntity
import java.io.File
import java.util.Date

/**
 * File object.
 *
 * @author Lukas Fülling (lukas@k40s.net)
 */
open class FMFile(var file: File) : Comparable<FMFile> {
    var name: String = file.name
    val permissions: String = (if (file.isDirectory) "d" else "-") +
            (if (file.canRead()) "r" else "-") +
            (if (file.canWrite()) "w" else "-") + if (file.canExecute()) "x" else "-"
    val lastModified: Date = Date(file.lastModified())
    var isDirectory: Boolean = file.isDirectory
    open var absolutePath: String = file.absolutePath

    private val extension: String
        /**
         * Gets a file's extension.
         *
         * @return the file's extension
         */
        get() = try {
            name.substring(name.indexOf('.') + 1)
        } catch (e: StringIndexOutOfBoundsException) {
            Log.e(TAG, "Unable to get file extension.", e)
            ""
        } catch (e: ArrayIndexOutOfBoundsException) {
            Log.e(TAG, "Unable to get file extension.", e)
            ""
        }
    val fileType: FileType
        get() {
            val extension = this.extension
            for (fileType in FileType.entries) {
                if (fileType.extension == extension) {
                    return fileType.withExtension(extension)
                }
            }
            return FileType.UNKNOWN.withExtension(extension)
        }
    val isArchive: Boolean
        get() {
            val type = fileType
            return type == FileType.ARCHIVE_ZIP || type == FileType.ARCHIVE_RAR || type == FileType.ARCHIVE_TAR || type == FileType.ARCHIVE_TGZ || type == FileType.ARCHIVE_P7Z
        }
    val isExplorableArchive: Boolean
        get() = fileType == FileType.ARCHIVE_ZIP

    override fun compareTo(other: FMFile): Int {
        return when (Pref<String>(PreferenceEntity.SORT_FILES_BY).value) {
            "datea" -> lastModified.time.compareTo(other.lastModified.time)
            "dated" -> other.lastModified.time.compareTo(lastModified.time)
            "named" -> other.name.compareTo(name, ignoreCase = true)
            "namea" -> name.compareTo(other.name, ignoreCase = true)
            else -> name.compareTo(other.name, ignoreCase = true)
        }
    }

    companion object {
        private val TAG = FMFile::class.java.canonicalName
        fun getMimeTypeFromFile(f: FMFile): String? {
            val fileExtension: String = try {
                val split =
                    f.name.split("\\.".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                split[split.size - 1]
            } catch (e: IndexOutOfBoundsException) {
                ""
            }
            return MimeTypeMap.getSingleton().getMimeTypeFromExtension(fileExtension)
        }
    }
}
