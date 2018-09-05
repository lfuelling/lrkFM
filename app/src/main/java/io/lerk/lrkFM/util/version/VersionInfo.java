package io.lerk.lrkFM.util.version;

import io.lerk.lrkFM.Handler;

public class VersionInfo {

    private static final String TAG = VersionInfo.class.getCanonicalName();

    private final Version current, latest;

    private static VersionInfo instance;

    private VersionInfo(Version current, Version latest) {
        this.current = current;
        this.latest = latest;
    }

    public Version getCurrent() {
        return current;
    }

    public Version getLatest() {
        return latest;
    }

    public static void parse(Handler<VersionInfo> callback, String current, String latest) {
        if (instance != null) {
            callback.handle(instance);
        } else {
            callback.handle(new VersionInfo(Version.fromString(current), Version.fromString(latest)));
        }
    }



}
