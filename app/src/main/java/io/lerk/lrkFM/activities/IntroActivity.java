package io.lerk.lrkFM.activities;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;

import io.lerk.lrkFM.R;

/**
 * Intro.
 *
 * @author Lukas Fülling (lukas@k40s.net)
 */
public class IntroActivity extends AppCompatActivity {

    private static int c = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_intro);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        final boolean permissionGranted = ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;

        FloatingActionButton fab = findViewById(R.id.fab);
        if(permissionGranted) {
            fab.setImageResource(R.drawable.ic_chevron_right_white_24dp);
        }
        fab.setOnClickListener(view -> {
            if (!permissionGranted && c <= 0) {
                FileActivity.verifyStoragePermissions(IntroActivity.this);
                fab.setImageResource(R.drawable.ic_chevron_right_white_24dp);
                c++;
            } else {
                Intent intent = new Intent(IntroActivity.this, FileActivity.class);
                intent.putExtra("firstStartDone", true);
                startActivity(intent);
                finish();
            }
        });
    }
}
