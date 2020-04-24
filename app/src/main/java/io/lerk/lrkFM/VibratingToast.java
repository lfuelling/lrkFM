package io.lerk.lrkFM;

import android.content.Context;
import android.os.Vibrator;
import android.util.Log;
import android.widget.Toast;

import io.lerk.lrkFM.consts.PreferenceEntity;

/**
 * Vibrating Toast.
 * <p>
 * Based on: https://stackoverflow.com/a/6109644/1979736
 *
 * @author Lukas Fülling (lukas@k40s.net)
 */
public class VibratingToast extends Toast {

    private static final String TAG = VibratingToast.class.getCanonicalName();

    /**
     * Constructor. Calling this will also show the toast.
     *
     * @param context       the context
     * @param text          the text to show
     * @param toastDuration toast duration. Must be a constant from {@link Toast} class
     * @see Toast#LENGTH_LONG
     * @see Toast#LENGTH_SHORT
     */
    public VibratingToast(Context context, CharSequence text, int toastDuration) {
        super(context);
        if (new Pref<Boolean>(PreferenceEntity.VIBRATING_TOASTS).getValue()) {
            Vibrator vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
            if (vibrator != null) {
                try {
                    int duration = Integer.parseInt(new Pref<String>(PreferenceEntity.VIBRATION_LENGTH).getValue());
                    vibrator.vibrate(duration);
                } catch (NumberFormatException e) {
                    Log.w(TAG, "Unable to parse vibration length.");
                }
            }
        }
        makeText(context, text, toastDuration).show();
    }
}