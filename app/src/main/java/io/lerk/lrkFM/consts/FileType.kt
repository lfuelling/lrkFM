package io.lerk.lrkFM.consts

import android.util.Log
import androidx.annotation.StringRes

import io.lerk.lrkFM.R
import io.lerk.lrkFM.entities.FMFile

/**
 * File types.
 *
 * @author Lukas Fülling (lukas@k40s.net)
 */
enum class FileType(@param:StringRes val i18n: Int, val extension: String?) {
    // known archives
    ARCHIVE_RAR(R.string.type_rar, "rar"),
    ARCHIVE_ZIP(R.string.type_zip, "zip"),
    ARCHIVE_TAR(R.string.type_tar, "tar"),
    ARCHIVE_P7Z(R.string.type_p7z, "7z"),
    ARCHIVE_TGZ(R.string.type_tgz, "gz"),

    //
    UNKNOWN(R.string.type_unknown, "");

    var fileExtension: String? = null
    private var handler: (FMFile) -> Unit = { Log.i(TAG, "No Handler for $extension") }
    fun handle(f: FMFile?) {
        handler(f!!)
    }

    fun newHandler(h: (FMFile) -> Unit): FileType {
        handler = h
        return this
    }

    /**
     * Setter for fileExtension that returns this.
     * @param fileExtension [.fileExtension]
     * @return this
     */
    fun withExtension(fileExtension: String?): FileType {
        this.fileExtension = fileExtension
        return this
    }

    companion object {
        val TAG = FileType::class.java.name
    }
}
