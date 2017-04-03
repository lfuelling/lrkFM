package io.lerk.lrkFM.util.files;

import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;
import android.view.ContextMenu;
import android.widget.EditText;
import android.widget.Toast;

import io.lerk.lrkFM.R;
import io.lerk.lrkFM.activities.FileActivity;
import io.lerk.lrkFM.entities.FMFile;
import io.lerk.lrkFM.util.ArchiveUtil;

import static android.widget.Toast.LENGTH_SHORT;
import static io.lerk.lrkFM.activities.FileActivity.PREF_USE_CONTEXT_FOR_OPS;
import static io.lerk.lrkFM.activities.FileActivity.PREF_USE_CONTEXT_FOR_OPS_TOAST;
import static io.lerk.lrkFM.util.files.OperationUtil.Operation.COPY;
import static io.lerk.lrkFM.util.files.OperationUtil.Operation.EXTRACT;
import static io.lerk.lrkFM.util.files.OperationUtil.Operation.MOVE;

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
     * @param fileName the file name for the title TODO: refactor, get name in this class
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
     * @param zip  zip file
     * @param menu menu
     */
    private void addExtractToMenu(FMFile zip, ContextMenu menu) {
        menu.add(0, ID_EXTRACT, 0, activity.getString(R.string.extract))
                .setOnMenuItemClickListener(i -> {
                    if (activity.getDefaultPreferences().getBoolean(PREF_USE_CONTEXT_FOR_OPS, true)) {
                        activity.addFileToOpContext(EXTRACT, zip);
                        if (activity.getDefaultPreferences().getBoolean(PREF_USE_CONTEXT_FOR_OPS_TOAST, true)) {
                            Toast.makeText(activity, activity.getString(R.string.file_added_to_context) + zip.getName(), LENGTH_SHORT).show();
                        }
                    } else {
                        AlertDialog alertDialog = arrayAdapter.getGenericFileOpDialog(
                                R.string.extract,
                                R.string.op_destination,
                                R.drawable.ic_present_to_all_black_24dp,
                                R.layout.layout_path_prompt,
                                (d) -> ArchiveUtil.extractArchive(((EditText) d.findViewById(R.id.destinationPath)).getText().toString(), zip),
                                (d) -> Log.d(TAG, "Cancelled."));
                        alertDialog.setOnShowListener(d -> arrayAdapter.presetPathForDialog(zip, alertDialog));
                        alertDialog.show();
                    }
                    activity.reloadCurrentDirectory();
                    return true;
                })
                .setVisible(zip.getExtension().equals(ArchiveUtil.ZIP_EXTENSION) || zip.getExtension().equals(ArchiveUtil.RAR_EXTENSION));
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
            activity.startActivity(Intent.createChooser(intent, activity.getString(R.string.share_app)));
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
                    (d) -> OperationUtil.rename(f, activity, d),
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
            if (activity.getDefaultPreferences().getBoolean(PREF_USE_CONTEXT_FOR_OPS, true)) {
                activity.addFileToOpContext(MOVE, f);
                if (activity.getDefaultPreferences().getBoolean(PREF_USE_CONTEXT_FOR_OPS_TOAST, true)) {
                    Toast.makeText(activity, activity.getString(R.string.file_added_to_context) + f.getName(), LENGTH_SHORT).show();
                }
            } else {
                AlertDialog alertDialog = arrayAdapter.getGenericFileOpDialog(
                        R.string.move,
                        R.string.op_destination,
                        R.drawable.ic_content_cut_black_24dp,
                        R.layout.layout_path_prompt,
                        (d) -> OperationUtil.move(f, activity, d),
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
            if (activity.getDefaultPreferences().getBoolean(PREF_USE_CONTEXT_FOR_OPS, true)) {
                activity.addFileToOpContext(COPY, f);
                if (activity.getDefaultPreferences().getBoolean(PREF_USE_CONTEXT_FOR_OPS_TOAST, true)) {
                    Toast.makeText(activity, activity.getString(R.string.file_added_to_context) + f.getName(), LENGTH_SHORT).show();
                }
            } else {
                AlertDialog alertDialog = arrayAdapter.getGenericFileOpDialog(
                        R.string.copy,
                        R.string.op_destination,
                        R.drawable.ic_content_copy_black_24dp,
                        R.layout.layout_path_prompt,
                        (d) -> OperationUtil.copy(f, activity, d),
                        (d) -> Log.d(TAG, "Cancelled."));
                alertDialog.setOnShowListener(d -> arrayAdapter.presetPathForDialog(f, alertDialog));
                alertDialog.show();
            }
            activity.reloadCurrentDirectory();
            return true;
        });
    }
}
