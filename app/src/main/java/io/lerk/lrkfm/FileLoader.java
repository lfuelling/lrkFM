package io.lerk.lrkfm;

import android.content.Context;
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
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * This class handles file loading.
 *
 * @author Lukas Fülling (lukas@k40s.net)
 */
public class FileLoader {

    private final String location;

    private final ArrayList<FMFile> files;

    public static final String TAG = FileLoader.class.getCanonicalName();

    /**
     * Constructor.
     *
     * @param location the location to load
     */
    public FileLoader(String location) {
        this.location = location;
        files = loadLocationFiles(this.location);
    }

    public ArrayList<FMFile> getFiles() {
        return files;
    }

    private ArrayList<FMFile> loadLocationFiles(String location) {
        File locationFile = new File(location);
        if (locationFile.isDirectory()) {
            if (locationFile.canRead()) {
                ArrayList<FMFile> result = new ArrayList<>();

                File[] fileList = locationFile.listFiles();
                if (fileList != null) {
                    Arrays.asList(fileList).forEach((f) -> {

                        Log.d(TAG, "Loading file: " + f.getName());
                        FMFile file = new FMFile(f);
                        result.add(file);
                    });
                    Log.d(TAG, "Loaded " + String.valueOf(result.size()) + " files");
                    return result;
                } else {
                    Log.d(TAG, "fileList is null");
                }
            } else {
                Log.w(TAG, "Can't read '" + location + "': Permission denied!");
            }
        } else {
            String parent = locationFile.getParent();
            Log.d(TAG, "Location '" + location + "' not a directory");
            if (parent != null) {
                ArrayList<FMFile> fmFiles = loadLocationFiles(parent);
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


}
