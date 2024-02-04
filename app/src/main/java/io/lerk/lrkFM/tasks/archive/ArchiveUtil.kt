package io.lerk.lrkFM.tasks.archive

import android.os.Looper
import android.util.Log
import com.github.junrar.Junrar
import com.github.junrar.exception.RarException

import io.lerk.lrkFM.consts.FileType
import io.lerk.lrkFM.entities.FMFile
import io.lerk.lrkFM.exceptions.BlockingStuffOnMainThreadException
import org.apache.commons.compress.archivers.ArchiveEntry
import org.apache.commons.compress.archivers.ArchiveException
import org.apache.commons.compress.archivers.ArchiveStreamFactory
import org.apache.commons.compress.archivers.sevenz.SevenZArchiveEntry
import org.apache.commons.compress.archivers.sevenz.SevenZFile
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicBoolean
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * Archive utility class.
 *
 * @author Lukas Fülling (lukas@k40s.net)
 */
internal class ArchiveUtil {
    @Throws(BlockingStuffOnMainThreadException::class)
    fun doExtractArchive(destination: String?, f: FMFile?): Boolean {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            throw BlockingStuffOnMainThreadException()
        }
        val result = AtomicBoolean(false)
        val fileType = f?.fileType
        if (fileType != FileType.ARCHIVE_P7Z) {
            if (fileType == FileType.ARCHIVE_RAR) {
                fileType.newHandler { fmFile: FMFile ->
                    val archiveFile = fmFile.file
                    val destinationFile = File(destination!!)
                    if (!destinationFile.exists()) {
                        if (destinationFile.mkdirs()) {
                            Log.i(TAG, "Folder created: '" + destinationFile.absolutePath + "'")
                        } else {
                            Log.e(
                                TAG,
                                "Unable to create folder: '" + destinationFile.absolutePath + "'"
                            )
                        }
                    }
                    try {
                        Junrar.extract(archiveFile, destinationFile)
                        result.set(true)
                    } catch (e: RarException) {
                        Log.e(TAG, "Unable to extract rar file '" + fmFile.absolutePath + "'!", e)
                        result.set(false)
                    } catch (e: IOException) {
                        Log.e(TAG, "Unable to extract rar file '" + fmFile.absolutePath + "'!", e)
                        result.set(false)
                    }
                }.handle(f)
            } else {
                // handle archives using commons compress
                fileType!!.newHandler(getArchiveExtractionCallback(destination, result, fileType))
                    .handle(f)
            }
        } else {
            fileType.newHandler { fi: FMFile ->
                val sevenZFile: SevenZFile
                try {
                    sevenZFile = SevenZFile(fi.file)
                    var entry: SevenZArchiveEntry
                    while (sevenZFile.nextEntry.also { entry = it } != null) {
                        if (entry.isDirectory) {
                            continue
                        }
                        val curfile = File(destination, entry.name)
                        val parent = curfile.parentFile
                        if (parent != null) {
                            if (!parent.exists()) {
                                if (parent.mkdirs()) {
                                    Log.i(TAG, "Folder created: '" + parent.absolutePath + "'")
                                } else {
                                    Log.e(
                                        TAG,
                                        "Unable to create folder: '" + parent.absolutePath + "'"
                                    )
                                }
                            }
                            val out = FileOutputStream(curfile)
                            val content = ByteArray(entry.size.toInt())
                            sevenZFile.read(content, 0, content.size)
                            out.write(content)
                            out.close()
                        } else {
                            throw IOException("Unable to get parent file for '" + curfile.absolutePath + "'!")
                        }
                    }
                    result.set(true)
                } catch (e: IOException) {
                    Log.e(TAG, "Error extracting " + fileType.extension)
                    result.set(false)
                }
            }.handle(f)
        }
        return result.get()
    }

    private fun getArchiveExtractionCallback(
        path: String?,
        result: AtomicBoolean,
        fileType: FileType?
    ): (FMFile) -> Unit {
        return { fi: FMFile ->
            try {
                FileInputStream(fi.file).use { `is` ->
                    val ais = ArchiveStreamFactory().createArchiveInputStream(
                        fileType!!.extension,
                        `is`
                    )
                    var entry: ArchiveEntry
                    while (true) {
                        val nextEntry = ais.nextEntry ?: break
                        nextEntry.also { entry = it }
                        if (entry.name.endsWith("/")) {
                            val dir = File(path + File.separator + entry.name)
                            if (!dir.exists()) {
                                dir.mkdirs()
                            }
                            continue
                        }
                        val outFile = File(path + File.separator + entry.name)
                        if (outFile.isDirectory) {
                            continue
                        }
                        if (outFile.exists()) {
                            continue
                        }
                        FileOutputStream(outFile).use { out ->
                            val buffer = ByteArray(1024)
                            var length = 0
                            while (ais.read(buffer).also { length = it } > 0) {
                                out.write(buffer, 0, length)
                                out.flush()
                            }
                        }
                        result.set(true)
                    }
                }
            } catch (e: IOException) {
                Log.e(TAG, "Error extracting " + fileType!!.extension, e)
                result.set(false)
            } catch (e: ArchiveException) {
                Log.e(TAG, "Error extracting " + fileType!!.extension, e)
                result.set(false)
            }
        }
    }

    @Throws(BlockingStuffOnMainThreadException::class)
    fun doCreateZip(files: CopyOnWriteArrayList<FMFile?>?, destination: File?): Boolean {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            throw BlockingStuffOnMainThreadException()
        }
        try {
            val fos = FileOutputStream(destination)
            val zipOut = ZipOutputStream(fos)
            for (f in files!!) {
                addFileToZip(f!!.file, f.name, zipOut)
            }
            zipOut.close()
            fos.close()
            return true
        } catch (e: IOException) {
            Log.e(TAG, "unable to create zip file!", e)
        }
        return false
    }

    companion object {
        private val TAG = ArchiveUtil::class.java.canonicalName

        /**
         * Adds a file to a ZipInputStream. Also walks subdirectories.
         *
         * @param f    the file
         * @param name the filename
         * @param zos  the ZipOutputStream
         * @throws IOException when there is an error
         */
        @Throws(IOException::class)
        private fun addFileToZip(f: File?, name: String?, zos: ZipOutputStream) {
            if (f!!.isDirectory) {
                val children = f.listFiles()
                for (childFile in children!!) {
                    addFileToZip(childFile, name + "/" + childFile.name, zos)
                }
                return
            }
            val fis = FileInputStream(f)
            val zipEntry = ZipEntry(name)
            zos.putNextEntry(zipEntry)
            val bytes = ByteArray(1024)
            var length: Int
            while (fis.read(bytes).also { length = it } >= 0) {
                zos.write(bytes, 0, length)
            }
            fis.close()
        }
    }
}
