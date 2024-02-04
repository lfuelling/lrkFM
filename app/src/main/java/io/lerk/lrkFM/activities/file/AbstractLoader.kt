package io.lerk.lrkFM.activities.file

import io.lerk.lrkFM.entities.FMFile
import io.lerk.lrkFM.exceptions.BlockingStuffOnMainThreadException
import io.lerk.lrkFM.exceptions.EmptyDirectoryException
import io.lerk.lrkFM.exceptions.NoAccessException

/**
 * Because I like to rant about <pre>ILoader</pre> 'nsuch.
 *
 * @author Lukas Fülling (lukas@k40s.net)
 */
abstract class AbstractLoader {
    @Throws(
        NoAccessException::class,
        EmptyDirectoryException::class,
        BlockingStuffOnMainThreadException::class
    )
    abstract fun loadLocationFiles(): ArrayList<FMFile>
    @Throws(
        NoAccessException::class,
        EmptyDirectoryException::class,
        BlockingStuffOnMainThreadException::class
    )
    protected abstract fun loadLocationFilesForPath(parent: String?): ArrayList<FMFile>
}
