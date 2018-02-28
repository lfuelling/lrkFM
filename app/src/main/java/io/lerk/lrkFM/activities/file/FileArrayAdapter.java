package io.lerk.lrkFM.activities.file;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.support.annotation.DrawableRes;
import android.support.annotation.IdRes;
import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.MimeTypeMap;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.List;

import io.lerk.lrkFM.R;
import io.lerk.lrkFM.entities.FMFile;

import static android.widget.Toast.LENGTH_SHORT;
import static io.lerk.lrkFM.activities.file.FileActivity.PREF_FILENAME_LENGTH;

/**
 * Heavily abused ArrayAdapter that also adds menus and listeners.
 *
 * @author Lukas Fülling (lukas@k40s.net)
 */
public class FileArrayAdapter extends ArrayAdapter<FMFile> {

    private static final String TAG = FileArrayAdapter.class.getCanonicalName();

    private FileActivity activity;

    public FileArrayAdapter(Context context, int resource, List<FMFile> items) {
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

    /**
     * Opens a file using the default app.
     *
     * @param f the file
     */
    private void openFile(FMFile f) {
        Intent i = new Intent(Intent.ACTION_VIEW);
        String mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(f.getExtension());
        i.setDataAndType(Uri.fromFile(f.getFile()), mimeType);
        i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        try {
            getContext().startActivity(i);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(getContext(), R.string.no_app_to_handle_file, LENGTH_SHORT).show();
        }
    }

    /**
     * Sets file name in the text field of a dialog.
     *
     * @param alertDialog     the dialog
     * @param destinationName the id of the EditText
     * @param name            the name
     */
    void presetNameForDialog(AlertDialog alertDialog, @IdRes int destinationName, String name) {
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
    void presetPathForDialog(FMFile f, AlertDialog alertDialog) {
        presetNameForDialog(alertDialog, R.id.destinationPath, f.getFile().getAbsolutePath());
    }

    /**
     * Gets the size of a file as formatted string.
     *
     * @param f the file
     * @return the size, formatted.
     */
    private String getSizeFormatted(FMFile f) {
        String[] units = new String[]{"B", "KiB", "MiB", "GiB", "TiB", "PiB"};
        Long length = f.getFile().length();
        if(length.equals(0L)) {
            return "0";
        }
        Double number = Math.floor(Math.log(length) / Math.log(1024));
        Double pow = Math.pow(1024, Math.floor(number));
        Double d = length / pow;
        String formattedString = d.toString().substring(0, d.toString().indexOf(".") + 2);
        return formattedString + ' ' + units[number.intValue()];
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
    AlertDialog getGenericFileOpDialog(@StringRes int positiveBtnText,
                                       @StringRes int title,
                                       @DrawableRes int icon,
                                       @LayoutRes int view,
                                       ButtonCallBackInterface positiveCallBack,
                                       ButtonCallBackInterface negativeCallBack) {
        AlertDialog dialog = new AlertDialog.Builder(activity)
                .setView(view)
                .setTitle(title)
                .setIcon(icon)
                .setCancelable(true).create();

        dialog.setButton(DialogInterface.BUTTON_POSITIVE, activity.getString(positiveBtnText), (d, i) -> positiveCallBack.handle(dialog));
        dialog.setButton(DialogInterface.BUTTON_NEGATIVE, activity.getString(R.string.cancel), (d, i) -> negativeCallBack.handle(dialog));
        dialog.setOnShowListener(dialog1 -> {
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

    /**
     * Initializes the UI for each file.
     *
     * @param f the file
     * @return the initialized UI
     */
    private View initUI(FMFile f) {
        assert activity != null;

        @SuppressLint("InflateParams") View v = LayoutInflater.from(activity).inflate(R.layout.layout_file, null); // works.

        if (f != null) {
            TextView fileNameView = v.findViewById(R.id.fileTitle);
            TextView filePermissions = v.findViewById(R.id.filePermissions);
            TextView fileDate = v.findViewById(R.id.fileDate);
            TextView fileSize = v.findViewById(R.id.fileSize);
            ImageView fileImage = v.findViewById(R.id.fileIcon);

            final String fileName = f.getName();
            if (fileNameView != null) {
                int maxLength = Integer.parseInt(activity.getDefaultPreferences().getString(PREF_FILENAME_LENGTH, "27"));
                if (fileName.length() >= maxLength) {
                    @SuppressLint("SetTextI18n") String output = fileName.substring(0, maxLength - 3) + "...";
                    fileNameView.setText(output); //shorten long names
                } else {
                    fileNameView.setText(fileName);
                }
            }
            if (filePermissions != null) {
                filePermissions.setText(f.getPermissions());
            }
            if (fileDate != null) {
                fileDate.setText(SimpleDateFormat.getDateTimeInstance().format(f.getLastModified()));
            }
            if (fileSize != null) {
                if (f.isDirectory()) {
                    fileSize.setVisibility(View.GONE);
                } else {
                    fileSize.setText(getSizeFormatted(f));
                }
            }
            if (fileImage != null) {
                if (!f.isDirectory()) {
                    if(f.getExtension().equals(ArchiveUtil.ZIP_EXTENSION) || f.getExtension().equals(ArchiveUtil.RAR_EXTENSION)) {
                        fileImage.setImageDrawable(getContext().getDrawable(R.drawable.ic_perm_media_black_24dp));
                    }
                    fileImage.setImageDrawable(getContext().getDrawable(R.drawable.ic_insert_drive_file_black_24dp));
                }
            }
            if (f.isDirectory()) {
                v.setOnClickListener(v1 -> activity.loadDirectory(f.getFile().getAbsolutePath()));
            } else {
                v.setOnClickListener(v1 -> openFile(f));
            }
            v.setOnCreateContextMenuListener((menu, view, info) -> new ContextMenuUtil(activity, this).initializeContextMenu(f, fileName, menu));
            ImageButton contextButton = v.findViewById(R.id.contextMenuButton);
            contextButton.setOnClickListener(v1 -> activity.getFileListView().showContextMenuForChild(v));

            for (FMFile contextFile : activity.getFileOpContext().getSecond()) {
                if (contextFile.getFile().getAbsolutePath().equals(f.getFile().getAbsolutePath())) {
                    v.setBackgroundColor(activity.getColor(R.color.primary));
                }
            }
        }
        return v;
    }
}
