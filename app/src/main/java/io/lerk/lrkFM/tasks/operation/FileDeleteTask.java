package io.lerk.lrkFM.tasks.operation;

import android.util.Log;

import io.lerk.lrkFM.Handler;
import io.lerk.lrkFM.R;
import io.lerk.lrkFM.activities.file.FileActivity;
import io.lerk.lrkFM.entities.FMFile;
import io.lerk.lrkFM.exceptions.BlockingStuffOnMainThreadException;

public class FileDeleteTask extends FileOperationTask {

    private static final String TAG = FileDeleteTask.class.getCanonicalName();
    private final FMFile f;

    public FileDeleteTask(FileActivity context, Handler<Boolean> callback, FMFile f) {
        super(context, callback);
        this.f = f;
        this.dialog.setTitle(R.string.deleting);
        this.dialog.setMessage(context.getString(R.string.deleting_detail) + FileActivity.WHITESPACE + f.getName());
    }

    @Override
    protected Boolean doInBackground(Void... voids) {
        try {
            return OperationUtil.doDeleteNoValidation(f);
        } catch (BlockingStuffOnMainThreadException e) {
            Log.wtf(TAG, "This should not happen here!", e);
            return false;
        }
    }
}
