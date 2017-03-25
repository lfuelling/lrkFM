package io.lerk.lrkfm.util;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.support.annotation.DrawableRes;
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

import java.io.File;
import java.util.List;

import io.lerk.lrkfm.R;
import io.lerk.lrkfm.activities.FileActivity;
import io.lerk.lrkfm.entities.FMFile;

import static android.widget.Toast.LENGTH_SHORT;

public class FileArrayAdapter extends ArrayAdapter<FMFile> {

    private static final int ID_COPY = 0;
    private static final int ID_MOVE = 1;
    private static final int ID_RENAME = 2;
    private static final int ID_DELETE = 3;
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

    private View initUI(FMFile f) {
        assert activity != null;

        @SuppressLint("InflateParams") View v = LayoutInflater.from(activity).inflate(R.layout.layout_file, null); // works.

        if (f != null) {
            TextView fileNameView = (TextView) v.findViewById(R.id.fileTitle);
            TextView filePermissions = (TextView) v.findViewById(R.id.filePermissions);
            TextView fileDate = (TextView) v.findViewById(R.id.fileDate);
            ImageView fileImage = (ImageView) v.findViewById(R.id.fileIcon);

            if (fileNameView != null) {
                fileNameView.setText(f.getName());
            }
            if (filePermissions != null) {
                filePermissions.setText(f.getPermissions());
            }
            if (fileDate != null) {
                fileDate.setText(f.getLastModified().toString());
            }
            if (fileImage != null) {
                if (!f.getDirectory()) {
                    fileImage.setImageDrawable(getContext().getDrawable(R.drawable.ic_insert_drive_file_black_24dp));
                }
            }
            if (f.getDirectory()) {
                v.setOnClickListener(v1 -> activity.loadDirectory(f.getFile().getAbsolutePath()));
            } else {
                v.setOnClickListener(v1 -> {
                    Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT, Uri.fromFile(f.getFile()));
                    i.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    i.setType(MimeTypeMap.getSingleton().getMimeTypeFromExtension(MimeTypeMap.getFileExtensionFromUrl(f.getFile().getAbsolutePath())));
                    try {
                        getContext().startActivity(i);
                    } catch (ActivityNotFoundException e) {
                        Toast.makeText(getContext(), R.string.no_app_to_handle_file, LENGTH_SHORT).show();
                    }
                });
            }
            v.setOnCreateContextMenuListener((menu, view, info) -> {
                menu.setHeaderTitle(f.getName());
                menu.add(0, ID_COPY, 0, "Copy").setOnMenuItemClickListener(item -> {
                    AlertDialog alertDialog = getGenericFileOpDialog(
                            R.string.op_copy_positive,
                            R.string.op_destination,
                            R.drawable.ic_content_copy_black_24dp,
                            R.layout.layout_path_prompt,
                            (d) -> {
                                Log.d(TAG, "Dismiss called!");
                                EditText editText = (EditText) d.findViewById(R.id.destinationPath);
                                String pathname = editText.getText().toString();
                                if (pathname.isEmpty()) {
                                    Toast.makeText(activity, R.string.err_empty_input, LENGTH_SHORT).show();
                                } else {
                                    FileUtil.copy(f, activity, new File(pathname));
                                }
                            },
                            (d) -> Log.d(TAG, "Cancelled."));
                    alertDialog.show();
                    activity.reloadCurrentDirectory();
                    return true;
                });
                menu.add(0, ID_MOVE, 0, "Move").setOnMenuItemClickListener(item -> {
                    AlertDialog alertDialog = getGenericFileOpDialog(
                            R.string.op_move_title,
                            R.string.op_destination,
                            R.drawable.ic_content_cut_black_24dp,
                            R.layout.layout_path_prompt,
                            (d) -> {
                                Log.d(TAG, "Dismiss called!");
                                EditText editText = (EditText) d.findViewById(R.id.destinationPath);
                                String pathname = editText.getText().toString();
                                if (pathname.isEmpty()) {
                                    Toast.makeText(activity, R.string.err_empty_input, LENGTH_SHORT).show();
                                } else {
                                    FileUtil.move(f, activity, new File(pathname));
                                }
                            },
                            (d) -> Log.d(TAG, "Cancelled."));
                    alertDialog.show();
                    activity.reloadCurrentDirectory();
                    return true;
                });
                menu.add(0, ID_RENAME, 0, "Rename").setOnMenuItemClickListener(item -> {
                    AlertDialog alertDialog = getGenericFileOpDialog(
                            R.string.op_rename_title,
                            R.string.op_rename_title,
                            R.drawable.ic_mode_edit_black_24dp,
                            R.layout.layout_name_prompt,
                            (d) -> {
                                Log.d(TAG, "Dismiss called!");
                                EditText editText = (EditText) d.findViewById(R.id.destinationName);
                                String newName = editText.getText().toString();
                                if (newName.isEmpty()) {
                                    Toast.makeText(activity, R.string.err_empty_input, LENGTH_SHORT).show();
                                } else {
                                    FileUtil.rename(f, activity, newName);
                                }
                            },
                            (d) -> Log.d(TAG, "Cancelled."));
                    alertDialog.show();
                    activity.reloadCurrentDirectory();
                    return true;
                });
                menu.add(0, ID_DELETE, 0, "Delete").setOnMenuItemClickListener(item -> {
                    if (!FileUtil.delete(f, activity)) {
                        Toast.makeText(activity, R.string.err_deleting_element, LENGTH_SHORT).show();
                    }
                    activity.reloadCurrentDirectory();
                    return true;
                });
            });

            ImageButton contextButton = (ImageButton) v.findViewById(R.id.contextMenuButton);
            contextButton.setOnClickListener(v1 -> {
                activity.getFileListView().showContextMenuForChild(v);
                Log.d(TAG, "Opening context menu!");
            });

        }
        return v;
    }

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

        EditText inputField = null;
        if (view == R.layout.layout_name_prompt) {
            inputField = (EditText) dialog.findViewById(R.id.destinationName);
        } else if (view == R.layout.layout_path_prompt) {
            inputField = (EditText) dialog.findViewById(R.id.destinationPath);
        }
        if (inputField != null) {
            String directory = activity.getCurrentDirectory();
            inputField.setText(directory);
            Log.d(TAG, "Destination set to: " + directory);
        } else {
            Log.w(TAG, "Unable to preset current path, text field is null!");
        }
        return dialog;
    }
}
