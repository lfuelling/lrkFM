package io.lerk.lrkFM.util;

import android.app.AlertDialog;
import android.support.annotation.NonNull;
import android.util.Log;
import android.widget.Toast;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

import io.lerk.lrkFM.R;
import io.lerk.lrkFM.activities.FileActivity;
import io.lerk.lrkFM.entities.FMFile;

/**
 * @author Lukas Fülling (lukas@k40s.net)
 */

public class FileUtil {

    private static final String TAG = FileUtil.class.getCanonicalName();

    public static boolean copy(FMFile f, FileActivity context, File destination) {
        final boolean[] success = {false};
        if (destination.isDirectory() && !f.getDirectory()) {
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
        return success[0];
    }

    public static boolean move(FMFile f, FileActivity context, File destination) {
        final boolean[] success = {false};
        if (destination.isDirectory() && !f.getDirectory()) {
            destination = new File(destination.getAbsolutePath() + File.separator + f.getName());
        }
        if (destination.exists()) {
            AlertDialog.Builder builder = getFileExistsDialogBuilder(context);
            final File tdest = destination; //for lambda
            builder.setOnDismissListener(dialogInterface -> success[0] = doMoveNoValidation(f, tdest))
                    .setOnCancelListener(dialogInterface -> success[0] = false).show();
        } else {
            success[0] = doMoveNoValidation(f, destination);
        }
        return success[0];
    }


    public static boolean rename(FMFile f, FileActivity context, String newName) {
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
        return success[0];
    }


    public static boolean deleteNoValidation(FMFile f) {
        final boolean res;
        if (!f.getFile().exists()) {
            res = false;
        } else {
            res = f.getFile().delete();
        }
        return res;
    }


    private static boolean doCopyNoValidation(FMFile f, File d) {
        try {
            // TODO: use when O is backwards compatible
           // if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
           //     Files.copy(f.getFile().toPath(), d.toPath(), StandardCopyOption.REPLACE_EXISTING);
           //} else {
                if (f.getFile().isDirectory()) {
                    FileUtils.copyDirectory(f.getFile(), d);
                } else {
                    FileUtils.copyFile(f.getFile(), d);
                }
           // }
            return true;
        } catch (IOException e) {
            Log.e(TAG, "Unable to copy file!", e);
            return false;
        }
    }

    private static boolean doMoveNoValidation(FMFile f, File d) {
        try {
            // TODO: use when O is backwards compatible
           // if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
           //     Files.move(f.getFile().toPath(), d.toPath(), StandardCopyOption.REPLACE_EXISTING);
           // } else {
                if (f.getFile().isDirectory()) {
                    FileUtils.moveDirectory(f.getFile(), d);
                } else {
                    FileUtils.moveFile(f.getFile(), d);
                }
           // }
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
        pathSplit.forEach(s -> path[0] += s + File.separator);
        return path[0] + newName;
    }

    public static void newDir(File d, FileActivity context) {
        if (!d.exists()) {
            //try {
                // TODO: use when O is backwards compatible
               // if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
               //     Files.createDirectory(d.toPath());
               // } else {
                    if (!d.mkdirs()) {
                        Toast.makeText(context, R.string.err_unable_to_mkdir, Toast.LENGTH_LONG).show();
                    }
                //}
            //} catch (IOException e) {
            //    Log.e(TAG, context.getString(R.string.err_unable_to_mkdir), e);
            //}
        } else {
            Toast.makeText(context, R.string.err_file_exists, Toast.LENGTH_LONG).show();
        }
    }
}
