package io.lerk.lrkFM.op;

import android.app.AlertDialog;
import android.util.Log;
import android.widget.EditText;
import android.widget.Toast;

import org.apache.commons.compress.archivers.ArchiveException;
import org.apache.commons.compress.archivers.ArchiveInputStream;
import org.apache.commons.compress.archivers.ArchiveStreamFactory;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import io.lerk.lrkFM.R;
import io.lerk.lrkFM.activities.FileActivity;
import io.lerk.lrkFM.consts.FileType;
import io.lerk.lrkFM.entities.FMFile;

import static android.widget.Toast.LENGTH_SHORT;
import static io.lerk.lrkFM.op.OperationUtil.getFileExistsDialogBuilder;

public class ArchiveUtil {

    private static final String TAG = ArchiveUtil.class.getCanonicalName();

    private FileActivity context;

    public ArchiveUtil(FileActivity context) {
        this.context = context;
    }

    public boolean extractArchive(String path, FMFile f) {
        AtomicBoolean result = new AtomicBoolean(false);

        FileType fileType = f.getFileType();
        fileType.newHandler(fi -> {
            try {
                InputStream is = new FileInputStream(fi.getFile());
                ArchiveInputStream ais = new ArchiveStreamFactory().createArchiveInputStream(fileType.getExtension(), is);
                ZipEntry entry = null;

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

                    FileOutputStream out = new FileOutputStream(outFile);
                    try {
                        byte[] buffer = new byte[1024];
                        //noinspection UnusedAssignment
                        int length = 0;
                        while ((length = ais.read(buffer)) > 0) {
                            out.write(buffer, 0, length);
                            out.flush();
                        }
                        out.close();
                    } catch (IOException e){
                        out.close();
                    }

                    result.set(true);
                }
            } catch (IOException | ArchiveException e) {
                Log.e(TAG, "Error extracting " + fileType.getExtension());
                result.set(false);
            }

        }).handle(f);

        context.clearFileOpCache();
        context.reloadCurrentDirectory();
        return result.get();
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
            Toast.makeText(context, R.string.unable_to_create_zip_file, Toast.LENGTH_LONG).show();
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

    /**
     * @param archive the archive
     * @param path the path (can be inside the archive)
     * @return path contents
     * @deprecated use {@link io.lerk.lrkFM.entities.FMArchive}
     */
    public ArrayList<FMFile> loadArchiveContents(FMFile archive, String path){

        if(path == null || path.isEmpty()) {
            path = "/";
        }
        ArrayList<FMFile> res = new ArrayList<>();
        FileType fileType = archive.getFileType();
        final String finalPath = path;
        fileType.newHandler(fi -> {
            try {
                InputStream is = new FileInputStream(fi.getFile());
                ArchiveInputStream ais = new ArchiveStreamFactory().createArchiveInputStream(fileType.getExtension(), is);
                ZipEntry entry;
                while ((entry = (ZipArchiveEntry) ais.getNextEntry()) != null) {
                    File outFile = new File(entry.getName());
                    if(!entry.getName().contains(finalPath)) {
                        continue;
                    }
                    res.add(new FMFile(outFile));
                }
            } catch (IOException | ArchiveException e) {
                Log.e(TAG, "Error extracting " + fileType.getExtension());
            }
        }).handle(archive);
        return res;
    }
}
