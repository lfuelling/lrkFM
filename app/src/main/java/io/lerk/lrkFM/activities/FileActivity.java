package io.lerk.lrkFM.activities;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import android.os.StrictMode;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.customtabs.CustomTabsIntent;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.v4.app.ActivityCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import io.lerk.lrkFM.entities.Bookmark;
import io.lerk.lrkFM.entities.FMFile;
import io.lerk.lrkFM.util.DiskUtil;
import io.lerk.lrkFM.util.FileArrayAdapter;
import io.lerk.lrkFM.util.FileLoader;
import io.lerk.lrkFM.R;
import io.lerk.lrkFM.util.FileUtil;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

public class FileActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    private static final String PREF_HOMEDIR = "home_dir";
    private static final String PREF_BOOKMARKS = "bookmarks";
    private static final String PREF_BOOKMARK_CURRENT_FOLDER = "bookmark_current_folder";
    private static final String PREF_BOOKMARK_EDIT_MODE = "bookmark_deletion_on";
    private static final String PREF_SHOW_TOAST = "show_toast_on_cd";
    private static final String TAG = FileActivity.class.getCanonicalName();
    private static final int REQUEST_EXTERNAL_STORAGE = 1;
    private static final String ROOT_DIR = "/";
    private static String[] PERMISSIONS_STORAGE = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };
    private ListView fileListView;
    private SharedPreferences preferences;
    private HashSet<Bookmark> bookmarkItems;
    private String currentDirectory = "";
    private Toolbar toolbar;
    private NavigationView navigationView;
    private TextView currentDirectoryTextView;

    public ListView getFileListView() {
        return fileListView;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        StrictMode.VmPolicy.Builder builder = new StrictMode.VmPolicy.Builder()
                .detectActivityLeaks()
                .detectLeakedClosableObjects()
                .detectLeakedRegistrationObjects()
                .detectLeakedSqlLiteObjects();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            builder.detectCleartextNetwork();
        }
        StrictMode.setVmPolicy(builder.build());

        preferences = PreferenceManager.getDefaultSharedPreferences(this);
        fileListView = (ListView) findViewById(R.id.fileView);
        registerForContextMenu(fileListView);

        toolbar = (Toolbar) findViewById(R.id.toolbar);
        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.addDrawerListener(new DrawerLayout.SimpleDrawerListener() {
            @Override
            public void onDrawerStateChanged(int newState) {
                loadUserBookmarks();
                super.onDrawerStateChanged(newState);
            }
        });
        navigationView = (NavigationView) findViewById(R.id.nav_view);

        setSupportActionBar(toolbar);
        navigationView.setNavigationItemSelectedListener(this);
        View headerView = navigationView.inflateHeaderView(R.layout.nav_header_main);
        currentDirectoryTextView = (TextView) headerView.findViewById(R.id.currentDirectoryTextView);
        loadUserBookmarks();

        TextView diskUsageTextView = (TextView) headerView.findViewById(R.id.diskUsage);
        {
            String defaultValue = getString(R.string.pref_header_unit_default_value);
            String s = null;
            if (preferences.getString("nav_header_unit", defaultValue).equals(getString(R.string.pref_header_unit_m))) {
                s = DiskUtil.freeSpaceMebi(true) + " MiB free";
            } else if (preferences.getString("nav_header_unit", defaultValue).equals(getString(R.string.pref_header_unit_m))) {
                s = DiskUtil.freeSpaceGibi(true) + " GiB free";
            }
            diskUsageTextView.setText(s);
        }
        fab.setOnClickListener((v) -> FileActivity.this.loadDirectory(new File(currentDirectory).getParent()));

        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);

        //noinspection deprecation
        drawer.setDrawerListener(toggle); // I ned dis
        toggle.syncState();
    }

    @Override
    protected void onPostCreate(@Nullable Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        loadHomeDir();
    }

    private void loadUserBookmarks() {
        Menu menu = navigationView.getMenu();
        Set<String> bookmarks = preferences.getStringSet(PREF_BOOKMARKS, null);
        if (bookmarks != null) {
            bookmarkItems = new HashSet<>();
            menu.removeGroup(R.id.bookmarksMenuGroup);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                bookmarks.forEach((s) -> addBookmarkToMenu(menu, s, bookmarks));
            } else { // fuck all of those vendors that don't update. You are making development ugly!
                for (String s : bookmarks) {
                    addBookmarkToMenu(menu, s, bookmarks);
                }
            }
        } else {
            Log.d(TAG, "User has no bookmarks");
        }
    }

    private void addBookmarkToMenu(Menu menu, String s, Set<String> bookmarks) {
        String[] split = s.split("/");
        int i = split.length - 1;
        if (i < 0) {
            i = 0;
        }
        String title = split[i];
        MenuItem item = menu.add(R.id.bookmarksMenuGroup, Menu.NONE, 2, title);
        item.setIcon(R.drawable.ic_bookmark_border_black_24dp);
        Bookmark bookmark = new Bookmark(s, title, item);
        if(preferences.getBoolean(PREF_BOOKMARK_EDIT_MODE, false)) {
            item.setActionView(R.layout.editable_menu_item);
            View v = item.getActionView();
            ImageButton deleteButton = (ImageButton) v.findViewById(R.id.menu_item_action_delete);
            deleteButton.setOnClickListener(v0 -> {
                menu.removeItem(item.getItemId());
                bookmarkItems.remove(bookmark);
                bookmarks.remove(s);
                preferences.edit().putStringSet(PREF_BOOKMARKS, bookmarks).apply();
            });
        }
        bookmarkItems.add(bookmark);
    }

    private void loadHomeDir() {
        String absolutePath = Environment.getExternalStorageDirectory().getAbsolutePath();
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        String startDir = preferences.getString(PREF_HOMEDIR, null);
        if (startDir == null) {
            preferences.edit().putString(PREF_HOMEDIR, absolutePath).apply();
            startDir = absolutePath;
        }
        loadDirectory(startDir);
    }

    public void reloadCurrentDirectory() {
        loadDirectory(currentDirectory);
    }

    public void loadDirectory(String startDir) {
        ArrayList<FMFile> files;
        if(FileActivity.verifyStoragePermissions(FileActivity.this)){
            reloadCurrentDirectory();
        }
        FileLoader fileLoader = new FileLoader(startDir);
        View errorText = findViewById(R.id.unableToLoadText);
        View emptyText = findViewById(R.id.emptyDirText);
        try {
            files = fileLoader.loadLocationFiles();
            fileListView.setVisibility(VISIBLE);
            errorText.setVisibility(GONE);
            emptyText.setVisibility(GONE);
            FileArrayAdapter adapter = new FileArrayAdapter(this, R.layout.layout_file, files);
            fileListView.setAdapter(adapter);
        } catch (FileLoader.NoAccessException e) {
            Log.w(TAG, "Can't read '" + startDir + "': Permission denied!");
            fileListView.setVisibility(GONE);
            errorText.setVisibility(VISIBLE);
            emptyText.setVisibility(GONE);
        } catch (FileLoader.EmptyDirectoryException e) {
            Log.w(TAG, "Can't read '" + startDir + "': Empty directory!");
            fileListView.setVisibility(GONE);
            errorText.setVisibility(GONE);
            emptyText.setVisibility(VISIBLE);
        }
        currentDirectory = startDir;
        setToolbarText();
    }

    private void setToolbarText() {
        if (toolbar != null) {
            if (!Objects.equals(currentDirectory, ROOT_DIR)) {
                String[] splitPath = currentDirectory.split("/");
                int i = splitPath.length - 1;
                if (i < 0) {
                    i = 0;
                }
                String title = splitPath[i];
                toolbar.setTitle(title);
            } else {
                toolbar.setTitle(currentDirectory);
            }
        }
        if (currentDirectoryTextView != null) {
            currentDirectoryTextView.setText(currentDirectory);
        }
        if (preferences.getBoolean(PREF_SHOW_TOAST, false)) {
            Toast.makeText(this, getText(R.string.toast_cd_new_dir) + " " + currentDirectory, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_settings) {
            launchSettings();
            return true;
        } else if (item.getItemId() == R.id.action_new_directory) {
            launchNewDirDialog();
            return true;
        } else if (item.getItemId() == R.id.action_reload_view) {
            reloadCurrentDirectory();
            return true;
        } else {
            return false;
        }
    }

    public String getCurrentDirectory() {
        return currentDirectory;
    }

    private void launchNewDirDialog() {
        AlertDialog newDirDialog = new AlertDialog.Builder(this)
                .setNegativeButton(R.string.cancel, (dialogInterface, i) -> Log.d(TAG, "Cancel pressed"))
                .setPositiveButton(R.string.okay, (dialogInterface, i) -> Log.d(TAG, "Dismiss pressed"))
                .setTitle(R.string.op_new_dir_title)
                .setView(R.layout.layout_name_prompt)
                .create();
        newDirDialog.setOnDismissListener(dialogInterface -> {
            String newDirName = currentDirectory + File.separator + ((EditText) newDirDialog.findViewById(R.id.destinationName)).getText().toString();
            FileUtil.newDir(new File(newDirName), FileActivity.this);
            reloadCurrentDirectory();
        });
        newDirDialog.show();
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.nav_home) {
            loadHomeDir();
        } else if (id == R.id.nav_path) {
            promptAndLoadPath();
        } else if (id == R.id.nav_settings) {
            launchSettings();
        } else if (id == R.id.nav_share) {
            shareApplication();
        } else if (id == R.id.nav_add_bookmark) {
            promptAndAddBookmark();
        } else if (id == R.id.nav_bug_report) {
            launchBugReportTab();
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                bookmarkItems.forEach(bookmark -> {
                    if (bookmark.getMenuItem().getItemId() == id) {
                        loadDirectory(bookmark.getPath());
                    }
                });
            } else {
                for (Bookmark bookmark : bookmarkItems) {
                    if (bookmark.getMenuItem().getItemId() == id) {
                        loadDirectory(bookmark.getPath());
                    }
                }
            }
        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    private void shareApplication() {
        ApplicationInfo app = getApplicationContext().getApplicationInfo();
        String filePath = app.sourceDir;
        Intent intent = new Intent(Intent.ACTION_SEND);
        // MIME of .apk is "application/vnd.android.package-archive".
        // but Bluetooth does not accept this. Let's use "*/*" instead.
        intent.setType("*/*");
        intent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(new File(filePath)));
        startActivity(Intent.createChooser(intent, getResources().getString(R.string.share_app)));
    }

    private void launchBugReportTab() {
        CustomTabsIntent.Builder builder = new CustomTabsIntent.Builder();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            builder.setToolbarColor(getColor(R.color.primary));
        } else {
            builder.setToolbarColor(getResources().getColor(R.color.primary));
        }
        CustomTabsIntent build = builder.build();
        build.launchUrl(this, Uri.parse("https://fahlbtharz.k40s.net/FileManagerCompetition/lrkFM/issues/new"));
    }

    @SuppressLint("ApplySharedPref")
    private void promptAndAddBookmark() {
        Set<String> stringSet = preferences.getStringSet(PREF_BOOKMARKS, new HashSet<>());
        if (preferences.getBoolean(PREF_BOOKMARK_CURRENT_FOLDER, false)) {
            stringSet.add(currentDirectory);
        } else {
            AlertDialog.Builder bookmarkDialogBuilder = new AlertDialog.Builder(this);
            bookmarkDialogBuilder
                    .setNegativeButton(R.string.cancel, (dialog, which) -> Log.d(TAG, "Cancel pressed!"))
                    .setNeutralButton(R.string.bookmark_this_folder, (dialog, which) -> {
                        stringSet.add(currentDirectory);
                        dialog.cancel();
                    }).setView(R.layout.layout_path_prompt)
                    .setTitle(R.string.bookmark_set_path);
            AlertDialog alertDialog = bookmarkDialogBuilder.create();
            alertDialog.setButton(AlertDialog.BUTTON_POSITIVE, getString(R.string.okay), (dialog, which) -> stringSet.add(((EditText) alertDialog.findViewById(R.id.destinationPath)).getText().toString()));
            alertDialog.setOnDismissListener(dialog -> {
                preferences.edit().putStringSet(PREF_BOOKMARKS, stringSet).commit();
                loadUserBookmarks();
            });
            alertDialog.show();
        }
    }

    private void promptAndLoadPath() {
        AlertDialog.Builder bookmarkDialogBuilder = new AlertDialog.Builder(this);
        bookmarkDialogBuilder
                .setNegativeButton(R.string.cancel, (dialog, which) -> Log.d(TAG, "Cancel pressed!"))
                .setView(R.layout.layout_path_prompt)
                .setTitle(R.string.nav_path);

        AlertDialog alertDialog = bookmarkDialogBuilder.create();
        alertDialog.setButton(AlertDialog.BUTTON_POSITIVE, getString(R.string.okay), (dialog, which) -> loadDirectory(((EditText) alertDialog.findViewById(R.id.destinationPath)).getText().toString()));
        alertDialog.show();
    }

    public void launchSettings() {
        Intent i = new Intent(this, SettingsActivity.class);
        startActivity(i);
    }

    public static boolean verifyStoragePermissions(Activity context) {
        // Check if we have write permission
        int permission = ActivityCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE);

        if (permission != PackageManager.PERMISSION_GRANTED) {
            // We don't have permission so prompt the user
            ActivityCompat.requestPermissions(
                    context,
                    PERMISSIONS_STORAGE,
                    REQUEST_EXTERNAL_STORAGE
            );
            return true;
        } else {
            return false;
        }
    }
}
