package io.lerk.lrkFM.tasks;

import android.os.AsyncTask;
import android.util.Log;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;

import io.lerk.lrkFM.Handler;

/**
 * {@link AsyncTask} that is called when the app checks for a new version.
 *
 * @author Lukas Fülling (lukas@k40s.net)
 */
public class VersionCheckTask extends AsyncTask<Void, String, String> {

    /**
     * Tag for logging.
     */
    public static final String TAG = VersionCheckTask.class.getCanonicalName();

    /**
     * Notification id.
     */
    public static final int NEW_VERSION_NOTIF = 42;

    /**
     * The callback {@link Handler}.
     */
    private final Handler<String> handler;

    public VersionCheckTask(Handler<String> handler) {
        this.handler = handler;
    }

    /**
     * Parses the version from the lrkFM PlayStore page.
     *
     * @param voids the void params (unused)
     * @return the new version as string.
     */
    @Override
    protected String doInBackground(Void... voids) {
        String newVersion = null;

        try {
            Document document = Jsoup.connect("https://play.google.com/store/apps/details?id=io.lerk.lrkFM&hl=en")
                    .timeout(30000)
                    .userAgent("Mozilla/5.0 (Linux; Android;) JSoup/1.8.3")
                    .referrer("https://play.google.com/store/apps/details?id=io.lerk.lrkFM")
                    .get();
            if (document != null) {
                Elements element = document.getElementsContainingOwnText("Current Version");
                for (Element ele : element) {
                    if (ele.siblingElements() != null) {
                        Elements sibElemets = ele.siblingElements();
                        for (Element sibElemet : sibElemets) {
                            newVersion = sibElemet.text();
                        }
                    }
                }
            } else {
                Log.e(TAG, "Unable to read version, document is null!");
            }
        } catch (IOException e) {
            Log.e(TAG, "Network error while fetching version!", e);
        }
        return newVersion;
    }

    /**
     * Calls the {@link Handler}.
     *
     * @param onlineVersion the current version.
     */
    @Override
    protected void onPostExecute(String onlineVersion) {
        handler.handle(onlineVersion);
    }


}
