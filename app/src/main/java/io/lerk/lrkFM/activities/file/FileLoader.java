package io.lerk.lrkFM.activities.file;

import android.os.Build;
import android.os.Looper;
import android.support.annotation.Nullable;
import android.util.Log;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

import io.lerk.lrkFM.entities.FMFile;
import io.lerk.lrkFM.exceptions.BlockingStuffOnMainThreadException;
import io.lerk.lrkFM.exceptions.EmptyDirectoryException;
import io.lerk.lrkFM.exceptions.NoAccessException;

/**
 * This class handles file loading.
 *
 * @author Lukas Fülling (lukas@k40s.net)
 */
public class FileLoader extends AbstractLoader {

    /**
     * The location.
     */
    private String location;

    /**
     * Logtag.
     */
    private static final String TAG = FileLoader.class.getCanonicalName();

    /**
     * Constructor.
     *
     * @param location the location to load
     */
    public FileLoader(String location) {
        this.location = location;
    }

    /**
     * Calls {@link #FileLoader(String)} with <pre>null</pre> as argument.
     * @see #loadLocationFilesForPath(String)
     * @throws NoAccessException if no access
     * @throws EmptyDirectoryException  if no contents
     */
    @Override
    public ArrayList<FMFile> loadLocationFiles() throws NoAccessException, EmptyDirectoryException, BlockingStuffOnMainThreadException {
        return this.loadLocationFilesForPath(null);
    }

    /**
     * Loads the contents of a directory.
     * @param parent the parent dir
     * @return the files and subdirectories
     * @throws NoAccessException if no access
     * @throws EmptyDirectoryException if no contents
     */
    @Override
    protected ArrayList<FMFile> loadLocationFilesForPath(@Nullable String parent) throws NoAccessException, EmptyDirectoryException, BlockingStuffOnMainThreadException {
        if(Looper.myLooper() == Looper.getMainLooper()) {
            throw new BlockingStuffOnMainThreadException();
        }
        if (parent != null) {
            location = parent;
        }
        if (location == null || location.isEmpty()) {
            location = "/";
        }
        if (!location.startsWith("/")) {
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
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                            //noinspection Convert2Lambda,SimplifyStreamApiCallChains
                            asList.stream().forEach(new Consumer<File>() {  // This operation fails if List.forEach or a Lambda is used. Get your shit together Android!
                                @Override
                                public void accept(File f) {
                                    Log.d(TAG, "Loading file: " + f.getName());
                                    FMFile file = new FMFile(f);
                                    result.add(file);
                                }
                            });
                        } else { // puny Marshmallow and Lollipop users!
                            for (File f : asList) {
                                Log.d(TAG, "Loading file: " + f.getName());
                                FMFile file = new FMFile(f);
                                result.add(file);
                            }
                        }
                        Log.i(TAG, "Loaded " + String.valueOf(result.size()) + " files");
                    } else {
                        Log.w(TAG, "Directory content is null!");
                        throw new EmptyDirectoryException(location);
                    }
                    return result;
                } else {
                    Log.w(TAG, "fileList is null");
                }
            } else {
                throw new NoAccessException("Unable to read specified file!");
            }
        } else {
            String newParent = locationFile.getParent();
            Log.d(TAG, "Location '" + location + "' not a directory");
            if (newParent != null) {
                ArrayList<FMFile> fmFiles = loadLocationFilesForPath(newParent);
                if (fmFiles != null) {
                    return fmFiles;
                } else {
                    Log.w(TAG, "Parent files are null.");
                }
            } else {
                Log.w(TAG, "Parent is null");
            }
        }
        Log.w(TAG, "Unable to load files");
        return new ArrayList<>();
    }

}
