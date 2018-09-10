package io.lerk.lrkFM.entities;

import java.io.File;

/**
 * @author Lukas Fülling (lukas@k40s.net)
 */
class FMArchiveFile extends FMFile {

    /**
     * Constructor.
     *
     * @param f the file
     */
    FMArchiveFile(File f) {
        super(f);
    }

    void setDirectory(boolean b) {
        this.directory = b;
    }
    void setAbsolutePath(String path) {
        this.absolutePath = path;
    }
}
