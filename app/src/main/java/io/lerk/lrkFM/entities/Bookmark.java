package io.lerk.lrkFM.entities;

import android.view.MenuItem;

/**
 * Bookmark item.
 *
 * @author Lukas Fülling (lukas@k40s.net)
 */
public class Bookmark {
    private String path, label;
    private MenuItem menuItem;

    public Bookmark(String path, String label, MenuItem menuItem) {
        this.path = path;
        this.label = label;
        this.menuItem = menuItem;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public void setMenuItem(MenuItem menuItem) {
        this.menuItem = menuItem;
    }

    public String getPath() {
        return path;
    }

    public String getLabel() {
        return label;
    }

    public MenuItem getMenuItem() {
        return menuItem;
    }
}
