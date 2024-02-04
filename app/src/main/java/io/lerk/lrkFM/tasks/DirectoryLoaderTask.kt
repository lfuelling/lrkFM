package io.lerk.lrkFM.tasks

import android.util.Log

import io.lerk.lrkFM.activities.file.FileActivity
import io.lerk.lrkFM.activities.file.FileLoader
import io.lerk.lrkFM.entities.FMFile
import io.lerk.lrkFM.exceptions.BlockingStuffOnMainThreadException
import io.lerk.lrkFM.exceptions.EmptyDirectoryException
import io.lerk.lrkFM.exceptions.NoAccessException

/**
 * Task to load directory contents.
 *
 * @author Lukas Fülling (lukas@k40s.net)
 */
class DirectoryLoaderTask
/**
 * Constructor.
 *
 * @param context  the current [FileActivity] instance.
 * @param path     the path to load
 * @param callback the callback to use.
 */(context: FileActivity?, private val path: String?, callback: (ArrayList<FMFile>) -> Unit) :
    ProgressDialogCallbackTask<ArrayList<FMFile>>(context, callback) {
    @Deprecated("Deprecated in Java")
    override fun doInBackground(vararg params: Void?): ArrayList<FMFile> {
        var files: ArrayList<FMFile> = ArrayList()
        val fileLoader = FileLoader(
            path
        )
        try {
            files = fileLoader.loadLocationFiles()
        } catch (e: NoAccessException) {
            Log.w(TAG, "Can't read '$path': Permission denied!")
        } catch (e: EmptyDirectoryException) {
            Log.w(TAG, "Can't read '$path': Empty directory!")
        } catch (e: BlockingStuffOnMainThreadException) {
            Log.wtf(TAG, "This should not happen!", e)
        }
        return files
    }

    companion object {
        private val TAG = DirectoryLoaderTask::class.java.canonicalName
    }
}
