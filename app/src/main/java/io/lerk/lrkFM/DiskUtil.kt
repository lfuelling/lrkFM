package io.lerk.lrkFM

import android.os.Environment
import android.os.StatFs
import android.util.Log

/**
 * Some helper methods for FS queries.
 *
 * @author Lukas Fülling (lukas@k40s.net)
 * @author Tom Susel (https://gist.github.com/toms972)
 */
object DiskUtil {
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
    private const val GIBIBYTE: Long = 1073741824 //Ghiblibyte
    private const val MEBIBYTE: Long = 1048576
    private val TAG = DiskUtil::class.java.canonicalName

    /**
     * Calculates total space on disk
     *
     * @param external If true will query external disk, otherwise will query internal disk.
     * @return Number of mega bytes on disk.
     */
    fun totalSpaceMebi(external: Boolean): Int {
        val statFs = getStats(external)
        val total = statFs.blockCountLong * statFs.blockSizeLong / MEBIBYTE
        Log.d(TAG, "Total disk space: $total")
        return total.toInt()
    }

    /**
     * Calculates total space on disk
     *
     * @param external If true will query external disk, otherwise will query internal disk.
     * @return Number of mega bytes on disk.
     */
    fun totalSpaceGibi(external: Boolean): Int {
        val statFs = getStats(external)
        val total = statFs.blockCountLong * statFs.blockSizeLong / GIBIBYTE
        Log.d(TAG, "Total disk space: $total")
        return total.toInt()
    }

    /**
     * Calculates free space on disk
     *
     * @param external If true will query external disk, otherwise will query internal disk.
     * @return Number of free mega bytes on disk.
     */
    fun freeSpaceMebi(external: Boolean): Int {
        val statFs = getStats(external)
        val availableBlocks = statFs.availableBlocksLong
        val blockSize = statFs.blockSizeLong
        val freeBytes = availableBlocks * blockSize
        Log.d(TAG, "Free disk space: " + (freeBytes / MEBIBYTE).toString())
        return (freeBytes / MEBIBYTE).toInt()
    }

    /**
     * Calculates free space on disk
     *
     * @param external If true will query external disk, otherwise will query internal disk.
     * @return Number of free mega bytes on disk.
     */
    fun freeSpaceGibi(external: Boolean): Int {
        val statFs = getStats(external)
        val availableBlocks = statFs.availableBlocksLong
        val blockSize = statFs.blockSizeLong
        val freeBytes = availableBlocks * blockSize
        Log.d(TAG, "Free disk space: " + (freeBytes / GIBIBYTE).toString())
        return (freeBytes / GIBIBYTE).toInt()
    }

    /**
     * Calculates occupied space on disk
     *
     * @param external If true will query external disk, otherwise will query internal disk.
     * @return Number of occupied mega bytes on disk.
     */
    fun busySpaceMebi(external: Boolean): Int {
        val statFs = getStats(external)
        val total = statFs.blockCountLong * statFs.blockSizeLong
        val free = statFs.availableBlocksLong * statFs.blockSizeLong
        val used = (total - free) / MEBIBYTE
        Log.d(TAG, "used disk space: " + (used / MEBIBYTE).toString())
        return used.toInt()
    }

    /**
     * Calculates occupied space on disk
     *
     * @param external If true will query external disk, otherwise will query internal disk.
     * @return Number of occupied mega bytes on disk.
     */
    fun busySpaceGibi(external: Boolean): Int {
        val statFs = getStats(external)
        val total = statFs.blockCountLong * statFs.blockSizeLong
        val free = statFs.availableBlocksLong * statFs.blockSizeLong
        val used = (total - free) / GIBIBYTE
        Log.d(TAG, "used disk space: " + (used / GIBIBYTE).toString())
        return used.toInt()
    }

    /**
     * Returns the file system stats.
     *
     * @param external if true, shows stats of external storage
     * @return the file system stats
     */
    private fun getStats(external: Boolean): StatFs {
        val path: String
        path = if (external) {
            Environment.getExternalStorageDirectory().absolutePath
        } else {
            Environment.getRootDirectory().absolutePath
        }
        return StatFs(path)
    }
}
