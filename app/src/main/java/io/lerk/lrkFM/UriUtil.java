package io.lerk.lrkFM;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;
import android.provider.OpenableColumns;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class UriUtil {
    private static final String TAG = UriUtil.class.getCanonicalName();

    /**
     * Returns the file name behind a content:// URI.
     *
     * @param ctx any valid context
     * @param uri the uri
     * @return filename of the file behind the uri
     * @throws Exception in case the URI is invalid
     */
    @Nullable
    public static String getFilenameForUri(@NonNull Context ctx, @NonNull Uri uri) throws Exception {

        final String scheme = uri.getScheme();
        if (scheme == null || !scheme.equals(ContentResolver.SCHEME_CONTENT)) {
            throw new Exception("Invalid scheme!");
        }

        String result = null;

        String[] columns = new String[]{MediaStore.Images.Media.DATA, OpenableColumns.DISPLAY_NAME};
        try (Cursor cursor = ctx.getContentResolver()
                .query(uri, columns, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                int idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                if (idx != -1) {
                    result = cursor.getString(idx);
                }

                if (idx == -1 || result == null) {
                    idx = cursor.getColumnIndex(MediaStore.Images.Media.DATA);
                    if (idx != -1) {
                        result = cursor.getString(idx);
                    }
                }
            }
        }

        return result;
    }

    public static String getFilenameWithoutExtension(@NonNull String filename) throws IndexOutOfBoundsException {
        return filename.substring(0, filename.lastIndexOf("."));
    }

    public static String getFileExtension(@NonNull String filename) throws Exception {
        int i = filename.lastIndexOf(".");
        String extension = filename.substring(i + 1);
        if (i == -1 || !extension.matches("\\w+")) {
            throw new Exception("File has no extension!");
        }
        return extension;
    }

    @Nullable
    public static File createTempFileFromUri(Context ctx, Uri uri) throws Exception {
        try (InputStream inputStream = ctx.getContentResolver().openInputStream(uri)) {
            String fullFileName = UriUtil.getFilenameForUri(ctx, uri);
            if (fullFileName != null) {
                String fileName = UriUtil.getFilenameWithoutExtension(fullFileName);
                String fileExtension = "." + UriUtil.getFileExtension(fullFileName);

                File tempFile = File.createTempFile(fileName, fileExtension, ctx.getCacheDir());
                if (!tempFile.exists()) {
                    throw new Exception("Unable to create temp file!");
                }

                Log.d(TAG, "Temp file created: '" + tempFile.getAbsolutePath() + "'!");

                if (inputStream != null) {
                    int availableBytes = inputStream.available();
                    byte[] buffer = new byte[availableBytes];
                    int readBytes = inputStream.read(buffer);

                    if (availableBytes == readBytes) {
                        try (OutputStream outStream = new FileOutputStream(tempFile)) {
                            outStream.write(buffer);
                            Log.d(TAG, "File written 'successfully'!");
                            outStream.close();
                            inputStream.close();
                            return tempFile;
                        } catch (IOException e) {
                            throw new Exception("Unable to write temp file!", e);
                        }
                    } else {
                        throw new Exception("Invalid byte count! Read: " + readBytes + ", Available: " + availableBytes + "!");
                    }
                } else {
                    throw new Exception("Unable to read stream from URI!");
                }

            } else {
                throw new Exception("Unable to retrieve filename!");
            }
        }
    }
}
