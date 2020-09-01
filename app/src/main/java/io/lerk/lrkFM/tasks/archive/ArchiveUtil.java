package io.lerk.lrkFM.tasks.archive;

import android.net.IpSecManager;
import android.os.Looper;
import android.util.Log;

import com.github.junrar.Junrar;
import com.github.junrar.exception.RarException;

import org.apache.commons.compress.archivers.ArchiveException;
import org.apache.commons.compress.archivers.ArchiveInputStream;
import org.apache.commons.compress.archivers.ArchiveStreamFactory;
import org.apache.commons.compress.archivers.sevenz.SevenZArchiveEntry;
import org.apache.commons.compress.archivers.sevenz.SevenZFile;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import io.lerk.lrkFM.Handler;
import io.lerk.lrkFM.consts.FileType;
import io.lerk.lrkFM.entities.FMFile;
import io.lerk.lrkFM.exceptions.BlockingStuffOnMainThreadException;

/**
 * Archive utility class.
 *
 * @author Lukas Fülling (lukas@k40s.net)
 */
class ArchiveUtil {

    static {
        System.loadLibrary("sevenzipjbinding");
    }

    private static final String TAG = ArchiveUtil.class.getCanonicalName();

    boolean doExtractArchive(String destination, FMFile f) throws BlockingStuffOnMainThreadException {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            throw new BlockingStuffOnMainThreadException();
        }
        AtomicBoolean result = new AtomicBoolean(false);

        FileType fileType = f.getFileType();
        if (fileType != FileType.ARCHIVE_P7Z) {
            if(fileType == FileType.ARCHIVE_RAR) {
                fileType.newHandler(fmFile -> {
                    File archiveFile = fmFile.getFile();
                    File destinationFile = new File(destination);
                    if (!destinationFile.exists()) {
                        if (destinationFile.mkdirs()) {
                            Log.i(TAG, "Folder created: '" + destinationFile.getAbsolutePath() + "'");
                        } else {
                            Log.e(TAG, "Unable to create folder: '" + destinationFile.getAbsolutePath() + "'");
                        }
                    }
                    try {
                        Junrar.extract(archiveFile, destinationFile);
                        result.set(true);
                    } catch (RarException | IOException e) {
                        Log.e(TAG, "Unable to extract rar file '" + fmFile.getAbsolutePath() + "'!", e);
                        result.set(false);
                    }
                }).handle(f);
            } else {
                // handle archives using commons compress
                fileType.newHandler(getArchiveExtractionCallback(destination, result, fileType)).handle(f);
            }
        } else {
            fileType.newHandler(fi -> {
                SevenZFile sevenZFile;
                try {
                    sevenZFile = new SevenZFile(fi.getFile());
                    SevenZArchiveEntry entry;
                    while ((entry = sevenZFile.getNextEntry()) != null) {
                        if (entry.isDirectory()) {
                            continue;
                        }
                        File curfile = new File(destination, entry.getName());
                        File parent = curfile.getParentFile();
                        if (parent != null) {
                            if (!parent.exists()) {
                                if (parent.mkdirs()) {
                                    Log.i(TAG, "Folder created: '" + parent.getAbsolutePath() + "'");
                                } else {
                                    Log.e(TAG, "Unable to create folder: '" + parent.getAbsolutePath() + "'");
                                }
                            }
                            FileOutputStream out = new FileOutputStream(curfile);
                            byte[] content = new byte[(int) entry.getSize()];
                            sevenZFile.read(content, 0, content.length);
                            out.write(content);
                            out.close();
                        } else {
                            throw new IOException("Unable to get parent file for '" + curfile.getAbsolutePath() + "'!");
                        }
                    }
                    result.set(true);
                } catch (IOException e) {
                    Log.e(TAG, "Error extracting " + fileType.getExtension());
                    result.set(false);
                }
            }).handle(f);
        }
        return result.get();
    }

    private Handler<FMFile> getArchiveExtractionCallback(String path, AtomicBoolean result, FileType fileType) {
        return fi -> {
            try (InputStream is = new FileInputStream(fi.getFile())) {

                ArchiveInputStream ais = new ArchiveStreamFactory().createArchiveInputStream(fileType.getExtension(), is);
                ZipEntry entry;

                while ((entry = (ZipArchiveEntry) ais.getNextEntry()) != null) {

                    if (entry.getName().endsWith("/")) {
                        File dir = new File(path + File.separator + entry.getName());
                        if (!dir.exists()) {
                            //noinspection ResultOfMethodCallIgnored
                            dir.mkdirs();
                        }
                        continue;
                    }

                    File outFile = new File(path + File.separator + entry.getName());

                    if (outFile.isDirectory()) {
                        continue;
                    }

                    if (outFile.exists()) {
                        continue;
                    }

                    try (FileOutputStream out = new FileOutputStream(outFile)) {
                        byte[] buffer = new byte[1024];
                        //noinspection UnusedAssignment
                        int length = 0;
                        while ((length = ais.read(buffer)) > 0) {
                            out.write(buffer, 0, length);
                            out.flush();
                        }
                    }

                    result.set(true);
                }
            } catch (IOException | ArchiveException e) {
                Log.e(TAG, "Error extracting " + fileType.getExtension(), e);
                result.set(false);
            }

        };
    }

    boolean doCreateZip(CopyOnWriteArrayList<FMFile> files, File destination) throws BlockingStuffOnMainThreadException {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            throw new BlockingStuffOnMainThreadException();
        }
        try {
            FileOutputStream fos = new FileOutputStream(destination);
            ZipOutputStream zipOut = new ZipOutputStream(fos);
            for (FMFile f : files) {
                addFileToZip(f.getFile(), f.getName(), zipOut);
            }
            zipOut.close();
            fos.close();
            return true;
        } catch (IOException e) {
            Log.e(TAG, "unable to create zip file!", e);
        }
        return false;
    }

    /**
     * Adds a file to a ZipInputStream. Also walks subdirectories.
     *
     * @param f    the file
     * @param name the filename
     * @param zos  the ZipOutputStream
     * @throws IOException when there is an error
     */
    private static void addFileToZip(File f, String name, ZipOutputStream zos) throws IOException {
        if (f.isDirectory()) {
            File[] children = f.listFiles();
            for (File childFile : children) {
                addFileToZip(childFile, name + "/" + childFile.getName(), zos);
            }
            return;
        }
        FileInputStream fis = new FileInputStream(f);
        ZipEntry zipEntry = new ZipEntry(name);
        zos.putNextEntry(zipEntry);
        byte[] bytes = new byte[1024];
        int length;
        while ((length = fis.read(bytes)) >= 0) {
            zos.write(bytes, 0, length);
        }
        fis.close();
    }
}
