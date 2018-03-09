package io.lerk.lrkFM.entities;

import android.util.Log;

import java.io.File;
import java.util.Arrays;
import java.util.Date;

import io.lerk.lrkFM.consts.FileType;
import io.lerk.lrkFM.operations.ArchiveUtil;

/**
 * File object.
 *
 * @author Lukas Fülling (lukas@k40s.net)
 */
public class FMFile {

    private static final String TAG = FMFile.class.getCanonicalName();

    private String name, permissions;
    private Date lastModified;
    private File file;
    private Boolean directory;

    /**
     * Constructor.
     *
     * @param f the file
     */
    public FMFile(File f) {
        this.file = f;
        this.name = this.file.getName();
        this.lastModified = new Date(f.lastModified());
        this.permissions = ((f.isDirectory()) ? "d" : "-") +
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

    public Boolean isDirectory() {
        return directory;
    }

    public String getPermissions() {
        return permissions;
    }

    public Date getLastModified() {
        return (Date) lastModified.clone();
    }

    public File getFile() {
        return file;
    }

    public void setFile(File file) {
        this.file = file;
    }

    /**
     * Gets a file's extension.
     *
     * @return the file's extension
     */
    private String getExtension() {
        try {
            return name.substring(name.indexOf('.') + 1, name.length());
        } catch (StringIndexOutOfBoundsException | ArrayIndexOutOfBoundsException e) {
            Log.e(TAG, "Unable to get file extension.", e);
            return "";
        }
    }

    public FileType getFileType() {
        for (FileType fileType : Arrays.asList(FileType.values())) {
            if (fileType.getExtension().equals(getExtension())) {
                return fileType;
            }
        }
        return FileType.UNKNOWN;
    }

    public boolean isArchive() {
        FileType type = getFileType();
        return type.equals(FileType.ARCHIVE_ZIP) ||
                type.equals(FileType.ARCHIVE_RAR) ||
                type.equals(FileType.ARCHIVE_TAR) ||
                type.equals(FileType.ARCHIVE_TGZ) ||
                type.equals(FileType.ARCHIVE_P7Z);
    }
}
