package io.lerk.lrkfm;

import java.util.Date;

/**
 * Created by lfuelling on 15.03.17.
 */

class FMFile {
    String name, permissions;
    Date lastModified;

    public FMFile(String name, String permissionString, Date date) {
        this.name = name;
        this.permissions = permissionString;
        this.lastModified = date;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPermissions() {
        return permissions;
    }

    public void setPermissions(String permissions) {
        this.permissions = permissions;
    }


    public Date getLastModified() {
        return lastModified;
    }

    public void setLastModified(Date lastModified) {
        this.lastModified = lastModified;
    }
}
