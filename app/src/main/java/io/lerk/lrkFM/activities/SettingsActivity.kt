package io.lerk.lrkFM.activities

import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Vibrator
import android.preference.EditTextPreference
import android.preference.Preference
import android.preference.PreferenceCategory
import android.preference.PreferenceFragment
import android.preference.PreferenceGroup
import android.util.Log
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.Toolbar
import io.lerk.lrkFM.Pref
import io.lerk.lrkFM.R
import io.lerk.lrkFM.activities.SettingsActivity.GeneralPreferenceFragment
import io.lerk.lrkFM.activities.themed.ThemedAppCompatPreferenceActivity
import io.lerk.lrkFM.consts.PreferenceEntity
import org.jraf.android.alibglitch.GlitchEffect

/**
 * Settings.
 */
@Suppress("deprecation") // :c

class SettingsActivity : ThemedAppCompatPreferenceActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupActionBar()
    }

    override fun onBuildHeaders(target: List<Header>) {
        super.onBuildHeaders(target)
        loadHeadersFromResource(R.xml.pref_headers, target)
    }

    /**
     * Set up the [android.app.ActionBar], if the API is available.
     */
    private fun setupActionBar() {
        val root = findViewById<View>(android.R.id.content).parent.parent.parent as LinearLayout
        val bar =
            LayoutInflater.from(this).inflate(R.layout.toolbar_settings, root, false) as Toolbar
        root.addView(bar, 0) // insert at top
        bar.setNavigationOnClickListener { v: View? -> finish() }
    }

    /**
     * This method stops fragment injection in malicious applications.
     * Make sure to deny any unknown fragments here.
     */
    override fun isValidFragment(fragmentName: String): Boolean {
        return PreferenceFragment::class.java.name == fragmentName || GeneralPreferenceFragment::class.java.name == fragmentName || UIPreferenceFragment::class.java.name == fragmentName || NotificationsPreferenceFragment::class.java.name == fragmentName
    }

    class GeneralPreferenceFragment : PreferenceFragment() {
        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            addPreferencesFromResource(R.xml.preferences_general)
            setHasOptionsMenu(true)
            try {
                val app_version = findPreference("app_version")
                app_version.summary =
                    activity.packageManager.getPackageInfo(activity.packageName, 0).versionName
                app_version.onPreferenceClickListener =
                    Preference.OnPreferenceClickListener { preference: Preference? ->
                        c++
                        if (c % 8 == 0) {
                            GlitchEffect.showGlitch(activity)
                        }
                        true
                    }
            } catch (e: PackageManager.NameNotFoundException) {
                Log.e(TAG, "Unable to get App version!", e)
            }
            addOnPreferenceChangeListeners(this.preferenceScreen)
        }

        override fun onOptionsItemSelected(item: MenuItem): Boolean {
            val id = item.itemId
            if (id == android.R.id.home) {
                startActivity(Intent(activity.applicationContext, SettingsActivity::class.java))
                return true
            }
            return super.onOptionsItemSelected(item)
        }

        companion object {
            private val TAG = GeneralPreferenceFragment::class.java.canonicalName
            private var c = 0
        }
    }

    class UIPreferenceFragment : PreferenceFragment() {
        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            addPreferencesFromResource(R.xml.preferences_ui)
            setHasOptionsMenu(true)
            addOnPreferenceChangeListeners(this.preferenceScreen)
            preferenceScreen.findPreference("theme").onPreferenceChangeListener =
                Preference.OnPreferenceChangeListener { p: Preference?, n: Any ->
                    AlertDialog.Builder(
                        context
                    )
                        .setTitle(R.string.restart_required_title)
                        .setMessage(R.string.restart_required_message)
                        .setNeutralButton(R.string.okay) { d: DialogInterface, w: Int -> d.dismiss() }
                        .create().show()
                    Pref<String>(PreferenceEntity.THEME).value = n.toString()
                    true
                }
        }

        override fun onOptionsItemSelected(item: MenuItem): Boolean {
            val id = item.itemId
            if (id == android.R.id.home) {
                startActivity(Intent(activity.applicationContext, SettingsActivity::class.java))
                return true
            }
            return super.onOptionsItemSelected(item)
        }
    }

    class NotificationsPreferenceFragment : PreferenceFragment() {
        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            addPreferencesFromResource(R.xml.preferences_notifications)
            setHasOptionsMenu(true)
            addOnPreferenceChangeListeners(this.preferenceScreen)
            val vibrationLengthPreference = findPreference(PreferenceEntity.VIBRATION_LENGTH.key)
            vibrationLengthPreference.summary =
                context.resources // add current value to description
                    .getString(R.string.pref_vibration_length_desc_init)
        }

        override fun onOptionsItemSelected(item: MenuItem): Boolean {
            val id = item.itemId
            if (id == android.R.id.home) {
                startActivity(Intent(activity.applicationContext, SettingsActivity::class.java))
                return true
            }
            return super.onOptionsItemSelected(item)
        }
    }

    companion object {
        /**
         * Adds a generic [android.preference.Preference.OnPreferenceChangeListener] to update the preferences with [Pref].
         * It does that recursively for every Preference contained in the supplied [PreferenceGroup].
         * This is what you get, when customizing too much stuff.
         *
         * @param preferenceGroup the current [PreferenceGroup]
         */
        private fun addOnPreferenceChangeListeners(preferenceGroup: PreferenceGroup) {
            for (i in 0 until preferenceGroup.preferenceCount) {
                val p = preferenceGroup.getPreference(i)
                if (p is PreferenceCategory) {
                    addOnPreferenceChangeListeners(p)
                } else {
                    setOnPreferenceChangeListener(p)
                }
            }
        }

        /**
         * This actually adds the listener.
         * This is called by [.addOnPreferenceChangeListeners].
         *
         * @param p the [Preference]
         */
        private fun setOnPreferenceChangeListener(p: Preference) {
            p.onPreferenceChangeListener =
                Preference.OnPreferenceChangeListener { preference: Preference, newValue: Any ->
                    if (preference.key == "filename_length") {
                        if (newValue.toString().toInt() <= 4) {
                            Toast.makeText(
                                preference.context,
                                R.string.err_filename_length_lower_than_four,
                                Toast.LENGTH_LONG
                            ).show()
                            (preference as EditTextPreference).text = "4"
                            return@OnPreferenceChangeListener false
                        }
                    } else if (preference.key == "vibration_length") {
                        preference.summary = preference.context.resources
                            .getString(R.string.pref_vibration_length_desc) + " " + newValue
                        val vibrator =
                            preference.context.getSystemService(VIBRATOR_SERVICE) as Vibrator
                        vibrator.vibrate((newValue as Int).toLong())
                    }
                    val preferenceEntity: PreferenceEntity? =
                        PreferenceEntity.Companion.determineByKey(preference.key)
                    if (preferenceEntity != null) {
                        if (newValue is Boolean) {
                            Pref<Boolean>(preferenceEntity).value = newValue
                            return@OnPreferenceChangeListener true
                        } else if (newValue is String || newValue is Int) {
                            val value = newValue.toString()
                            Pref<String>(preferenceEntity).value = value
                            return@OnPreferenceChangeListener true
                        } else if (newValue is HashSet<*>) {
                            Pref<HashSet<*>>(preferenceEntity).value = newValue
                            return@OnPreferenceChangeListener true
                        } else {
                            return@OnPreferenceChangeListener false
                        }
                    } else {
                        return@OnPreferenceChangeListener false
                    }
                }
        }
    }
}
