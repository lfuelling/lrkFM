package io.lerk.lrkFM.util;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.ArrayList;

import io.lerk.lrkFM.activities.FileActivity;
import io.lerk.lrkFM.entities.FMFile;
import io.lerk.lrkFM.op.ArchiveUtil;

public class ArchiveLoader extends AbstractLoader {
    private final FMFile archive;

    public ArchiveLoader(@NonNull FMFile archive) {
        this.archive = archive;
    }

    @Override
    public ArrayList<FMFile> loadLocationFiles() {
        return this.loadLocationFilesInternal(null);
    }

    @Override
    protected ArrayList<FMFile> loadLocationFilesInternal(@Nullable String parent) {
        return new ArchiveUtil(FileActivity.get()).loadArchiveContents(archive, parent);
    }
}
