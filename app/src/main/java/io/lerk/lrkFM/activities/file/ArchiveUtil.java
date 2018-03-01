package io.lerk.lrkFM.activities.file;

import android.util.Log;

import com.github.junrar.extract.ExtractArchive;
import com.google.firebase.crash.FirebaseCrash;
import com.google.firebase.perf.FirebasePerformance;
import com.google.firebase.perf.metrics.Trace;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import io.lerk.lrkFM.entities.FMFile;

public class ArchiveUtil {

    private static final String TAG = ArchiveUtil.class.getCanonicalName();
    public static final String RAR_EXTENSION = "rar";
    public static final String ZIP_EXTENSION = "zip";

    /**
     * Unpacks a rar file.
     *
     * @param destination destination path (directory)
     * @param rar         rarfile
     * @return if the destination is a directory after extracting
     */
    private static boolean unpackRar(String destination, FMFile rar) {
        Trace trace = FirebasePerformance.getInstance().newTrace("extract_rar");
        trace.start();
        new ExtractArchive().extractArchive(rar.getFile(), new File(destination));
        boolean success = new File(destination).isDirectory();
        trace.putAttribute("success", String.valueOf(success));
        trace.stop();
        return success;
    }

    public static boolean extractArchive(String path, FMFile f, FileActivity context) {
        boolean result = false;
        if (f.getExtension().equals(RAR_EXTENSION)) {
            result = unpackRar(path, f);

        } else { //noinspection SimplifiableIfStatement
            if (f.getExtension().equals(ZIP_EXTENSION)) {
                result = unpackZip(path, f);
            }
        }
        context.clearFileOpCache();
        context.reloadCurrentDirectory();
        return result;
    }

    /**
     * Method to extract a zip archive.
     *
     * @param path Path where the zip will be extracted
     * @param zip  Zip file
     * @return result of extraction
     */
    @SuppressWarnings("ResultOfMethodCallIgnored")
    private static boolean unpackZip(String path, FMFile zip) {
        byte[] buffer = new byte[1024];

        Trace trace = FirebasePerformance.getInstance().newTrace("extract_zip");
        trace.start();
        try {
            //create output directory is not exists
            File folder = new File(path);
            if (!folder.exists()) {
                trace.putAttribute("mkdir_success", String.valueOf(folder.mkdirs()));
            }
            //get the zip file content
            ZipInputStream zis = new ZipInputStream(new FileInputStream(zip.getFile()));
            //get the zipped file list entry
            ZipEntry ze = zis.getNextEntry();

            while (ze != null) {
                String fileName = ze.getName();
                File newFile = new File(path + File.separator + fileName);
                System.out.println("file unzip : " + newFile.getAbsoluteFile());

                //create all non exists folders
                //else you will hit FileNotFoundException for compressed folder
                new File(newFile.getParent()).mkdirs();
                FileOutputStream fos = new FileOutputStream(newFile);
                int len;
                while ((len = zis.read(buffer)) > 0) {
                    fos.write(buffer, 0, len);
                }
                fos.close();
                ze = zis.getNextEntry();
            }
            zis.closeEntry();
            zis.close();
            trace.putAttribute("unpack_success", "true");
            trace.stop();
        } catch (IOException ex) {
            Log.e(TAG, "Unable to extract zip!", ex);
            trace.putAttribute("unpack_success", "true");
            trace.stop();
            FirebaseCrash.report(ex);
            return false;
        }
        return true;
    }
}
