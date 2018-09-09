package io.lerk.lrkFM.tasks.operation;

import android.app.AlertDialog;
import android.support.annotation.Nullable;
import android.util.Log;
import android.widget.EditText;
import android.widget.Toast;

import java.io.File;
import java.lang.ref.WeakReference;

import io.lerk.lrkFM.Handler;
import io.lerk.lrkFM.R;
import io.lerk.lrkFM.activities.FileActivity;
import io.lerk.lrkFM.entities.FMFile;
import io.lerk.lrkFM.exceptions.BlockingStuffOnMainThreadException;

import static android.widget.Toast.LENGTH_SHORT;

public class FileMoveTask extends FileOperationTask {
    private static final String TAG = FileMoveTask.class.getCanonicalName();
    private final WeakReference<FileActivity> contextRef;
    private final FMFile f;
    private final File destination;


    public FileMoveTask(FileActivity context, Handler<Boolean> callback, FMFile f, @Nullable AlertDialog d) {
        this(context, callback, f, (d != null) ? ((EditText) d.findViewById(R.id.destinationName)).getText().toString() : context.getCurrentDirectory());
    }

    private FileMoveTask(FileActivity context, Handler<Boolean> callback, FMFile f, String newName) {
        super(context, callback);
        this.contextRef = new WeakReference<>(context);
        this.f = f;
        if (newName.isEmpty()) {
            Toast.makeText(context, R.string.err_empty_input, LENGTH_SHORT).show();
            this.cancel(true);
        }
        destination = new File(OperationUtil.getFullPathForRename(f, newName));
        if (destination.exists()) {
            getFileExistsDialogBuilder(context)
                    .setOnCancelListener(d -> cancel(true)) //cancel task on "no"
                    .create().show();
        }
        this.dialog.setTitle(R.string.moving);
        this.dialog.setMessage(context.getString(R.string.moving_detail) + FileActivity.WHITESPACE + f.getName());
    }

    @Override
    protected Boolean doInBackground(Void... voids) {
        try {
            return OperationUtil.doMove(f, destination);
        } catch (BlockingStuffOnMainThreadException e) {
            Log.wtf(TAG, "This should not happen here!", e);
            return false;
        }
    }

    @Override
    protected void onPostExecute(Boolean success) {
        super.onPostExecute(success);
        contextRef.get().clearFileOpCache();
        contextRef.get().reloadCurrentDirectory();
    }
}
