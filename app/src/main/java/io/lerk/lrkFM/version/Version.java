package io.lerk.lrkFM.version;

import android.support.annotation.NonNull;

public class Version {
    private final Integer major, minor, patch, sub;
    private final Boolean beta, rc;

    private static final String SPLIT_BETA = "b";
    private static final String SPLIT_RC = "rc";

    private Version(Integer major, Integer minor, Integer patch, Integer sub, Boolean beta, Boolean rc) {
        this.major = major;
        this.minor = minor;
        this.patch = patch;
        this.sub = sub;
        this.beta = beta;
        this.rc = rc;
    }

    static Version fromString(String versionString) throws NumberFormatException {
        String[] versionSplit = versionString.split("\\.");
        String[] patchSplit;

        boolean beta = versionString.split(SPLIT_BETA).length > 1;
        boolean rc = versionString.split(SPLIT_RC).length > 1;

        try {
            if (beta) {
                patchSplit = versionSplit[(versionSplit.length > 2) ? 2 : 1].split(SPLIT_BETA);
            } else if (rc) {
                patchSplit = versionSplit[(versionSplit.length > 2) ? 2 : 1].split(SPLIT_RC);
            } else {
                patchSplit = new String[]{"0", "0"};
            }
        } catch (IndexOutOfBoundsException e) {
            patchSplit = new String[]{"0", "0"};
        }

        return new Version(
                Integer.parseInt(versionSplit[0]),
                Integer.parseInt(versionSplit[1].split(SPLIT_BETA)[0].split(SPLIT_RC)[0]),
                Integer.parseInt(patchSplit[0]),
                Integer.parseInt(patchSplit[1]),
                beta,
                rc
        );
    }

    public boolean newerThan(Version v) {
        if (this.major > v.major) {
            return true;
        } else if (this.major.equals(v.major)) {
            if (this.minor > v.minor) {
                return true;
            } else if (this.minor.equals(v.minor)) {
                if (this.patch > v.patch) {
                    return true;
                } else if (this.patch.equals(v.patch)) {
                    return this.sub > v.sub;
                }
            }
        }
        return false;
    }

    @NonNull
    @Override
    public String toString() {
        return major + "." + minor + "." + patch +
                ((beta) ? SPLIT_BETA +
                        ((sub > 9) ? sub : "0" + sub) : (rc) ? SPLIT_RC +
                        ((sub > 9) ? sub : "0" + sub) : "latest");
        // nosleep
    }

    public Integer getMajor() {
        return major;
    }

    public Integer getMinor() {
        return minor;
    }

    public Integer getPatch() {
        return patch;
    }

    public Integer getSub() {
        return sub;
    }

    public Boolean getBeta() {
        return beta;
    }

    public Boolean getRc() {
        return rc;
    }
}
