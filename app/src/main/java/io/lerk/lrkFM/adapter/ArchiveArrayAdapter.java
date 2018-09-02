package io.lerk.lrkFM.adapter;

import android.annotation.SuppressLint;
import android.content.Context;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;

import io.lerk.lrkFM.R;
import io.lerk.lrkFM.entities.FMArchive;
import io.lerk.lrkFM.entities.FMFile;
import io.lerk.lrkFM.util.ContextMenuUtil;
import io.lerk.lrkFM.util.PrefUtils;

import static io.lerk.lrkFM.consts.PreferenceEntity.FILENAME_LENGTH;

/**
 * @author Lukas Fülling (lukas@k40s.net)
 */
public class ArchiveArrayAdapter extends BaseArrayAdapter {

    private static final String TAG = ArchiveArrayAdapter.class.getCanonicalName();

    private final FMArchive archive;

    public ArchiveArrayAdapter(Context context, int resource, List<FMFile> items, FMArchive archive) {
        super(context, resource, items);
        this.archive = archive;
    }

    @NonNull
    @Override
    public View getView(int position, View convertView, @NonNull ViewGroup parent) {
        return initUI(getItem(position));
    }

    /**
     * Opens a file using the default app.
     *
     * @param f the file
     */
    @Override
    protected void openFile(FMFile f) {
        if(f.isDirectory()) {
            activity.loadArchivePath(f.getAbsolutePath(), archive);
        } else {
            Toast.makeText(activity, R.string.only_browsing_in_archives, Toast.LENGTH_LONG).show();
        }
    }

    /**
     * Initializes the UI for each file.
     *
     * @param f the file
     * @return the initialized UI
     */
    @Override
    protected View initUI(FMFile f) {
        assert activity != null;

        @SuppressLint("InflateParams") View v = LayoutInflater.from(activity).inflate(R.layout.layout_archive_file, null); // works.

        if (f != null) {
            TextView fileNameView = v.findViewById(R.id.fileTitle);
            ImageView fileImage = v.findViewById(R.id.fileIcon);

            final String fileName = f.getName();
            if (fileNameView != null) {
                int maxLength = Integer.parseInt(new PrefUtils<String>(FILENAME_LENGTH).getValue());
                if (fileName.length() >= maxLength) {
                    @SuppressLint("SetTextI18n") String output = fileName.substring(0, maxLength - 3) + "...";
                    fileNameView.setText(output); //shorten long names
                } else {
                    fileNameView.setText(fileName);
                }
            }
            if (fileImage != null) {
                if (!f.isDirectory()) {
                    if (f.isArchive()) {
                        fileImage.setImageDrawable(getContext().getDrawable(R.drawable.ic_perm_media_black_24dp));
                    } else {
                        fileImage.setImageDrawable(getContext().getDrawable(R.drawable.ic_insert_drive_file_black_24dp));
                    }
                }
            }

            v.setOnClickListener(v1 -> openFile(f));
            v.setOnCreateContextMenuListener((menu, view, info) -> new ContextMenuUtil(activity, this).initializeContextMenu(f, fileName, menu));
            ImageButton contextButton = v.findViewById(R.id.contextMenuButton);
            contextButton.setOnClickListener(v1 -> activity.getFileListView().showContextMenuForChild(v));

            for (FMFile contextFile : activity.getFileOpContext().getSecond()) {
                if (contextFile.getFile().getAbsolutePath().equals(f.getFile().getAbsolutePath())) {
                    v.setBackgroundColor(activity.getColor(R.color.default_primary));
                }
            }
        }
        return v;
    }
}
