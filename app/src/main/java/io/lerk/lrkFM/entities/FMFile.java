package io.lerk.lrkFM.entities;

import android.support.annotation.NonNull;
import android.util.Log;
import android.webkit.MimeTypeMap;

import java.io.File;
import java.util.Date;

import io.lerk.lrkFM.Pref;
import io.lerk.lrkFM.consts.FileType;
import io.lerk.lrkFM.consts.PreferenceEntity;

/**
 * File object.
 *
 * @author Lukas Fülling (lukas@k40s.net)
 */
public class FMFile implements Comparable<FMFile> {

    private static final String TAG = FMFile.class.getCanonicalName();

    private String name, permissions;
    private Date lastModified;
    private File file;
    Boolean directory;
    String absolutePath;

    /**
     * Constructor.
     *
     * @param f the file
     */
    public FMFile(@NonNull File f) {
        this.file = f;
        this.name = this.file.getName();
        this.lastModified = new Date(f.lastModified());
        this.permissions = ((f.isDirectory()) ? "d" : "-") +
                ((this.file.canRead()) ? "r" : "-") +
                ((this.file.canWrite()) ? "w" : "-") +
                ((this.file.canExecute()) ? "x" : "-"); // lol
        this.directory = f.isDirectory();
        this.absolutePath = f.getAbsolutePath();
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

    public String getAbsolutePath() {
        return absolutePath;
    }

    /**
     * Gets a file's extension.
     *
     * @return the file's extension
     */
    private String getExtension() {
        try {
            return name.substring(name.indexOf('.') + 1);
        } catch (StringIndexOutOfBoundsException | ArrayIndexOutOfBoundsException e) {
            Log.e(TAG, "Unable to get file extension.", e);
            return "";
        }
    }

    public FileType getFileType() {
        String extension = getExtension();
        for (FileType fileType : FileType.values()) {
            if (fileType.getExtension().equals(extension)) {
                return fileType.withExtension(extension);
            }
        }
        return FileType.UNKNOWN.withExtension(extension);
    }

    public boolean isArchive() {
        FileType type = getFileType();
        return type.equals(FileType.ARCHIVE_ZIP) ||
                type.equals(FileType.ARCHIVE_RAR) ||
                type.equals(FileType.ARCHIVE_TAR) ||
                type.equals(FileType.ARCHIVE_TGZ) ||
                type.equals(FileType.ARCHIVE_P7Z);
    }

    public boolean isExplorableArchive() {
        return getFileType().equals(FileType.ARCHIVE_ZIP);
    }

    public static String getMimeTypeFromFile(FMFile f) {
        String fileExtension;
        try {
            String[] split = f.getName().split("\\.");
            fileExtension = split[split.length - 1];
        } catch (IndexOutOfBoundsException e) {
            fileExtension = "";
        }
        return MimeTypeMap.getSingleton().getMimeTypeFromExtension(fileExtension);
    }

    @Override
    public int compareTo(FMFile o) {
        String sortParam = new Pref<String>(PreferenceEntity.SORT_FILES_BY).getValue();
        switch (sortParam) {
            case "datea":
                return Long.compare(this.lastModified.getTime(), o.lastModified.getTime());
            case "dated":
                return Long.compare(o.lastModified.getTime(), this.lastModified.getTime());
            case "named":
                return o.name.compareToIgnoreCase(this.name);
            case "namea":
            default:
                return this.name.compareToIgnoreCase(o.name);
        }
    }
}
