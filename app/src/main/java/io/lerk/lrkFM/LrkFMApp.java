package io.lerk.lrkFM;

import android.content.Context;
import android.support.multidex.MultiDexApplication;

import java.lang.ref.WeakReference;

public class LrkFMApp extends MultiDexApplication {

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
