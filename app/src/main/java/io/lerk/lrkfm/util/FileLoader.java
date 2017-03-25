package io.lerk.lrkfm.util;

import android.support.annotation.Nullable;
import android.util.Log;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

import io.lerk.lrkfm.entities.FMFile;

/**
 * This class handles file loading.
 *
 * @author Lukas Fülling (lukas@k40s.net)
 */
public class FileLoader {

    private String location;

    private static final String TAG = FileLoader.class.getCanonicalName();

    /**
     * Constructor.
     *
     * @param location the location to load
     */
    public FileLoader(String location) {
        this.location = location;
    }

    public ArrayList<FMFile> loadLocationFiles() throws NoAccessException, EmptyDirectoryException {
        return this.loadLocationFiles(null);
    }

    private ArrayList<FMFile> loadLocationFiles(@Nullable String parent) throws NoAccessException, EmptyDirectoryException {
        if (parent != null) {
            location = parent;
        }
        if(location == null || location.isEmpty()) {
            location = "/";
        }
        if(!location.startsWith("/")) {
            throw new NoAccessException("Invalid path: " + location);
        }
        File locationFile = new File(location);
        if (locationFile.isDirectory()) {
            if (locationFile.canRead()) {
                ArrayList<FMFile> result = new ArrayList<>();
                File[] fileList = locationFile.listFiles();
                if (fileList != null) {
                    List<File> asList = Arrays.asList(fileList);
                    if (!asList.isEmpty()) {
                        //noinspection Convert2Lambda,SimplifyStreamApiCallChains
                        asList.stream().forEach(new Consumer<File>() {  // This operation fails if List.forEach or a Lambda is used. Get your shit together Android!
                            @Override
                            public void accept(File f) {
                                Log.d(TAG, "Loading file: " + f.getName());
                                FMFile file = new FMFile(f);
                                result.add(file);
                            }
                        });
                        Log.d(TAG, "Loaded " + String.valueOf(result.size()) + " files");
                    } else {
                        Log.d(TAG, "Directory content is null!");
                        throw new EmptyDirectoryException(location);
                    }
                    return result;
                } else {
                    Log.d(TAG, "fileList is null");
                }
            } else {
                throw new NoAccessException("Unable to read specified file!");
            }
        } else {
            String newParent = locationFile.getParent();
            Log.d(TAG, "Location '" + location + "' not a directory");
            if (newParent != null) {
                ArrayList<FMFile> fmFiles = loadLocationFiles(newParent);
                if (fmFiles != null) {
                    return fmFiles;
                } else {
                    Log.d(TAG, "Parent files are null.");
                }
            } else {
                Log.d(TAG, "Parent is null");
            }
        }
        Log.w(TAG, "Unable to load files");
        return new ArrayList<>();
    }

    public class NoAccessException extends Exception implements Serializable {
        static final long serialVersionUID = 10L;
        public NoAccessException(String message) {
            super(message);
        }
    }

    public class EmptyDirectoryException extends Exception implements Serializable {
        static final long serialVersionUID = 10L;
        public EmptyDirectoryException(String message) {
            super(message);
        }
    }
}
