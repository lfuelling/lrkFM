package io.lerk.lrkFM.tasks.operation;

import android.app.AlertDialog;

import io.lerk.lrkFM.Handler;
import io.lerk.lrkFM.R;
import io.lerk.lrkFM.activities.file.FileActivity;
import io.lerk.lrkFM.tasks.ProgressDialogCallbackTask;

/**
 * Abstract operation task.
 *
 * @author Lukas Fülling (lukas@k40s.net)
 */
public abstract class FileOperationTask extends ProgressDialogCallbackTask<Boolean> {

    FileOperationTask(FileActivity context, Handler<Boolean> callback) {
        super(context, callback);
        dialog.setCancelable(false);
    }

    public static AlertDialog.Builder getFileExistsDialogBuilder(FileActivity context) {
        return new AlertDialog.Builder(context)
                .setCancelable(true)
                .setTitle(R.string.warn_file_exists_title)
                .setMessage(R.string.warn_file_exists_msg)
                .setPositiveButton(R.string.yes, (dialogInterface, i) -> dialogInterface.dismiss())
                .setNegativeButton(R.string.cancel, (dialogInterface, i) -> dialogInterface.cancel());
    }
}
