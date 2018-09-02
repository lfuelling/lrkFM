package io.lerk.lrkFM.adapter;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.drawable.Drawable;
import android.support.annotation.DrawableRes;
import android.support.annotation.IdRes;
import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;

import java.util.List;

import io.lerk.lrkFM.Handler;
import io.lerk.lrkFM.R;
import io.lerk.lrkFM.activities.FileActivity;
import io.lerk.lrkFM.entities.FMFile;

/**
 * @author Lukas Fülling (lukas@k40s.net)
 */
public abstract class BaseArrayAdapter extends ArrayAdapter<FMFile> {

    public static final String TAG = BaseArrayAdapter.class.getCanonicalName();

    FileActivity activity;

    BaseArrayAdapter(Context context, int resource, List<FMFile> items) {
        super(context, resource, items);
        if (context instanceof FileActivity) {
            this.activity = (FileActivity) context;
        } else {
            Log.d(TAG, "Context is no FileActivity: " + context.getClass().getName());
        }
    }

    @NonNull
    @Override
    public View getView(int position, View convertView, @NonNull ViewGroup parent) {
        return initUI(getItem(position));
    }

    protected abstract View initUI(FMFile f);

    protected abstract void openFile(FMFile f);

    /**
     * Sets file name in the text field of a dialog.
     *
     * @param alertDialog     the dialog
     * @param destinationName the id of the EditText
     * @param name            the name
     */
    public void presetNameForDialog(AlertDialog alertDialog, @IdRes int destinationName, String name) {
        EditText editText = alertDialog.findViewById(destinationName);
        if (editText != null) {
            editText.setText(name);
        } else {
            Log.w(TAG, "Unable to find view, can not set file title.");
        }
    }

    /**
     * Adds the path of a file to a dialog.
     *
     * @param f           the file
     * @param alertDialog the dialog
     * @see #presetNameForDialog(AlertDialog, int, String)
     */
    public void presetPathForDialog(FMFile f, AlertDialog alertDialog) {
        presetNameForDialog(alertDialog, R.id.destinationPath, f.getFile().getAbsolutePath());
    }

    /**
     * Utility method to create an AlertDialog.
     *
     * @param positiveBtnText  the text of the positive button
     * @param title            the title
     * @param icon             the icon
     * @param view             the content view
     * @param positiveCallBack the positive callback
     * @param negativeCallBack the negative callback
     * @return the dialog
     */
    public AlertDialog getGenericFileOpDialog(
            @StringRes int positiveBtnText,
            @StringRes int title,
            @DrawableRes int icon,
            @LayoutRes int view,
            Handler<AlertDialog> positiveCallBack,
            Handler<AlertDialog> negativeCallBack) {

        AlertDialog dialog = new AlertDialog.Builder(activity)
                .setView(view)
                .setTitle(title)
                .setCancelable(true).create();

        dialog.setButton(DialogInterface.BUTTON_POSITIVE, activity.getString(positiveBtnText), (d, i) -> positiveCallBack.handle(dialog));
        dialog.setButton(DialogInterface.BUTTON_NEGATIVE, activity.getString(R.string.cancel), (d, i) -> negativeCallBack.handle(dialog));
        dialog.setOnShowListener(dialog1 -> {

            ImageView dialogIcon = dialog.findViewById(R.id.dialogIcon);
            dialogIcon.setImageDrawable(getContext().getDrawable(icon));

            EditText inputField;
            if (view == R.layout.layout_name_prompt) {
                inputField = dialog.findViewById(R.id.destinationName);
                if (inputField != null) {
                    String name = activity.getTitleFromPath(activity.getCurrentDirectory());
                    inputField.setText(name);
                    Log.d(TAG, "Destination set to: " + name);
                } else {
                    Log.w(TAG, "Unable to preset current name, text field is null!");
                }
            } else if (view == R.layout.layout_path_prompt) {
                inputField = dialog.findViewById(R.id.destinationPath);
                if (inputField != null) {
                    String directory = activity.getCurrentDirectory();
                    inputField.setText(directory);
                    Log.d(TAG, "Destination set to: " + directory);
                } else {
                    Log.w(TAG, "Unable to preset current path, text field is null!");
                }
            }
        });
        return dialog;
    }
}
