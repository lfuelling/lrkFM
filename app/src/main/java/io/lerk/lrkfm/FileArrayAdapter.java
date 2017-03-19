package io.lerk.lrkfm;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.List;

public class FileArrayAdapter extends ArrayAdapter<FMFile> {

    public FileArrayAdapter(Context context, int resource, List<FMFile> items) {
        super(context, resource, items);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        View v = convertView;

        if (v == null) {
            LayoutInflater vi;
            vi = LayoutInflater.from(getContext());
            v = vi.inflate(R.layout.layout_file, null);
        }

        FMFile f = getItem(position);

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
                if(!f.getDirectory()) {
                    fileImage.setImageDrawable(getContext().getDrawable(R.drawable.ic_insert_drive_file_black_24dp));
                }
            }
            v.setOnClickListener(v1 -> {
                Context context = FileArrayAdapter.this.getContext();
                if (context instanceof FileActivity) {
                    FileActivity fa = (FileActivity) context;
                    fa.loadDirectory(f.getFile().getAbsolutePath());
                }
            });
        }

        return v;
    }
}
