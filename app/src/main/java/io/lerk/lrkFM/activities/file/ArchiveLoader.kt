package io.lerk.lrkFM.activities.file

import io.lerk.lrkFM.entities.FMArchive
import io.lerk.lrkFM.entities.FMFile

/**
 * Class used to load archives.
 *
 * @author Lukas Fülling (lukas@k40s.net)
 */
class ArchiveLoader
/**
 * Constructor.
 * @param archiveFile the file to load
 * @param path the path to load
 */ internal constructor(
    /**
     * The archive-
     */
    private val archive: FMArchive,
    /**
     * The path to the archive.
     */
    private val path: String?
) : AbstractLoader() {
    public override fun loadLocationFiles(): ArrayList<FMFile> {
        return loadLocationFilesForPath(path)
    }

    public override fun loadLocationFilesForPath(parent: String?): ArrayList<FMFile> {
        return archive.getContentForPath(parent ?: "/")
    }
}
