package io.lerk.lrkFM.activities;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import io.lerk.lrkFM.R;
import io.lerk.lrkFM.activities.file.FileActivity;
import io.lerk.lrkFM.activities.themed.ThemedAppCompatActivity;

/**
 * Intro.
 *
 * @author Lukas Fülling (lukas@k40s.net)
 */
public class IntroActivity extends ThemedAppCompatActivity {

    private static Integer c = 0;

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_intro);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        final boolean permissionGranted = ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;

        FloatingActionButton fab = findViewById(R.id.fab);
        if (permissionGranted) {
            fab.setImageResource(R.drawable.ic_chevron_right_white_24dp);
        }
        fab.setOnClickListener(view -> {
            if (!permissionGranted && c <= 0) {
                FileActivity.verifyStoragePermissions(IntroActivity.this);
                fab.setImageResource(R.drawable.ic_chevron_right_white_24dp);
                c++;
            } else {
                launchMainAndFinish(savedInstanceState);
            }
        });
    }

    /**
     * Launch {@link FileActivity} and call {@link #finish()}.
     */
    private void launchMainAndFinish(@Nullable Bundle savedInstanceState) {
        Intent intent = new Intent(IntroActivity.this, FileActivity.class);
        intent.putExtra("firstStartDone", true);
        if (savedInstanceState != null) {
            intent.putExtras(savedInstanceState);
        }
        startActivity(intent);
        finish();
    }
}
