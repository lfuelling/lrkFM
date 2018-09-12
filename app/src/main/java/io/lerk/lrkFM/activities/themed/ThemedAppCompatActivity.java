package io.lerk.lrkFM.activities.themed;

import android.app.Activity;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;

import io.lerk.lrkFM.R;
import io.lerk.lrkFM.util.PrefUtils;

import static io.lerk.lrkFM.consts.PreferenceEntity.THEME;

/**
 * Adds theme support.
 */
public abstract class ThemedAppCompatActivity extends AppCompatActivity {
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setThemeFromPreferences(this);
    }

    /**
     * Reads the current theme using {@link PrefUtils} and sets it.
     */
    static void setThemeFromPreferences(Activity context) {
        String currentTheme = new PrefUtils<String>(THEME).getValue();
        String defaultVal = context.getString(R.string.pref_themes_value_default);
        boolean defaultOrDark = currentTheme.equals(defaultVal);
        context.setTheme(defaultOrDark ? R.style.AppTheme : R.style.AppTheme_Dark);
    }
}
