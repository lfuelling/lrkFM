package io.lerk.lrkFM

import android.content.Context
import android.os.Vibrator
import android.util.Log
import android.widget.Toast
import io.lerk.lrkFM.consts.PreferenceEntity

/**
 * Vibrating Toast.
 *
 *
 * Based on: https://stackoverflow.com/a/6109644/1979736
 *
 * @author Lukas Fülling (lukas@k40s.net)
 */
class VibratingToast(context: Context, text: CharSequence?, toastDuration: Int) : Toast(context) {
    /**
     * Constructor. Calling this will also show the toast.
     *
     * @param context       the context
     * @param text          the text to show
     * @param toastDuration toast duration. Must be a constant from [Toast] class
     * @see Toast.LENGTH_LONG
     *
     * @see Toast.LENGTH_SHORT
     */
    init {
        if (Pref<Boolean>(PreferenceEntity.VIBRATING_TOASTS).value == true) {
            val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            try {
                val duration = Pref<String>(PreferenceEntity.VIBRATION_LENGTH).value?.toInt()
                vibrator.vibrate(duration?.toLong() ?: 0)
            } catch (e: NumberFormatException) {
                Log.w(TAG, "Unable to parse vibration length.")
            }
        }
        makeText(context, text, toastDuration).show()
    }

    companion object {
        private val TAG = VibratingToast::class.java.canonicalName
    }
}