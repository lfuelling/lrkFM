package io.lerk.lrkFM.entities;

import android.support.annotation.Nullable;

/**
 * History entry.
 *
 * @author Lukas Fülling (lukas@k40s.net)
 */
public class HistoryEntry {
    private final FMArchive archive;
    private final String path;
    private final Boolean inArchive;

    public HistoryEntry(String path, @Nullable FMArchive archive) {
        this.path = path;
        this.inArchive = archive != null;
        this.archive = archive;
    }

    public String getPath() {
        return path;
    }

    public Boolean isInArchive() {
        return inArchive;
    }

    @Nullable
    public FMArchive getArchive() {
        return archive;
    }
}
