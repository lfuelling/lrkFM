package io.lerk.lrkFM.util;

import java.io.File;

import io.lerk.lrkFM.entities.FMArchive;
import io.lerk.lrkFM.entities.FMFile;

import static io.lerk.lrkFM.consts.PreferenceEntity.PERFORMANCE_REPORTING;

/**
 * @author Lukas Fülling (lukas@k40s.net)
 */
public class ArchiveParentFinder {
    private String path;
    private boolean archive;
    private FMArchive archiveFile;

    public ArchiveParentFinder(String path) {
        this.path = path;
    }

    public boolean isArchive() {
        return archive;
    }

    public FMArchive getArchiveFile() {
        return archiveFile;
    }

    public ArchiveParentFinder invoke() {
        archive = false;
        archiveFile = null;
        String tPath = path;

        while (!"/".equals(tPath)) {
            FMFile f = new FMFile(new File(tPath));
            if (f.isArchive()) {
                archive = true;
                archiveFile = new FMArchive(new File(tPath));
            }
            tPath = new File(tPath).getParent();

            if(archive || tPath == null || tPath.isEmpty()) {
                break;
            }
        }

        return this;
    }
}
