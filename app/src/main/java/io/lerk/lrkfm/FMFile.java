package io.lerk.lrkfm;

import java.util.Date;

/**
 * Created by lfuelling on 15.03.17.
 */

class FMFile {
    String name, permissions, owner, group;
    Date lastModified, created;

    public FMFile(String name, String permissions, String owner, String group, Date lastModified, Date created) {
        this.name = name;
        this.permissions = permissions;
        this.owner = owner;
        this.group = group;
        this.lastModified = lastModified;
        this.created = created;
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

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    public String getGroup() {
        return group;
    }

    public void setGroup(String group) {
        this.group = group;
    }

    public Date getLastModified() {
        return lastModified;
    }

    public void setLastModified(Date lastModified) {
        this.lastModified = lastModified;
    }

    public Date getCreated() {
        return created;
    }

    public void setCreated(Date created) {
        this.created = created;
    }
}
