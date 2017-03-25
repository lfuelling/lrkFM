package io.lerk.lrkFM.activities;

import android.os.Bundle;
import android.preference.PreferenceActivity;

import io.lerk.lrkFM.R;

public class SettingsActivity extends PreferenceActivity {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //noinspection deprecation
        addPreferencesFromResource(R.xml.preferences);
    }
}
