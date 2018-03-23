package io.lerk.lrkFM.util;

import com.google.firebase.perf.FirebasePerformance;
import com.google.firebase.perf.metrics.Trace;

import java.io.File;

import io.lerk.lrkFM.entities.FMArchive;
import io.lerk.lrkFM.entities.FMFile;

import static io.lerk.lrkFM.consts.Preference.PERFORMANCE_REPORTING;

/**
 * @author Lukas Fülling (lukas@k40s.net)
 */
public class ArchiveParentFinder {
    private String path;
    private boolean archive;
    private FMArchive archiveFile;

    public ArchiveParentFinder(String path) {
        this.path = path;
    }

    public boolean isArchive() {
        return archive;
    }

    public FMArchive getArchiveFile() {
        return archiveFile;
    }

    public ArchiveParentFinder invoke() {
        archive = false;
        archiveFile = null;
        String tPath = path;
        Trace trace = null;
        if(new PrefUtils<Boolean>(PERFORMANCE_REPORTING).getValue()){
            trace = FirebasePerformance.startTrace("check_if_parent_is_archive");
        }
        while (!"/".equals(tPath)) {
            FMFile f = new FMFile(new File(tPath));
            if (f.isArchive()) {
                archive = true;
                archiveFile = new FMArchive(new File(tPath));
            }
            tPath = new File(tPath).getParent();
            if(trace != null) {
                trace.incrementCounter("parent_depth");
            }
            if(archive || tPath == null || tPath.isEmpty()) {
                break;
            }
        }
        if(trace!=null){
            trace.putAttribute("archive", String.valueOf(archive));
            trace.stop();
        }
        return this;
    }
}
