package io.lerk.lrkFM.tasks

import android.os.AsyncTask
import android.util.Log

import org.jsoup.Jsoup
import java.io.IOException

/**
 * [AsyncTask] that is called when the app checks for a new version.
 *
 * @author Lukas Fülling (lukas@k40s.net)
 */
class VersionCheckTask(
    /**
     * The callback handler.
     */
    private val handler: (String) -> Unit
) : AsyncTask<Void?, String?, String?>() {
    /**
     * Parses the version from the lrkFM PlayStore page.
     *
     * @param params the void params (unused)
     * @return the new version as string.
     */
    @Deprecated("Deprecated in Java")
    override fun doInBackground(vararg params: Void?): String? {
        var newVersion: String? = null
        try {
            val document =
                Jsoup.connect("https://play.google.com/store/apps/details?id=io.lerk.lrkFM&hl=en")
                    .timeout(30000)
                    .userAgent("Mozilla/5.0 (Linux; Android;) JSoup/1.8.3")
                    .referrer("https://play.google.com/store/apps/details?id=io.lerk.lrkFM")
                    .get()
            val element = document.getElementsContainingOwnText("Current Version")
            for (ele in element) {
                val sibElemets = ele.siblingElements()
                for (sibElemet in sibElemets) {
                    newVersion = sibElemet.text()
                }
            }
        } catch (e: IOException) {
            Log.e(TAG, "Network error while fetching version!", e)
        }
        return newVersion
    }

    /**
     * Calls the handler.
     *
     * @param onlineVersion the current version.
     */
    override fun onPostExecute(onlineVersion: String?) {
        handler(onlineVersion!!)
    }

    companion object {
        /**
         * Tag for logging.
         */
        val TAG = VersionCheckTask::class.java.canonicalName

        /**
         * Notification id.
         */
        const val NEW_VERSION_NOTIF = 42
    }
}
