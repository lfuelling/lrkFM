package io.lerk.lrkFM.operations;

import android.app.AlertDialog;
import android.os.Build;
import android.util.Log;
import android.widget.EditText;
import android.widget.Toast;

import com.github.junrar.extract.ExtractArchive;
import com.google.firebase.crash.FirebaseCrash;
import com.google.firebase.perf.FirebasePerformance;
import com.google.firebase.perf.metrics.Trace;

import org.apache.commons.compress.archivers.sevenz.SevenZArchiveEntry;
import org.apache.commons.compress.archivers.sevenz.SevenZFile;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.apache.commons.compress.utils.IOUtils;
import org.apache.commons.compress.utils.SeekableInMemoryByteChannel;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import io.lerk.lrkFM.R;
import io.lerk.lrkFM.activities.FileActivity;
import io.lerk.lrkFM.entities.FMFile;

import static android.widget.Toast.LENGTH_SHORT;
import static io.lerk.lrkFM.operations.OperationUtil.getFileExistsDialogBuilder;

public class ArchiveUtil {

    private static final String TAG = ArchiveUtil.class.getCanonicalName();

    public static final String RAR_EXTENSION = "rar";
    public static final String ZIP_EXTENSION = "zip";
    public static final String SEVENZIP_EXTENSION = "7z";
    public static final String TAR_EXTENSION = "tar";
    public static final String GZ_EXTENSION = "gz";

    private FileActivity context;

    public ArchiveUtil(FileActivity context) {
        this.context = context;
    }

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

    public boolean extractArchive(String path, FMFile f) {
        boolean result = false;
        switch (f.getExtension()) {
            case RAR_EXTENSION:
                result = unpackRar(path, f);
                break;
            case ZIP_EXTENSION:
                result = unpackZip(path, f);
                break;
            case SEVENZIP_EXTENSION:
                result = unpack7Zip(path, f);
                break;
            case TAR_EXTENSION:
                result = unpackTar(path, f, GZ_EXTENSION.equals(f.getExtension()));
                break;
            default:
                Toast.makeText(context, R.string.unable_to_recognize_archive_format, Toast.LENGTH_LONG).show();
                break;
        }
        context.clearFileOpCache();
        context.reloadCurrentDirectory();
        return result;
    }

    private boolean unpack7Zip(String path, FMFile f) {
        try {
            SevenZFile sevenZFile = new SevenZFile(f.getFile());
            SevenZArchiveEntry entry;
            while ((entry = sevenZFile.getNextEntry()) != null) {
                if (entry.isDirectory()) {
                    continue;
                }
                File curfile = new File(path + File.separator + entry.getName());
                File parent = curfile.getParentFile();
                if (!parent.exists()) {
                    if (parent.mkdirs()) {
                        Log.d(TAG, "extraction of parent dir successful");
                    } else {
                        Log.w(TAG, "extraction of parent dir unsuccessful");
                    }
                }
                FileOutputStream out = new FileOutputStream(curfile);
                byte[] content = new byte[(int) entry.getSize()];
                sevenZFile.read(content, 0, content.length);
                out.write(content);
                out.close();
            }
            return true;
        } catch (IOException e) {
            Log.e(TAG, "Unable to read 7zip");
            return false;
        }
    }


    private boolean unpackTar(String path, FMFile f, boolean isGzipped) {
        try (TarArchiveInputStream fin = new TarArchiveInputStream((isGzipped) ? new FileInputStream(f.getFile()) : new GzipCompressorInputStream(new FileInputStream(f.getFile())))) {
            TarArchiveEntry entry;
            while ((entry = fin.getNextTarEntry()) != null) {
                if (entry.isDirectory()) {
                    continue;
                }
                File curfile = new File(path + File.separator + entry.getName());
                File parent = curfile.getParentFile();
                if (!parent.exists()) {
                    if (parent.mkdirs()) {
                        Log.d(TAG, "extraction of parent dir successful");
                    } else {
                        Log.w(TAG, "extraction of parent dir unsuccessful");
                    }
                }
                IOUtils.copy(fin, new FileOutputStream(curfile));
            }
            return true;
        } catch (IOException e) {
            Log.e(TAG, "Unable to read 7zip");
            return false;
        }
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

    public boolean createZipFile(ArrayList<FMFile> files, AlertDialog d) {
        Log.d(TAG, "Creating ZIP...");

        EditText editText = d.findViewById(R.id.destinationName);
        String fileName = editText.getText().toString();
        if (fileName.isEmpty() || fileName.startsWith("/")) {
            Toast.makeText(context, R.string.err_invalid_input_zip, LENGTH_SHORT).show();
            return false;
        } else if (!fileName.endsWith(".zip")) {
            fileName = fileName + ".zip";
        }

        File destination = new File(context.getCurrentDirectory() + "/" + fileName);
        final boolean[] success = {false};
        if (destination.exists()) {
            AlertDialog.Builder builder = getFileExistsDialogBuilder(context);
            final File tdest = destination; //for lambda
            builder.setOnDismissListener(dialogInterface -> success[0] = doCreateZipNoValidation(files, tdest))
                    .setOnCancelListener(dialogInterface -> success[0] = false).show();
        } else {
            success[0] = doCreateZipNoValidation(files, destination);
        }
        context.clearFileOpCache();
        context.reloadCurrentDirectory();
        return success[0];
    }

    private boolean doCreateZipNoValidation(ArrayList<FMFile> files, File destination) {
        Trace trace = FirebasePerformance.getInstance().newTrace("create_zip");
        trace.start();
        try {
            FileOutputStream fos = new FileOutputStream(destination);
            ZipOutputStream zipOut = new ZipOutputStream(fos);
            for (FMFile f : files) {
                addFileToZip(f.getFile(), f.getName(), zipOut);
            }
            zipOut.close();
            fos.close();
            trace.putAttribute("success", "true");
            trace.stop();
            return true;
        } catch (IOException e) {
            Toast.makeText(context, R.string.unable_to_create_zip_file, Toast.LENGTH_LONG).show();
            FirebaseCrash.report(e);
            trace.putAttribute("success", "false");
            trace.stop();
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
