package io.lerk.lrkFM.entities

import java.io.File

/**
 * Archive file (file inside an archive).
 *
 * @author Lukas Fülling (lukas@k40s.net)
 */
internal class FMArchiveFile(f: File) : FMFile(f) {
    var directory: Boolean? = null
}