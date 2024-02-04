package io.lerk.lrkFM.entities

import android.os.Looper
import android.util.Log
import io.lerk.lrkFM.consts.FileType
import io.lerk.lrkFM.exceptions.BlockingStuffOnMainThreadException
import org.apache.commons.compress.archivers.ArchiveEntry
import org.apache.commons.compress.archivers.ArchiveException
import org.apache.commons.compress.archivers.ArchiveStreamFactory
import org.apache.commons.compress.archivers.sevenz.SevenZArchiveEntry
import org.apache.commons.compress.archivers.sevenz.SevenZFile
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.util.Objects

/**
 * @author Lukas Fülling (lukas@k40s.net)
 */
class FMArchive(f: File?) : FMFile(f!!) {
    private var contents: HashMap<String, ArrayList<FMFile>>? = null

    /**
     * Constructor.
     */
    init {
        if (!this.isArchive) {
            contents = null
            throw ClassCastException("Error creating " + f!!.name + "as archive:" + FMFile::class.java.name + " cannot be cast to " + FMArchive::class.java.name)
        } else {
            contents = calculateArchiveContents()
        }
    }

    /**
     * Gets archive content for path.
     *
     * @param path the relative path inside the archive
     * @return the contents
     */
    fun getContentForPath(path: String): ArrayList<FMFile> {
        var rPath: String
        rPath = if (!path.startsWith(ROOT_DIR)) {
            ROOT_DIR + path
        } else {
            try {
                path.split(name.toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()[1]
            } catch (e: ArrayIndexOutOfBoundsException) {
                ROOT_DIR
            }
        }
        if (rPath != ROOT_DIR && rPath.endsWith("/")) {
            rPath = rPath.substring(0, rPath.length - 1)
        }
        var pathContents = contents!![rPath]
        if (pathContents == null && rPath == ROOT_DIR) {
            pathContents = ArrayList()
            for (s in contents!!.keys) {
                val split =
                    s.split(File.separator.toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                if (split.size == 2) {
                    pathContents.add(FMFile(File(split[1])))
                }
            }
        }
        return ArrayList(Objects.requireNonNull(pathContents))
    }

    /**
     * Calculates the archive contents.
     *
     * @return a [HashMap] containing the relative path of the file in the archive and the file.
     */
    @Throws(BlockingStuffOnMainThreadException::class)
    private fun calculateArchiveContents(): HashMap<String, ArrayList<FMFile>> {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            throw BlockingStuffOnMainThreadException()
        }
        val res = HashMap<String, ArrayList<FMFile>>()
        when (fileType) {
            FileType.ARCHIVE_P7Z -> readP7ZFile(res)
            FileType.ARCHIVE_ZIP, FileType.ARCHIVE_RAR, FileType.ARCHIVE_TGZ, FileType.ARCHIVE_TAR -> readCCFile(
                res
            )

            FileType.UNKNOWN -> Log.w(TAG, "Unable to read archive!")
        }
        return res
    }

    /**
     * Read an archive file using commons compress.
     *
     * @param res the result HashMap
     */
    private fun readCCFile(res: HashMap<String, ArrayList<FMFile>>) {
        try {
            FileInputStream(file).use { `is` ->
                val ais = ArchiveStreamFactory().createArchiveInputStream(
                    fileType.extension, `is`
                )
                var entry: ArchiveEntry
                while (ais.nextEntry.also { entry = it } != null) {
                    val filePath: String? = entry.name
                    val outFile = FMArchiveFile(File(entry.name))
                    outFile.isDirectory = entry.isDirectory
                    outFile.absolutePath = entry.name
                    val fileParent = File(filePath!!).parent
                    val parent = ROOT_DIR + (fileParent ?: "")
                    var pathContents = res[parent]
                    if (pathContents == null) {
                        pathContents = ArrayList()
                    }
                    pathContents.add(outFile)
                    res[parent] = pathContents
                }
                ais.close()
            }
        } catch (e: IOException) {
            Log.e(TAG, "Error reading " + fileType.extension)
        } catch (e: ArchiveException) {
            Log.e(TAG, "Error reading " + fileType.extension)
        }
    }

    /**
     * Reads an archive file using p7zip.
     *
     * @param res the result HashMap
     */
    private fun readP7ZFile(res: HashMap<String, ArrayList<FMFile>>) {
        try {
            SevenZFile(file).use { sevenZFile ->
                var entry: SevenZArchiveEntry
                while (sevenZFile.nextEntry.also { entry = it } != null) {
                    val filePath: String? = entry.name
                    val outFile = FMArchiveFile(File(entry.name))
                    outFile.isDirectory = entry.isDirectory
                    outFile.absolutePath = entry.name
                    val fileParent = File(filePath!!).parent
                    val parent = ROOT_DIR + (fileParent ?: "")
                    var pathContents = res[parent]
                    if (pathContents == null) {
                        pathContents = ArrayList()
                    }
                    pathContents.add(outFile)
                    res[parent] = pathContents
                }
            }
        } catch (e: IOException) {
            Log.e(TAG, "Error reading " + fileType.extension)
        }
    }

    companion object {
        val TAG = FMArchive::class.java.canonicalName
        private const val ROOT_DIR = "/"
    }
}
