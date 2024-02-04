package io.lerk.lrkFM.tasks.operation

import android.util.Log
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException

/**
 * This class contains utility methods that will be removed once [android.os.Build.VERSION_CODES.N]
 * has a high enough user acceptance to make it the minimum required version.
 *
 * @author Lukas Fülling (lukas@k40s.net)
 * @see OperationUtil
 */
object CompatOperations {
    val TAG = CompatOperations::class.java.canonicalName
    @Throws(IOException::class)
    fun copyFile(file: File?, destination: File?) {
        var destination = destination
        if (destination!!.isDirectory) {
            destination = File(destination.absolutePath + "/" + file!!.name)
        }
        if (destination.parentFile.mkdirs()) {
            Log.d(TAG, "mkdirs was necessary!")
        }
        val src = FileInputStream(file).channel
        val dest = FileOutputStream(destination).channel
        dest.transferFrom(src, 0, src.size())
    }

    @Throws(IOException::class)
    fun moveFile(file: File?, destination: File?) {
        copyFile(file, destination)
        if (!file!!.delete()) { // delete file
            Log.w(TAG, "Unable to remove source!")
        }
    }

    @Throws(IOException::class)
    fun copyDirectory(file: File?, destination: File?) {
        for (f in file!!.listFiles()) {
            val dest = File(destination!!.absolutePath + "/" + f.name)
            if (f.isDirectory) {
                copyDirectory(f, dest)
            } else {
                copyFile(f, dest)
            }
        }
    }

    @Throws(IOException::class)
    fun moveDirectory(file: File?, destination: File) {
        for (f in file!!.listFiles()) {
            val dest = File(destination.absolutePath + "/" + f.name)
            if (f.isDirectory) {
                moveDirectory(f, dest)
            } else {
                moveFile(f, dest)
            }
        }
    }
}
