package io.lerk.lrkFM;

import android.content.Context;
import android.content.SharedPreferences;
import android.support.annotation.NonNull;
import android.util.Log;

import java.util.HashSet;
import java.util.Set;

import io.lerk.lrkFM.consts.PreferenceEntity;

/**
 * Yes, I really like hacky stuff.
 * <p>
 * To read settings you can replace
 * <pre>PreferenceManager.getDefaultSharedPreferences(context).getBoolean(MY_PREFERENCE.getKey(), false);</pre>
 * with <pre>new Pref&lt;Boolean&gt;(MY_PREFERENCE).getValue();</pre>.
 *
 * To write settings you can replace
 * <pre>PreferenceManager.getDefaultSharedPreferences(context).edit().putBoolean(MY_PREFERENCE.getKey(), false).apply();</pre>
 * with <pre>new Pref&lt;Boolean;&gt;(MY_PREFERENCE).setValue(false);</pre>
 *
 * @author Lukas Fülling (lukas@k40s.net)
 * @see PreferenceEntity
 *
 * @param <T> The type of preference to use. Currently {@link Boolean}, {@link String}, {@link HashSet}.
 */
public class Pref<T> {

    public static final String TAG = Pref.class.getCanonicalName();

    /**
     * The preference.
     */
    private final PreferenceEntity preference;

    /**
     * The preference key.
     */
    private final String key;

    /**
     * The preference type.
     */
    private final Class type;

    /**
     * The context.
     *
     * @see LrkFMApp#getContext()
     */
    private final Context context;

    /**
     * The preferences.
     */
    private final SharedPreferences preferences;

    /**
     * Constructor.
     *
     * @param preference the preference to use
     */
    public Pref(PreferenceEntity preference) {
        this.preference = preference;
        type = preference.getType();
        key = preference.getKey();
        context = LrkFMApp.getContext();
        preferences = context.getSharedPreferences(preference.getStore().getName(), Context.MODE_PRIVATE);
    }

    /**
     * Gets a value.
     *
     * @return the value of the preference.
     */
    @SuppressWarnings("unchecked")
    public T getValue() {
        if (context != null) {
            try {
                if (type.equals(Boolean.class)) {
                    return (T) Boolean.valueOf(preferences.getBoolean(key, (Boolean) preference.getDefaultValue()));
                } else if (type.equals(String.class)) {
                    return (T) preferences.getString(key, (String) preference.getDefaultValue());
                } else if (type.equals(HashSet.class)) {
                    return (T) preferences.getStringSet(key, (HashSet) preference.getDefaultValue());
                }
            } catch (ClassCastException e) {
                handleError(e);
            }
        } else {
            Log.e(TAG, "Unable to get preference: context is null!");
        }
        return null;
    }

    /**
     * Let's you set a value.
     *
     * @param value the value.
     */
    @SuppressWarnings("unchecked")
    public void setValue(@NonNull T value) {
        if (context != null) {
            try {
                if (type.equals(Boolean.class)) {
                    preferences.edit().putBoolean(key, (Boolean) value).apply();
                } else if (type.equals(String.class)) {
                    preferences.edit().putString(key, (String) value).apply();
                } else if (type.equals(HashSet.class)) {
                    preferences.edit().putStringSet(key, (Set<String>) value).apply();
                }
            } catch (ClassCastException e) {
                handleError(e);
            }
        } else {
            Log.e(TAG, "Unable to get preference: context is null!");
        }
    }

    /**
     * Does logging.
     *
     * @param e The {@link ClassCastException}
     */
    private void handleError(ClassCastException e) {
        Log.e(TAG, "Unable to cast preference '" +
                key + "' to " + type.getCanonicalName(), e);
    }
}
