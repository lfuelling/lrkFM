package io.lerk.lrkFM.consts;

import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.util.Log;

import io.lerk.lrkFM.Handler;
import io.lerk.lrkFM.R;
import io.lerk.lrkFM.entities.FMFile;

/**
 * File types.
 *
 * @author Lukas Fülling (lukas@k40s.net)
 */
public enum FileType {
    // known archives
    ARCHIVE_RAR(R.string.type_rar, "rar"),
    ARCHIVE_ZIP(R.string.type_zip, "zip"),
    ARCHIVE_TAR(R.string.type_tar, "tar"),
    ARCHIVE_P7Z(R.string.type_p7z, "7z"),
    ARCHIVE_TGZ(R.string.type_tgz, "gz"),
    //

    UNKNOWN(R.string.type_unknown, "");

    public static  final String TAG = FileType.class.getName();

    private final String extension;
    private final int i18n;
    private String fileExtension = null;

    private Handler<FMFile> handler = new Handler<FMFile>() {
        @Override
        public void handle(FMFile fmFile) {
            Log.i(TAG, "No Handler for " + extension);
        }
    };

    FileType(@StringRes int i18n, @Nullable String extension) {
        this.i18n = i18n;
        this.extension = extension;
    }

    public String getExtension() {
        return extension;
    }

    public int getI18n() {
        return i18n;
    }

    public void handle(FMFile f){
        handler.handle(f);
    }

    public FileType newHandler(Handler<FMFile> h){
        this.handler = h;
        return this;
    }

    public String getFileExtension() {
            return fileExtension;
    }

    public void setFileExtension(String fileExtension) {
        this.fileExtension = fileExtension;
    }

    /**
     * Setter for fileExtension that returns this.
     * @param fileExtension {@link #fileExtension}
     * @return this
     */
    public FileType withExtension(String fileExtension) {
        setFileExtension(fileExtension);
        return this;
    }
}
