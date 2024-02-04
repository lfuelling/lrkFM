package io.lerk.lrkFM.tasks.archive


import io.lerk.lrkFM.activities.file.ArchiveParentFinder
import io.lerk.lrkFM.entities.FMFile
import io.lerk.lrkFM.exceptions.BlockingStuffOnMainThreadException
import io.lerk.lrkFM.tasks.CallbackTask

/**
 * Task to find the parent archive of a file.
 *
 * @author Lukas Fülling (lukas@k40s.net)
 */
class ArchiveParentFinderTask
/**
 * Constructor.
 *
 * @param callback the callback to use.
 */(private val fmFile: FMFile, callback: (ArchiveParentFinder) -> Unit) :
    CallbackTask<ArchiveParentFinder>(callback) {
    @Deprecated("Deprecated in Java")
    override fun doInBackground(vararg voids: Void?): ArchiveParentFinder {
        return try {
            ArchiveParentFinder(fmFile.absolutePath).invoke()
        } catch (e: BlockingStuffOnMainThreadException) {
            throw RuntimeException("This should not happen!", e)
        }
    }
}
