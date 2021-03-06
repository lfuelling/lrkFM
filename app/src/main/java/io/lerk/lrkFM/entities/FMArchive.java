package io.lerk.lrkFM.entities;

import android.os.Looper;
import android.util.Log;

import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.ArchiveException;
import org.apache.commons.compress.archivers.ArchiveInputStream;
import org.apache.commons.compress.archivers.ArchiveStreamFactory;
import org.apache.commons.compress.archivers.sevenz.SevenZArchiveEntry;
import org.apache.commons.compress.archivers.sevenz.SevenZFile;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Objects;

import io.lerk.lrkFM.exceptions.BlockingStuffOnMainThreadException;

/**
 * @author Lukas Fülling (lukas@k40s.net)
 */
public class FMArchive extends FMFile {

    public final static String TAG = FMArchive.class.getCanonicalName();

    private final HashMap<String, ArrayList<FMFile>> contents;
    private static final String ROOT_DIR = "/";

    /**
     * Constructor.
     *
     * @param f the file
     */
    public FMArchive(File f) throws BlockingStuffOnMainThreadException {
        super(f);
        if (!this.isArchive()) {
            this.contents = null;
            throw new ClassCastException("Error creating " + f.getName() + "as archive:" + FMFile.class.getName() + " cannot be cast to " + FMArchive.class.getName());
        } else {
            this.contents = calculateArchiveContents();
        }
    }

    /**
     * Gets archive content for path.
     *
     * @param path the relative path inside the archive
     * @return the contents
     */
    public ArrayList<FMFile> getContentForPath(String path) {
        String rPath;
        if (!path.startsWith(ROOT_DIR)) {
            rPath = ROOT_DIR + path;
        } else {
            try {
                rPath = path.split(getName())[1];
            } catch (ArrayIndexOutOfBoundsException e) {
                rPath = ROOT_DIR;
            }
        }
        if (!rPath.equals(ROOT_DIR) && rPath.endsWith("/")) {
            rPath = rPath.substring(0, rPath.length() - 1);
        }
        ArrayList<FMFile> pathContents = contents.get(rPath);
        if (pathContents == null && rPath.equals(ROOT_DIR)) {
            pathContents = new ArrayList<>();
            for (String s : contents.keySet()) {
                String[] split = s.split(File.separator);
                if (split.length == 2) {
                    pathContents.add(new FMFile(new File(split[1])));
                }
            }
        }

        return new ArrayList<>(Objects.requireNonNull(pathContents));
    }

    /**
     * Calculates the archive contents.
     *
     * @return a {@link HashMap} containing the relative path of the file in the archive and the file.
     */
    private HashMap<String, ArrayList<FMFile>> calculateArchiveContents() throws BlockingStuffOnMainThreadException {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            throw new BlockingStuffOnMainThreadException();
        }
        HashMap<String, ArrayList<FMFile>> res = new HashMap<>();

        switch (getFileType()) {
            case ARCHIVE_P7Z:
                readP7ZFile(res);
                break;
            case ARCHIVE_ZIP:
            case ARCHIVE_RAR:
            case ARCHIVE_TGZ:
            case ARCHIVE_TAR:
                readCCFile(res);
                break;
            case UNKNOWN:
            default:
                Log.w(TAG, "Unable to read archive!");
                break;
        }
        return res;
    }

    /**
     * Read an archive file using commons compress.
     *
     * @param res the result HashMap
     */
    private void readCCFile(HashMap<String, ArrayList<FMFile>> res) {
        try (InputStream is = new FileInputStream(getFile())) {

            ArchiveInputStream ais = new ArchiveStreamFactory().createArchiveInputStream(getFileType().getExtension(), is);
            ArchiveEntry entry;
            while ((entry = ais.getNextEntry()) != null) {
                String filePath;

                filePath = entry.getName();

                FMArchiveFile outFile = new FMArchiveFile(new File(entry.getName()));
                outFile.setDirectory(entry.isDirectory());
                outFile.setAbsolutePath(entry.getName());

                String fileParent = new File(filePath).getParent();
                String parent = ROOT_DIR + ((fileParent != null) ? fileParent : "");
                ArrayList<FMFile> pathContents = res.get(parent);
                if (pathContents == null) {
                    pathContents = new ArrayList<>();
                }

                pathContents.add(outFile);
                res.put(parent, pathContents);
            }
            ais.close();
        } catch (IOException | ArchiveException e) {
            Log.e(TAG, "Error reading " + getFileType().getExtension());
        }
    }

    /**
     * Reads an archive file using p7zip.
     *
     * @param res the result HashMap
     */
    private void readP7ZFile(HashMap<String, ArrayList<FMFile>> res) {
        try (SevenZFile sevenZFile = new SevenZFile(getFile())) {
            SevenZArchiveEntry entry;
            while ((entry = sevenZFile.getNextEntry()) != null) {
                String filePath;

                filePath = entry.getName();

                FMArchiveFile outFile = new FMArchiveFile(new File(entry.getName()));
                outFile.setDirectory(entry.isDirectory());
                outFile.setAbsolutePath(entry.getName());

                String fileParent = new File(filePath).getParent();
                String parent = ROOT_DIR + ((fileParent != null) ? fileParent : "");
                ArrayList<FMFile> pathContents = res.get(parent);
                if (pathContents == null) {
                    pathContents = new ArrayList<>();
                }

                pathContents.add(outFile);
                res.put(parent, pathContents);
            }
        } catch (IOException e) {
            Log.e(TAG, "Error reading " + getFileType().getExtension());
        }
    }
}
