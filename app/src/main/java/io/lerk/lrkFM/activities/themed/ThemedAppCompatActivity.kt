package io.lerk.lrkFM.activities.themed

import android.app.Activity
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import io.lerk.lrkFM.Pref
import io.lerk.lrkFM.R
import io.lerk.lrkFM.consts.PreferenceEntity

/**
 * Adds theme support.
 */
abstract class ThemedAppCompatActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setThemeFromPreferences(this)
    }

    companion object {
        /**
         * Reads the current theme using [Pref] and sets it.
         */
        fun setThemeFromPreferences(context: Activity) {
            val currentTheme = Pref<String>(PreferenceEntity.THEME).value
            val defaultVal = context.getString(R.string.pref_themes_value_default)
            val defaultOrDark = currentTheme == defaultVal
            context.setTheme(if (defaultOrDark) R.style.AppTheme else R.style.AppTheme_Dark)
        }
    }
}
