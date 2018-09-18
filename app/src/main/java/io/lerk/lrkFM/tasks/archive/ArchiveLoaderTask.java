package io.lerk.lrkFM.tasks.archive;

import io.lerk.lrkFM.Handler;
import io.lerk.lrkFM.entities.FMArchive;
import io.lerk.lrkFM.entities.FMFile;
import io.lerk.lrkFM.exceptions.BlockingStuffOnMainThreadException;
import io.lerk.lrkFM.tasks.CallbackTask;

public class ArchiveLoaderTask extends CallbackTask<FMArchive> {

    private final FMFile file;

    /**
     * Constructor.
     *
     * @param callback the callback to use.
     */
    public ArchiveLoaderTask(FMFile file, Handler<FMArchive> callback) {
        super(callback);
        this.file = file;
    }

    @Override
    protected FMArchive doInBackground(Void... voids) {
        try {
            return new FMArchive(file.getFile());
        } catch (BlockingStuffOnMainThreadException e) {
            throw new RuntimeException("This should not happen!", e);
        }
    }
}
