package io.lerk.lrkFM.entities;

import java.io.File;
import java.util.Date;

/**
 * File object.
 * @author Lukas Fülling (lukas@k40s.net)
 */
public class FMFile {
    String name, permissions;
    Date lastModified;
    File file;
    Boolean directory;

    /**
     * Constructor.
     * @param f the file
     */
    public FMFile(File f) {
        this.file  = f;
        this.name = this.file.getName();
        this.lastModified = new Date(f.lastModified());
        this.permissions =  ((f.isDirectory()) ? "d" : "-") +
                ((this.file.canRead()) ? "r" : "-") +
                ((this.file.canWrite()) ? "w" : "-") +
                ((this.file.canExecute()) ? "x" : "-"); // lol
        this.directory = f.isDirectory();
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Boolean getDirectory() {
        return directory;
    }

    public void setDirectory(Boolean directory) {
        this.directory = directory;
    }

    public String getPermissions() {
        return permissions;
    }

    public void setPermissions(String permissions) {
        this.permissions = permissions;
    }


    public Date getLastModified() {
        return lastModified;
    }

    public void setLastModified(Date lastModified) {
        this.lastModified = lastModified;
    }

    public File getFile() {
        return file;
    }

    public void setFile(File file) {
        this.file = file;
    }
}
