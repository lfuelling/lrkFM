package io.lerk.lrkFM.util;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.ArrayList;

import io.lerk.lrkFM.entities.FMArchive;
import io.lerk.lrkFM.entities.FMFile;

public class ArchiveLoader extends AbstractLoader {
    private final FMArchive archive;
    private String path;

    public ArchiveLoader(@NonNull FMArchive archiveFile, String path) {
        this.archive = archiveFile;
        this.path = path;
    }

    @Override
    public ArrayList<FMFile> loadLocationFiles() {
        return this.loadLocationFilesForPath(path);
    }

    @Override
    public ArrayList<FMFile> loadLocationFilesForPath(@Nullable String parent) {
        return archive.getContentForPath((parent != null) ? parent : "/");
    }
}
