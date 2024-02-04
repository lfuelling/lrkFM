package io.lerk.lrkFM.entities

/**
 * History entry.
 *
 * @author Lukas Fülling (lukas@k40s.net)
 */
class HistoryEntry(val path: String?, val archive: FMArchive?) {
    val isInArchive: Boolean

    init {
        isInArchive = archive != null
    }
}
