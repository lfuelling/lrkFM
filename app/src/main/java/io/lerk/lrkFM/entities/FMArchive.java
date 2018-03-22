package io.lerk.lrkFM.entities;

import android.util.Log;

import com.google.firebase.crash.FirebaseCrash;
import com.google.firebase.perf.FirebasePerformance;
import com.google.firebase.perf.metrics.Trace;

import org.apache.commons.compress.archivers.ArchiveException;
import org.apache.commons.compress.archivers.ArchiveInputStream;
import org.apache.commons.compress.archivers.ArchiveStreamFactory;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.zip.ZipEntry;

import io.lerk.lrkFM.consts.Preference;
import io.lerk.lrkFM.util.PrefUtils;

/**
 * @author Lukas Fülling (lukas@k40s.net)
 */
public class FMArchive extends FMFile {

    public final static String TAG = FMArchive.class.getCanonicalName();

    private final HashMap<String, ArrayList<FMFile>> contents;

    /**
     * Constructor.
     *
     * @param f the file
     */
    public FMArchive(File f) {
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
        return new ArrayList<>(contents.get(path));
    }

    /**
     * Calculates the archive contents.
     *
     * @return a {@link HashMap} containing the relative path of the file in the archive and the file.
     */
    private HashMap<String, ArrayList<FMFile>> calculateArchiveContents() {
        HashMap<String, ArrayList<FMFile>> res = new HashMap<>();
        Trace trace = null;

        if (new PrefUtils<Boolean>(Preference.PERFORMANCE_REPORTING).getValue()) {
            trace = FirebasePerformance.getInstance().newTrace("calc_archive_contents");
            trace.start();
        }

        try {
            InputStream is = new FileInputStream(getFile());
            ArchiveInputStream ais = new ArchiveStreamFactory().createArchiveInputStream(getFileType().getExtension(), is);
            ZipEntry entry;
            while ((entry = (ZipArchiveEntry) ais.getNextEntry()) != null) {
                String filePath;
                if (!entry.isDirectory()) {
                    String[] split = entry.getName().split("/");
                    filePath = split[split.length - 2];
                } else {
                    filePath = entry.getName();
                }
                FMFile outFile = new FMFile(new File(entry.getName()));

                ArrayList<FMFile> pathContents = res.get(filePath);
                if (pathContents == null) {
                    pathContents = new ArrayList<>();
                }
                pathContents.add(outFile);
                res.put(filePath, pathContents);
            }
            if (trace != null) {
                trace.putAttribute("success", String.valueOf(true));
            }
        } catch (IOException | ArchiveException e) {
            Log.e(TAG, "Error extracting " + getFileType().getExtension());
            if (trace != null) {
                trace.putAttribute("success", String.valueOf(false));
            }
            FirebaseCrash.report(e);
        }

        return res;
    }
}
