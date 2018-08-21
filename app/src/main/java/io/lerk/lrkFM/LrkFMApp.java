package io.lerk.lrkFM;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;
import android.support.multidex.MultiDexApplication;

import java.lang.ref.WeakReference;
import java.util.Objects;

public class LrkFMApp extends MultiDexApplication {

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

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = getString(R.string.app_name);
            String description = getString(R.string.app_name);
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);

            Objects.requireNonNull(getSystemService(NotificationManager.class)).createNotificationChannel(channel);
        }
    }
}
