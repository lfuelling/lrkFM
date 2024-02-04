package io.lerk.lrkFM.tasks.operation

import android.os.Build
import android.os.Looper
import android.util.Log
import io.lerk.lrkFM.entities.FMFile
import io.lerk.lrkFM.exceptions.BlockingStuffOnMainThreadException
import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.Arrays
import java.util.function.Consumer

/**
 * Utility class for file operations.
 *
 * @author Lukas Fülling (lukas@k40s.net)
 * @see CompatOperations
 */
internal object OperationUtil {
    private val TAG = OperationUtil::class.java.canonicalName
    @Throws(BlockingStuffOnMainThreadException::class)
    fun doCopy(f: FMFile?, destination: File?): Boolean {
        Log.i(TAG, "Starting copy...")
        return doCopyNoValidation(f, destination)
    }

    @Throws(BlockingStuffOnMainThreadException::class)
    fun doMove(f: FMFile?, destination: File): Boolean {
        Log.i(TAG, "Starting move...")
        return doMoveNoValidation(f, destination)
    }

    @Throws(BlockingStuffOnMainThreadException::class)
    fun doDeleteNoValidation(f: FMFile?): Boolean {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            throw BlockingStuffOnMainThreadException()
        }
        if (f!!.isDirectory) {
            try {
                for (file in f.file.listFiles()!!) {
                    doDeleteNoValidation(FMFile(file))
                }
            } catch (e: NullPointerException) {
                Log.e(TAG, "No permission for this file.", e)
            }
        }
        return f.file.exists() && f.file.delete()
    }

    @Throws(BlockingStuffOnMainThreadException::class)
    private fun doCopyNoValidation(f: FMFile?, d: File?): Boolean {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            throw BlockingStuffOnMainThreadException()
        }
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                Files.copy(f!!.file.toPath(), d!!.toPath(), StandardCopyOption.REPLACE_EXISTING)
            } else {
                if (f!!.file.isDirectory) {
                    CompatOperations.copyDirectory(f.file, d)
                } else {
                    CompatOperations.copyFile(f.file, d)
                }
            }
            true
        } catch (e: IOException) {
            Log.e(TAG, "Unable to copy file!", e)
            false
        }
    }

    @Throws(BlockingStuffOnMainThreadException::class)
    private fun doMoveNoValidation(f: FMFile?, d: File): Boolean {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            throw BlockingStuffOnMainThreadException()
        }
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                // Files.move() seems to be broken :c
                Files.copy(f!!.file.toPath(), d.toPath(), StandardCopyOption.REPLACE_EXISTING)
                if (!f.file.delete()) {
                    Log.w(TAG, "Unable to remove source file!")
                }
            } else {
                if (f!!.file.isDirectory) {
                    CompatOperations.moveDirectory(f.file, d)
                } else {
                    CompatOperations.moveFile(f.file, d)
                }
            }
            true
        } catch (e: IOException) {
            Log.e(TAG, "Unable to move file!", e)
            false
        }
    }

    fun getFullPathForRename(f: FMFile, newName: String): String {
        val path = arrayOf("")
        val pathSplit = ArrayList(
            listOf(
                *f.file.absolutePath.split(File.separator.toRegex()).dropLastWhile { it.isEmpty() }
                    .toTypedArray()))
        pathSplit.removeAt(pathSplit.size - 1)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            pathSplit.forEach(Consumer { s: String -> path[0] += s + File.separator })
        } else { // Ohooold version uuusers, uuupdate youuur phoooooone......
            for (s in pathSplit) {
                path[0] += s + File.separator
            }
        }
        return path[0] + newName
    }
}
