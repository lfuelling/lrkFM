package io.lerk.lrkFM;

import android.app.Application;
import android.content.Context;

import java.lang.ref.WeakReference;

/**
 * Main application class.
 *
 * @author Lukas Fülling (lukas@k40s.net)
 */
public class LrkFMApp extends Application {

    public static final String CHANNEL_ID = "lrkFM";
    private static WeakReference<Context> context;

    @Override
    public void onCreate() {
        super.onCreate();
        context = new WeakReference<>(this);
    }

    public static Context getContext() {
        return context.get();
    }


}
