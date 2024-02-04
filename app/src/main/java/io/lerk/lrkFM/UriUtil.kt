package io.lerk.lrkFM

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

object UriUtil {
    private val TAG = UriUtil::class.java.canonicalName

    /**
     * Returns the file name behind a content:// URI.
     *
     * @param ctx any valid context
     * @param uri the uri
     * @return filename of the file behind the uri
     * @throws Exception in case the URI is invalid
     */
    @Throws(Exception::class)
    fun getFilenameForUri(ctx: Context, uri: Uri): String? {
        val scheme = uri.scheme
        if (scheme == null || scheme != ContentResolver.SCHEME_CONTENT) {
            throw Exception("Invalid scheme!")
        }
        var result: String? = null
        val columns = arrayOf(MediaStore.Images.Media.DATA, OpenableColumns.DISPLAY_NAME)
        ctx.contentResolver
            .query(uri, columns, null, null, null).use { cursor ->
                if (cursor != null && cursor.moveToFirst()) {
                    var idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (idx != -1) {
                        result = cursor.getString(idx)
                    }
                    if (idx == -1 || result == null) {
                        idx = cursor.getColumnIndex(MediaStore.Images.Media.DATA)
                        if (idx != -1) {
                            result = cursor.getString(idx)
                        }
                    }
                }
            }
        return result
    }

    @Throws(IndexOutOfBoundsException::class)
    fun getFilenameWithoutExtension(filename: String): String {
        return filename.substring(0, filename.lastIndexOf("."))
    }

    @Throws(Exception::class)
    fun getFileExtension(filename: String): String {
        val i = filename.lastIndexOf(".")
        val extension = filename.substring(i + 1)
        if (i == -1 || !extension.matches("\\w+".toRegex())) {
            throw Exception("File has no extension!")
        }
        return extension
    }

    @Throws(Exception::class)
    fun createTempFileFromUri(ctx: Context, uri: Uri): File {
        ctx.contentResolver.openInputStream(uri).use { inputStream ->
            val fullFileName = getFilenameForUri(ctx, uri)
            if (fullFileName != null) {
                val fileName = getFilenameWithoutExtension(fullFileName)
                val fileExtension = "." + getFileExtension(fullFileName)
                val tempFile = File.createTempFile(fileName, fileExtension, ctx.cacheDir)
                if (!tempFile.exists()) {
                    throw Exception("Unable to create temp file!")
                }
                Log.d(TAG, "Temp file created: '" + tempFile.absolutePath + "'!")
                if (inputStream != null) {
                    val availableBytes = inputStream.available()
                    val buffer = ByteArray(availableBytes)
                    val readBytes = inputStream.read(buffer)
                    if (availableBytes == readBytes) {
                        try {
                            FileOutputStream(tempFile).use { outStream ->
                                outStream.write(buffer)
                                Log.d(TAG, "File written 'successfully'!")
                                outStream.close()
                                inputStream.close()
                                return tempFile
                            }
                        } catch (e: IOException) {
                            throw Exception("Unable to write temp file!", e)
                        }
                    } else {
                        throw Exception("Invalid byte count! Read: $readBytes, Available: $availableBytes!")
                    }
                } else {
                    throw Exception("Unable to read stream from URI!")
                }
            } else {
                throw Exception("Unable to retrieve filename!")
            }
        }
    }
}
