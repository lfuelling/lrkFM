package io.lerk.lrkFM.activities.file

import io.lerk.lrkFM.entities.FMArchive
import io.lerk.lrkFM.entities.FMFile
import io.lerk.lrkFM.exceptions.BlockingStuffOnMainThreadException
import java.io.File

/**
 * Finds parent archives.
 *
 * @author Lukas Fülling (lukas@k40s.net)
 */
class ArchiveParentFinder(private val path: String?) {
    var isArchive = false
        private set
    var archiveFile: FMArchive? = null
        private set

    @Throws(BlockingStuffOnMainThreadException::class)
    operator fun invoke(): ArchiveParentFinder {
        isArchive = false
        archiveFile = null
        var tPath = path
        while ("/" != tPath && tPath != null) {
            val f = FMFile(File(tPath))
            if (f.isArchive) {
                isArchive = true
                archiveFile = FMArchive(File(tPath))
            }
            tPath = File(tPath).parent
            if (isArchive || tPath == null || tPath.isEmpty()) {
                break
            }
        }
        return this
    }
}
