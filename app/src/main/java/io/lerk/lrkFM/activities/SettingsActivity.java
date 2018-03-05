package io.lerk.lrkFM.activities;

import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.SwitchPreference;
import android.util.Log;

import io.lerk.lrkFM.R;

@SuppressLint("ExportedPreferenceActivity")
@SuppressWarnings("deprecation")
public class SettingsActivity extends PreferenceActivity {
    private static final String TAG = SettingsActivity.class.getCanonicalName();

    /**
     * {@inheritDoc}
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.preferences);

        try {
            findPreference("app_version").setSummary(this.getPackageManager().getPackageInfo(this.getPackageName(), 0).versionName);
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(TAG, "Unable to get App version!", e);
        }

        Preference context_for_ops_toast = findPreference("context_for_ops_toast");
        SwitchPreference context_for_ops = (SwitchPreference) findPreference("context_for_ops");

        context_for_ops_toast.setEnabled(context_for_ops.isChecked());
        context_for_ops.setOnPreferenceChangeListener((preference, newValue) -> {
            context_for_ops_toast.setEnabled((Boolean) newValue);
            return true;
        });
    }
}
