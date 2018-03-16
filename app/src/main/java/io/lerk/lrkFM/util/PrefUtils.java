package io.lerk.lrkFM.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.util.Log;

import com.google.firebase.analytics.FirebaseAnalytics;

import java.lang.reflect.ParameterizedType;
import java.util.HashSet;

import io.lerk.lrkFM.LrkFMApp;
import io.lerk.lrkFM.consts.Preference;

/**
 * Yes, I really like hacky stuff.
 *
 * To read settings you can replace
 * <pre>PreferenceManager.getDefaultSharedPreferences(context).getBoolean(MY_PREFERENCE.getKey(), false);</pre>
 * with <pre>new PrefUtils&lt;Boolean&gt;(MY_PREFERENCE).getValue();</pre>.
 *
 * To write settings you can replace
 * <pre>PreferenceManager.getDefaultSharedPreferences(context).edit().putBoolean(MY_PREFERENCE.getKey(), false).apply();</pre>
 * with <pre>new PrefUtils&lt;Boolean;&gt;(MY_PREFERENCE).setValue(false);</pre>
 *
 * @author Lukas Fülling (lukas@k40s.net)
 * @see Preference
 */
public class PrefUtils<T> {

    public static final String TAG = PrefUtils.class.getCanonicalName();

    /**
     * The preference.
     */
    private final Preference preference;

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
     * @see LrkFMApp#getContext()
     */
    private final Context context;

    /**
     * The preferences.
     */
    private final SharedPreferences preferences;

    /**
     * Constructor.
     * @param preference the preference to use
     */
    public PrefUtils(Preference preference) {
        this.preference = preference;
        type = preference.getType();
        key = preference.getKey();
        context = LrkFMApp.getContext();
        preferences = context.getSharedPreferences(preference.getStore().getName(), Context.MODE_PRIVATE);
    }

    /**
     * Gets a value.
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
        }
        return null;
    }

    /**
     * Let's you set a value. Currently {@link Boolean}, {@link String}, {@link HashSet}.
     * @param value the value.
     */
    @SuppressWarnings("unchecked")
    public void setValue(@NonNull T value) {
        if(context != null) {
            try {
                if (type.equals(Boolean.class)) {
                    preferences.edit().putBoolean(key, (Boolean) value).apply();
                } else if (type.equals(String.class)) {
                    preferences.edit().putString(key, (String) value).apply();
                } else if (type.equals(HashSet.class)) {
                    preferences.edit().putStringSet(key, (HashSet<String>) value).apply();
                }
            } catch (ClassCastException e) {
                handleError(e);
            }
        }
    }

    /**
     * Does logging and analytics.
     * @param e The {@link ClassCastException}
     */
    private void handleError(ClassCastException e) {
        Log.e(TAG, "Unable to cast preference '" +
                key + "' from " +
                ((Class) ((ParameterizedType) getClass().getGenericSuperclass()).getActualTypeArguments()[0]).getCanonicalName() +
                " to " + type.getCanonicalName(), e);
        Bundle bundle = new Bundle();
        bundle.putString("preference_key", key);
        bundle.putString("preference_type", type.getCanonicalName());
        FirebaseAnalytics.getInstance(context).logEvent("unable_to_cast_preference", bundle);
    }
}
