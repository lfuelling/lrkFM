package io.lerk.lrkFM;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Environment;
import android.os.StatFs;
import android.provider.MediaStore;
import android.provider.OpenableColumns;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Some helper methods for FS queries.
 *
 * @author Lukas Fülling (lukas@k40s.net)
 * @author Tom Susel (https://gist.github.com/toms972)
 */
public class DiskUtil {
    /*
                              !         !
                             ! !       ! !
                            ! . !     ! . !
                             ! ^^^^^^^^^ ^
                             ^             ^
                           ^  (0)       (0)  ^
                          ^        ""         ^
                         ^   ***************    ^
                       ^   *                 *   ^
                      ^   *   /\   /\   /\    *    ^
                     ^   *                     *    ^
                    ^   *   /\   /\   /\   /\   *    ^
                   ^   *                         *    ^
                   ^  *                           *   ^
                   ^  *                           *   ^
                    ^ *                           *  ^
                     ^*                           * ^
                      ^ *                        * ^
                      ^  *                      *  ^
                        ^  *       ) (         * ^
                            ^^^^^^^^ ^^^^^^^^^
    */
    private static final long GIBIBYTE = 1073741824; //Ghiblibyte

    private static final long MEBIBYTE = 1048576;

    private static final String TAG = DiskUtil.class.getCanonicalName();

    /**
     * Calculates total space on disk
     *
     * @param external If true will query external disk, otherwise will query internal disk.
     * @return Number of mega bytes on disk.
     */
    public static int totalSpaceMebi(boolean external) {
        StatFs statFs = getStats(external);
        long total = (statFs.getBlockCountLong() * statFs.getBlockSizeLong()) / MEBIBYTE;
        Log.d(TAG, "Total disk space: " + String.valueOf(total));
        return (int) total;
    }

    /**
     * Calculates total space on disk
     *
     * @param external If true will query external disk, otherwise will query internal disk.
     * @return Number of mega bytes on disk.
     */
    public static int totalSpaceGibi(boolean external) {
        StatFs statFs = getStats(external);
        long total = (statFs.getBlockCountLong() * statFs.getBlockSizeLong()) / GIBIBYTE;
        Log.d(TAG, "Total disk space: " + String.valueOf(total));
        return (int) total;
    }

    /**
     * Calculates free space on disk
     *
     * @param external If true will query external disk, otherwise will query internal disk.
     * @return Number of free mega bytes on disk.
     */
    public static int freeSpaceMebi(boolean external) {
        StatFs statFs = getStats(external);
        long availableBlocks = statFs.getAvailableBlocksLong();
        long blockSize = statFs.getBlockSizeLong();
        long freeBytes = availableBlocks * blockSize;
        Log.d(TAG, "Free disk space: " + String.valueOf(freeBytes / MEBIBYTE));
        return (int) (freeBytes / MEBIBYTE);
    }

    /**
     * Calculates free space on disk
     *
     * @param external If true will query external disk, otherwise will query internal disk.
     * @return Number of free mega bytes on disk.
     */
    public static int freeSpaceGibi(boolean external) {
        StatFs statFs = getStats(external);
        long availableBlocks = statFs.getAvailableBlocksLong();
        long blockSize = statFs.getBlockSizeLong();
        long freeBytes = availableBlocks * blockSize;
        Log.d(TAG, "Free disk space: " + String.valueOf(freeBytes / GIBIBYTE));
        return (int) (freeBytes / GIBIBYTE);
    }

    /**
     * Calculates occupied space on disk
     *
     * @param external If true will query external disk, otherwise will query internal disk.
     * @return Number of occupied mega bytes on disk.
     */
    public static int busySpaceMebi(boolean external) {
        StatFs statFs = getStats(external);
        long total = (statFs.getBlockCountLong() * statFs.getBlockSizeLong());
        long free = (statFs.getAvailableBlocksLong() * statFs.getBlockSizeLong());
        long used = (total - free) / MEBIBYTE;
        Log.d(TAG, "used disk space: " + String.valueOf(used / MEBIBYTE));
        return (int) used;
    }

    /**
     * Calculates occupied space on disk
     *
     * @param external If true will query external disk, otherwise will query internal disk.
     * @return Number of occupied mega bytes on disk.
     */
    public static int busySpaceGibi(boolean external) {
        StatFs statFs = getStats(external);
        long total = (statFs.getBlockCountLong() * statFs.getBlockSizeLong());
        long free = (statFs.getAvailableBlocksLong() * statFs.getBlockSizeLong());
        long used = (total - free) / GIBIBYTE;
        Log.d(TAG, "used disk space: " + String.valueOf(used / GIBIBYTE));
        return (int) used;
    }

    /**
     * Returns the file system stats.
     *
     * @param external if true, shows stats of external storage
     * @return the file system stats
     */
    private static StatFs getStats(boolean external) {
        String path;

        if (external) {
            path = Environment.getExternalStorageDirectory().getAbsolutePath();
        } else {
            path = Environment.getRootDirectory().getAbsolutePath();
        }

        return new StatFs(path);
    }
}
