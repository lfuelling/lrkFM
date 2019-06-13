package io.lerk.lrkFM.tasks.operation;

import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;

/**
 * This class contains utility methods that will be removed once {@link android.os.Build.VERSION_CODES#N}
 * has a high enough user acceptance to make it the minimum required version.
 *
 * @author Lukas Fülling (lukas@k40s.net)
 * @see OperationUtil
 */
public class CompatOperations {

    public static final String TAG = CompatOperations.class.getCanonicalName();

    static void copyFile(File file, File destination) throws IOException {
        if (destination.isDirectory()) {
            destination = new File(destination.getAbsolutePath() + "/" + file.getName());
        }
        if (destination.getParentFile().mkdirs()) {
            Log.d(TAG, "mkdirs was necessary!");
        }
        FileChannel src = new FileInputStream(file).getChannel();
        FileChannel dest = new FileOutputStream(destination).getChannel();
        dest.transferFrom(src, 0, src.size());
    }

    static void moveFile(File file, File destination) throws IOException {
        copyFile(file, destination);
        if (!file.delete()) { // delete file
            Log.w(TAG, "Unable to remove source!");
        }
    }

    static void copyDirectory(File file, File destination) throws IOException {
        for (File f : file.listFiles()) {
            File dest = new File(destination.getAbsolutePath() + "/" + f.getName());
            if (f.isDirectory()) {
                copyDirectory(f, dest);
            } else {
                copyFile(f, dest);
            }
        }
    }

    static void moveDirectory(File file, File destination) throws IOException {
        for (File f : file.listFiles()) {
            File dest = new File(destination.getAbsolutePath() + "/" + f.getName());
            if (f.isDirectory()) {
                moveDirectory(f, dest);
            } else {
                moveFile(f, dest);
            }
        }
    }
}
