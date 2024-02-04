package io.lerk.lrkFM.activities.file

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.app.PendingIntent
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.StrictMode
import android.os.StrictMode.VmPolicy
import android.util.Log
import android.util.TypedValue
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.AttrRes
import androidx.annotation.ColorInt
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.content.res.AppCompatResources
import androidx.appcompat.widget.Toolbar
import androidx.browser.customtabs.CustomTabColorSchemeParams
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.drawerlayout.widget.DrawerLayout.SimpleDrawerListener
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.navigation.NavigationView
import io.lerk.lrkFM.DiskUtil
import io.lerk.lrkFM.EditablePair
import io.lerk.lrkFM.LrkFMApp
import io.lerk.lrkFM.Pref
import io.lerk.lrkFM.R
import io.lerk.lrkFM.UriUtil
import io.lerk.lrkFM.VibratingToast
import io.lerk.lrkFM.activities.IntroActivity
import io.lerk.lrkFM.activities.SettingsActivity
import io.lerk.lrkFM.activities.themed.ThemedAppCompatActivity
import io.lerk.lrkFM.adapter.ArchiveArrayAdapter
import io.lerk.lrkFM.adapter.BaseArrayAdapter
import io.lerk.lrkFM.adapter.FileArrayAdapter
import io.lerk.lrkFM.consts.Operation
import io.lerk.lrkFM.consts.PreferenceEntity
import io.lerk.lrkFM.entities.Bookmark
import io.lerk.lrkFM.entities.FMArchive
import io.lerk.lrkFM.entities.FMFile
import io.lerk.lrkFM.entities.HistoryEntry
import io.lerk.lrkFM.tasks.DirectoryLoaderTask
import io.lerk.lrkFM.tasks.VersionCheckTask
import io.lerk.lrkFM.tasks.archive.ArchiveCreationTask
import io.lerk.lrkFM.tasks.archive.ArchiveExtractionTask
import io.lerk.lrkFM.tasks.archive.ArchiveLoaderTask
import io.lerk.lrkFM.tasks.archive.ArchiveParentFinderTask
import io.lerk.lrkFM.tasks.operation.FileCopyTask
import io.lerk.lrkFM.tasks.operation.FileDeleteTask
import io.lerk.lrkFM.tasks.operation.FileMoveTask
import io.lerk.lrkFM.tasks.operation.FileOperationTask
import io.lerk.lrkFM.version.VersionInfo
import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.util.TreeSet
import java.util.concurrent.CopyOnWriteArrayList
import java.util.function.Consumer

class FileActivity : ThemedAppCompatActivity() {
    /**
     * Getter for the fileListView.
     *
     * @return fileListView
     * @see .fileListView
     */
    /**
     * [ListView] that contains the files inside the current directory.
     */
    var fileListView: ListView? = null
        private set

    /**
     * Bookmarks.
     */
    private var bookmarkItems: HashSet<Bookmark>? = null
    /**
     * Getter for the current directory.
     *
     * @return the current directory.
     */
    /**
     * The current directory (empty when starting the app).
     */
    var currentDirectory: String? = ""
        private set

    /**
     * The [Toolbar].
     */
    private var toolbar: Toolbar? = null

    /**
     * The navigation drawer.
     */
    private var navigationView: NavigationView? = null

    /**
     * The [TextView] containing the shortened path of the current directoy.
     *
     * @see .shortenDirectoryPath
     */
    private var currentDirectoryTextView: TextView? = null

    /**
     * The header view of the [.navigationView].
     */
    private var headerView: View? = null

    /**
     * The history.
     */
    private var historyMap: HashMap<Int?, HistoryEntry>? = null

    /**
     * The historyCounter (sorry).
     */
    private var historyCounter: Int? = null

    /**
     * The [android.widget.ArrayAdapter] implementation used in the [.fileListView].
     */
    private var arrayAdapter: BaseArrayAdapter? = null

    /**
     * The file operation context containing the [Operation] to do and a list of [FMFile] objects.
     */
    val fileOpContext = EditablePair<Operation?, CopyOnWriteArrayList<FMFile?>>(
        Operation.NONE,
        CopyOnWriteArrayList()
    )

    /**
     * If we currently are inside an archive.
     */
    private var exploringArchive = false

    /**
     * Error text visible when no files are shown.
     */
    private var errorText: View? = null

    /**
     * Error text/Hint visible when no files are shown.
     */
    private var emptyText: View? = null
    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(STATE_KEY_CURRENT_DIR, currentDirectory)
        outState.putString(STATE_KEY_OP_CONTEXT_OP, fileOpContext.first?.name)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            outState.putStringArray(STATE_KEY_OP_CONTEXT_FILES,
                fileOpContext.second.map { it?.absolutePath }.toTypedArray()
            )
        } else {
            val fileList = ArrayList<String?>()
            for (i in fileOpContext.second.indices) {
                fileList.add(fileOpContext.second[i]?.absolutePath)
            }
            outState.putStringArray(STATE_KEY_OP_CONTEXT_FILES, fileList.toTypedArray<String?>())
        }
    }

    /**
     * {@inheritDoc}
     */
    override fun onPostResume() {
        super.onPostResume()
        setFreeSpaceText()
    }

    /**
     * {@inheritDoc}
     */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        val dataString = intent.dataString
        if (dataString != null) {
            if (dataString.isNotEmpty()) {
                Log.d(TAG, "Intent dataString present: '$dataString'")
                var iFile: File? = null
                if (dataString.startsWith("content://")) {
                    AlertDialog.Builder(this)
                        .setView(R.layout.layout_extract_now_prompt)
                        .setPositiveButton(R.string.yes) { dialog: DialogInterface, which: Int ->
                            try {
                                dialog.dismiss()
                                extractFromUri(dataString, currentDirectory)
                            } catch (e: Exception) {
                                Log.e(TAG, "Unable to extract archive from URI!", e)
                            }
                        }
                        .setNegativeButton(R.string.no) { dialog: DialogInterface, which: Int ->
                            try {
                                dialog.dismiss()
                                extractFromUri(dataString, null)
                            } catch (e: Exception) {
                                Log.e(TAG, "Unable to extract archive from URI!", e)
                            }
                        }.create().show()
                } else if (dataString.startsWith("file://")) {
                    iFile = try {
                        File(
                            Uri.decode(
                                dataString.split("://".toRegex()).dropLastWhile { it.isEmpty() }
                                    .toTypedArray()[1]))
                    } catch (e: IndexOutOfBoundsException) {
                        File(Uri.decode(dataString))
                    }
                }
                if (iFile != null && iFile.exists()) {
                    loadFileFromIntent(iFile)
                }
            }
        }
    }

    @Throws(Exception::class)
    private fun extractFromUri(dataString: String, targetDir: String?) {
        try {
            val uri = Uri.parse(dataString)
            Log.d(TAG, "Extracting from uri: '$dataString'...")
            val tempFile = UriUtil.createTempFileFromUri(this, uri)
            if (targetDir == null) {
                val dia = AlertDialog.Builder(this)
                    .setView(R.layout.layout_path_prompt_dir)
                    .setTitle(R.string.extraction_path)
                    .setNegativeButton(R.string.cancel) { dialog: DialogInterface, _: Int -> dialog.dismiss() }
                    .create()
                dia.setButton(
                    DialogInterface.BUTTON_POSITIVE,
                    getString(R.string.okay)
                ) { dialog: DialogInterface, _: Int ->
                    dialog.dismiss()
                    val pathFromInput =
                        (dia.findViewById<View>(R.id.destinationPath) as EditText).text.toString()
                    doExtractFromUri(tempFile, pathFromInput)
                }
                dia.show()
            } else {
                doExtractFromUri(tempFile, targetDir)
            }
        } catch (e: Exception) {
            throw Exception("Unable to extract archive from URI!", e)
        }
    }

    private fun doExtractFromUri(tempFile: File, targetPath: String) {
        ArchiveExtractionTask(this, targetPath, FMFile(tempFile)) { success: Boolean? ->
            if (!tempFile.delete() && tempFile.exists()) {
                Log.w(TAG, "Unable to delete temp file!")
            }
            clearFileOpCache()
            if (!success!!) {
                reloadCurrentDirectory()
                Toast.makeText(this, R.string.unable_to_extract_archive, Toast.LENGTH_LONG).show()
            } else {
                loadPath(targetPath)
            }
        }.execute()
    }

    /**
     * Loads a file coming from an intent.
     *
     * @param iFile the file to load
     */
    private fun loadFileFromIntent(iFile: File) {
        val file = FMFile(iFile)
        ArchiveParentFinderTask(
            file
        ) { parentFinder: ArchiveParentFinder ->
            ArchiveLoaderTask(file) { a: FMArchive? ->
                var archiveToExtract: FMArchive? = null
                if (!file.isArchive && parentFinder.isArchive) {
                    archiveToExtract = parentFinder.archiveFile
                } else if (file.isArchive) {
                    archiveToExtract = a
                }
                if (archiveToExtract != null) {
                    val archivePath = archiveToExtract.file.absolutePath
                    val archiveParent =
                        archivePath.substring(0, archivePath.lastIndexOf(File.separator))
                    loadPath(archiveParent)
                    addFileToOpContext(Operation.EXTRACT, archiveToExtract)
                    if (Pref<Boolean>(PreferenceEntity.USE_CONTEXT_FOR_OPS_TOAST).value == true) {
                        Toast.makeText(
                            this,
                            getString(R.string.file_added_to_context) + archiveToExtract.name,
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    val alwaysExtractInCurrentPref =
                        Pref<Boolean>(PreferenceEntity.ALWAYS_EXTRACT_IN_CURRENT_DIR)
                    if (alwaysExtractInCurrentPref.value == true) {
                        finishFileOperation()
                    } else {
                        AlertDialog.Builder(this)
                            .setView(R.layout.layout_extract_now_prompt)
                            .setPositiveButton(R.string.yes) { _: DialogInterface?, _: Int -> finishFileOperation() }
                            .setNeutralButton(R.string.yes_and_remember) { _: DialogInterface?, _: Int ->
                                alwaysExtractInCurrentPref.value = true
                            }
                            .setNegativeButton(R.string.no) { _: DialogInterface?, _: Int ->
                                Log.d(
                                    TAG,
                                    "noop"
                                )
                            }
                            .create().show()
                    }
                } else {
                    Toast.makeText(
                        this,
                        R.string.unable_to_recognize_archive_format,
                        Toast.LENGTH_LONG
                    ).show()
                }
            }.execute()
        } // load the archive file
            .execute() // load it's contents
    }

    /**
     * {@inheritDoc}
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (intent.hasExtra(FIRST_START_EXTRA) && intent.getBooleanExtra(
                FIRST_START_EXTRA,
                false
            )
        ) {
            Pref<Boolean>(PreferenceEntity.FIRST_START).value = false
        } else if (Pref<Boolean>(PreferenceEntity.FIRST_START).value == true ||
            Pref<Boolean>(PreferenceEntity.ALWAYS_SHOW_INTRO).value == true
        ) {
            startActivity(Intent(this, IntroActivity::class.java))
            finish()
        }
        setContentView(R.layout.activity_main)
        val builder = VmPolicy.Builder()
            .detectActivityLeaks()
            .detectLeakedClosableObjects()
            .detectLeakedRegistrationObjects()
            .detectLeakedSqlLiteObjects()
        builder.detectCleartextNetwork()
        StrictMode.setVmPolicy(builder.build())
        verifyStoragePermissions(this@FileActivity)
        initUi()
        if (Pref<Boolean>(PreferenceEntity.UPDATE_NOTIFICATION).value == true) {
            checkForUpdate()
        }
        if (savedInstanceState == null) {
            loadHomeDir()
        }
    }

    /**
     * Starts a [VersionCheckTask] to show a notification once a new version is out.
     */
    private fun checkForUpdate() {
        VersionCheckTask { result: String? ->
            try {
                if (!result.isNullOrEmpty()) {
                    VersionInfo.Companion.parse(
                        { v: VersionInfo? ->
                            if (v?.latest?.newerThan(v.current) == true) {
                                val intent = Intent(
                                    Intent.ACTION_VIEW,
                                    Uri.parse("market://details?id=io.lerk.lrkfm")
                                )
                                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                                val pendingIntent = PendingIntent.getActivity(
                                    this@FileActivity.applicationContext,
                                    0,
                                    intent,
                                    PendingIntent.FLAG_IMMUTABLE
                                )
                                val notificationBuilder: NotificationCompat.Builder =
                                    NotificationCompat.Builder(
                                        this@FileActivity,
                                        LrkFMApp.Companion.CHANNEL_ID
                                    )
                                        .setSmallIcon(R.drawable.ic_launcher)
                                        .setContentTitle(getText(R.string.notif_update_title))
                                        .setContentText(getText(R.string.notif_update_content).toString() + result)
                                        .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                                        .setContentIntent(pendingIntent)
                                        .setAutoCancel(true)
                                if (ActivityCompat.checkSelfPermission(
                                        this,
                                        Manifest.permission.POST_NOTIFICATIONS
                                    ) == PackageManager.PERMISSION_GRANTED
                                ) {
                                    NotificationManagerCompat.from(this@FileActivity.applicationContext)
                                        .notify(
                                            VersionCheckTask.Companion.NEW_VERSION_NOTIF,
                                            notificationBuilder.build()
                                        )
                                } else {
                                    Log.i(
                                        TAG,
                                        "Unable to notify for new version: Permission not granted!"
                                    )
                                }

                            }
                            Log.i(
                                TAG,
                                "Running version: '" + v!!.current + "', latest: '" + v.current + "'"
                            )
                        },
                        this@FileActivity.packageManager.getPackageInfo(
                            this@FileActivity.packageName,
                            0
                        ).versionName,
                        result
                    )
                } else {
                    Log.e(TAG, "Unable to fetch version!")
                }
            } catch (e: PackageManager.NameNotFoundException) {
                Log.wtf(TAG, "Unable to get package name!", e)
            }
        }.execute()
    }

    /**
     * Restores [.currentDirectory] and [.fileOpContext] from a [Bundle] created in [.onSaveInstanceState].
     *
     * @param savedInstanceState the previously saved instance state.
     */
    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        loadPath(savedInstanceState.getString(STATE_KEY_CURRENT_DIR))
        val savedOperation = Operation.valueOf(
            savedInstanceState.getString(STATE_KEY_OP_CONTEXT_OP)!!
        )
        var opContextFiles = savedInstanceState.getStringArray(STATE_KEY_OP_CONTEXT_FILES)
        if (opContextFiles == null) {
            opContextFiles = arrayOf()
        }
        val savedFilePaths = listOf(*opContextFiles)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            savedFilePaths.forEach(Consumer { s: String? ->
                addFileToOpContext(
                    savedOperation, FMFile(
                        File(s!!)
                    )
                )
            })
        } else {
            for (i in savedFilePaths.indices) {
                addFileToOpContext(savedOperation, FMFile(File(savedFilePaths[i])))
            }
        }
    }

    @ColorInt
    fun getColorByAttr(@AttrRes id: Int): Int {
        val typedValue = TypedValue()
        val theme = theme
        theme.resolveAttribute(id, typedValue, true)
        return typedValue.data
    }

    /**
     * Initializes the navigation ui.
     */
    private fun initUi() {
        fileListView = findViewById(R.id.fileView)
        errorText = findViewById(R.id.unableToLoadText)
        emptyText = findViewById(R.id.emptyDirText)
        toolbar = findViewById(R.id.toolbar)
        navigationView = findViewById(R.id.nav_view)
        headerView = navigationView?.inflateHeaderView(R.layout.nav_header_main)
        currentDirectoryTextView = headerView?.findViewById(R.id.currentDirectoryTextView)
        registerForContextMenu(fileListView)
        val fab = findViewById<FloatingActionButton>(R.id.fab)
        fab.setOnClickListener { _: View? ->
            if (historyCounter!! > 0 && historyMap!!.isNotEmpty()) {
                removeFromHistoryAndGoBack()
            } else {
                currentDirectory?.let { dir ->
                    when {
                        dir.startsWith("/") && dir != "/" -> {
                            loadPath(File(dir).parent)
                        }

                        else -> {
                            Toast.makeText(
                                applicationContext,
                                R.string.err_already_at_file_root,
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                } ?: run {
                    Log.wtf(TAG, "currentDirectory is null!")
                }
            }
        }
        val drawer = findViewById<DrawerLayout>(R.id.drawer_layout)
        drawer.addDrawerListener(object : SimpleDrawerListener() {
            override fun onDrawerStateChanged(newState: Int) {
                loadUserBookmarks()
                super.onDrawerStateChanged(newState)
            }
        })
        setSupportActionBar(toolbar)
        navigationView?.setNavigationItemSelectedListener(NavigationView.OnNavigationItemSelectedListener { item: MenuItem ->
            val id = item.itemId
            if (id == R.id.nav_home) {
                loadHomeDir()
            } else if (id == R.id.nav_path) {
                promptAndLoadPath()
            } else if (id == R.id.nav_settings) {
                launchSettings()
            } else if (id == R.id.nav_add_bookmark) {
                promptAndAddBookmark()
            } else if (id == R.id.nav_bug_report) {
                launchBugReportTab()
            } else {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    bookmarkItems!!.forEach(Consumer { bookmark: Bookmark ->
                        if (bookmark.menuItem.itemId == id) {
                            loadPath(bookmark.path)
                        }
                    })
                } else {
                    for (bookmark in bookmarkItems!!) {
                        if (bookmark.menuItem.itemId == id) {
                            loadPath(bookmark.path)
                        }
                    }
                }
            }
            drawer.closeDrawer(GravityCompat.START)
            true
        })
        loadUserBookmarks()
        setFreeSpaceText()
        val toggle = ActionBarDrawerToggle(
            this,
            drawer,
            toolbar,
            R.string.navigation_drawer_open,
            R.string.navigation_drawer_close
        )
        drawer.setDrawerListener(toggle) // I ned dis
        toggle.syncState()
    }

    /**
     * Adds the free space text to the navigation drawer header.
     */
    private fun setFreeSpaceText() {
        val diskUsageTextView = headerView!!.findViewById<TextView>(R.id.diskUsage)
        var s: String? = null
        val navHeaderUnit = Pref<String>(PreferenceEntity.NAV_HEADER_UNIT).value
        if (navHeaderUnit == getString(R.string.pref_header_unit_m_value)) {
            s = DiskUtil.freeSpaceMebi(true).toString() + " MiB " + getString(R.string.free)
        } else if (navHeaderUnit == getString(R.string.pref_header_unit_g_value)) {
            s = DiskUtil.freeSpaceGibi(true).toString() + " GiB " + getString(R.string.free)
        }
        if (s == null) {
            Log.e(TAG, "Unable to get free space! requested: $navHeaderUnit")
        }
        diskUsageTextView.text = s
    }

    /**
     * Loads the bookmarks into the menu.
     */
    private fun loadUserBookmarks() {
        val menu = navigationView!!.menu
        val bookmarks: TreeSet<String> = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            TreeSet(Comparator.comparing { s: String? -> getTitleFromPath(s) })
        } else { // FUCK THEM PUNY OLD VERSION USERS!
            TreeSet { o1: String?, o2: String? -> getTitleFromPath(o1).compareTo(getTitleFromPath(o2)) }
        }
        bookmarks.addAll(Pref<HashSet<String>>(PreferenceEntity.BOOKMARKS).value!!)
        if (!bookmarks.isEmpty()) {
            bookmarkItems = HashSet()
            menu.removeGroup(R.id.bookmarksMenuGroup)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                bookmarks.forEach(Consumer { s: String -> addBookmarkToMenu(menu, s, bookmarks) })
            } else { // also fuck all of those vendors that don't update. You are making development ugly!
                for (s in bookmarks) {
                    addBookmarkToMenu(menu, s, bookmarks)
                }
            }
        } else {
            Log.d(TAG, "User has no bookmarks")
        }
    }

    /**
     * Adds a bookmark to the navigation drawer.
     *
     * @param menu      the menu
     * @param s         the bookmark's display text
     * @param bookmarks the bookmarks
     */
    private fun addBookmarkToMenu(menu: Menu, s: String, bookmarks: MutableSet<String>) {
        val title = getTitleFromPath(s)
        val item = menu.add(R.id.bookmarksMenuGroup, Menu.NONE, 2, title)
        item.setIcon(R.drawable.ic_bookmark_border_black_24dp) // this is auto tinted by android
        val bookmark = Bookmark(s, title, item)
        if (Pref<Boolean>(PreferenceEntity.BOOKMARK_EDIT_MODE).value == true) {
            item.setActionView(R.layout.editable_menu_item)
            val v = item.actionView
            val deleteButton = v!!.findViewById<ImageButton>(R.id.menu_item_action_delete)
            val editButton = v.findViewById<ImageButton>(R.id.menu_item_action_edit)
            deleteButton.setImageDrawable(
                AppCompatResources.getDrawable(
                    applicationContext,
                    R.drawable.ic_delete_white_24dp
                )
            )
            editButton.setImageDrawable(
                AppCompatResources.getDrawable(
                    applicationContext,
                    R.drawable.ic_edit_white_24dp
                )
            )
            deleteButton.setOnClickListener { _: View? ->
                removeBookmarkFromMenu(
                    menu,
                    s,
                    bookmarks,
                    item,
                    bookmark
                )
            }
            editButton.setOnClickListener { _: View? ->
                val dia = AlertDialog.Builder(this)
                    .setView(R.layout.layout_path_prompt)
                    .setNegativeButton(R.string.cancel) { _: DialogInterface?, _: Int ->
                        Log.d(
                            TAG,
                            "Operation canceled."
                        )
                    }
                    .create()
                dia.setOnShowListener { _: DialogInterface? ->
                    (dia.findViewById<View>(R.id.destinationPath) as EditText).setText(
                        s
                    )
                }
                dia.setButton(
                    DialogInterface.BUTTON_POSITIVE,
                    getString(R.string.okay)
                ) { _: DialogInterface?, _: Int ->
                    removeBookmarkFromMenu(menu, s, bookmarks, item, bookmark)
                    addBookmarkToMenu(
                        menu,
                        (dia.findViewById<View>(R.id.destinationPath) as EditText).text.toString(),
                        bookmarks
                    )
                }
                dia.show()
            }
        }
        bookmarkItems!!.add(bookmark)
    }

    /**
     * Returns the last part of a path.
     *
     * @param s absolute path
     * @return last item of path
     */
    fun getTitleFromPath(s: String?): String {
        return if (s != null) {
            if (s != ROOT_DIR) {
                val split = s.split("/".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                var i = split.size - 1
                if (i < 0) {
                    i = 0
                }
                split[i]
            } else {
                s
            }
        } else {
            "null"
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
    private fun removeBookmarkFromMenu(
        menu: Menu,
        s: String,
        bookmarks: MutableSet<String>,
        item: MenuItem,
        bookmark: Bookmark
    ) {
        menu.removeItem(item.itemId)
        bookmarkItems!!.remove(bookmark)
        bookmarks.remove(s)
        Pref<HashSet<String>>(PreferenceEntity.BOOKMARKS).value =
            HashSet(bookmarks)
    }

    /**
     * Loads home dir selected by user.
     */
    @SuppressLint("UseSparseArrays")
    private fun loadHomeDir() {
        resetHistory()
        val absolutePath = Environment.getExternalStorageDirectory().absolutePath
        val homeDirPreference = Pref<String>(PreferenceEntity.HOME_DIR)
        var startDir = homeDirPreference.value
        if (startDir == null) {
            homeDirPreference.value = absolutePath
            startDir = absolutePath
        }
        loadPath(startDir)
    }

    /**
     * F5
     */
    fun reloadCurrentDirectory() {
        loadPath(currentDirectory)
    }

    @Suppress("deprecation") // one of the two methods that may call the "deprecated" functions.
    fun loadArchivePath(path: String?, archive: FMArchive?) {
        loadArchive(path, archive)
    }

    /**
     * Changes directory.
     *
     * @param path the path to cd into
     */
    @Suppress("deprecation") // one of the two methods that may call the "deprecated" functions.
    fun loadPath(path: String?) {
        ArchiveParentFinderTask(FMFile(File(path!!))) { archiveResult: ArchiveParentFinder ->
            val archive = archiveResult.isArchive
            val aF = archiveResult.archiveFile
            if (archive) {
                loadArchive(path, aF)
            } else {
                loadDirectory(path)
            }
        }.execute()
    }

    /**
     * Changes directory.
     *
     * @param path directory to load
     */
    @Deprecated("may only be called by {@link #loadPath(String)}")  // see above
    private fun loadDirectory(path: String?) {
        currentDirectory = path
        DirectoryLoaderTask(this, path) { files ->
            fileListView!!.visibility = View.VISIBLE
            errorText!!.visibility = View.GONE
            emptyText!!.visibility = View.GONE
            if (arrayAdapter == null || exploringArchive) {
                if (exploringArchive) {
                    exploringArchive = false
                }
                arrayAdapter = FileArrayAdapter(this, R.layout.layout_file, files)
                arrayAdapter?.sort { obj, o -> obj!!.compareTo(o!!) }
                fileListView!!.adapter = arrayAdapter
            } else {
                (fileListView!!.adapter as BaseArrayAdapter).clear()
                (fileListView!!.adapter as BaseArrayAdapter).addAll(files)
                (fileListView!!.adapter as BaseArrayAdapter).sort { obj, o ->
                    obj!!.compareTo(
                        o!!
                    )
                } // sort method automatically calls `notifyDataSetChanged`
            }
            if (historyMap == null) {
                resetHistory()
            }
            val entry = historyMap!![historyCounter]
            if (entry != null && entry.path != currentDirectory) {
                historyCounter = historyCounter!! + 1
                historyMap!![historyCounter] = HistoryEntry(currentDirectory, null)
            }
            setToolbarText()
            setFreeSpaceText()
        }.execute()
    }

    /**
     * Resets [.historyMap] and [.historyCounter].
     */
    private fun resetHistory() {
        historyMap = HashMap()
        historyCounter = 0
    }

    /**
     * Changes "directory" into an archive.
     *
     * @param path archive to load
     */
    @Deprecated("may only be called by {@link #loadPath(String)}")  // see above
    private fun loadArchive(path: String?, archive: FMArchive?) {
        val files: ArrayList<FMFile>
        val loader = ArchiveLoader(archive!!, path)
        exploringArchive = true
        files = loader.loadLocationFiles()
        fileListView!!.visibility = View.VISIBLE
        errorText!!.visibility = View.GONE
        emptyText!!.visibility = View.GONE
        arrayAdapter = ArchiveArrayAdapter(this, R.layout.layout_file, files, archive)
        arrayAdapter?.sort { obj, o -> obj!!.compareTo(o!!) }
        fileListView!!.adapter = arrayAdapter
        currentDirectory = path
        historyCounter = historyCounter!! + 1
        historyMap!![historyCounter] = HistoryEntry(currentDirectory, null)
        setToolbarText()
        setFreeSpaceText()
    }

    /**
     * Removes the most recent item from the history and goes back.
     */
    private fun removeFromHistoryAndGoBack() {
        var key = historyCounter!! - 1
        if (key < 0) {
            key = 0
        }
        val entry = historyMap!![key]
        historyMap!!.remove(key)
        if (entry != null && entry.path!!.isNotEmpty() && !entry.isInArchive) {
            loadPath(entry.path)
        } else if (entry != null && entry.path!!.isNotEmpty() && entry.isInArchive) {
            loadArchivePath(entry.path, entry.archive)
        }
        historyCounter = key
    }

    /**
     * Sets the text in the toolbar.
     */
    private fun setToolbarText() {
        if (toolbar != null) {
            if (currentDirectory != ROOT_DIR) {
                val entry = historyMap!![historyCounter!! - 1]
                if (entry != null && entry.isInArchive) {
                    if (entry.archive != null) {
                        toolbar!!.title = getTitleFromPath(entry.archive.absolutePath)
                    } else {
                        Log.e(TAG, "Unable to get archive path from entry: '" + entry.path + "'")
                        toolbar!!.setTitle(R.string.archive)
                    }
                }
                toolbar!!.title = getTitleFromPath(currentDirectory)
            } else {
                toolbar!!.title = currentDirectory
            }
        }
        if (currentDirectoryTextView != null) {
            val maxLength = Pref<String>(PreferenceEntity.HEADER_PATH_LENGTH).value!!.toInt()
            if (currentDirectory!!.length > maxLength) {
                currentDirectoryTextView!!.text = shortenDirectoryPath(maxLength)
            } else {
                currentDirectoryTextView!!.text = currentDirectory
            }
        }
        if (Pref<Boolean>(PreferenceEntity.SHOW_TOAST).value == true) {
            Toast.makeText(
                this,
                getText(R.string.toast_cd_new_dir).toString() + WHITESPACE + currentDirectory,
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    /**
     * Shortens the current path by limiting directory paths
     * to one char length until the desired maxLength is reached.
     *
     * @param maxLength max length (chars) of the result
     * @return the shortened string
     */
    private fun shortenDirectoryPath(maxLength: Int): String {
        val res = arrayOf(currentDirectory)
        val dirs =
            ArrayList(listOf(*res[0]!!.split("/".toRegex()).dropLastWhile { it.isEmpty() }
                .toTypedArray()))
        var i = 0
        while (dirs.size >= i) {
            if (dirs[0].isEmpty()) {
                dirs.removeAt(0)
                Log.d(TAG, "Element was empty and removed.")
            }
            res[0] = ""
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                dirs.forEach(Consumer { s: String -> res[0] += File.separator + s })
            } else { // I could save two lines of code without old versions!
                for (s in dirs) {
                    res[0] += File.separator + s
                }
            }
            if (res[0]!!.length > maxLength) {
                try {
                    dirs[i] = dirs[i].substring(0, 1)
                } catch (e: IndexOutOfBoundsException) {
                    Log.d(TAG, "This can happen.", e)
                }
            }
            i++
        }
        if (res.size > maxLength) {
            Log.w(TAG, "Could not shorten the string any further :c")
        }
        return res[0]!!
    }

    /**
     * Handles the back button.
     */
    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        val drawer = findViewById<DrawerLayout>(R.id.drawer_layout)
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START)
        } else if (historyCounter!! > 0 && historyMap!!.isNotEmpty()) {
            removeFromHistoryAndGoBack()
        } else {
            super.onBackPressed()
        }
    }

    /**
     * Prepares the options menu.
     *
     * @param menu the menu
     * @return true
     */
    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        super.onPrepareOptionsMenu(menu)
        val operationInProgress =
            !(fileOpContext.first == Operation.NONE || fileOpContext.second.isEmpty())
        val paste = menu.findItem(R.id.action_paste).setVisible(operationInProgress)
            .setTitle(fileOpContext.first!!.title)
        if (operationInProgress) {
            var title = paste.title.toString()
            if (title.contains("(")) {
                title = title.substring(0, paste.title.toString().indexOf("("))
            }
            paste.setTitle(title + " (" + fileOpContext.second.size + ")")
        }
        menu.findItem(R.id.action_clear_op_context).setVisible(operationInProgress)
        val noOperationButSelections =
            fileOpContext.first == Operation.NONE && !fileOpContext.second.isEmpty()
        menu.findItem(R.id.action_copy).setVisible(noOperationButSelections)
        menu.findItem(R.id.action_move).setVisible(noOperationButSelections)
        menu.findItem(R.id.action_delete).setVisible(noOperationButSelections)
        return true
    }

    /**
     * Creates the options menu.
     *
     * @param menu the menu to create
     * @return true
     */
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.main, menu)
        return true
    }

    /**
     * Called when an item in the options menu is clicked.
     *
     * @param item the id of the clicked item
     * @return false if item id not "found"
     */
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.settings -> {
                launchSettings()
                true
            }

            R.id.overflow_new_file -> {
                launchNewFileDialog()
                true
            }

            R.id.new_directory -> {
                launchNewDirDialog()
                true
            }

            R.id.action_reload_view -> {
                reloadCurrentDirectory()
                true
            }

            R.id.action_paste -> {
                finishFileOperation()
                true
            }

            R.id.action_copy -> {
                fileOpContext.first = Operation.COPY
                true
            }

            R.id.action_move -> {
                fileOpContext.first = Operation.MOVE
                true
            }

            R.id.action_delete -> {
                fileOpContext.first = Operation.DELETE
                finishFileOperation()
                true
            }

            R.id.action_clear_op_context -> {
                clearFileOpCache()
                true
            }

            else -> {
                false
            }
        }
    }

    /**
     * Clears the current file operation context.
     */
    fun clearFileOpCache() {
        fileOpContext.first = Operation.NONE
        fileOpContext.second = CopyOnWriteArrayList()
        reloadCurrentDirectory()
    }

    /**
     * Finishes the current file operation context.
     */
    fun finishFileOperation() {
        val operation = fileOpContext.first
        val files = fileOpContext.second
        if (operation != Operation.NONE && !files.isEmpty()) {
            if (operation == Operation.COPY) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    files.forEach(Consumer { f: FMFile? ->
                        FileCopyTask(
                            this,
                            { _: Boolean? -> },
                            f,
                            null
                        ).execute()
                    })
                } else { // -_-
                    for (f in files) {
                        FileCopyTask(this, { _: Boolean? -> }, f, null).execute()
                    }
                }
            } else if (operation == Operation.MOVE) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    files.forEach(Consumer { f: FMFile? ->
                        FileMoveTask(
                            this,
                            { _: Boolean? -> },
                            f,
                            null
                        ).execute()
                    })
                } else { // -_-
                    for (f in files) {
                        FileMoveTask(this, { s: Boolean? -> }, f, null).execute()
                    }
                }
            } else if (operation == Operation.EXTRACT) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    files.forEach(Consumer { f: FMFile? ->
                        ArchiveExtractionTask(this, currentDirectory, f) { success: Boolean? ->
                            clearFileOpCache()
                            reloadCurrentDirectory()
                            if (!success!!) {
                                Toast.makeText(
                                    this,
                                    R.string.unable_to_extract_archive,
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        }.execute()
                    })
                } else { // -_-
                    for (f in files) {
                        ArchiveExtractionTask(this, currentDirectory, f) { success: Boolean? ->
                            clearFileOpCache()
                            reloadCurrentDirectory()
                            if (!success!!) {
                                Toast.makeText(
                                    this,
                                    R.string.unable_to_extract_archive,
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        }.execute()
                    }
                }
            } else if (operation == Operation.CREATE_ZIP) {
                val alertDialog = arrayAdapter!!.getGenericFileOpDialog(
                    R.string.create_zip_file,
                    R.string.op_destination,
                    R.drawable.ic_archive_black_24dp,
                    R.layout.layout_name_prompt,
                    { d ->
                        var tmpName: String?
                        val editText = d?.findViewById<EditText>(R.id.destinationName)
                        tmpName = editText?.text.toString()
                        if (tmpName.isEmpty() || tmpName.startsWith("/")) {
                            Toast.makeText(this, R.string.err_invalid_input_zip, Toast.LENGTH_SHORT)
                                .show()
                            tmpName = null
                        } else if (!tmpName.endsWith(".zip")) {
                            tmpName = "$tmpName.zip"
                        }
                        val destination = File("$currentDirectory/$tmpName")
                        if (destination.exists()) {
                            val builder: AlertDialog.Builder =
                                FileOperationTask.Companion.getFileExistsDialogBuilder(this)
                            builder.setOnDismissListener { _: DialogInterface? ->
                                ArchiveCreationTask(this, files, destination) { success: Boolean? ->
                                    clearFileOpCache()
                                    reloadCurrentDirectory()
                                    if (!success!!) {
                                        Toast.makeText(
                                            this,
                                            R.string.unable_to_create_zip_file,
                                            Toast.LENGTH_LONG
                                        ).show()
                                    }
                                }.execute()
                            }.show()
                        } else {
                            ArchiveCreationTask(this, files, destination) { success: Boolean? ->
                                clearFileOpCache()
                                reloadCurrentDirectory()
                                if (!success!!) {
                                    Toast.makeText(
                                        this,
                                        R.string.unable_to_create_zip_file,
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                            }.execute()
                        }
                    }
                ) { _: AlertDialog? -> Log.d(TAG, "Cancelled.") }
                alertDialog.show()
            } else if (operation == Operation.DELETE) {
                AlertDialog.Builder(this)
                    .setTitle(R.string.delete)
                    .setMessage(
                        getString(R.string.warn_delete_mult_msg_start) + WHITESPACE +
                                files.size + WHITESPACE +
                                getString(R.string.warn_delete_mult_msg_end)
                    )
                    .setNegativeButton(R.string.cancel) { dialogInterface: DialogInterface, _: Int -> dialogInterface.cancel() }
                    .setPositiveButton(R.string.yes) { dialogInterface: DialogInterface, _: Int ->
                        for (j in files.indices) {
                            val f = files[j]
                            FileDeleteTask(this, { b: Boolean? ->
                                if (!b!!) {
                                    Toast.makeText(
                                        this, getString(R.string.err_deleting_element_mult) +
                                                WHITESPACE + f!!.name,
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                                if (j == files.size) {
                                    // clear operation when last file is deleted
                                    clearFileOpCache()
                                }
                                reloadCurrentDirectory()
                            }, f).execute()
                        }
                        dialogInterface.dismiss()
                    }.show()
            } else {
                Toast.makeText(this, R.string.invalid_operation, Toast.LENGTH_LONG).show()
            }
        } else {
            Log.w(TAG, "No operation set!")
        }
    }

    /**
     * Launches a prompt to create a new directory.
     */
    private fun launchNewDirDialog() {
        val newDirDialog = AlertDialog.Builder(this)
            .setNegativeButton(R.string.cancel) { _: DialogInterface?, _: Int ->
                Log.d(
                    TAG, "Cancel pressed"
                )
            }
            .setTitle(R.string.new_directory)
            .setView(R.layout.layout_name_prompt)
            .create()
        newDirDialog.setButton(
            DialogInterface.BUTTON_POSITIVE,
            getString(R.string.okay)
        ) { _: DialogInterface?, _: Int ->
            val newDirName =
                currentDirectory + File.separator + (newDirDialog.findViewById<View>(R.id.destinationName) as EditText).text.toString()
            newDir(File(newDirName))
            reloadCurrentDirectory()
        }
        newDirDialog.show()
    }

    private fun newDir(d: File) {
        if (!d.exists()) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    Files.createDirectory(d.toPath())
                } else {
                    if (!d.mkdirs()) {
                        Toast.makeText(this, R.string.err_unable_to_mkdir, Toast.LENGTH_LONG).show()
                    }
                }
            } catch (e: IOException) {
                Log.e(TAG, getString(R.string.err_unable_to_mkdir), e)
            }
        } else {
            Toast.makeText(this, R.string.err_file_exists, Toast.LENGTH_LONG).show()
        }
    }

    /**
     * Launches a prompt to create a new file.
     */
    private fun launchNewFileDialog() {
        val newFileDialog = AlertDialog.Builder(this)
            .setNegativeButton(R.string.cancel) { _: DialogInterface?, _: Int ->
                Log.d(
                    TAG, "Cancel pressed"
                )
            }
            .setTitle(R.string.new_file)
            .setView(R.layout.layout_filename_prompt)
            .create()
        newFileDialog.setButton(
            DialogInterface.BUTTON_POSITIVE,
            getString(R.string.okay)
        ) { _: DialogInterface?, _: Int ->
            val newFileName =
                currentDirectory + File.separator + (newFileDialog.findViewById<View>(R.id.filedestinationName) as EditText).text.toString()
            newFile(File(newFileName))
            reloadCurrentDirectory()
        }
        newFileDialog.show()
    }

    private fun newFile(file: File) {
        if (!file.exists()) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    Files.createFile(file.toPath())
                } else {
                    if (!file.createNewFile()) {
                        Toast.makeText(this, "Unable to create file.", Toast.LENGTH_LONG).show()
                    }
                }
            } catch (e: IOException) {
                Log.e(TAG, "Unable to create file.", e)
            }
        } else {
            Toast.makeText(this, "File already exists", Toast.LENGTH_LONG).show()
        }
    }

    /**
     * {@inheritDoc}
     */
    override fun onStop() {
        super.onStop()
        Pref<String?>(PreferenceEntity.CURRENT_DIR_CACHE).value = currentDirectory
    }

    /**
     * {@inheritDoc}
     */
    override fun onDestroy() {
        super.onDestroy()
        Pref<String>(PreferenceEntity.CURRENT_DIR_CACHE).value = ""
    }

    /**
     * {@inheritDoc}
     */
    override fun onResume() {
        super.onResume()
        val cachedDirectory = Pref<String>(PreferenceEntity.CURRENT_DIR_CACHE).value
        if (cachedDirectory != "") {
            currentDirectory = cachedDirectory
        }
    }

    /**
     * Opens the bug report page.
     */
    private fun launchBugReportTab() {
        val builder = CustomTabsIntent.Builder()
        val params = CustomTabColorSchemeParams.Builder()
            .setToolbarColor(ContextCompat.getColor(this, R.color.default_primary_dark))
            .build()
        builder.setDefaultColorSchemeParams(params)
        val build = builder.build()
        build.launchUrl(this, Uri.parse("https://github.com/lfuelling/lrkFM/issues/new"))
    }

    /**
     * Adds a bookmark. Prompts for location.
     */
    private fun promptAndAddBookmark() {
        val stringSet: HashSet<String?>? =
            Pref<HashSet<String?>>(PreferenceEntity.BOOKMARKS).value
        if (Pref<Boolean>(PreferenceEntity.BOOKMARK_CURRENT_FOLDER).value == true) {
            stringSet?.add(currentDirectory)
        } else {
            val bookmarkDialogBuilder = AlertDialog.Builder(this)
            bookmarkDialogBuilder
                .setNegativeButton(R.string.cancel) { _: DialogInterface?, _: Int ->
                    Log.d(
                        TAG,
                        "Cancel pressed!"
                    )
                }
                .setNeutralButton(R.string.bookmark_this_folder) { dialog: DialogInterface, _: Int ->
                    stringSet?.add(currentDirectory)
                    dialog.cancel()
                }.setView(R.layout.layout_path_prompt)
                .setTitle(R.string.bookmark_set_path)
            val alertDialog = bookmarkDialogBuilder.create()
            alertDialog.setButton(
                AlertDialog.BUTTON_POSITIVE,
                getString(R.string.okay)
            ) { _: DialogInterface?, _: Int ->
                stringSet?.add(
                    (alertDialog.findViewById<View>(
                        R.id.destinationPath
                    ) as EditText).text.toString()
                )
            }
            alertDialog.setOnDismissListener { _: DialogInterface? ->
                Pref<Set<String?>>(PreferenceEntity.BOOKMARKS).value = stringSet
                loadUserBookmarks()
            }
            alertDialog.show()
        }
    }

    /**
     * Changes directory. With a prompt.
     *
     * @see .loadDirectory
     */
    private fun promptAndLoadPath() {
        val bookmarkDialogBuilder = AlertDialog.Builder(this)
        bookmarkDialogBuilder
            .setNegativeButton(R.string.cancel) { _: DialogInterface?, _: Int ->
                Log.d(
                    TAG,
                    "Cancel pressed!"
                )
            }
            .setView(R.layout.layout_path_prompt)
            .setTitle(R.string.nav_path)
        val alertDialog = bookmarkDialogBuilder.create()
        alertDialog.setButton(
            AlertDialog.BUTTON_POSITIVE,
            getString(R.string.okay)
        ) { _: DialogInterface?, _: Int -> loadPath((alertDialog.findViewById<View>(R.id.destinationPath) as EditText).text.toString()) }
        alertDialog.show()
    }

    /**
     * Launches the [SettingsActivity].
     */
    private fun launchSettings() {
        val i = Intent(this, SettingsActivity::class.java)
        startActivity(i)
    }

    /**
     * Adds a file to the operation context.
     * If the selected operation mode is not the same used in the current context,
     * the previous state will be discarded.
     *
     * @param op operation to do
     * @param f  file to operate on
     */
    fun addFileToOpContext(op: Operation, f: FMFile?) {
        if (fileOpContext.first != op) {
            if (Pref<Boolean>(PreferenceEntity.USE_CONTEXT_FOR_OPS_TOAST).value == true) {
                VibratingToast(this, getString(R.string.switching_op_mode), Toast.LENGTH_SHORT)
            }
            fileOpContext.first = op
            fileOpContext.second = CopyOnWriteArrayList()
        }
        fileOpContext.second.add(f)
    }

    companion object {
        /**
         * Logtag.
         */
        private val TAG = FileActivity::class.java.canonicalName

        /**
         * 0 if we have no permission.
         */
        private const val REQUEST_EXTERNAL_STORAGE = 1

        /**
         * The rootfs path ('/').
         */
        private const val ROOT_DIR = "/"

        /**
         * Keyword for [Activity.onSaveInstanceState].
         */
        private const val STATE_KEY_CURRENT_DIR = "state_current_dir"

        /**
         * Keyword for [Activity.onSaveInstanceState].
         */
        private const val STATE_KEY_OP_CONTEXT_OP = "state_current_op"

        /**
         * Keyword for [Activity.onSaveInstanceState].
         */
        private const val STATE_KEY_OP_CONTEXT_FILES = "state_current_op_files"

        /**
         * The permissions we need to do our job.
         */
        private val PERMISSIONS_STORAGE = arrayOf(
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        )

        /**
         * Extra used in [IntroActivity].
         *
         * @see Intent.hasExtra
         */
        const val FIRST_START_EXTRA = "firstStartDone"

        /**
         * 👀
         */
        const val WHITESPACE = " "

        /**
         * Checks if permission to access files is granted.
         *
         * @param context the context
         */
        fun verifyStoragePermissions(context: Activity?) {
            if (ActivityCompat.checkSelfPermission(
                    context!!,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    context,
                    PERMISSIONS_STORAGE,
                    REQUEST_EXTERNAL_STORAGE
                )
            }
        }
    }
}
