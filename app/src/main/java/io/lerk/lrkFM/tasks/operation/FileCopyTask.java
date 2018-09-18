package io.lerk.lrkFM.tasks.operation;

import android.app.AlertDialog;
import android.util.Log;
import android.widget.EditText;
import android.widget.Toast;

import java.io.File;
import java.lang.ref.WeakReference;

import io.lerk.lrkFM.Handler;
import io.lerk.lrkFM.R;
import io.lerk.lrkFM.activities.file.FileActivity;
import io.lerk.lrkFM.entities.FMFile;
import io.lerk.lrkFM.exceptions.BlockingStuffOnMainThreadException;

import static android.widget.Toast.LENGTH_SHORT;

public class FileCopyTask extends FileOperationTask {

    private static final String TAG = FileCopyTask.class.getCanonicalName();
    private final WeakReference<FileActivity> contextRef;
    private final FMFile f;
    private File destination;

    public FileCopyTask(FileActivity context, Handler<Boolean> callback, FMFile f, AlertDialog d) {
        super(context, callback);
        setDestination(context, f, d);
        this.contextRef = new WeakReference<>(context);
        this.f = f;
        this.dialog.setTitle(R.string.copying);
        this.dialog.setMessage(context.getString(R.string.copying_detail) + FileActivity.WHITESPACE + destination.getName());
    }

    @Override
    protected Boolean doInBackground(Void... voids) {
        try {
            return OperationUtil.doCopy(f, destination);
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

    private void setDestination(FileActivity context, FMFile f, AlertDialog d) {
        String pathname;
        if (d != null) {
            EditText editText = d.findViewById(R.id.destinationPath);
            pathname = editText.getText().toString();
            if (pathname.isEmpty()) {
                Toast.makeText(context, R.string.err_empty_input, LENGTH_SHORT).show();
                cancel(true);
            }
        } else {
            pathname = context.getCurrentDirectory();
        }

        destination = new File(pathname);
        if (destination.isDirectory() && !f.isDirectory()) {
            destination = new File(destination.getAbsolutePath() + File.separator + f.getName());
        }
        if (destination.exists()) {
            getFileExistsDialogBuilder(context)
                    .setOnCancelListener(dialogInterface -> cancel(true))
                    .create().show();
        }
    }
}
