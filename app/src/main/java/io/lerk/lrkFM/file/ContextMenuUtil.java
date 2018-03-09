package io.lerk.lrkFM.file;

import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.ContextMenu;
import android.widget.EditText;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Objects;

import io.lerk.lrkFM.R;
import io.lerk.lrkFM.activities.FileActivity;
import io.lerk.lrkFM.entities.FMFile;
import io.lerk.lrkFM.consts.Operation;
import io.lerk.lrkFM.operations.OperationUtil;
import io.lerk.lrkFM.util.EditablePair;

import static android.widget.Toast.LENGTH_SHORT;
import static io.lerk.lrkFM.consts.Operation.COPY;
import static io.lerk.lrkFM.consts.Operation.CREATE_ZIP;
import static io.lerk.lrkFM.consts.Operation.EXTRACT;
import static io.lerk.lrkFM.consts.Operation.MOVE;
import static io.lerk.lrkFM.consts.Preference.ALWAYS_EXTRACT_IN_CURRENT_DIR;
import static io.lerk.lrkFM.consts.Preference.USE_CONTEXT_FOR_OPS;
import static io.lerk.lrkFM.consts.Preference.USE_CONTEXT_FOR_OPS_TOAST;

/**
 * @author Lukas Fülling (lukas@k40s.net)
 */
class ContextMenuUtil {

    private static final int ID_COPY = 0;
    private static final int ID_MOVE = 1;
    private static final int ID_RENAME = 2;
    private static final int ID_DELETE = 3;
    private static final int ID_EXTRACT = 4;
    private static final int ID_SHARE = 5;
    private static final int ID_COPY_PATH = 6;
    private static final int ID_ADD_TO_ZIP = 7;
    private static final int ID_CREATE_ZIP = 8;

    private static final String TAG = ContextMenuUtil.class.getCanonicalName();
    private final FileActivity activity;
    private final FileArrayAdapter arrayAdapter;

    ContextMenuUtil(FileActivity activity, FileArrayAdapter arrayAdapter) {
        this.activity = activity;
        this.arrayAdapter = arrayAdapter;
    }

    /**
     * Adds menu buttons to context menu.
     *
     * @param f        the file
     * @param fileName the file name for the title
     * @param menu     the context menu to fill
     */
    void initializeContextMenu(FMFile f, String fileName, ContextMenu menu) {
        menu.setHeaderTitle(fileName);
        addCopyToMenu(f, menu);
        addMoveToMenu(f, menu);
        addRenameToMenu(f, menu);
        addExtractToMenu(f, menu);
        addShareToMenu(f, menu);
        addDeleteToMenu(f, menu);
        addCopyPathToMenu(f, menu);
        addCreateZipToMenu(f, menu);
    }

    /**
     * Adds delete button to menu.
     *
     * @param f    the file
     * @param menu the menu
     */
    private void addDeleteToMenu(FMFile f, ContextMenu menu) {
        menu.add(0, ID_DELETE, 0, activity.getString(R.string.delete)).setOnMenuItemClickListener(item -> {
            new AlertDialog.Builder(activity)
                    .setTitle(R.string.delete)
                    .setMessage(activity.getString(R.string.warn_delete_msg) + f.getName() + "?")
                    .setNegativeButton(R.string.cancel, (dialogInterface, i) -> dialogInterface.cancel())
                    .setPositiveButton(R.string.yes, (dialogInterface, i) -> {
                        if (!OperationUtil.deleteNoValidation(f)) {
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
     *
     * @param file  file file
     * @param menu menu
     */
    private void addExtractToMenu(FMFile file, ContextMenu menu) {
        menu.add(0, ID_EXTRACT, 0, activity.getString(R.string.extract))
                .setOnMenuItemClickListener(i -> {
                    if (activity.getDefaultPreferences().getBoolean(USE_CONTEXT_FOR_OPS.getKey(), true)) {
                        activity.addFileToOpContext(EXTRACT, file);
                        if (activity.getDefaultPreferences().getBoolean(USE_CONTEXT_FOR_OPS_TOAST.getKey(), true)) {
                            Toast.makeText(activity, activity.getString(R.string.file_added_to_context) + file.getName(), LENGTH_SHORT).show();
                        }

                        if(PreferenceManager.getDefaultSharedPreferences(activity).getBoolean(ALWAYS_EXTRACT_IN_CURRENT_DIR.getKey(), false)) {
                            activity.finishFileOperation();
                        } else {
                            new AlertDialog.Builder(activity)
                                    .setView(R.layout.layout_extract_now_prompt)
                                    .setPositiveButton(R.string.yes, (dialog, which) -> activity.finishFileOperation())
                                    .setNegativeButton(R.string.no, (dialog, which) -> Log.d(TAG, "noop")).create().show();
                        }
                    } else {
                        AlertDialog alertDialog = arrayAdapter.getGenericFileOpDialog(
                                R.string.extract,
                                R.string.op_destination,
                                R.drawable.ic_present_to_all_black_24dp,
                                R.layout.layout_path_prompt,
                                (d) -> activity.archiveUtil.extractArchive(((EditText) d.findViewById(R.id.destinationPath)).getText().toString(), file),
                                (d) -> Log.d(TAG, "Cancelled."));
                        alertDialog.setOnShowListener(d -> arrayAdapter.presetPathForDialog(file, alertDialog));
                        alertDialog.show();
                    }
                    activity.reloadCurrentDirectory();
                    return true;
                })
                .setVisible(file.isArchive());
    }

    /**
     * Adds share to menu.
     *
     * @param f    the file
     * @param menu the menu
     */
    private void addShareToMenu(FMFile f, ContextMenu menu) {
        menu.add(0, ID_SHARE, 0, activity.getString(R.string.share)).setOnMenuItemClickListener(i -> {
            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType("*/*");
            intent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(f.getFile()));
            activity.startActivity(Intent.createChooser(intent, activity.getString(R.string.share_file)));
            return true;
        });
    }

    /**
     * Adds rename to menu.
     *
     * @param f    the file
     * @param menu the menu
     */
    private void addRenameToMenu(FMFile f, ContextMenu menu) {
        menu.add(0, ID_RENAME, 0, activity.getString(R.string.rename)).setOnMenuItemClickListener(item -> {
            AlertDialog alertDialog = arrayAdapter.getGenericFileOpDialog(
                    R.string.rename,
                    R.string.rename,
                    R.drawable.ic_mode_edit_black_24dp,
                    R.layout.layout_name_prompt,
                    (d) -> activity.operationUtil.rename(f, d),
                    (d) -> Log.d(TAG, "Cancelled."));
            alertDialog.setOnShowListener(d -> arrayAdapter.presetNameForDialog(alertDialog, R.id.destinationName, f.getName()));
            alertDialog.show();
            activity.reloadCurrentDirectory();
            return true;
        });
    }

    /**
     * Adds move to menu.
     *
     * @param f    the file
     * @param menu the menu
     */
    private void addMoveToMenu(FMFile f, ContextMenu menu) {
        menu.add(0, ID_MOVE, 0, activity.getString(R.string.move)).setOnMenuItemClickListener(item -> {
            if (activity.getDefaultPreferences().getBoolean(USE_CONTEXT_FOR_OPS.getKey(), true)) {
                activity.addFileToOpContext(MOVE, f);
                if (activity.getDefaultPreferences().getBoolean(USE_CONTEXT_FOR_OPS_TOAST.getKey(), true)) {
                    Toast.makeText(activity, activity.getString(R.string.file_added_to_context) + f.getName(), LENGTH_SHORT).show();
                }
            } else {
                AlertDialog alertDialog = arrayAdapter.getGenericFileOpDialog(
                        R.string.move,
                        R.string.op_destination,
                        R.drawable.ic_content_cut_black_24dp,
                        R.layout.layout_path_prompt,
                        (d) -> activity.operationUtil.move(f, d),
                        (d) -> Log.d(TAG, "Cancelled."));
                alertDialog.setOnShowListener(d -> arrayAdapter.presetPathForDialog(f, alertDialog));
                alertDialog.show();
            }
            activity.reloadCurrentDirectory();
            return true;
        });
    }

    /**
     * Adds copy to menu.
     *
     * @param f    the file
     * @param menu the menu
     */
    private void addCopyToMenu(FMFile f, ContextMenu menu) {
        menu.add(0, ID_COPY, 0, activity.getString(R.string.copy)).setOnMenuItemClickListener(item -> {
            if (activity.getDefaultPreferences().getBoolean(USE_CONTEXT_FOR_OPS.getKey(), true)) {
                activity.addFileToOpContext(COPY, f);
                if (activity.getDefaultPreferences().getBoolean(USE_CONTEXT_FOR_OPS_TOAST.getKey(), true)) {
                    Toast.makeText(activity, activity.getString(R.string.file_added_to_context) + f.getName(), LENGTH_SHORT).show();
                }
            } else {
                AlertDialog alertDialog = arrayAdapter.getGenericFileOpDialog(
                        R.string.copy,
                        R.string.op_destination,
                        R.drawable.ic_content_copy_black_24dp,
                        R.layout.layout_path_prompt,
                        (d) -> activity.operationUtil.copy(f, d),
                        (d) -> Log.d(TAG, "Cancelled."));
                alertDialog.setOnShowListener(d -> arrayAdapter.presetPathForDialog(f, alertDialog));
                alertDialog.show();
            }
            activity.reloadCurrentDirectory();
            return true;
        });
    }

    /**
     * Adds "copy path" to menu.
     *
     * @param f    the file
     * @param menu the menu
     */
    private void addCopyPathToMenu(FMFile f, ContextMenu menu) {
        menu.add(0, ID_COPY_PATH, 0, R.string.copy_path).setOnMenuItemClickListener(item -> {
            ((ClipboardManager) Objects.requireNonNull(activity.getSystemService(Context.CLIPBOARD_SERVICE))).setPrimaryClip(ClipData.newPlainText(activity.getString(R.string.file_location), f.getFile().getAbsolutePath()));
            return true;
        });
    }

    /**
     * Adds "Create zip" and "add to zip" to menu.
     *
     * @param f    the file
     * @param menu the menu
     */
    private void addCreateZipToMenu(FMFile f, ContextMenu menu) {

        EditablePair<Operation, ArrayList<FMFile>> fileOpContext = activity.getFileOpContext();

        boolean zipFileReady = fileOpContext.getFirst().equals(CREATE_ZIP) && fileOpContext.getSecond().size() >= 1;

        menu.add(0, ID_ADD_TO_ZIP, 0, (zipFileReady) ? activity.getString(R.string.add_to_zip) : activity.getString(R.string.new_zip_file)).setOnMenuItemClickListener(item -> {
            activity.addFileToOpContext(CREATE_ZIP, f);
            if (activity.getDefaultPreferences().getBoolean(USE_CONTEXT_FOR_OPS_TOAST.getKey(), true)) {
                Toast.makeText(activity, activity.getString(R.string.file_added_to_context) + f.getName(), LENGTH_SHORT).show();
            }
            activity.reloadCurrentDirectory();
            return true;
        });

        if (zipFileReady) {
            menu.add(0, ID_CREATE_ZIP, 0, activity.getString(R.string.create_zip_file)).setOnMenuItemClickListener(item -> {
                if (fileOpContext.getFirst().equals(CREATE_ZIP)) {
                    AlertDialog alertDialog = arrayAdapter.getGenericFileOpDialog(
                            R.string.create_zip_file,
                            R.string.op_destination,
                            R.drawable.ic_archive_black_24dp,
                            R.layout.layout_name_prompt,
                            (d) -> activity.archiveUtil.createZipFile(fileOpContext.getSecond(), d),
                            (d) -> Log.d(TAG, "Cancelled."));
                    alertDialog.show();
                    activity.reloadCurrentDirectory();
                    return true;
                } else {
                    Log.w(TAG, "Illegal operation mode. Expected " + CREATE_ZIP + " but was: " + fileOpContext.getFirst());
                }
                return false;
            }).setVisible(fileOpContext.getFirst().equals(CREATE_ZIP));
        }
    }
}
