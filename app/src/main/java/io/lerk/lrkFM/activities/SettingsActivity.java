package io.lerk.lrkFM.activities;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceFragment;
import android.preference.PreferenceGroup;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.widget.LinearLayout;
import android.widget.Toast;

import org.jraf.android.alibglitch.GlitchEffect;

import java.util.HashSet;

import io.lerk.lrkFM.Pref;
import io.lerk.lrkFM.R;
import io.lerk.lrkFM.activities.themed.ThemedAppCompatPreferenceActivity;
import io.lerk.lrkFM.consts.PreferenceEntity;

/**
 * Settings.
 */
public class SettingsActivity extends ThemedAppCompatPreferenceActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getFragmentManager().beginTransaction().replace(android.R.id.content, new GeneralPreferenceFragment()).commit();
        setupActionBar();
    }

    /**
     * Set up the {@link android.app.ActionBar}, if the API is available.
     */
    private void setupActionBar() {
        LinearLayout root = (LinearLayout) findViewById(android.R.id.content).getParent().getParent().getParent();
        Toolbar bar = (Toolbar) LayoutInflater.from(this).inflate(R.layout.toolbar_settings, root, false);
        root.addView(bar, 0); // insert at top
        bar.setNavigationOnClickListener(v -> finish());
    }

    /**
     * This method stops fragment injection in malicious applications.
     * Make sure to deny any unknown fragments here.
     */
    protected boolean isValidFragment(String fragmentName) {
        return PreferenceFragment.class.getName().equals(fragmentName)
                || GeneralPreferenceFragment.class.getName().equals(fragmentName);
    }

    public static class GeneralPreferenceFragment extends PreferenceFragment {

        private static final String TAG = GeneralPreferenceFragment.class.getCanonicalName();

        private static int c = 0;

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.preferences);
            setHasOptionsMenu(true);

            try {
                Preference app_version = findPreference("app_version");
                app_version.setSummary(getActivity().getPackageManager().getPackageInfo(getActivity().getPackageName(), 0).versionName);
                app_version.setOnPreferenceClickListener(preference -> {
                    c++;
                    if (c % 8 == 0) {
                        GlitchEffect.showGlitch(getActivity());
                    }
                    return true;
                });
            } catch (PackageManager.NameNotFoundException e) {
                Log.e(TAG, "Unable to get App version!", e);
            }

            addOnPreferenceChangeListeners(this.getPreferenceScreen());

            getPreferenceScreen().findPreference("theme").setOnPreferenceChangeListener((p, n) -> {
                new AlertDialog.Builder(getContext())
                        .setTitle(R.string.restart_required_title)
                        .setMessage(R.string.restart_required_message)
                        .setNeutralButton(R.string.okay, (d, w) -> d.dismiss())
                        .create().show();
                new Pref<String>(PreferenceEntity.THEME).setValue(String.valueOf(n));
                return true;
            });
        }

        @Override
        public boolean onOptionsItemSelected(MenuItem item) {
            int id = item.getItemId();
            if (id == android.R.id.home) {
                startActivity(new Intent(getActivity().getApplicationContext(), SettingsActivity.class));
                return true;
            }
            return super.onOptionsItemSelected(item);
        }
    }

    /**
     * Adds a generic {@link android.preference.Preference.OnPreferenceChangeListener} to update the preferences with {@link Pref}. It does that recursively for every Preference contained in the supplied {@link PreferenceGroup}
     * This is what you get, when customizing too much stuff.
     *
     * @param preferenceGroup the current {@link PreferenceGroup}
     */
    private static void addOnPreferenceChangeListeners(PreferenceGroup preferenceGroup) {
        for (int i = 0; i < preferenceGroup.getPreferenceCount(); i++) {
            Preference p = preferenceGroup.getPreference(i);
            if (p instanceof PreferenceCategory) {
                addOnPreferenceChangeListeners((PreferenceCategory) p);
            } else {
                setOnPreferenceChangeListener(p);
            }
        }
    }

    /**
     * This actually adds the listener. This is called by {@link #addOnPreferenceChangeListeners(PreferenceGroup)}
     *
     * @param p the {@link Preference}
     */
    private static void setOnPreferenceChangeListener(Preference p) {
        p.setOnPreferenceChangeListener((preference, newValue) -> {
            if (preference.getKey().equals("filename_length")) {
                if (Integer.parseInt(String.valueOf(newValue)) >= 4) {
                    return true;
                } else {
                    Toast.makeText(preference.getContext(), R.string.err_filename_length_lower_than_four, Toast.LENGTH_LONG).show();
                    ((EditTextPreference) preference).setText("4");
                    return true;
                }
            }
            PreferenceEntity preferenceEntity = PreferenceEntity.determineByKey(preference.getKey());
            if (preferenceEntity != null) {
                if (newValue instanceof Boolean) {
                    new Pref<Boolean>(preferenceEntity).setValue((Boolean) newValue);
                    return true;
                } else if (newValue instanceof String) {
                    new Pref<String>(preferenceEntity).setValue((String) newValue);
                    return true;
                } else if (newValue instanceof HashSet) {
                    new Pref<HashSet>(preferenceEntity).setValue((HashSet) newValue);
                    return true;
                } else {
                    return false;
                }
            } else {
                return false;
            }
        });
    }
}
