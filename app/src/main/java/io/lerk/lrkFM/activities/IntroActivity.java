package io.lerk.lrkFM.activities;

import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;

import io.lerk.lrkFM.R;

public class IntroActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_intro);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(view -> {
            Intent intent = new Intent(this, FileActivity.class);
            intent.putExtra("firstStartDone", true);
            startActivity(intent);
        });
    }

    public void launchSettings(View view) {
        startActivity(new Intent(this, SettingsActivity.class));
    }

    public void grantPermission(View view) {
        FileActivity.verifyStoragePermissions(this);
    }
}
