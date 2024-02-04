package io.lerk.lrkFM

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import io.lerk.lrkFM.consts.PreferenceEntity

/**
 * Yes, I really like hacky stuff.
 *
 *
 * To read settings you can replace
 * <pre>PreferenceManager.getDefaultSharedPreferences(context).getBoolean(MY_PREFERENCE.getKey(), false);</pre>
 * with <pre>new Pref&lt;Boolean&gt;(MY_PREFERENCE).getValue();</pre>.
 *
 *
 * To write settings you can replace
 * <pre>PreferenceManager.getDefaultSharedPreferences(context).edit().putBoolean(MY_PREFERENCE.getKey(), false).apply();</pre>
 * with <pre>new Pref&lt;Boolean;&gt;(MY_PREFERENCE).setValue(false);</pre>
 *
 *
 * Apparently androidx/Android Q introduces Integers as valid preference type.
 * Since [SharedPreferences] (afaik) don't support that, they should be handled as Strings.
 *
 * @param <T> The type of preference to use. Currently [Boolean], [String], [HashSet].
 * @author Lukas Fülling (lukas@k40s.net)
 * @see PreferenceEntity
</T> */
class Pref<T>(
    /**
     * The preference.
     */
    private val preference: PreferenceEntity
) {
    /**
     * The preference key.
     */
    private val key: String?

    /**
     * The preference type.
     */
    private val type: Class<*>?

    /**
     * The context.
     *
     * @see LrkFMApp.getContext
     */
    private val context: Context?

    /**
     * The preferences.
     */
    private val preferences: SharedPreferences

    /**
     * Constructor.
     *
     * @param preference the preference to use
     */
    init {
        type = preference.type
        key = preference.key
        context = LrkFMApp.Companion.getContext()
        preferences =
            context!!.getSharedPreferences(preference.store.name, Context.MODE_PRIVATE)
    }

    var value: T?
        /**
         * Gets a value.
         *
         * @return the value of the preference.
         */
        get() {
            if (context != null) {
                try {
                    when (type) {
                        Boolean::class.java -> {
                            return java.lang.Boolean.valueOf(
                                preferences.getBoolean(
                                    key,
                                    (preference.defaultValue as Boolean)
                                )
                            ) as T
                        }
                        String::class.java -> {
                            return preferences.getString(key, preference.defaultValue as String) as T?
                        }
                        HashSet::class.java -> {
                            return preferences.getStringSet(
                                key,
                                preference.defaultValue as Set<String>
                            ) as T?
                        }
                    }
                } catch (e: ClassCastException) {
                    handleError(e)
                }
            } else {
                Log.e(TAG, "Unable to get preference: context is null!")
            }
            return null
        }

    /**
     * Let's you set a value.
     *
     * @param value the value.
     */
    set(value: T?) {
        if (context != null) {
            try {
                if (type == Boolean::class.java) {
                    preferences.edit().putBoolean(key, (value as Boolean)).apply()
                } else if (type == String::class.java) {
                    preferences.edit().putString(key, value as String).apply()
                } else if (type == HashSet::class.java) {
                    preferences.edit().putStringSet(key, value as Set<String?>).apply()
                }
            } catch (e: ClassCastException) {
                handleError(e)
            }
        } else {
            Log.e(TAG, "Unable to get preference: context is null!")
        }
    }

    /**
     * Does logging.
     *
     * @param e The [ClassCastException]
     */
    private fun handleError(e: ClassCastException) {
        Log.e(
            TAG, "Unable to cast preference '" +
                    key + "' to " + type!!.canonicalName, e
        )
    }

    companion object {
        val TAG = Pref::class.java.canonicalName
    }
}
