package io.lerk.lrkfm;

import android.app.Activity;
import android.content.Context;
import android.support.annotation.Nullable;
import android.util.Log;

import org.apache.commons.io.FileSystemUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.Date;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * This class handles file loading.
 *
 * @author Lukas Fülling (lukas@k40s.net)
 */
public class FileLoader {

    private String location;

    public static final String TAG = FileLoader.class.getCanonicalName();

    /**
     * Constructor.
     *
     * @param location the location to load
     */
    public FileLoader(String location) {
        this.location = location;
    }

    public ArrayList<FMFile> loadLocationFiles() throws NoAccessException {
        return this.loadLocationFiles(null);
    }

    private ArrayList<FMFile> loadLocationFiles(@Nullable String parent) throws NoAccessException {
        if(parent != null) {
            location = parent;
        }
        File locationFile = new File(location);
        if (locationFile.isDirectory()) {
            if (locationFile.canRead()) {
                ArrayList<FMFile> result = new ArrayList<>();

                File[] fileList = locationFile.listFiles();
                if (fileList != null) {
                    List<File> asList = Arrays.asList(fileList);
                    if(!asList.isEmpty()) {
                        //noinspection Convert2Lambda
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


    class NoAccessException extends Exception {
        public NoAccessException(String message) {
            super(message);
        }
    }
}
