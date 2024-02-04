package io.lerk.lrkFM.activities.file;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.StrictMode;

import androidx.annotation.AttrRes;
import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.browser.customtabs.CustomTabColorSchemeParams;
import androidx.browser.customtabs.CustomTabsIntent;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.navigation.NavigationView;

import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.widget.Toolbar;

import android.util.Log;
import android.util.TypedValue;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.CopyOnWriteArrayList;

import io.lerk.lrkFM.DiskUtil;
import io.lerk.lrkFM.EditablePair;
import io.lerk.lrkFM.UriUtil;
import io.lerk.lrkFM.Pref;
import io.lerk.lrkFM.R;
import io.lerk.lrkFM.VibratingToast;
import io.lerk.lrkFM.activities.IntroActivity;
import io.lerk.lrkFM.activities.SettingsActivity;
import io.lerk.lrkFM.activities.themed.ThemedAppCompatActivity;
import io.lerk.lrkFM.adapter.ArchiveArrayAdapter;
import io.lerk.lrkFM.adapter.BaseArrayAdapter;
import io.lerk.lrkFM.adapter.FileArrayAdapter;
import io.lerk.lrkFM.consts.Operation;
import io.lerk.lrkFM.entities.Bookmark;
import io.lerk.lrkFM.entities.FMArchive;
import io.lerk.lrkFM.entities.FMFile;
import io.lerk.lrkFM.entities.HistoryEntry;
import io.lerk.lrkFM.tasks.DirectoryLoaderTask;
import io.lerk.lrkFM.tasks.VersionCheckTask;
import io.lerk.lrkFM.tasks.archive.ArchiveCreationTask;
import io.lerk.lrkFM.tasks.archive.ArchiveExtractionTask;
import io.lerk.lrkFM.tasks.archive.ArchiveLoaderTask;
import io.lerk.lrkFM.tasks.archive.ArchiveParentFinderTask;
import io.lerk.lrkFM.tasks.operation.FileCopyTask;
import io.lerk.lrkFM.tasks.operation.FileDeleteTask;
import io.lerk.lrkFM.tasks.operation.FileMoveTask;
import io.lerk.lrkFM.tasks.operation.FileOperationTask;
import io.lerk.lrkFM.version.VersionInfo;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;
import static android.widget.Toast.LENGTH_SHORT;
import static io.lerk.lrkFM.LrkFMApp.CHANNEL_ID;
import static io.lerk.lrkFM.consts.Operation.COPY;
import static io.lerk.lrkFM.consts.Operation.CREATE_ZIP;
import static io.lerk.lrkFM.consts.Operation.DELETE;
import static io.lerk.lrkFM.consts.Operation.EXTRACT;
import static io.lerk.lrkFM.consts.Operation.MOVE;
import static io.lerk.lrkFM.consts.Operation.NONE;
import static io.lerk.lrkFM.consts.PreferenceEntity.ALWAYS_EXTRACT_IN_CURRENT_DIR;
import static io.lerk.lrkFM.consts.PreferenceEntity.ALWAYS_SHOW_INTRO;
import static io.lerk.lrkFM.consts.PreferenceEntity.BOOKMARKS;
import static io.lerk.lrkFM.consts.PreferenceEntity.BOOKMARK_CURRENT_FOLDER;
import static io.lerk.lrkFM.consts.PreferenceEntity.BOOKMARK_EDIT_MODE;
import static io.lerk.lrkFM.consts.PreferenceEntity.CURRENT_DIR_CACHE;
import static io.lerk.lrkFM.consts.PreferenceEntity.FIRST_START;
import static io.lerk.lrkFM.consts.PreferenceEntity.HEADER_PATH_LENGTH;
import static io.lerk.lrkFM.consts.PreferenceEntity.HOME_DIR;
import static io.lerk.lrkFM.consts.PreferenceEntity.NAV_HEADER_UNIT;
import static io.lerk.lrkFM.consts.PreferenceEntity.SHOW_TOAST;
import static io.lerk.lrkFM.consts.PreferenceEntity.UPDATE_NOTIFICATION;
import static io.lerk.lrkFM.consts.PreferenceEntity.USE_CONTEXT_FOR_OPS_TOAST;
import static io.lerk.lrkFM.tasks.VersionCheckTask.NEW_VERSION_NOTIF;

public class FileActivity extends ThemedAppCompatActivity {

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
     * Keyword for {@link Activity#onSaveInstanceState(Bundle)}.
     */
    private static final String STATE_KEY_CURRENT_DIR = "state_current_dir";

    /**
     * Keyword for {@link Activity#onSaveInstanceState(Bundle)}.
     */
    private static final String STATE_KEY_OP_CONTEXT_OP = "state_current_op";

    /**
     * Keyword for {@link Activity#onSaveInstanceState(Bundle)}.
     */
    private static final String STATE_KEY_OP_CONTEXT_FILES = "state_current_op_files";

    /**
     * The permissions we need to do our job.
     */
    private static String[] PERMISSIONS_STORAGE = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };

    /**
     * {@link ListView} that contains the files inside the current directory.
     */
    private ListView fileListView;

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
    private EditablePair<Operation, CopyOnWriteArrayList<FMFile>> fileOpContext = new EditablePair<>(NONE, new CopyOnWriteArrayList<>());

    /**
     * Extra used in {@link IntroActivity}.
     *
     * @see Intent#hasExtra(String)
     */
    public static final String FIRST_START_EXTRA = "firstStartDone";

    /**
     * 👀
     */
    public static final String WHITESPACE = " ";

    /**
     * If we currently are inside an archive.
     */
    private boolean exploringArchive;

    /**
     * Error text visible when no files are shown.
     */
    private View errorText;

    /**
     * Error text/Hint visible when no files are shown.
     */
    private View emptyText;

    /**
     * Getter for the fileListView.
     *
     * @return fileListView
     * @see #fileListView
     */
    public ListView getFileListView() {
        return fileListView;
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(STATE_KEY_CURRENT_DIR, currentDirectory);
        outState.putString(STATE_KEY_OP_CONTEXT_OP, fileOpContext.getFirst().name());
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            outState.putStringArray(STATE_KEY_OP_CONTEXT_FILES, fileOpContext.getSecond().stream()
                    .map(FMFile::getAbsolutePath).toArray(String[]::new));
        } else {
            ArrayList<String> fileList = new ArrayList<>();
            for (int i = 0; i < fileOpContext.getSecond().size(); i++) {
                fileList.add(fileOpContext.getSecond().get(i).getAbsolutePath());
            }
            outState.putStringArray(STATE_KEY_OP_CONTEXT_FILES, fileList.toArray(new String[0]));
        }
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
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        String dataString = intent.getDataString();
        if (dataString != null) {
            if (!dataString.isEmpty()) {
                Log.d(TAG, "Intent dataString present: '" + dataString + "'");
                File iFile = null;
                if (dataString.startsWith("content://")) {

                    new AlertDialog.Builder(this)
                            .setView(R.layout.layout_extract_now_prompt)
                            .setPositiveButton(R.string.yes, (dialog, which) -> {
                                try {
                                    dialog.dismiss();
                                    extractFromUri(dataString, currentDirectory);
                                } catch (Exception e) {
                                    Log.e(TAG, "Unable to extract archive from URI!", e);
                                }
                            })
                            .setNegativeButton(R.string.no, (dialog, which) -> {
                                try {
                                    dialog.dismiss();
                                    extractFromUri(dataString, null);
                                } catch (Exception e) {
                                    Log.e(TAG, "Unable to extract archive from URI!", e);
                                }
                            }).create().show();
                } else if (dataString.startsWith("file://")) {
                    try {
                        iFile = new File(Uri.decode(dataString.split("://")[1]));
                    } catch (IndexOutOfBoundsException e) {
                        iFile = new File(Uri.decode(dataString));
                    }
                }
                if (iFile != null && iFile.exists()) {
                    loadFileFromIntent(iFile);
                }
            }
        }
    }

    private void extractFromUri(String dataString, @Nullable String targetDir) throws Exception {
        try {
            Uri uri = Uri.parse(dataString);
            Log.d(TAG, "Extracting from uri: '" + dataString + "'...");
            File tempFile = UriUtil.createTempFileFromUri(this, uri);

            if(targetDir == null) {
                AlertDialog dia = new AlertDialog.Builder(this)
                        .setView(R.layout.layout_path_prompt_dir)
                        .setTitle(R.string.extraction_path)
                        .setNegativeButton(R.string.cancel, (dialog, which) -> dialog.dismiss())
                        .create();
                dia.setButton(DialogInterface.BUTTON_POSITIVE, getString(R.string.okay), (dialog, which) -> {
                    dialog.dismiss();
                    String pathFromInput = ((EditText) dia.findViewById(R.id.destinationPath)).getText().toString();
                    doExtractFromUri(tempFile, pathFromInput);
                });
                dia.show();
            } else {
                doExtractFromUri(tempFile, targetDir);
            }
        } catch (Exception e) {
            throw new Exception("Unable to extract archive from URI!", e);
        }
    }

    private void doExtractFromUri(File tempFile, String targetPath) {
        new ArchiveExtractionTask(this, targetPath, new FMFile(tempFile), success -> {
            if(!tempFile.delete() && tempFile.exists()) {
                Log.w(TAG, "Unable to delete temp file!");
            }
            clearFileOpCache();
            if (!success) {
                reloadCurrentDirectory();
                Toast.makeText(this, R.string.unable_to_extract_archive, Toast.LENGTH_LONG).show();
            } else {
                loadPath(targetPath);
            }
        }).execute();
    }

    /**
     * Loads a file coming from an intent.
     *
     * @param iFile the file to load
     */
    private void loadFileFromIntent(File iFile) {
        FMFile file = new FMFile(iFile);
        new ArchiveParentFinderTask(file, parentFinder ->
                new ArchiveLoaderTask(file, a -> {
                    FMArchive archiveToExtract = null;
                    if (!file.isArchive() && parentFinder.isArchive()) {
                        archiveToExtract = parentFinder.getArchiveFile();
                    } else if (file.isArchive()) {
                        archiveToExtract = a;
                    }
                    if (archiveToExtract != null) {
                        String archivePath = archiveToExtract.getFile().getAbsolutePath();
                        String archiveParent = archivePath.substring(0, archivePath.lastIndexOf(File.separator));
                        loadPath(archiveParent);
                        addFileToOpContext(EXTRACT, archiveToExtract);
                        if (new Pref<Boolean>(USE_CONTEXT_FOR_OPS_TOAST).getValue()) {
                            Toast.makeText(this, getString(R.string.file_added_to_context) + archiveToExtract.getName(), LENGTH_SHORT).show();
                        }

                        Pref<Boolean> alwaysExtractInCurrentPref = new Pref<>(ALWAYS_EXTRACT_IN_CURRENT_DIR);
                        if (alwaysExtractInCurrentPref.getValue()) {
                            finishFileOperation();
                        } else {
                            new AlertDialog.Builder(this)
                                    .setView(R.layout.layout_extract_now_prompt)
                                    .setPositiveButton(R.string.yes, (dialog, which) -> finishFileOperation())
                                    .setNeutralButton(R.string.yes_and_remember, (dialog, which) -> alwaysExtractInCurrentPref.setValue(true))
                                    .setNegativeButton(R.string.no, (dialog, which) -> Log.d(TAG, "noop")).create().show();
                        }
                    } else {
                        Toast.makeText(this, R.string.unable_to_recognize_archive_format, Toast.LENGTH_LONG).show();
                    }
                }).execute() // load the archive file
        ).execute(); // load it's contents
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getIntent().hasExtra(FIRST_START_EXTRA) && getIntent().getBooleanExtra(FIRST_START_EXTRA, false)) {
            new Pref<Boolean>(FIRST_START).setValue(false);
        } else if (new Pref<Boolean>(FIRST_START).getValue() || new Pref<Boolean>(ALWAYS_SHOW_INTRO).getValue()) {
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

        StrictMode.setVmPolicy(builder.build());

        FileActivity.verifyStoragePermissions(FileActivity.this);

        initUi();

        if (new Pref<Boolean>(UPDATE_NOTIFICATION).getValue()) {
            checkForUpdate();
        }

        if (savedInstanceState == null) {
            loadHomeDir();
        }
    }

    /**
     * Starts a {@link VersionCheckTask} to show a notification once a new version is out.
     */
    private void checkForUpdate() {
        new VersionCheckTask(result -> {
            try {
                if (result != null && !result.isEmpty()) {
                    VersionInfo.parse(v -> {
                        if (v.getLatest().newerThan(v.getCurrent())) {
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
                        Log.i(TAG, "Running version: '" + v.getCurrent() + "', latest: '" + v.getCurrent() + "'");
                    }, FileActivity.this.getPackageManager().getPackageInfo(FileActivity.this.getPackageName(), 0).versionName, result);
                } else {
                    Log.e(TAG, "Unable to fetch version!");
                }
            } catch (PackageManager.NameNotFoundException e) {
                Log.wtf(TAG, "Unable to get package name!", e);
            }
        }).execute();
    }

    /**
     * Restores {@link #currentDirectory} and {@link #fileOpContext} from a {@link Bundle} created in {@link #onSaveInstanceState(Bundle)}.
     *
     * @param savedInstanceState the previusly saved instance state.
     */
    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        loadPath(savedInstanceState.getString(STATE_KEY_CURRENT_DIR));
        Operation savedOperation = Operation.valueOf(savedInstanceState.getString(STATE_KEY_OP_CONTEXT_OP));
        String[] opContextFiles = savedInstanceState.getStringArray(STATE_KEY_OP_CONTEXT_FILES);
        if (opContextFiles == null) {
            opContextFiles = new String[]{};
        }
        List<String> savedFilePaths = Arrays.asList(opContextFiles);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            savedFilePaths.forEach(s -> addFileToOpContext(savedOperation, new FMFile(new File(s))));
        } else {
            for (int i = 0; i < savedFilePaths.size(); i++) {
                addFileToOpContext(savedOperation, new FMFile(new File(savedFilePaths.get(i))));
            }
        }
    }

    @ColorInt
    public int getColorByAttr(@AttrRes int id) {
        TypedValue typedValue = new TypedValue();
        Resources.Theme theme = getTheme();
        theme.resolveAttribute(id, typedValue, true);
        return typedValue.data;
    }

    /**
     * Initializes the navigation ui.
     */
    private void initUi() {
        fileListView = findViewById(R.id.fileView);
        errorText = findViewById(R.id.unableToLoadText);
        emptyText = findViewById(R.id.emptyDirText);
        toolbar = findViewById(R.id.toolbar);
        navigationView = findViewById(R.id.nav_view);
        headerView = navigationView.inflateHeaderView(R.layout.nav_header_main);
        currentDirectoryTextView = headerView.findViewById(R.id.currentDirectoryTextView);

        registerForContextMenu(fileListView);

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

        setSupportActionBar(toolbar);
        navigationView.setNavigationItemSelectedListener(item -> {
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

            drawer.closeDrawer(GravityCompat.START);
            return true;
        });
        loadUserBookmarks();
        setFreeSpaceText();
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);

        //noinspection deprecation
        drawer.setDrawerListener(toggle); // I ned dis
        toggle.syncState();
    }

    /**
     * Adds the free space text to the navigation drawer header.
     */
    private void setFreeSpaceText() {
        TextView diskUsageTextView = headerView.findViewById(R.id.diskUsage);

        String s = null;
        String nav_header_unit = new Pref<String>(NAV_HEADER_UNIT).getValue();
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
        bookmarks.addAll(new Pref<HashSet<String>>(BOOKMARKS).getValue());
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
        item.setIcon(R.drawable.ic_bookmark_border_black_24dp); // this is auto tinted by android
        Bookmark bookmark = new Bookmark(s, title, item);

        if (new Pref<Boolean>(BOOKMARK_EDIT_MODE).getValue()) {
            item.setActionView(R.layout.editable_menu_item);

            View v = item.getActionView();
            ImageButton deleteButton = v.findViewById(R.id.menu_item_action_delete);
            ImageButton editButton = v.findViewById(R.id.menu_item_action_edit);

            deleteButton.setImageDrawable(getDrawable(R.drawable.ic_delete_white_24dp));
            editButton.setImageDrawable(getDrawable(R.drawable.ic_edit_white_24dp));

            deleteButton.setOnClickListener(v0 -> removeBookmarkFromMenu(menu, s, bookmarks, item, bookmark));
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
    private void removeBookmarkFromMenu(Menu menu, String s, Set<String> bookmarks, MenuItem item, Bookmark bookmark) {
        menu.removeItem(item.getItemId());
        bookmarkItems.remove(bookmark);
        bookmarks.remove(s);
        new Pref<HashSet<String>>(BOOKMARKS).setValue(new HashSet<>(bookmarks));
    }

    /**
     * Loads home dir selected by user.
     */
    @SuppressLint("UseSparseArrays")
    private void loadHomeDir() {
        resetHistory();
        String absolutePath = Environment.getExternalStorageDirectory().getAbsolutePath();
        Pref<String> homeDirPreference = new Pref<>(HOME_DIR);
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
        new ArchiveParentFinderTask(new FMFile(new File(path)), archiveResult -> {
            boolean archive = archiveResult.isArchive();
            FMArchive aF = archiveResult.getArchiveFile();
            if (archive) {
                loadArchive(path, aF);
            } else {
                loadDirectory(path);
            }
        }).execute();
    }

    /**
     * Changes directory.
     *
     * @param path directory to load
     * @deprecated may only be called by {@link #loadPath(String)}
     */
    @SuppressWarnings("DeprecatedIsStillUsed") // see above
    private void loadDirectory(String path) {
        currentDirectory = path;
        new DirectoryLoaderTask(this, path, files -> {
            if (files != null) {
                fileListView.setVisibility(VISIBLE);
                errorText.setVisibility(GONE);
                emptyText.setVisibility(GONE);

                if (arrayAdapter == null || exploringArchive) {
                    if (exploringArchive) {
                        exploringArchive = false;
                    }
                    arrayAdapter = new FileArrayAdapter(this, R.layout.layout_file, files);
                    arrayAdapter.sort(FMFile::compareTo);
                    fileListView.setAdapter(arrayAdapter);
                } else {
                    ((BaseArrayAdapter) fileListView.getAdapter()).clear();
                    ((BaseArrayAdapter) fileListView.getAdapter()).addAll(files);
                    ((BaseArrayAdapter) fileListView.getAdapter()).sort(FMFile::compareTo); // sort method automatically calls `notifyDataSetChanged`
                }
            } else {
                fileListView.setVisibility(GONE);
                errorText.setVisibility(VISIBLE);
                emptyText.setVisibility(GONE);
            }

            if (historyMap == null) {
                resetHistory();
            }

            HistoryEntry entry = historyMap.get(historyCounter);
            if (entry != null && !entry.getPath().equals(currentDirectory)) {
                historyMap.put(historyCounter++, new HistoryEntry(currentDirectory, null));
            }
            setToolbarText();
            setFreeSpaceText();
        }).execute();
    }

    /**
     * Resets {@link #historyMap} and {@link #historyCounter}.
     */
    private void resetHistory() {
        historyMap = new HashMap<>();
        historyCounter = 0;
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
        exploringArchive = true;

        files = loader.loadLocationFiles();
        fileListView.setVisibility(VISIBLE);
        errorText.setVisibility(GONE);
        emptyText.setVisibility(GONE);

        arrayAdapter = new ArchiveArrayAdapter(this, R.layout.layout_file, files, archive);
        arrayAdapter.sort(FMFile::compareTo);
        fileListView.setAdapter(arrayAdapter);

        currentDirectory = path;
        historyMap.put(historyCounter++, new HistoryEntry(currentDirectory, archive));
        setToolbarText();
        setFreeSpaceText();
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
                    if (entry.getArchive() != null) {
                        toolbar.setTitle(getTitleFromPath(entry.getArchive().getAbsolutePath()));
                    } else {
                        Log.e(TAG, "Unable to get archive path from entry: '" + entry.getPath() + "'");
                        toolbar.setTitle(R.string.archive);
                    }
                }
                toolbar.setTitle(getTitleFromPath(currentDirectory));
            } else {
                toolbar.setTitle(currentDirectory);
            }
        }
        if (currentDirectoryTextView != null) {
            int maxLength = Integer.parseInt(new Pref<String>(HEADER_PATH_LENGTH).getValue());

            if (currentDirectory.length() > maxLength) {
                currentDirectoryTextView.setText(shortenDirectoryPath(maxLength));
            } else {
                currentDirectoryTextView.setText(currentDirectory);
            }
        }
        if (new Pref<Boolean>(SHOW_TOAST).getValue()) {
            Toast.makeText(this, getText(R.string.toast_cd_new_dir) + WHITESPACE + currentDirectory, Toast.LENGTH_SHORT).show();
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
        boolean operationInProgress = !(fileOpContext.getFirst().equals(NONE) || fileOpContext.getSecond().isEmpty());
        MenuItem paste = menu.findItem(R.id.action_paste).setVisible(operationInProgress)
                .setTitle(fileOpContext.getFirst().getTitle());
        if (operationInProgress) {
            String title = paste.getTitle().toString();
            if (title.contains("(")) {
                title = title.substring(0, paste.getTitle().toString().indexOf("("));
            }
            paste.setTitle(title + " (" + fileOpContext.getSecond().size() + ")");
        }
        menu.findItem(R.id.action_clear_op_context).setVisible(operationInProgress);

        boolean noOperationButSelections = fileOpContext.getFirst().equals(NONE) && !fileOpContext.getSecond().isEmpty();
        menu.findItem(R.id.action_copy).setVisible(noOperationButSelections);
        menu.findItem(R.id.action_move).setVisible(noOperationButSelections);
        menu.findItem(R.id.action_delete).setVisible(noOperationButSelections);

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
        } else if (item.getItemId() == R.id.overflow_new_file) {
            launchNewFileDialog();
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
        } else if (item.getItemId() == R.id.action_copy) {
            fileOpContext.setFirst(COPY);
            return true;
        } else if (item.getItemId() == R.id.action_move) {
            fileOpContext.setFirst(MOVE);
            return true;
        } else if (item.getItemId() == R.id.action_delete) {
            fileOpContext.setFirst(DELETE);
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
        fileOpContext.setSecond(new CopyOnWriteArrayList<>());
        reloadCurrentDirectory();
    }

    /**
     * Finishes the current file operation context.
     */
    public void finishFileOperation() {
        Operation operation = fileOpContext.getFirst();
        CopyOnWriteArrayList<FMFile> files = fileOpContext.getSecond();
        if (!operation.equals(NONE) && !files.isEmpty()) {
            if (operation.equals(COPY)) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    files.forEach((f) -> new FileCopyTask(this, s -> {
                    }, f, null).execute());
                } else { // -_-
                    for (FMFile f : files) {
                        new FileCopyTask(this, s -> {
                        }, f, null).execute();
                    }
                }
            } else if (operation.equals(MOVE)) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    files.forEach((f) -> new FileMoveTask(this, s -> {
                    }, f, null).execute());
                } else { // -_-
                    for (FMFile f : files) {
                        new FileMoveTask(this, s -> {
                        }, f, null).execute();
                    }
                }
            } else if (operation.equals(EXTRACT)) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    files.forEach((f) -> new ArchiveExtractionTask(this, currentDirectory, f, success -> {
                        clearFileOpCache();
                        reloadCurrentDirectory();
                        if (!success) {
                            Toast.makeText(this, R.string.unable_to_extract_archive, Toast.LENGTH_LONG).show();
                        }
                    }).execute());
                } else { // -_-
                    for (FMFile f : files) {
                        new ArchiveExtractionTask(this, currentDirectory, f, success -> {
                            clearFileOpCache();
                            reloadCurrentDirectory();
                            if (!success) {
                                Toast.makeText(this, R.string.unable_to_extract_archive, Toast.LENGTH_LONG).show();
                            }
                        }).execute();
                    }
                }
            } else if (operation.equals(CREATE_ZIP)) {
                AlertDialog alertDialog = arrayAdapter.getGenericFileOpDialog(
                        R.string.create_zip_file,
                        R.string.op_destination,
                        R.drawable.ic_archive_black_24dp,
                        R.layout.layout_name_prompt,
                        (d) -> {
                            String tmpName;
                            EditText editText = d.findViewById(R.id.destinationName);
                            tmpName = editText.getText().toString();
                            if (tmpName.isEmpty() || tmpName.startsWith("/")) {
                                Toast.makeText(this, R.string.err_invalid_input_zip, LENGTH_SHORT).show();
                                tmpName = null;
                            } else if (!tmpName.endsWith(".zip")) {
                                tmpName = tmpName + ".zip";
                            }

                            File destination = new File(currentDirectory + "/" + tmpName);
                            if (destination.exists()) {
                                AlertDialog.Builder builder = FileOperationTask.getFileExistsDialogBuilder(this);
                                builder.setOnDismissListener(dialogInterface -> new ArchiveCreationTask(this, files, destination, success -> {
                                    clearFileOpCache();
                                    reloadCurrentDirectory();
                                    if (!success) {
                                        Toast.makeText(this, R.string.unable_to_create_zip_file, Toast.LENGTH_LONG).show();
                                    }
                                }).execute()).show();
                            } else {
                                new ArchiveCreationTask(this, files, destination, success -> {
                                    clearFileOpCache();
                                    reloadCurrentDirectory();
                                    if (!success) {
                                        Toast.makeText(this, R.string.unable_to_create_zip_file, Toast.LENGTH_LONG).show();
                                    }
                                }).execute();
                            }
                        },
                        (d) -> Log.d(TAG, "Cancelled."));
                alertDialog.show();
            } else if (operation.equals(DELETE)) {
                new AlertDialog.Builder(this)
                        .setTitle(R.string.delete)
                        .setMessage(getString(R.string.warn_delete_mult_msg_start) + FileActivity.WHITESPACE +
                                files.size() + FileActivity.WHITESPACE +
                                getString(R.string.warn_delete_mult_msg_end))
                        .setNegativeButton(R.string.cancel, (dialogInterface, i) -> dialogInterface.cancel())
                        .setPositiveButton(R.string.yes, (dialogInterface, i) -> {
                            for (int j = 0; j < files.size(); j++) {
                                FMFile f = files.get(j);
                                int idx = j;
                                new FileDeleteTask(this, b -> {
                                    if (!b) {
                                        Toast.makeText(this, getString(R.string.err_deleting_element_mult) +
                                                        FileActivity.WHITESPACE + f.getName(),
                                                LENGTH_SHORT).show();
                                    }
                                    if (idx == files.size()) {
                                        // clear operation when last file is deleted
                                        clearFileOpCache();
                                    }
                                    reloadCurrentDirectory();
                                }, f).execute();
                            }
                            dialogInterface.dismiss();
                        }).show();
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
            newDir(new File(newDirName));
            reloadCurrentDirectory();
        });
        newDirDialog.show();
    }

    void newDir(File d) {
        if (!d.exists()) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    Files.createDirectory(d.toPath());
                } else {
                    if (!d.mkdirs()) {
                        Toast.makeText(this, R.string.err_unable_to_mkdir, Toast.LENGTH_LONG).show();
                    }
                }
            } catch (IOException e) {
                Log.e(TAG, getString(R.string.err_unable_to_mkdir), e);
            }
        } else {
            Toast.makeText(this, R.string.err_file_exists, Toast.LENGTH_LONG).show();
        }
    }

    /**
     * Launches a prompt to create a new file.
     */
    private void launchNewFileDialog() {
        AlertDialog newFileDialog = new AlertDialog.Builder(this)
                .setNegativeButton(R.string.cancel, (dialogInterface, i) -> Log.d(TAG, "Cancel pressed"))
                .setTitle(R.string.new_file)
                .setView(R.layout.layout_filename_prompt)
                .create();
        newFileDialog.setButton(DialogInterface.BUTTON_POSITIVE, getString(R.string.okay), (file, i) -> {
            String newFileName = currentDirectory + File.separator + ((EditText) newFileDialog.findViewById(R.id.filedestinationName)).getText().toString();
            newFile(new File(newFileName));
            reloadCurrentDirectory();
        });
        newFileDialog.show();
    }

    void newFile(File file) {
        if (!file.exists()) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    Files.createFile(file.toPath());
                } else {
                    if (!file.createNewFile()) {
                        Toast.makeText(this, "Unable to create file.", Toast.LENGTH_LONG).show();
                    }
                }
            } catch (IOException e) {
                Log.e(TAG, "Unable to create file.", e);
            }
        } else {
            Toast.makeText(this, "File already exists", Toast.LENGTH_LONG).show();
        }
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
        new Pref<String>(CURRENT_DIR_CACHE).setValue(currentDirectory);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        new Pref<String>(CURRENT_DIR_CACHE).setValue("");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onResume() {
        super.onResume();
        String cachedDirectory = new Pref<String>(CURRENT_DIR_CACHE).getValue();
        if (!cachedDirectory.equals("")) {
            currentDirectory = cachedDirectory;
        }
    }

    /**
     * Opens the bug report page.
     */
    private void launchBugReportTab() {
        CustomTabsIntent.Builder builder = new CustomTabsIntent.Builder();
        CustomTabColorSchemeParams params = new CustomTabColorSchemeParams.Builder()
                .setToolbarColor(getColorByAttr(R.color.default_primary_dark))
                .build();
        builder.setDefaultColorSchemeParams(params);
        CustomTabsIntent build = builder.build();
        build.launchUrl(this, Uri.parse("https://github.com/lfuelling/lrkFM/issues/new"));
    }

    /**
     * Adds a bookmark. Prompts for location.
     */
    private void promptAndAddBookmark() {
        Set<String> stringSet = new Pref<HashSet<String>>(BOOKMARKS).getValue();
        if (new Pref<Boolean>(BOOKMARK_CURRENT_FOLDER).getValue()) {
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
                new Pref<Set<String>>(BOOKMARKS).setValue(stringSet);
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
            if (new Pref<Boolean>(USE_CONTEXT_FOR_OPS_TOAST).getValue()) {
                new VibratingToast(this, getString(R.string.switching_op_mode), Toast.LENGTH_SHORT);
            }
            fileOpContext.setFirst(op);
            fileOpContext.setSecond(new CopyOnWriteArrayList<>());
        }
        fileOpContext.getSecond().add(f);
    }

    public EditablePair<Operation, CopyOnWriteArrayList<FMFile>> getFileOpContext() {
        return fileOpContext;
    }
}
