package io.lerk.lrkFM.exceptions;

import java.io.File;

import io.lerk.lrkFM.entities.FMFile;

public class FileExistsException extends Exception {
    private final FMFile file;
    private final File destination;

    public FileExistsException(FMFile file, File destination) {
        super("File exists: '" + destination.getAbsolutePath() + "'");
        this.file = file;
        this.destination = destination;
    }

    public FMFile getFile() {
        return file;
    }

    public File getDestination() {
        return destination;
    }
}
