package io.lerk.lrkFM.activities;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
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
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
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
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

import io.lerk.lrkFM.adapter.BaseArrayAdapter;
import io.lerk.lrkFM.entities.FMArchive;
import io.lerk.lrkFM.entities.HistoryEntry;
import io.lerk.lrkFM.exceptions.EmptyDirectoryException;
import io.lerk.lrkFM.exceptions.NoAccessException;
import io.lerk.lrkFM.op.ArchiveUtil;
import io.lerk.lrkFM.entities.Bookmark;
import io.lerk.lrkFM.adapter.ArchiveArrayAdapter;
import io.lerk.lrkFM.util.ArchiveLoader;
import io.lerk.lrkFM.util.ArchiveParentFinder;
import io.lerk.lrkFM.util.DiskUtil;
import io.lerk.lrkFM.EditablePair;
import io.lerk.lrkFM.consts.Operation;
import io.lerk.lrkFM.op.OperationUtil;
import io.lerk.lrkFM.util.PrefUtils;
import io.lerk.lrkFM.R;
import io.lerk.lrkFM.entities.FMFile;
import io.lerk.lrkFM.adapter.FileArrayAdapter;
import io.lerk.lrkFM.util.FileLoader;
import io.lerk.lrkFM.util.VersionCheckTask;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;
import static io.lerk.lrkFM.LrkFMApp.CHANNEL_ID;
import static io.lerk.lrkFM.consts.Operation.COPY;
import static io.lerk.lrkFM.consts.Operation.CREATE_ZIP;
import static io.lerk.lrkFM.consts.Operation.EXTRACT;
import static io.lerk.lrkFM.consts.Operation.MOVE;
import static io.lerk.lrkFM.consts.Operation.NONE;
import static io.lerk.lrkFM.consts.PreferenceEntity.BOOKMARKS;
import static io.lerk.lrkFM.consts.PreferenceEntity.BOOKMARK_CURRENT_FOLDER;
import static io.lerk.lrkFM.consts.PreferenceEntity.BOOKMARK_EDIT_MODE;
import static io.lerk.lrkFM.consts.PreferenceEntity.FIRST_START;
import static io.lerk.lrkFM.consts.PreferenceEntity.HEADER_PATH_LENGTH;
import static io.lerk.lrkFM.consts.PreferenceEntity.HOME_DIR;
import static io.lerk.lrkFM.consts.PreferenceEntity.SHOW_TOAST;
import static io.lerk.lrkFM.consts.PreferenceEntity.SORT_FILES_BY;
import static io.lerk.lrkFM.consts.PreferenceEntity.UPDATE_NOTIFICATION;
import static io.lerk.lrkFM.consts.PreferenceEntity.USE_CONTEXT_FOR_OPS_TOAST;
import static io.lerk.lrkFM.util.VersionCheckTask.NEW_VERSION_NOTIF;

public class FileActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    /**
     * Logtag.
     */
    private static final String TAG = FileActivity.class.getCanonicalName();

    /**
     * 0 if we have no permission.
     */
    private static final int REQUEST_EXTERNAL_STORAGE = 1;

    /**
     * The rootfs path ('/').
     */
    private static final String ROOT_DIR = "/";

    /**
     * Keyword for {@link SharedPreferences}
     */
    private static final String CURRENT_DIR_CACHE = "current_dir_cached";

    /**
     * The permissions we need to do our job.
     */
    private static String[] PERMISSIONS_STORAGE = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };

    /**
     * Self.
     */
    private static WeakReference<FileActivity> context;

    /**
     * {@link ListView} that contains the files inside the current directory.
     */
    private ListView fileListView;

    /**
     * The {@link SharedPreferences} of this app.
     */
    private SharedPreferences preferences;

    /**
     * Bookmarks.
     */
    private HashSet<Bookmark> bookmarkItems;

    /**
     * The current directory (empty when starting the app).
     */
    private String currentDirectory = "";

    /**
     * The {@link Toolbar}.
     */
    private Toolbar toolbar;

    /**
     * The navigation drawer.
     */
    private NavigationView navigationView;

    /**
     * The {@link TextView} containing the shortened path of the current directoy.
     *
     * @see #shortenDirectoryPath(int)
     */
    private TextView currentDirectoryTextView;

    /**
     * The header view of the {@link #navigationView}.
     */
    private View headerView;

    /**
     * The history.
     */
    private HashMap<Integer, HistoryEntry> historyMap;

    /**
     * The historyCounter (sorry).
     */
    private Integer historyCounter;

    /**
     * The {@link android.widget.ArrayAdapter} implementation used in the {@link #fileListView}.
     */
    private BaseArrayAdapter arrayAdapter;

    /**
     * The file operation context containing the {@link Operation} to do and a list of {@link FMFile} objects.
     */
    private EditablePair<Operation, ArrayList<FMFile>> fileOpContext = new EditablePair<>(NONE, new ArrayList<>());

    /**
     * The {@link ArchiveUtil}.
     */
    public ArchiveUtil archiveUtil;

    /**
     * The {@link OperationUtil}.
     */
    public OperationUtil operationUtil;

    /**
     * Extra used in {@link IntroActivity}.
     *
     * @see Intent#hasExtra(String)
     */
    public static final String FIRST_START_EXTRA = "firstStartDone";

    /**
     * Getter for the fileListView.
     *
     * @return fileListView
     * @see #fileListView
     */
    public ListView getFileListView() {
        return fileListView;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onPostResume() {
        super.onPostResume();
        setFreeSpaceText();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onPostCreate(@Nullable Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        loadHomeDir();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        context = new WeakReference<>(this);

        preferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());

        PrefUtils<Boolean> firstStartPref = new PrefUtils<>(FIRST_START);
        if (getIntent().hasExtra(FIRST_START_EXTRA) && getIntent().getBooleanExtra(FIRST_START_EXTRA, false)) {
            firstStartPref.setValue(false);
        } else if (firstStartPref.getValue()) {
            startActivity(new Intent(this, IntroActivity.class));
            finish();
        }


        setContentView(R.layout.activity_main);

        StrictMode.VmPolicy.Builder builder = new StrictMode.VmPolicy.Builder()
                .detectActivityLeaks()
                .detectLeakedClosableObjects()
                .detectLeakedRegistrationObjects()
                .detectLeakedSqlLiteObjects();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            builder.detectCleartextNetwork();
        }

        archiveUtil = new ArchiveUtil(this);
        operationUtil = new OperationUtil(this);

        StrictMode.setVmPolicy(builder.build());

        FileActivity.verifyStoragePermissions(FileActivity.this);

        fileListView = findViewById(R.id.fileView);
        registerForContextMenu(fileListView);

        toolbar = findViewById(R.id.toolbar);

        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener((v) -> {
            if (historyCounter > 0 && !historyMap.isEmpty()) {
                removeFromHistoryAndGoBack();
            } else {
                if (currentDirectory != null && currentDirectory.startsWith("/") && !currentDirectory.equals("/")) {
                    FileActivity.this.loadPath(new File(currentDirectory).getParent());
                } else if (currentDirectory != null) {
                    Toast.makeText(getApplicationContext(), R.string.err_already_at_file_root, Toast.LENGTH_LONG).show();
                } else {
                    Log.wtf(TAG, "currentDirectory is null!");
                }
            }
        });

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

        if (new PrefUtils<Boolean>(UPDATE_NOTIFICATION).getValue()) {
            new VersionCheckTask(result -> {
                String currentVersion = null;
                try {
                    currentVersion = FileActivity.this.getPackageManager().getPackageInfo(FileActivity.this.getPackageName(), 0).versionName;
                    if (result != null && !result.isEmpty()) {
                        Log.d(TAG, "Current version: '" + currentVersion + "' PlayStore version: '" + result + "'");
                        Integer currentVersionInt = Integer.valueOf(currentVersion.replaceAll("\\.", ""));
                        Integer onlineVersionInt = Integer.valueOf(result.replaceAll("\\.", ""));
                        if (currentVersionInt < onlineVersionInt) {
                            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=io.lerk.lrkfm"));
                            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                            PendingIntent pendingIntent = PendingIntent.getActivity(FileActivity.this.getApplicationContext(), 0, intent, 0);
                            NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(FileActivity.this, CHANNEL_ID)
                                    .setSmallIcon(R.drawable.ic_launcher)
                                    .setContentTitle(FileActivity.this.getText(R.string.notif_update_title))
                                    .setContentText(FileActivity.this.getText(R.string.notif_update_content) + result)
                                    .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                                    .setContentIntent(pendingIntent)
                                    .setAutoCancel(true);
                            NotificationManagerCompat.from(FileActivity.this.getApplicationContext()).notify(NEW_VERSION_NOTIF, notificationBuilder.build());
                        }
                    }
                } catch (PackageManager.NameNotFoundException e) {
                    Log.e(TAG, "Unable to get package name!", e);
                }
            }).execute();
        }
    }

    /**
     * Initializes the navigation drawer and it's header.
     */
    private void initNavAndHeader() {
        navigationView = findViewById(R.id.nav_view);
        setSupportActionBar(toolbar);
        navigationView.setNavigationItemSelectedListener(this);
        headerView = navigationView.inflateHeaderView(R.layout.nav_header_main);
        currentDirectoryTextView = headerView.findViewById(R.id.currentDirectoryTextView);
        loadUserBookmarks();
        setFreeSpaceText();
    }

    /**
     * Adds the free space text to the navigation drawer header.
     */
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

    /**
     * Loads the bookmarks into the menu.
     */
    private void loadUserBookmarks() {
        Menu menu = navigationView.getMenu();
        TreeSet<String> bookmarks;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            bookmarks = new TreeSet<>(Comparator.comparing(this::getTitleFromPath));
        } else { // FUCK THEM PUNY OLD VERSION USERS!
            //noinspection ComparatorCombinators
            bookmarks = new TreeSet<>((o1, o2) -> getTitleFromPath(o1).compareTo(getTitleFromPath(o2)));
        }
        bookmarks.addAll(new PrefUtils<HashSet<String>>(BOOKMARKS).getValue());
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

    /**
     * Adds a bookmark to the navigation drawer.
     *
     * @param menu      the menu
     * @param s         the bookmark's display text
     * @param bookmarks the bookmarks
     */
    private void addBookmarkToMenu(Menu menu, String s, Set<String> bookmarks) {
        String title = getTitleFromPath(s);
        MenuItem item = menu.add(R.id.bookmarksMenuGroup, Menu.NONE, 2, title);
        item.setIcon(R.drawable.ic_bookmark_border_black_24dp);
        Bookmark bookmark = new Bookmark(s, title, item);
        if (new PrefUtils<Boolean>(BOOKMARK_EDIT_MODE).getValue()) {
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

    /**
     * Returns the last part of a path.
     *
     * @param s absolute path
     * @return last item of path
     */
    public String getTitleFromPath(String s) {
        if (s != null) {
            if (!s.equals(ROOT_DIR)) {
                String[] split = s.split("/");
                int i = split.length - 1;
                if (i < 0) {
                    i = 0;
                }
                return split[i];
            } else {
                return s;
            }
        } else {
            return "null";
        }
    }

    /**
     * Removes a bookmark.
     *
     * @param menu      the bookmarks menu
     * @param s         entry to remove
     * @param bookmarks the bookmarks
     * @param item      the menu item to remove
     * @param bookmark  the bookmark to remove
     */
    @SuppressLint("ApplySharedPref")
    private void removeBookmarkFromMenu(Menu menu, String s, Set<String> bookmarks, MenuItem item, Bookmark bookmark) {
        menu.removeItem(item.getItemId());
        bookmarkItems.remove(bookmark);
        bookmarks.remove(s);
        new PrefUtils<Set<String>>(BOOKMARKS).setValue(bookmarks);
    }

    /**
     * Loads home dir selected by user.
     */
    @SuppressLint("UseSparseArrays")
    private void loadHomeDir() {
        historyMap = new HashMap<>();
        historyCounter = 0;
        String absolutePath = Environment.getExternalStorageDirectory().getAbsolutePath();
        PrefUtils<String> homeDirPreference = new PrefUtils<>(HOME_DIR);
        String startDir = homeDirPreference.getValue();
        if (startDir == null) {
            homeDirPreference.setValue(absolutePath);
            startDir = absolutePath;
        }
        loadPath(startDir);
    }

    /**
     * F5
     */
    public void reloadCurrentDirectory() {
        loadPath(currentDirectory);
    }

    @SuppressWarnings("deprecation")
    // one of the two methods that may call the "deprecated" functions.
    public void loadArchivePath(String path, FMArchive archive) {
        loadArchive(path, archive);
    }

    /**
     * Changes directory.
     *
     * @param path the path to cd into
     */
    @SuppressWarnings("deprecation")
    // one of the two methods that may call the "deprecated" functions.
    public void loadPath(String path) {
        ArchiveParentFinder archiveResult = new ArchiveParentFinder(path).invoke();
        boolean archive = archiveResult.isArchive();
        FMArchive aF = archiveResult.getArchiveFile();
        if (archive) {
            loadArchive(path, aF);
        } else {
            loadDirectory(path);
        }
    }

    /**
     * Changes directory.
     *
     * @param path directory to load
     * @deprecated may only be called by {@link #loadPath(String)}
     */
    @SuppressWarnings("DeprecatedIsStillUsed") // see above
    private void loadDirectory(String path) {
        ArrayList<FMFile> files;
        FileLoader fileLoader = new FileLoader(path);
        View errorText = findViewById(R.id.unableToLoadText);
        View emptyText = findViewById(R.id.emptyDirText);
        try {
            files = fileLoader.loadLocationFiles();
            fileListView.setVisibility(VISIBLE);
            errorText.setVisibility(GONE);
            emptyText.setVisibility(GONE);

            arrayAdapter = new FileArrayAdapter(this, R.layout.layout_file, sortFilesByPreference(files, new PrefUtils<String>(SORT_FILES_BY).getValue()));
            fileListView.setAdapter(arrayAdapter);
        } catch (NoAccessException e) {
            Log.w(TAG, "Can't read '" + path + "': Permission denied!");
            fileListView.setVisibility(GONE);
            errorText.setVisibility(VISIBLE);
            emptyText.setVisibility(GONE);
        } catch (EmptyDirectoryException e) {
            Log.w(TAG, "Can't read '" + path + "': Empty directory!");
            fileListView.setVisibility(GONE);
            errorText.setVisibility(GONE);
            emptyText.setVisibility(VISIBLE);
        }
        currentDirectory = path;

        HistoryEntry entry = historyMap.get(historyCounter);
        if (entry != null && !entry.getPath().equals(currentDirectory)) {
            historyMap.put(historyCounter++, new HistoryEntry(currentDirectory, null));
        }
        setToolbarText();
        setFreeSpaceText();
    }

    /**
     * Changes "directory" into an archive.
     *
     * @param path archive to load
     * @deprecated may only be called by {@link #loadPath(String)}
     */
    @SuppressWarnings("DeprecatedIsStillUsed") // see above
    private void loadArchive(String path, FMArchive archive) {
        ArrayList<FMFile> files;
        ArchiveLoader loader = new ArchiveLoader(archive, path);
        View errorText = findViewById(R.id.unableToLoadText);
        View emptyText = findViewById(R.id.emptyDirText);

        files = loader.loadLocationFiles();
        fileListView.setVisibility(VISIBLE);
        errorText.setVisibility(GONE);
        emptyText.setVisibility(GONE);

        arrayAdapter = new ArchiveArrayAdapter(this, R.layout.layout_file, sortFilesByPreference(files, new PrefUtils<String>(SORT_FILES_BY).getValue()), archive);
        fileListView.setAdapter(arrayAdapter);

        currentDirectory = path;
        historyMap.put(historyCounter++, new HistoryEntry(currentDirectory, archive));
        setToolbarText();
        setFreeSpaceText();
    }

    /**
     * Sorts files by user preference.
     *
     * @param files files to sort
     * @param pref  chosen preference
     * @return sorted list
     */
    private ArrayList<FMFile> sortFilesByPreference(ArrayList<FMFile> files, String pref) {
        if (pref.equals(getString(R.string.pref_sortby_value_name))) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                files.sort(Comparator.comparing(FMFile::getName));
            } else { // FUCKING OUT OF DATE USERS >.<
                //noinspection ComparatorCombinators,RedundantCast
                Arrays.sort((FMFile[]) files.toArray(new FMFile[files.size()]), (o1, o2) -> o1.getName().compareTo(o2.getName()));
            }
        } else if (pref.equals(getString(R.string.pref_sortby_value_date))) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                files.sort(Comparator.comparing(FMFile::getLastModified));
            } else { // you see how beautiful the code above is?
                //noinspection ComparatorCombinators,RedundantCast
                Arrays.sort((FMFile[]) files.toArray(new FMFile[files.size()]), (o1, o2) -> o1.getLastModified().compareTo(o2.getLastModified()));
            }
        } else {
            Log.d(TAG, "This sort method is not implemented, skipping file sort!");
        }
        return files;
    }

    /**
     * Removes the most recent item from the history and goes back.
     */
    private void removeFromHistoryAndGoBack() {
        int key = historyCounter - 1;
        if (key < 0) {
            key = 0;
        }
        HistoryEntry entry = historyMap.get(key);
        historyMap.remove(key);
        if (entry != null && !entry.getPath().isEmpty() && !entry.isInArchive()) {
            loadPath(entry.getPath());
        } else if (entry != null && !entry.getPath().isEmpty() && entry.isInArchive()) {
            loadArchivePath(entry.getPath(), entry.getArchive());
        }
        historyCounter = key;
    }

    /**
     * Sets the text in the toolbar.
     */
    private void setToolbarText() {
        if (toolbar != null) {
            if (!Objects.equals(currentDirectory, ROOT_DIR)) {
                HistoryEntry entry = historyMap.get(historyCounter - 1);
                if (entry != null && entry.isInArchive()) {
                    toolbar.setTitle(getTitleFromPath(entry.getArchive().getAbsolutePath()));
                }
                toolbar.setTitle(getTitleFromPath(currentDirectory));
            } else {
                toolbar.setTitle(currentDirectory);
            }
        }
        if (currentDirectoryTextView != null) {
            int maxLength = Integer.parseInt(new PrefUtils<String>(HEADER_PATH_LENGTH).getValue());

            if (currentDirectory.length() > maxLength) {
                currentDirectoryTextView.setText(shortenDirectoryPath(maxLength));
            } else {
                currentDirectoryTextView.setText(currentDirectory);
            }
        }
        if (new PrefUtils<Boolean>(SHOW_TOAST).getValue()) {
            Toast.makeText(this, getText(R.string.toast_cd_new_dir) + " " + currentDirectory, Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Shortens the current path by limiting directory paths
     * to one char length until the desired maxLength is reached.
     *
     * @param maxLength max length (chars) of the result
     * @return the shortened string
     */
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

    /**
     * Handles the back button.
     */
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

    /**
     * Prepares the options menu.
     *
     * @param menu the menu
     * @return true
     */
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

    /**
     * Creates the options menu.
     *
     * @param menu the menu to create
     * @return true
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    /**
     * Called when an item in the options menu is clicked.
     *
     * @param item the id of the clicked item
     * @return false if item id not "found"
     */
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

    /**
     * Clears the current file operation context.
     */
    public void clearFileOpCache() {
        fileOpContext.setFirst(NONE);
        fileOpContext.setSecond(new ArrayList<>());
        reloadCurrentDirectory();
    }

    /**
     * Finishes the current file operation context.
     */
    public void finishFileOperation() {
        Operation operation = fileOpContext.getFirst();
        ArrayList<FMFile> files = fileOpContext.getSecond();
        if (!operation.equals(NONE) && !files.isEmpty()) {
            if (operation.equals(COPY)) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    files.forEach((f) -> operationUtil.copy(f, null));
                } else { // -_-
                    for (FMFile f : files) {
                        operationUtil.copy(f, null);
                    }
                }
            } else if (operation.equals(MOVE)) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    files.forEach((f) -> operationUtil.move(f, null));
                } else { // -_-
                    for (FMFile f : files) {
                        operationUtil.move(f, null);
                    }
                }
            } else if (operation.equals(EXTRACT)) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    files.forEach((f) -> archiveUtil.extractArchive(currentDirectory, f));
                } else { // -_-
                    for (FMFile f : files) {
                        archiveUtil.extractArchive(currentDirectory, f);
                    }
                }
            } else if (operation.equals(CREATE_ZIP)) {
                AlertDialog alertDialog = arrayAdapter.getGenericFileOpDialog(
                        R.string.create_zip_file,
                        R.string.op_destination,
                        R.drawable.ic_archive_black_24dp,
                        R.layout.layout_name_prompt,
                        (d) -> archiveUtil.createZipFile(files, d),
                        (d) -> Log.d(TAG, "Cancelled."));
                alertDialog.show();
            } else {
                Toast.makeText(this, R.string.invalid_operation, Toast.LENGTH_LONG).show();
            }
        } else {
            Log.w(TAG, "No operation set!");
        }
    }

    /**
     * Getter for the current directory.
     *
     * @return the current directory.
     */
    public String getCurrentDirectory() {
        return currentDirectory;
    }

    /**
     * Launches a prompt to create a new directory.
     */
    private void launchNewDirDialog() {
        AlertDialog newDirDialog = new AlertDialog.Builder(this)
                .setNegativeButton(R.string.cancel, (dialogInterface, i) -> Log.d(TAG, "Cancel pressed"))
                .setTitle(R.string.new_directory)
                .setView(R.layout.layout_name_prompt)
                .create();
        newDirDialog.setButton(DialogInterface.BUTTON_POSITIVE, getString(R.string.okay), (d, i) -> {
            String newDirName = currentDirectory + File.separator + ((EditText) newDirDialog.findViewById(R.id.destinationName)).getText().toString();
            operationUtil.newDir(new File(newDirName));
            reloadCurrentDirectory();
        });
        newDirDialog.show();
    }

    /**
     * Called when a navigation item is selected.
     *
     * @param item the selected navigation item id
     * @return true
     */
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
                        loadPath(bookmark.getPath());
                    }
                });
            } else {
                for (Bookmark bookmark : bookmarkItems) {
                    if (bookmark.getMenuItem().getItemId() == id) {
                        loadPath(bookmark.getPath());
                    }
                }
            }
        }

        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    /**
     * Checks if permission to access files is granted.
     *
     * @param context the context
     */
    public static void verifyStoragePermissions(Activity context) {
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(context, PERMISSIONS_STORAGE, REQUEST_EXTERNAL_STORAGE);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onStop() {
        super.onStop();
        preferences.edit().putString(CURRENT_DIR_CACHE, currentDirectory).apply();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        preferences.edit().putString(CURRENT_DIR_CACHE, "").apply();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onResume() {
        super.onResume();
        if (!preferences.getString(CURRENT_DIR_CACHE, "").equals("")) {
            currentDirectory = preferences.getString(CURRENT_DIR_CACHE, "");
        }
    }

    /**
     * Opens the bug report page.
     */
    private void launchBugReportTab() {
        CustomTabsIntent.Builder builder = new CustomTabsIntent.Builder();
        builder.setToolbarColor(getColor(R.color.primary));
        CustomTabsIntent build = builder.build();
        build.launchUrl(this, Uri.parse("https://github.com/lfuelling/lrkFM/issues/new"));
    }

    /**
     * Adds a bookmark. Prompts for location.
     */
    @SuppressLint("ApplySharedPref")
    private void promptAndAddBookmark() {
        Set<String> stringSet = new PrefUtils<HashSet<String>>(BOOKMARKS).getValue();
        if (new PrefUtils<Boolean>(BOOKMARK_CURRENT_FOLDER).getValue()) {
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
                preferences.edit().putStringSet(BOOKMARKS.getKey(), stringSet).commit();
                loadUserBookmarks();
            });
            alertDialog.show();
        }
    }

    /**
     * Changes directory. With a prompt.
     *
     * @see #loadDirectory(String)
     */
    private void promptAndLoadPath() {
        AlertDialog.Builder bookmarkDialogBuilder = new AlertDialog.Builder(this);
        bookmarkDialogBuilder
                .setNegativeButton(R.string.cancel, (dialog, which) -> Log.d(TAG, "Cancel pressed!"))
                .setView(R.layout.layout_path_prompt)
                .setTitle(R.string.nav_path);

        AlertDialog alertDialog = bookmarkDialogBuilder.create();
        alertDialog.setButton(AlertDialog.BUTTON_POSITIVE, getString(R.string.okay), (dialog, which) -> loadPath(((EditText) alertDialog.findViewById(R.id.destinationPath)).getText().toString()));
        alertDialog.show();
    }

    /**
     * Launches the {@link SettingsActivity}.
     */
    public void launchSettings() {
        Intent i = new Intent(this, SettingsActivity.class);
        startActivity(i);
    }

    /**
     * Adds a file to the operation context.
     * If the selected operation mode is not the same used in the current context,
     * the previous state will be discarded.
     *
     * @param op operation to do
     * @param f  file to operate on
     */
    public void addFileToOpContext(Operation op, FMFile f) {
        if (!fileOpContext.getFirst().equals(op)) {
            if (new PrefUtils<Boolean>(USE_CONTEXT_FOR_OPS_TOAST).getValue()) {
                Toast.makeText(this, getString(R.string.switching_op_mode), Toast.LENGTH_SHORT).show();
            }
            fileOpContext.setFirst(op);
            fileOpContext.setSecond(new ArrayList<>());
        }
        fileOpContext.getSecond().add(f);
    }

    public EditablePair<Operation, ArrayList<FMFile>> getFileOpContext() {
        return fileOpContext;
    }

    public static FileActivity get() {
        return context.get();
    }
}
