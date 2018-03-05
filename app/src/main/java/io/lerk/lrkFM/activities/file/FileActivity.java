package io.lerk.lrkFM.activities.file;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
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

import com.google.firebase.analytics.FirebaseAnalytics;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

import io.lerk.lrkFM.activities.SettingsActivity;
import io.lerk.lrkFM.entities.Bookmark;
import io.lerk.lrkFM.entities.FMFile;
import io.lerk.lrkFM.util.DiskUtil;
import io.lerk.lrkFM.util.EditablePair;
import io.lerk.lrkFM.R;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;
import static io.lerk.lrkFM.activities.file.OperationUtil.Operation.COPY;
import static io.lerk.lrkFM.activities.file.OperationUtil.Operation.CREATE_ZIP;
import static io.lerk.lrkFM.activities.file.OperationUtil.Operation.EXTRACT;
import static io.lerk.lrkFM.activities.file.OperationUtil.Operation.MOVE;
import static io.lerk.lrkFM.activities.file.OperationUtil.Operation.NONE;

public class FileActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    private static final String PREF_HOMEDIR = "home_dir";
    private static final String PREF_BOOKMARKS = "bookmarks";
    private static final String PREF_BOOKMARK_CURRENT_FOLDER = "bookmark_current_folder";
    private static final String PREF_BOOKMARK_EDIT_MODE = "bookmark_deletion_on";
    private static final String PREF_SHOW_TOAST = "show_toast_on_cd";
    public static final String PREF_FILENAME_LENGTH = "filename_length";
    private static final String PREF_HEADER_PATH_LENGTH = "header_path_length";
    private static final String TAG = FileActivity.class.getCanonicalName();
    private static final int REQUEST_EXTERNAL_STORAGE = 1;
    private static final String ROOT_DIR = "/";
    private static final String CURRENT_DIR_CACHE = "current_dir_cached";
    private static final String PREF_SORT_FILES_BY = "sort_files_by";
    public static final String PREF_USE_CONTEXT_FOR_OPS = "context_for_ops";
    public static final String PREF_USE_CONTEXT_FOR_OPS_TOAST = "context_for_ops_toast";
    public static final String PREF_ALWAYS_EXTRACT_IN_CURRENT_DIR = "always_extract_in_current_folder";
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
    private View headerView;
    private HashMap<Integer, String> historyMap;
    private Integer historyCounter;
    private FileArrayAdapter arrayAdapter;
    private EditablePair<OperationUtil.Operation, ArrayList<FMFile>> fileOpContext = new EditablePair<>(NONE, new ArrayList<>());
    private FirebaseAnalytics analytics;

    public ListView getFileListView() {
        return fileListView;
    }

    @Override
    protected void onPostResume() {
        super.onPostResume();
        setFreeSpaceText();
    }

    @Override
    protected void onPostCreate(@Nullable Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        loadHomeDir();
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

        analytics = FirebaseAnalytics.getInstance(this);

        FileActivity.verifyStoragePermissions(FileActivity.this);

        preferences = PreferenceManager.getDefaultSharedPreferences(this);
        fileListView = findViewById(R.id.fileView);
        registerForContextMenu(fileListView);

        toolbar = findViewById(R.id.toolbar);

        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener((v) -> FileActivity.this.loadDirectory(new File(currentDirectory).getParent()));

        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        drawer.addDrawerListener(new DrawerLayout.SimpleDrawerListener() {
            @Override
            public void onDrawerStateChanged(int newState) {
                loadUserBookmarks();
                super.onDrawerStateChanged(newState);
            }
        });

        initNavAndHeader();

        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);

        //noinspection deprecation
        drawer.setDrawerListener(toggle); // I ned dis
        toggle.syncState();
    }

    private void initNavAndHeader() {
        navigationView = findViewById(R.id.nav_view);
        setSupportActionBar(toolbar);
        navigationView.setNavigationItemSelectedListener(this);
        headerView = navigationView.inflateHeaderView(R.layout.nav_header_main);
        currentDirectoryTextView = headerView.findViewById(R.id.currentDirectoryTextView);
        loadUserBookmarks();
        setFreeSpaceText();
    }

    private void setFreeSpaceText() {
        TextView diskUsageTextView = headerView.findViewById(R.id.diskUsage);

        String defaultValue = getString(R.string.pref_header_unit_default_value);
        String s = null;
        String nav_header_unit = preferences.getString("nav_header_unit", defaultValue);
        if (nav_header_unit.equals(getString(R.string.pref_header_unit_m_value))) {
            s = DiskUtil.freeSpaceMebi(true) + " MiB " + getString(R.string.free);
        } else if (nav_header_unit.equals(getString(R.string.pref_header_unit_g_value))) {
            s = DiskUtil.freeSpaceGibi(true) + " GiB " + getString(R.string.free);
        }
        if (s == null) {
            Log.e(TAG, "Unable to get free space! requested: " + nav_header_unit);
        }
        diskUsageTextView.setText(s);
    }

    private void loadUserBookmarks() {
        Menu menu = navigationView.getMenu();
        TreeSet<String> bookmarks;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            bookmarks = new TreeSet<>(Comparator.comparing(this::getTitleFromPath));
        } else { // FUCK THEM PUNY OLD VERSION USERS!
            //noinspection ComparatorCombinators
            bookmarks = new TreeSet<>((o1, o2) -> getTitleFromPath(o1).compareTo(getTitleFromPath(o2)));
        }
        bookmarks.addAll(preferences.getStringSet(PREF_BOOKMARKS, new HashSet<>()));
        if (!bookmarks.isEmpty()) {
            bookmarkItems = new HashSet<>();
            menu.removeGroup(R.id.bookmarksMenuGroup);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                bookmarks.forEach((s) -> addBookmarkToMenu(menu, s, bookmarks));
            } else { // also fuck all of those vendors that don't update. You are making development ugly!
                for (String s : bookmarks) {
                    addBookmarkToMenu(menu, s, bookmarks);
                }
            }
        } else {
            Log.d(TAG, "User has no bookmarks");
        }
    }

    private void addBookmarkToMenu(Menu menu, String s, Set<String> bookmarks) {
        String title = getTitleFromPath(s);
        MenuItem item = menu.add(R.id.bookmarksMenuGroup, Menu.NONE, 2, title);
        item.setIcon(R.drawable.ic_bookmark_border_black_24dp);
        Bookmark bookmark = new Bookmark(s, title, item);
        if (preferences.getBoolean(PREF_BOOKMARK_EDIT_MODE, false)) {
            item.setActionView(R.layout.editable_menu_item);
            View v = item.getActionView();
            ImageButton deleteButton = v.findViewById(R.id.menu_item_action_delete);
            deleteButton.setOnClickListener(v0 -> removeBookmarkFromMenu(menu, s, bookmarks, item, bookmark));
            ImageButton editButton = v.findViewById(R.id.menu_item_action_edit);
            editButton.setOnClickListener((v1) -> {
                AlertDialog dia = new AlertDialog.Builder(this)
                        .setView(R.layout.layout_path_prompt)
                        .setNegativeButton(R.string.cancel, (dialog, which) -> Log.d(TAG, "Operation canceled."))
                        .create();
                dia.setOnShowListener(dialog -> ((EditText) dia.findViewById(R.id.destinationPath)).setText(s));
                dia.setButton(DialogInterface.BUTTON_POSITIVE, getString(R.string.okay), (dialog, which) -> {
                    removeBookmarkFromMenu(menu, s, bookmarks, item, bookmark);
                    addBookmarkToMenu(menu, ((EditText) dia.findViewById(R.id.destinationPath)).getText().toString(), bookmarks);
                });
                dia.show();
            });
        }
        bookmarkItems.add(bookmark);
    }

    public String getTitleFromPath(String s) {
        if (!ROOT_DIR.equals(s)) {
            String[] split = s.split("/");
            int i = split.length - 1;
            if (i < 0) {
                i = 0;
            }
            return split[i];
        } else {
            return s;
        }
    }

    @SuppressLint("ApplySharedPref")
    private void removeBookmarkFromMenu(Menu menu, String s, Set<String> bookmarks, MenuItem item, Bookmark bookmark) {
        menu.removeItem(item.getItemId());
        bookmarkItems.remove(bookmark);
        bookmarks.remove(s);
        preferences.edit().putStringSet(PREF_BOOKMARKS, bookmarks).commit();
    }

    @SuppressLint("UseSparseArrays")
    private void loadHomeDir() {
        historyMap = new HashMap<>();
        historyCounter = 0;
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
        FileLoader fileLoader = new FileLoader(startDir);
        View errorText = findViewById(R.id.unableToLoadText);
        View emptyText = findViewById(R.id.emptyDirText);
        try {
            files = fileLoader.loadLocationFiles();
            fileListView.setVisibility(VISIBLE);
            errorText.setVisibility(GONE);
            emptyText.setVisibility(GONE);

            arrayAdapter = new FileArrayAdapter(this, R.layout.layout_file, sortFilesByPreference(files, preferences.getString(PREF_SORT_FILES_BY, getString(R.string.pref_sortby_value_default))));
            fileListView.setAdapter(arrayAdapter);
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
        historyMap.put(historyCounter++, currentDirectory);
        setToolbarText();
        setFreeSpaceText();
    }

    private ArrayList<FMFile> sortFilesByPreference(ArrayList<FMFile> files, String pref) {
        if (pref.equals(getString(R.string.pref_sortby_value_name))) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                files.sort(Comparator.comparing(FMFile::getName));
            } else { // FUCKING OUT OF DATE USERS >.<
                //noinspection ComparatorCombinators,RedundantCast
                Arrays.sort((FMFile[]) files.toArray(), (o1, o2) -> o1.getName().compareTo(o2.getName()));
            }
        } else if (pref.equals(getString(R.string.pref_sortby_value_date))) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                files.sort(Comparator.comparing(FMFile::getLastModified));
            } else { // you see how beautiful the code above is?
                //noinspection ComparatorCombinators,RedundantCast
                Arrays.sort((FMFile[]) files.toArray(), (o1, o2) -> o1.getLastModified().compareTo(o2.getLastModified()));
            }
        } else {
            Log.d(TAG, "This sort method is not implemented, skipping file sort!");
        }
        return files;
    }

    private void removeFromHistoryAndGoBack() {
        int key = historyCounter - 2;
        String s = historyMap.get(key);
        historyMap.remove(key);
        if (s != null && !s.isEmpty()) {
            loadDirectory(s);
        }
        historyCounter = key + 1;
    }

    private void setToolbarText() {
        if (toolbar != null) {
            if (!Objects.equals(currentDirectory, ROOT_DIR)) {
                toolbar.setTitle(getTitleFromPath(currentDirectory));
            } else {
                toolbar.setTitle(currentDirectory);
            }
        }
        if (currentDirectoryTextView != null) {
            int maxLength = Integer.parseInt(preferences.getString(PREF_HEADER_PATH_LENGTH, getString(R.string.pref_header_path_length_default)));

            if (currentDirectory.length() > maxLength) {
                currentDirectoryTextView.setText(shortenDirectoryPath(maxLength));
            } else {
                currentDirectoryTextView.setText(currentDirectory);
            }
        }
        if (preferences.getBoolean(PREF_SHOW_TOAST, false)) {
            Toast.makeText(this, getText(R.string.toast_cd_new_dir) + " " + currentDirectory, Toast.LENGTH_SHORT).show();
        }
    }

    @NonNull
    private String shortenDirectoryPath(int maxLength) {
        final String[] res = {currentDirectory};
        ArrayList<String> dirs = new ArrayList<>(Arrays.asList(res[0].split("/")));

        for (int i = 0; dirs.size() >= i; i++) {
            if (dirs.get(0).isEmpty()) {
                dirs.remove(0);
                Log.d(TAG, "Element was empty and removed.");
            }
            res[0] = "";
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                dirs.forEach((s) -> res[0] += File.separator + s);
            } else { // I could save two lines of code without old versions!
                for (String s : dirs) {
                    res[0] += File.separator + s;
                }
            }
            if (res[0].length() > maxLength) {
                try {
                    dirs.set(i, dirs.get(i).substring(0, 1));
                } catch (IndexOutOfBoundsException e) {
                    Log.d(TAG, "This can happen.", e);
                }
            }
        }

        if (res.length > maxLength) {
            Log.w(TAG, "Could not shorten the string any further :c");
        }
        return res[0];
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else if (historyCounter > 0 && !historyMap.isEmpty()) {
            removeFromHistoryAndGoBack();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        boolean visible = !(fileOpContext.getFirst().equals(NONE) || fileOpContext.getSecond().isEmpty());
        MenuItem paste = menu.findItem(R.id.action_paste).setVisible(visible)
                .setTitle(fileOpContext.getFirst().getTitle());
        if (visible) {
            String title = paste.getTitle().toString();
            if (title.contains("(")) {
                title = title.substring(0, paste.getTitle().toString().indexOf("("));
            }
            paste.setTitle(title + " (" + fileOpContext.getSecond().size() + ")");
        }
        menu.findItem(R.id.action_clear_op_context).setVisible(visible);
        return true;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.settings) {
            launchSettings();
            return true;
        } else if (item.getItemId() == R.id.new_directory) {
            launchNewDirDialog();
            return true;
        } else if (item.getItemId() == R.id.action_reload_view) {
            reloadCurrentDirectory();
            return true;
        } else if (item.getItemId() == R.id.action_paste) {
            finishFileOperation();
            return true;
        } else if (item.getItemId() == R.id.action_clear_op_context) {
            clearFileOpCache();
            return true;
        } else {
            return false;
        }
    }

    void clearFileOpCache() {
        fileOpContext.setFirst(NONE);
        fileOpContext.setSecond(new ArrayList<>());
        reloadCurrentDirectory();
    }

     void finishFileOperation() {
        if (!fileOpContext.getFirst().equals(NONE) && !fileOpContext.getSecond().isEmpty()) {
            if (fileOpContext.getFirst().equals(COPY)) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    fileOpContext.getSecond().forEach((f) -> OperationUtil.copy(f, this, null));
                } else { // -_-
                    for (FMFile f : fileOpContext.getSecond()) {
                        OperationUtil.copy(f, this, null);
                    }
                }
            } else if (fileOpContext.getFirst().equals(MOVE)) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    fileOpContext.getSecond().forEach((f) -> OperationUtil.move(f, this, null));
                } else { // -_-
                    for (FMFile f : fileOpContext.getSecond()) {
                        OperationUtil.move(f, this, null);
                    }
                }
            } else if (fileOpContext.getFirst().equals(EXTRACT)) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    fileOpContext.getSecond().forEach((f) -> ArchiveUtil.extractArchive(currentDirectory, f, this));
                } else { // -_-
                    for (FMFile f : fileOpContext.getSecond()) {
                        ArchiveUtil.extractArchive(currentDirectory, f, this);
                    }
                }
            } else if(fileOpContext.getFirst().equals(CREATE_ZIP)) {
                AlertDialog alertDialog = arrayAdapter.getGenericFileOpDialog(
                        R.string.create_zip_file,
                        R.string.op_destination,
                        R.drawable.ic_archive_black_24dp,
                        R.layout.layout_name_prompt,
                        (d) -> OperationUtil.createZipFile(fileOpContext.getSecond(), this, d),
                        (d) -> Log.d(TAG, "Cancelled."));
                alertDialog.show();
            }
        } else {
            Log.w(TAG, "No operation set!");
        }
    }

    public String getCurrentDirectory() {
        return currentDirectory;
    }

    private void launchNewDirDialog() {
        AlertDialog newDirDialog = new AlertDialog.Builder(this)
                .setNegativeButton(R.string.cancel, (dialogInterface, i) -> Log.d(TAG, "Cancel pressed"))
                .setTitle(R.string.new_directory)
                .setView(R.layout.layout_name_prompt)
                .create();
        newDirDialog.setButton(DialogInterface.BUTTON_POSITIVE, getString(R.string.okay), (d, i) -> {
            String newDirName = currentDirectory + File.separator + ((EditText) newDirDialog.findViewById(R.id.destinationName)).getText().toString();
            OperationUtil.newDir(new File(newDirName), FileActivity.this);
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

        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    public static void verifyStoragePermissions(Activity context) {
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            // We don't have permission so prompt the user
            ActivityCompat.requestPermissions(context, PERMISSIONS_STORAGE, REQUEST_EXTERNAL_STORAGE);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        preferences.edit().putString(CURRENT_DIR_CACHE, currentDirectory).apply();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        preferences.edit().putString(CURRENT_DIR_CACHE, "").apply();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!preferences.getString(CURRENT_DIR_CACHE, "").equals("")) {
            currentDirectory = preferences.getString(CURRENT_DIR_CACHE, "");
        }
    }

    private void launchBugReportTab() {
        logEvent("report_bug", new Bundle());
        CustomTabsIntent.Builder builder = new CustomTabsIntent.Builder();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            builder.setToolbarColor(getColor(R.color.primary));
        } else { // hate 'em -_-
            //noinspection deprecation
            builder.setToolbarColor(getResources().getColor(R.color.primary));
        }
        CustomTabsIntent build = builder.build();
        build.launchUrl(this, Uri.parse("https://github.com/lfuelling/lrkFM/issues/new"));
    }

    @SuppressLint("ApplySharedPref")
    private void promptAndAddBookmark() {
        logEvent("add_bookmark", new Bundle());
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
        logEvent("open_path_prompt", new Bundle());
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


    public SharedPreferences getDefaultPreferences() {
        return preferences;
    }

    public void addFileToOpContext(OperationUtil.Operation op, FMFile f) {
        if (!fileOpContext.getFirst().equals(op)) {
            if (preferences.getBoolean(PREF_USE_CONTEXT_FOR_OPS_TOAST, true)) {
                Toast.makeText(this, getString(R.string.switching_op_mode), Toast.LENGTH_SHORT).show();
            }
            fileOpContext.setFirst(op);
            fileOpContext.setSecond(new ArrayList<>());
        }
        fileOpContext.getSecond().add(f);
    }

    public EditablePair<OperationUtil.Operation, ArrayList<FMFile>> getFileOpContext() {
        return fileOpContext;
    }

    /**
     * Logs event with Firebase.
     *
     * @param label the label
     * @param bundle the event content
     */
    public void logEvent(String label, Bundle bundle){
        analytics.logEvent(label, bundle);
    }
}
