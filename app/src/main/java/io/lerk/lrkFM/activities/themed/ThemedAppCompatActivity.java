package io.lerk.lrkFM.activities.themed;

import android.app.Activity;
import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import io.lerk.lrkFM.Pref;
import io.lerk.lrkFM.R;

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
     * Reads the current theme using {@link Pref} and sets it.
     */
    static void setThemeFromPreferences(Activity context) {
        String currentTheme = new Pref<String>(THEME).getValue();
        String defaultVal = context.getString(R.string.pref_themes_value_default);
        boolean defaultOrDark = currentTheme.equals(defaultVal);
        context.setTheme(defaultOrDark ? R.style.AppTheme : R.style.AppTheme_Dark);
    }
}
