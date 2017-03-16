package io.lerk.lrkfm;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.support.customtabs.CustomTabsIntent;
import android.support.design.widget.FloatingActionButton;
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
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import java.util.ArrayList;

public class FileActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    private static final String PREF_HOMEDIR = "home_dir";
    private static final String TAG = FileActivity.class.getCanonicalName();
    private ListView fileListView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        fileListView = (ListView) findViewById(R.id.fileView);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);

        setSupportActionBar(toolbar);
        navigationView.setNavigationItemSelectedListener(this);

        fab.setOnClickListener((v) -> {
            CustomTabsIntent.Builder builder = new CustomTabsIntent.Builder();
            builder.setToolbarColor(getColor(R.color.primary_dark));
            CustomTabsIntent build = builder.build();
            build.launchUrl(this, Uri.parse("https://fahlbtharz.k40s.net/FileManagerCompetition/lrkFM/issues/new"));
        });

        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        loadHomeDir();
    }

    private void loadHomeDir() {
        String defaultStartDirPath = Environment.getExternalStorageDirectory().getAbsolutePath() + "/";
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        String startDir = preferences.getString(PREF_HOMEDIR, defaultStartDirPath);
        addFileListToView(startDir);
    }

    private void addFileListToView(String startDir) {
        ArrayList<FMFile> files = new FileLoader(startDir).getFiles();
        if (files.isEmpty()) {
            fileListView.setVisibility(View.GONE);
            findViewById(R.id.unableToLoadText).setVisibility(View.VISIBLE);
        } else {
            ArrayAdapter<FMFile> adapter = new ArrayAdapter<>(this, R.layout.layout_file, files);
            fileListView.setAdapter(adapter);
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

        } else if (id == R.id.nav_share) {

        } else if (id == R.id.nav_todo) {

        } else if (id == R.id.nav_view) {

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
                        addFileListToView(inputPath);
                        dialog.dismiss();
                    } else {
                        Toast.makeText(FileActivity.this, R.string.invalid_path, Toast.LENGTH_LONG).show();
                        Log.w(TAG, "Invalid path!");
                    }
                })
                .setNegativeButton(getString(R.string.cancel), (dialog, which) -> dialog.cancel())
                .show();
    }

}
