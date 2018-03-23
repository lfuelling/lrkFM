package io.lerk.lrkFM.entities;

import java.io.File;

/**
 * @author Lukas Fülling (lukas@k40s.net)
 */
public class FMArchiveFile extends FMFile {
    /**
     * Constructor.
     *
     * @param f the file
     */
    public FMArchiveFile(File f) {
        super(f);
    }
    public void setDirectory(boolean b) {
        this.directory = b;
    }
}
