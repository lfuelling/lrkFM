package io.lerk.lrkfm.activities;

import android.os.Bundle;
import android.preference.PreferenceActivity;

import io.lerk.lrkfm.R;

public class SettingsActivity extends PreferenceActivity {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //noinspection deprecation
        addPreferencesFromResource(R.xml.preferences);
    }
}
