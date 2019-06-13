package io.lerk.lrkFM.version;

import io.lerk.lrkFM.Handler;

/**
 * Version singleton.
 *
 * @author Lukas Fülling (lukas@k40s.net)
 */
public class VersionInfo {

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
        if (instance == null) {
            instance = new VersionInfo(Version.fromString(current), Version.fromString(latest));
        }
        callback.handle(instance);
    }

}
