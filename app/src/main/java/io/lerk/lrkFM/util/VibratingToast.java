package io.lerk.lrkFM.util;

import android.content.Context;
import android.os.Vibrator;
import android.widget.Toast;
import io.lerk.lrkFM.consts.PreferenceEntity;

/**
 * Vibrating Toast.
 *
 * Based on: https://stackoverflow.com/a/6109644/1979736
 *
 * @author Lukas Fülling (lukas@k40s.net)
 */
public class VibratingToast extends Toast {

    public VibratingToast(Context context, CharSequence text, int duration) {
        super(context);
        if(new PrefUtils<Boolean>(PreferenceEntity.VIBRATING_TOASTS).getValue()) {
            ((Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE)).vibrate(120);
        }
        makeText(context, text, duration).show();
    }
}