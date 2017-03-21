package io.lerk.lrkfm.util;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.MimeTypeMap;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;

import io.lerk.lrkfm.R;
import io.lerk.lrkfm.activities.FileActivity;
import io.lerk.lrkfm.entities.FMFile;

public class FileArrayAdapter extends ArrayAdapter<FMFile> {

    private static final int ID_COPY = 0;
    private static final int ID_MOVE = 1;
    private static final int ID_RENAME = 2;
    private static final int ID_DELETE = 3;

    public FileArrayAdapter(Context context, int resource, List<FMFile> items) {
        super(context, resource, items);
    }

    @NonNull
    @Override
    public View getView(int position, View convertView, @NonNull ViewGroup parent) {
        return initUI(getItem(position));
    }

    private View initUI(FMFile f) {

        Context context = getContext();

        View v = LayoutInflater.from(context).inflate(R.layout.layout_file, null);

        if (f != null) {
            TextView fileNameView = (TextView) v.findViewById(R.id.fileTitle);
            TextView filePermissions = (TextView) v.findViewById(R.id.filePermissions);
            TextView fileDate = (TextView) v.findViewById(R.id.fileDate);
            ImageView fileImage = (ImageView) v.findViewById(R.id.fileIcon);

            if (fileNameView != null) {
                fileNameView.setText(f.getName());
            }
            if (filePermissions != null) {
                filePermissions.setText(f.getPermissions());
            }
            if (fileDate != null) {
                fileDate.setText(f.getLastModified().toString());
            }
            if (fileImage != null) {
                if (!f.getDirectory()) {
                    fileImage.setImageDrawable(getContext().getDrawable(R.drawable.ic_insert_drive_file_black_24dp));
                }
            }
            if (f.getDirectory()) {
                v.setOnClickListener(v1 -> {

                    if (context instanceof FileActivity) {
                        FileActivity fa = (FileActivity) context;
                        fa.loadDirectory(f.getFile().getAbsolutePath());
                    }
                });
            } else {
                v.setOnClickListener(v1 -> {
                    Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT, Uri.fromFile(f.getFile()));
                    i.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    i.setType(MimeTypeMap.getSingleton().getMimeTypeFromExtension(MimeTypeMap.getFileExtensionFromUrl(f.getFile().getAbsolutePath())));
                    try {
                        getContext().startActivity(i);
                    } catch (ActivityNotFoundException e) {
                        Toast.makeText(getContext(), R.string.no_app_to_handle_file, Toast.LENGTH_SHORT).show();
                    }
                });
                v.setOnCreateContextMenuListener((menu, view, info) -> {
                    menu.add(0, ID_COPY, 0, "Copy").setOnMenuItemClickListener(item -> FileUtil.copy(f, context));
                    menu.add(0, ID_MOVE, 0, "Move").setOnMenuItemClickListener(item -> FileUtil.move(f, context));
                    menu.add(0, ID_RENAME, 0, "Rename").setOnMenuItemClickListener(item -> FileUtil.rename(f, context));
                    menu.add(0, ID_DELETE, 0, "Delete").setOnMenuItemClickListener(item -> FileUtil.delete(f, context));
                });
            }

        }
        return v;
    }
}
