package io.lerk.lrkFM.tasks.archive;

import io.lerk.lrkFM.Handler;
import io.lerk.lrkFM.activities.file.ArchiveParentFinder;
import io.lerk.lrkFM.entities.FMFile;
import io.lerk.lrkFM.exceptions.BlockingStuffOnMainThreadException;
import io.lerk.lrkFM.tasks.CallbackTask;

/**
 * Task to find the parent archive of a file.
 *
 * @author Lukas Fülling (lukas@k40s.net)
 */
public class ArchiveParentFinderTask extends CallbackTask<ArchiveParentFinder> {

    private final FMFile fmFile;

    /**
     * Constructor.
     *
     * @param callback the callback to use.
     */
    public ArchiveParentFinderTask(FMFile fmFile, Handler<ArchiveParentFinder> callback) {
        super(callback);
        this.fmFile = fmFile;
    }

    @Override
    protected ArchiveParentFinder doInBackground(Void... voids) {
        try {
            return new ArchiveParentFinder(fmFile.getAbsolutePath()).invoke();
        } catch (BlockingStuffOnMainThreadException e) {
            throw new RuntimeException("This should not happen!", e);
        }
    }
}
