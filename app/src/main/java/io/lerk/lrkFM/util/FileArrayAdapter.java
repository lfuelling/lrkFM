package io.lerk.lrkFM.util;

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
import android.view.ContextMenu;
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
import io.lerk.lrkFM.activities.FileActivity;
import io.lerk.lrkFM.entities.FMFile;

import static android.widget.Toast.LENGTH_SHORT;
import static io.lerk.lrkFM.activities.FileActivity.PREF_FILENAME_LENGTH;
import static io.lerk.lrkFM.activities.FileActivity.PREF_USE_CONTEXT_FOR_OPS;
import static io.lerk.lrkFM.activities.FileActivity.PREF_USE_CONTEXT_FOR_OPS_TOAST;
import static io.lerk.lrkFM.util.FileUtil.Operation.COPY;
import static io.lerk.lrkFM.util.FileUtil.Operation.EXTRACT;
import static io.lerk.lrkFM.util.FileUtil.Operation.MOVE;

/**
 * Heavily abused ArrayAdapter that also adds menus and listeners.
 * @author Lukas Fülling (lukas@k40s.net)
 */
public class FileArrayAdapter extends ArrayAdapter<FMFile> {

    private static final int ID_COPY = 0;
    private static final int ID_MOVE = 1;
    private static final int ID_RENAME = 2;
    private static final int ID_DELETE = 3;
    private static final String TAG = FileArrayAdapter.class.getCanonicalName();
    private static final int ID_SHARE = 5;
    private static final int ID_EXTRACT = 4;

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
     * Adds menu buttons to context menu.
     * @param f the file
     * @param fileName the file name for the title TODO: refactor, get name in this class
     * @param menu the context menu to fill
     */
    private void initializeContextMenu(FMFile f, String fileName, ContextMenu menu) {
        menu.setHeaderTitle(fileName);
        addCopyToMenu(f, menu);
        addMoveToMenu(f, menu);
        addRenameToMenu(f, menu);
        if(f.getExtension().equals("zip")) {
            addExtractToMenu(f, menu);
        }
        addShareToMenu(f, menu);
        addDeleteToMenu(f, menu);
    }

    /**
     * Adds delete button to menu.
     * @param f the file
     * @param menu the menu
     */
    private void addDeleteToMenu(FMFile f, ContextMenu menu) {
        menu.add(0, ID_DELETE, 0, activity.getString(R.string.delete)).setOnMenuItemClickListener(item -> {
            new AlertDialog.Builder(activity)
                    .setTitle(R.string.delete)
                    .setMessage(activity.getString(R.string.warn_delete_msg) + f.getName() + "?")
                    .setNegativeButton(R.string.cancel, (dialogInterface, i) -> dialogInterface.cancel())
                    .setPositiveButton(R.string.yes, (dialogInterface, i) -> {
                        if (!FileUtil.deleteNoValidation(f)) {
                            Toast.makeText(activity, R.string.err_deleting_element, LENGTH_SHORT).show();
                        }
                        activity.reloadCurrentDirectory();
                        dialogInterface.dismiss();
                    })
                    .show();
            return true;
        });
    }

    /**
     * Adds extract to menu.
     * @param zip zip file
     * @param menu menu
     */
    private void addExtractToMenu(FMFile zip, ContextMenu menu) {
        menu.add(0, ID_EXTRACT, 0, activity.getString(R.string.extract)).setOnMenuItemClickListener(i -> {
            if(activity.getDefaultPreferences().getBoolean(PREF_USE_CONTEXT_FOR_OPS, true)) {
                activity.addFileToOpContext(EXTRACT, zip);
                if(activity.getDefaultPreferences().getBoolean(PREF_USE_CONTEXT_FOR_OPS_TOAST, true)){
                    Toast.makeText(activity, activity.getString(R.string.file_added_to_context)  + zip.getName(), LENGTH_SHORT).show();
                }
            } else {
                AlertDialog alertDialog = getGenericFileOpDialog(
                        R.string.extract,
                        R.string.op_destination,
                        R.drawable.ic_present_to_all_black_24dp,
                        R.layout.layout_path_prompt,
                        (d) -> ArchiveUtil.unpackZip(((EditText)d.findViewById(R.id.destinationPath)).getText().toString(), zip),
                        (d) -> Log.d(TAG, "Cancelled."));
                alertDialog.setOnShowListener(d -> presetPathForDialog(zip, alertDialog));
                alertDialog.show();
            }
            activity.reloadCurrentDirectory();
            return true;
        });
    }

    /**
     * Adds share to menu.
     * @param f the file
     * @param menu the menu
     */
    private void addShareToMenu(FMFile f, ContextMenu menu) {
        menu.add(0, ID_SHARE, 0, activity.getString(R.string.share)).setOnMenuItemClickListener(i -> {
            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType("*/*");
            intent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(f.getFile()));
            activity.startActivity(Intent.createChooser(intent, activity.getString(R.string.share_app)));
            return true;
        });
    }

    /**
     * Adds rename to menu.
     * @param f the file
     * @param menu the menu
     */
    private void addRenameToMenu(FMFile f, ContextMenu menu) {
        menu.add(0, ID_RENAME, 0, activity.getString(R.string.rename)).setOnMenuItemClickListener(item -> {
            AlertDialog alertDialog = getGenericFileOpDialog(
                    R.string.rename,
                    R.string.rename,
                    R.drawable.ic_mode_edit_black_24dp,
                    R.layout.layout_name_prompt,
                    (d) -> FileUtil.rename(f, activity,d),
                    (d) -> Log.d(TAG, "Cancelled."));
            alertDialog.setOnShowListener(d -> presetNameForDialog(alertDialog, R.id.destinationName, f.getName()));
            alertDialog.show();
            activity.reloadCurrentDirectory();
            return true;
        });
    }

    /**
     * Adds move to menu.
     * @param f the file
     * @param menu the menu
     */
    private void addMoveToMenu(FMFile f, ContextMenu menu) {
        menu.add(0, ID_MOVE, 0, activity.getString(R.string.move)).setOnMenuItemClickListener(item -> {
                    if(activity.getDefaultPreferences().getBoolean(PREF_USE_CONTEXT_FOR_OPS, true)) {
                        activity.addFileToOpContext(MOVE, f);
                        if(activity.getDefaultPreferences().getBoolean(PREF_USE_CONTEXT_FOR_OPS_TOAST, true)){
                            Toast.makeText(activity, activity.getString(R.string.file_added_to_context)  + f.getName(), LENGTH_SHORT).show();
                        }
                    } else {
                        AlertDialog alertDialog = getGenericFileOpDialog(
                                R.string.move,
                                R.string.op_destination,
                                R.drawable.ic_content_cut_black_24dp,
                                R.layout.layout_path_prompt,
                                (d) -> FileUtil.move(f, activity, d),
                                (d) -> Log.d(TAG, "Cancelled."));
                        alertDialog.setOnShowListener(d -> presetPathForDialog(f, alertDialog));
                        alertDialog.show();
                    }
            activity.reloadCurrentDirectory();
            return true;
        });
    }

    /**
     * Adds copy to menu.
     * @param f the file
     * @param menu the menu
     */
    private void addCopyToMenu(FMFile f, ContextMenu menu) {
        menu.add(0, ID_COPY, 0, activity.getString(R.string.copy)).setOnMenuItemClickListener(item -> {
            if(activity.getDefaultPreferences().getBoolean(PREF_USE_CONTEXT_FOR_OPS, true)) {
                activity.addFileToOpContext(COPY, f);
                if(activity.getDefaultPreferences().getBoolean(PREF_USE_CONTEXT_FOR_OPS_TOAST, true)){
                    Toast.makeText(activity, activity.getString(R.string.file_added_to_context)  + f.getName(), LENGTH_SHORT).show();
                }
            } else {
                AlertDialog alertDialog = getGenericFileOpDialog(
                        R.string.copy,
                        R.string.op_destination,
                        R.drawable.ic_content_copy_black_24dp,
                        R.layout.layout_path_prompt,
                        (d) -> FileUtil.copy(f, activity, d),
                        (d) -> Log.d(TAG, "Cancelled."));
                alertDialog.setOnShowListener(d -> presetPathForDialog(f, alertDialog));
                alertDialog.show();
            }
            activity.reloadCurrentDirectory();
            return true;
        });
    }

    /**
     * Opens a file using the default app.
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
     * @param alertDialog the dialog
     * @param destinationName the id of the EditText
     * @param name the name
     */
    private void presetNameForDialog(AlertDialog alertDialog, @IdRes int destinationName, String name) {
        EditText editText = (EditText) alertDialog.findViewById(destinationName);
        if (editText != null) {
            editText.setText(name);
        } else {
            Log.w(TAG, "Unable to find view, can not set file title.");
        }
    }

    /**
     * Adds the path of a file to a dialog.
     * @param f the file
     * @param alertDialog the dialog
     * @see #presetNameForDialog(AlertDialog, int, String)
     */
    private void presetPathForDialog(FMFile f, AlertDialog alertDialog) {
        presetNameForDialog(alertDialog, R.id.destinationPath, f.getFile().getAbsolutePath());
    }

    /**
     * Gets the size of a file as formatted string.
     * @param f the file
     * @return the size, formatted.
     */
    private String getSizeFormatted(FMFile f) {
        String[] units = new String[]{"B", "KiB", "MiB", "GiB", "TiB", "PiB"};
        Long length = f.getFile().length();
        Double number = Math.floor(Math.log(length) / Math.log(1024));
        Double pow = Math.pow(1024, Math.floor(number));
        Double d = length / pow;
        String formattedString = d.toString().substring(0, d.toString().indexOf(".") + 2);
        return formattedString + ' ' + units[number.intValue()];
    }

    /**
     * Utility class to create an AlertDialog.
     * @param positiveBtnText the text of the positive button
     * @param title the title
     * @param icon the icon
     * @param view the content view
     * @param positiveCallBack the positive callback
     * @param negativeCallBack the negative callback
     * @return the dialog
     */
    private AlertDialog getGenericFileOpDialog(@StringRes int positiveBtnText,
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
            EditText inputField = null;
            if (view == R.layout.layout_name_prompt) {
                inputField = (EditText) dialog.findViewById(R.id.destinationName);
                if (inputField != null) {
                    String name = activity.getTitleFromPath(activity.getCurrentDirectory());
                    inputField.setText(name);
                    Log.d(TAG, "Destination set to: " + name);
                } else {
                    Log.w(TAG, "Unable to preset current name, text field is null!");
                }
            } else if (view == R.layout.layout_path_prompt) {
                inputField = (EditText) dialog.findViewById(R.id.destinationPath);
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
     * @param f the file
     * @return the initialized UI
     */
    private View initUI(FMFile f) {
        assert activity != null;

        @SuppressLint("InflateParams") View v = LayoutInflater.from(activity).inflate(R.layout.layout_file, null); // works.

        if (f != null) {
            TextView fileNameView = (TextView) v.findViewById(R.id.fileTitle);
            TextView filePermissions = (TextView) v.findViewById(R.id.filePermissions);
            TextView fileDate = (TextView) v.findViewById(R.id.fileDate);
            TextView fileSize = (TextView) v.findViewById(R.id.fileSize);
            ImageView fileImage = (ImageView) v.findViewById(R.id.fileIcon);

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
                if (f.getDirectory()) {
                    fileSize.setVisibility(View.GONE);
                } else {
                    fileSize.setText(getSizeFormatted(f));
                }
            }
            if (fileImage != null) {
                if (!f.getDirectory()) {
                    fileImage.setImageDrawable(getContext().getDrawable(R.drawable.ic_insert_drive_file_black_24dp));
                }
            }
            if (f.getDirectory()) {
                v.setOnClickListener(v1 -> activity.loadDirectory(f.getFile().getAbsolutePath()));
            } else {
                v.setOnClickListener(v1 -> openFile(f));
            }
            v.setOnCreateContextMenuListener((menu, view, info) -> initializeContextMenu(f, fileName, menu));
            ImageButton contextButton = (ImageButton) v.findViewById(R.id.contextMenuButton);
            contextButton.setOnClickListener(v1 -> activity.getFileListView().showContextMenuForChild(v));

            for(FMFile contextFile: activity.getFileOpContext().getSecond()) {
                if(contextFile.getFile().getAbsolutePath().equals(f.getFile().getAbsolutePath())) {
                    v.setBackgroundColor(activity.getColor(R.color.primary));
                }
            }
        }
        return v;
    }
}
