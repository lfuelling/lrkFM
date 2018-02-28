package io.lerk.lrkFM.activities.file;

import android.app.AlertDialog;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.util.Log;
import android.widget.EditText;
import android.widget.Toast;

import com.google.firebase.FirebaseException;
import com.google.firebase.crash.FirebaseCrash;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import io.lerk.lrkFM.R;
import io.lerk.lrkFM.entities.FMFile;

import static android.widget.Toast.LENGTH_SHORT;

/**
 * Utility class for file operations.
 *
 * @author Lukas Fülling (lukas@k40s.net)
 */
public class OperationUtil {

    private static final String TAG = OperationUtil.class.getCanonicalName();

    public static boolean copy(FMFile f, FileActivity context, @Nullable AlertDialog d) {
        Log.d(TAG, "Starting copy...");

        String pathname;

        if (d != null) {
            EditText editText = d.findViewById(R.id.destinationPath);
            pathname = editText.getText().toString();
            if (pathname.isEmpty()) {
                Toast.makeText(context, R.string.err_empty_input, LENGTH_SHORT).show();
                return false;
            }
        } else {
            pathname = context.getCurrentDirectory();
        }

        File destination = new File(pathname);
        final boolean[] success = {false};
        if (destination.isDirectory() && !f.isDirectory()) {
            destination = new File(destination.getAbsolutePath() + File.separator + f.getName());
        }
        if (destination.exists()) {
            AlertDialog.Builder builder = getFileExistsDialogBuilder(context);
            final File tdest = destination; //for lambda
            builder.setOnDismissListener(dialogInterface -> success[0] = doCopyNoValidation(f, tdest))
                    .setOnCancelListener(dialogInterface -> success[0] = false).show();
        } else {
            success[0] = doCopyNoValidation(f, destination);
        }
        context.clearFileOpCache();
        context.reloadCurrentDirectory();
        return success[0];
    }

    public static boolean move(FMFile f, FileActivity context, @Nullable AlertDialog d) {
        Log.d(TAG, "Starting Move...");

        String pathname;

        if (d != null) {
            EditText editText = d.findViewById(R.id.destinationPath);
            pathname = editText.getText().toString();
            if (pathname.isEmpty()) {
                Toast.makeText(context, R.string.err_empty_input, LENGTH_SHORT).show();
                return false;
            }
        } else {
            pathname = context.getCurrentDirectory();
        }

        File destination = new File(pathname);
        final boolean[] success = {false};
        if (destination.isDirectory() && !f.isDirectory()) {
            destination = new File(destination.getAbsolutePath() + File.separator + f.getName());
        }
        if (!destination.isDirectory() && destination.exists()) {
            AlertDialog.Builder builder = getFileExistsDialogBuilder(context);
            final File tdest = destination; //for lambda
            builder.setOnDismissListener(dialogInterface -> success[0] = doMoveNoValidation(f, tdest))
                    .setOnCancelListener(dialogInterface -> success[0] = false).show();
        } else {
            success[0] = doMoveNoValidation(f, destination);
        }
        context.clearFileOpCache();
        context.reloadCurrentDirectory();
        return success[0];

    }


    public static boolean rename(FMFile f, FileActivity context, AlertDialog d) {
        Log.d(TAG, "Starting rename...");
        EditText editText = d.findViewById(R.id.destinationName);
        String newName = editText.getText().toString();
        if (newName.isEmpty()) {
            Toast.makeText(context, R.string.err_empty_input, LENGTH_SHORT).show();
            return false;
        } else {
            final boolean[] success = {false};
            File destination = new File(getFullPathForRename(f, newName));
            if (destination.exists()) {
                AlertDialog.Builder builder = getFileExistsDialogBuilder(context);
                final File tdest = destination; //for lambda
                builder.setOnDismissListener(dialogInterface -> success[0] = doMoveNoValidation(f, tdest))
                        .setOnCancelListener(dialogInterface -> success[0] = false).show();
            } else {
                success[0] = doMoveNoValidation(f, destination);
            }
            context.clearFileOpCache();
            context.reloadCurrentDirectory();
            return success[0];
        }
    }


    public static boolean deleteNoValidation(FMFile f) {
        return f.getFile().exists() && f.getFile().delete();
    }


    private static boolean doCopyNoValidation(FMFile f, File d) {
        try {

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                Files.copy(f.getFile().toPath(), d.toPath(), StandardCopyOption.REPLACE_EXISTING);
            } else {
                if (f.getFile().isDirectory()) {
                    FileUtils.copyDirectory(f.getFile(), d);
                } else {
                    FileUtils.copyFile(f.getFile(), d);
                }
            }
            return true;
        } catch (IOException e) {
            Log.e(TAG, "Unable to copy file!", e);
            return false;
        }
    }

    private static boolean doMoveNoValidation(FMFile f, File d) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                Files.move(f.getFile().toPath(), d.toPath(), StandardCopyOption.REPLACE_EXISTING);
            } else {
                if (f.getFile().isDirectory()) {
                    FileUtils.moveDirectory(f.getFile(), d);
                } else {
                    FileUtils.moveFile(f.getFile(), d);
                }
            }
            return true;
        } catch (IOException e) {
            Log.e(TAG, "Unable to move file!", e);
            return false;
        }
    }

    private static AlertDialog.Builder getFileExistsDialogBuilder(FileActivity context) {
        return new AlertDialog.Builder(context)
                .setCancelable(true)
                .setTitle(R.string.warn_file_exists_title)
                .setMessage(R.string.warn_file_exists_msg)
                .setPositiveButton(R.string.yes, (dialogInterface, i) -> dialogInterface.dismiss())
                .setNegativeButton(R.string.cancel, (dialogInterface, i) -> dialogInterface.cancel());
    }

    @NonNull
    private static String getFullPathForRename(FMFile f, String newName) {
        final String[] path = {""};
        ArrayList<String> pathSplit = new ArrayList<>(Arrays.asList(f.getFile().getAbsolutePath().split(File.separator)));
        pathSplit.remove(pathSplit.size() - 1);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            pathSplit.forEach(s -> path[0] += s + File.separator);
        } else { // Ohooold version uuusers, uuupdate youuur phoooooone......
            for (String s : pathSplit) {
                path[0] += s + File.separator;
            }
        }
        return path[0] + newName;
    }

    public static void newDir(File d, FileActivity context) {
        if (!d.exists()) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    Files.createDirectory(d.toPath());
                } else {
                    if (!d.mkdirs()) {
                        Toast.makeText(context, R.string.err_unable_to_mkdir, Toast.LENGTH_LONG).show();
                    }
                }
            } catch (IOException e) {
                Log.e(TAG, context.getString(R.string.err_unable_to_mkdir), e);
            }
        } else {
            Toast.makeText(context, R.string.err_file_exists, Toast.LENGTH_LONG).show();
        }
    }

    public static boolean createZipFile(ArrayList<FMFile> files, FileActivity context, AlertDialog d) {
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
            builder.setOnDismissListener(dialogInterface -> success[0] = doCreateZipNoValidation(files, tdest, context))
                    .setOnCancelListener(dialogInterface -> success[0] = false).show();
        } else {
            success[0] = doCreateZipNoValidation(files, destination, context);
        }
        context.clearFileOpCache();
        context.reloadCurrentDirectory();
        return success[0];
    }

    private static boolean doCreateZipNoValidation(ArrayList<FMFile> files, File destination, FileActivity context) {
        try {
            FileOutputStream fos = new FileOutputStream(destination);
            ZipOutputStream zipOut = new ZipOutputStream(fos);
            for (FMFile f : files) {
                zipFile(f.getFile(), f.getName(), zipOut);
            }
            zipOut.close();
            fos.close();

            return true;
        } catch (IOException e) {
            Toast.makeText(context, R.string.unable_to_create_zip_file, Toast.LENGTH_LONG).show();
            FirebaseCrash.report(e);
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
    private static void zipFile(File f, String name, ZipOutputStream zos) throws IOException {
        if (f.isDirectory()) {
            File[] children = f.listFiles();
            for (File childFile : children) {
                zipFile(childFile, name + "/" + childFile.getName(), zos);
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

    public enum Operation {
        COPY(R.string.copy),
        MOVE(R.string.move),
        EXTRACT(R.string.extract),
        CREATE_ZIP(R.string.new_zip_file),
        NONE(R.string.todo);

        private final int title;

        /**
         * File Operation.
         *
         * @param title May not contain <pre>(</pre>
         * @see FileActivity#onPrepareOptionsMenu
         */
        Operation(@StringRes int title) {
            this.title = title;
        }

        public int getTitle() {
            return title;
        }
    }
}
