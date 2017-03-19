package io.lerk.lrkfm;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Application;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.StrictMode;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.customtabs.CustomTabsIntent;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.text.InputType;
import android.util.Log;
import android.view.View;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import static android.view.View.GONE;

public class FileActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    private static final String PREF_HOMEDIR = "home_dir";
    private static final String PREF_BOOKMARKS = "bookmarks";
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
    private HashMap<String, MenuItem> bookmarkItems;
    private String currentDirectory = "";
    private Toolbar toolbar;
    private NavigationView navigationView;
    private TextView currentDirectoryTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        StrictMode.VmPolicy.Builder builder = new StrictMode.VmPolicy.Builder()
                .detectActivityLeaks()
                .detectCleartextNetwork()
                .detectLeakedClosableObjects()
                .detectLeakedRegistrationObjects()
                .detectLeakedSqlLiteObjects();
        StrictMode.setVmPolicy(builder.build());

        preferences = PreferenceManager.getDefaultSharedPreferences(this);
        fileListView = (ListView) findViewById(R.id.fileView);

        toolbar = (Toolbar) findViewById(R.id.toolbar);
        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        navigationView = (NavigationView) findViewById(R.id.nav_view);

        setSupportActionBar(toolbar);
        navigationView.setNavigationItemSelectedListener(this);
        View headerView = navigationView.inflateHeaderView(R.layout.nav_header_main);
        currentDirectoryTextView = (TextView) headerView.findViewById(R.id.currentDirectoryTextView);
        loadUserBookmarks();

        fab.setOnClickListener((v) -> FileActivity.this.loadDirectory(new File(currentDirectory).getParent()));

        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
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
        bookmarkItems = new HashMap<>();

        if (bookmarks != null) {
            bookmarks.forEach((s) -> {
                String[] split = s.split("/");
                int i = split.length - 1;
                bookmarkItems.put(s, menu.add(R.id.bookmarksMenuGroup, Menu.NONE, 2, split[(i < 0) ? 0 : i])); //FIXME see #11
            });
        } else {
            Log.d(TAG, "User has no bookmarks");
        }
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

    void loadDirectory(String startDir) {
        ArrayList<FMFile> files = null;
        FileLoader fileLoader = new FileLoader(startDir);
        try {
            files = fileLoader.loadLocationFiles();
        } catch (FileLoader.NoAccessException e) {
            if (FileActivity.verifyStoragePermissions(FileActivity.this)) {
                try {
                    files = fileLoader.loadLocationFiles();
                } catch (FileLoader.NoAccessException e1) {
                    Log.w(TAG, "Can't read '" + startDir + "': Permission granted but file.canRead() returned false!");
                }
            } else {
                Log.w(TAG, "Can't read '" + startDir + "': Permission denied!");
            }
        }

        View errorText = findViewById(R.id.unableToLoadText);
        if (files == null || files.isEmpty()) {
            fileListView.setVisibility(GONE);
            errorText.setVisibility(View.VISIBLE);
        } else {
            if (fileListView.getVisibility() != View.VISIBLE) {
                fileListView.setVisibility(View.VISIBLE);
            }
            if (errorText.getVisibility() != GONE) {
                errorText.setVisibility(GONE);
            }
            FileArrayAdapter adapter = new FileArrayAdapter(this, R.layout.layout_file, files);
            fileListView.setAdapter(adapter);
        }
        currentDirectory = startDir;
        if (toolbar != null) {
            if (!Objects.equals(currentDirectory, ROOT_DIR)) {
                String[] splitPath = currentDirectory.split("/");
                int i = splitPath.length - 1;
                String title = splitPath[(i < 0) ? 0 : i]; // dirty hack to prevent IndexOutOfBoundsException
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
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_home) {
            loadHomeDir();
        } else if (id == R.id.nav_path) {
            promptAndLoadPath();
        } else if (id == R.id.nav_settings) {
            Intent i = new Intent(this, SettingsActivity.class);
            startActivity(i);
        } else if (id == R.id.nav_share) {
            ApplicationInfo app = getApplicationContext().getApplicationInfo();
            String filePath = app.sourceDir;
            Intent intent = new Intent(Intent.ACTION_SEND);
            // MIME of .apk is "application/vnd.android.package-archive".
            // but Bluetooth does not accept this. Let's use "*/*" instead.
            intent.setType("*/*");
            intent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(new File(filePath)));
            startActivity(Intent.createChooser(intent, getResources().getString(R.string.share_app)));
        } else if (id == R.id.nav_add_bookmark) {
            Set<String> stringSet = preferences.getStringSet(PREF_BOOKMARKS, new HashSet<>());
            stringSet.add(currentDirectory);
            preferences.edit().putStringSet(PREF_BOOKMARKS, stringSet).apply();
            loadUserBookmarks();
        } else if (id == R.id.nav_bug_report) {
            CustomTabsIntent.Builder builder = new CustomTabsIntent.Builder();
            builder.setToolbarColor(getColor(R.color.primary));
            CustomTabsIntent build = builder.build();
            build.launchUrl(this, Uri.parse("https://fahlbtharz.k40s.net/FileManagerCompetition/lrkFM/issues/new"));
        } else {
            bookmarkItems.forEach((k, v) -> {
                if (id == v.getItemId()) {
                    this.loadDirectory(k);
                }
            });
        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    private void promptAndLoadPath() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        // Set up the input
        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);

        builder.setTitle(getString(R.string.nav_path))
                .setView(input)
                .setPositiveButton(getString(R.string.okay), (dialog, which) -> {
                    String inputPath = input.getText().toString();
                    if (inputPath.matches("([/]\\w*[/]\\w*)")) {
                        loadDirectory(inputPath);
                        dialog.dismiss();
                    } else {
                        Toast.makeText(FileActivity.this, R.string.invalid_path, Toast.LENGTH_LONG).show();
                        Log.w(TAG, "Invalid path!");
                    }
                })
                .setNegativeButton(getString(R.string.cancel), (dialog, which) -> dialog.cancel())
                .show();
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
