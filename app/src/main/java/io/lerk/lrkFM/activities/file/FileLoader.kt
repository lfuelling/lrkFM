package io.lerk.lrkFM.activities.file

import android.os.Looper
import android.util.Log
import io.lerk.lrkFM.entities.FMFile
import io.lerk.lrkFM.exceptions.BlockingStuffOnMainThreadException
import io.lerk.lrkFM.exceptions.EmptyDirectoryException
import io.lerk.lrkFM.exceptions.NoAccessException
import java.io.File

/**
 * This class handles file loading.
 *
 * @author Lukas Fülling (lukas@k40s.net)
 */
class FileLoader
/**
 * Constructor.
 *
 * @param location the location to load
 */(
    /**
     * The location.
     */
    private var location: String?
) : AbstractLoader() {
    /**
     * Calls [.FileLoader] with <pre>null</pre> as argument.
     *
     * @throws NoAccessException       if no access
     * @throws EmptyDirectoryException if no contents
     * @see .loadLocationFilesForPath
     */
    @Throws(
        NoAccessException::class,
        EmptyDirectoryException::class,
        BlockingStuffOnMainThreadException::class
    )
    public override fun loadLocationFiles(): ArrayList<FMFile> {
        return loadLocationFilesForPath(null)
    }

    /**
     * Loads the contents of a directory.
     *
     * @param parent the parent dir
     * @return the files and subdirectories
     * @throws NoAccessException       if no access
     * @throws EmptyDirectoryException if no contents
     */
    @Throws(
        NoAccessException::class,
        EmptyDirectoryException::class,
        BlockingStuffOnMainThreadException::class
    )
    override fun loadLocationFilesForPath(parent: String?): ArrayList<FMFile> {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            throw BlockingStuffOnMainThreadException()
        }
        if (parent != null) {
            location = parent
        }
        if (location == null || location!!.isEmpty()) {
            location = "/"
        }
        if (!location!!.startsWith("/")) {
            throw NoAccessException("Invalid path: $location")
        }
        val locationFile = File(location!!)
        if (locationFile.isDirectory) {
            if (locationFile.canRead()) {
                val result = ArrayList<FMFile>()
                val fileList = locationFile.listFiles()
                if (fileList != null) {
                    val asList = listOf(*fileList)
                    if (asList.isNotEmpty()) {
                        for (f in asList) {
                            Log.d(TAG, "Loading file: " + f.name)
                            val file = FMFile(f)
                            result.add(file)
                        }
                        Log.i(TAG, "Loaded " + result.size + " files")
                    } else {
                        Log.w(TAG, "Directory content is null!")
                        throw EmptyDirectoryException(location)
                    }
                    return result
                } else {
                    Log.w(TAG, "fileList is null")
                }
            } else {
                throw NoAccessException("Unable to read specified file!")
            }
        } else {
            val newParent = locationFile.parent
            Log.d(TAG, "Location '$location' not a directory")
            if (newParent != null) {
                return loadLocationFilesForPath(newParent)
            } else {
                Log.w(TAG, "Parent is null")
            }
        }
        Log.w(TAG, "Unable to load files")
        return ArrayList()
    }

    companion object {
        /**
         * Logtag.
         */
        private val TAG = FileLoader::class.java.canonicalName
    }
}
