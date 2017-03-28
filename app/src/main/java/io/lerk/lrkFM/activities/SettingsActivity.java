package io.lerk.lrkFM.activities;

import android.content.pm.PackageManager;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.util.Log;

import io.lerk.lrkFM.R;

public class SettingsActivity extends PreferenceActivity {
    private static final String TAG = SettingsActivity.class.getCanonicalName();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //noinspection deprecation
        addPreferencesFromResource(R.xml.preferences);
        //noinspection deprecation
        try {
            findPreference("app_version").setSummary(this.getPackageManager().getPackageInfo(this.getPackageName(), 0).versionName);
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(TAG, "Unable to get App version!", e);
        }
    }
}
