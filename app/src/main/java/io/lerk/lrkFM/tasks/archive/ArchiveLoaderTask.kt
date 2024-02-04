package io.lerk.lrkFM.tasks.archive


import io.lerk.lrkFM.entities.FMArchive
import io.lerk.lrkFM.entities.FMFile
import io.lerk.lrkFM.exceptions.BlockingStuffOnMainThreadException
import io.lerk.lrkFM.tasks.CallbackTask

/**
 * Task that loads an archive.
 *
 * @author Lukas Fülling (lukas@k40s.net)
 */
class ArchiveLoaderTask
/**
 * Constructor.
 *
 * @param callback the callback to use.
 */(private val file: FMFile, callback: (FMArchive) -> Unit) : CallbackTask<FMArchive>(callback) {
    @Deprecated("Deprecated in Java")
    override fun doInBackground(vararg voids: Void?): FMArchive {
        return try {
            FMArchive(file.file)
        } catch (e: BlockingStuffOnMainThreadException) {
            throw RuntimeException("This should not happen!", e)
        }
    }
}
