package io.lerk.lrkFM.adapter;

import android.annotation.SuppressLint;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Vibrator;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

import io.lerk.lrkFM.Pref;
import io.lerk.lrkFM.R;
import io.lerk.lrkFM.entities.FMArchive;
import io.lerk.lrkFM.entities.FMFile;
import io.lerk.lrkFM.activities.file.ContextMenuUtil;

import static android.widget.Toast.LENGTH_SHORT;
import static io.lerk.lrkFM.consts.PreferenceEntity.FILENAME_LENGTH;
import static io.lerk.lrkFM.consts.PreferenceEntity.SHOW_MENU_BUTTON_INSTEAD_OF_CHECKBOX;
import static io.lerk.lrkFM.consts.PreferenceEntity.ZIPS_EXPLORABLE;

/**
 * Heavily abused ArrayAdapter that also adds menus and listeners.
 *
 * @author Lukas Fülling (lukas@k40s.net)
 */
public class FileArrayAdapter extends BaseArrayAdapter {

    public FileArrayAdapter(Context context, int resource, List<FMFile> items) {
        super(context, resource, items);
    }

    /**
     * Opens a file using the default app.
     *
     * @param f the file
     */
    @Override
    protected void openFile(FMFile f) {
        if (new Pref<Boolean>(ZIPS_EXPLORABLE).getValue() && f.isArchive() && !(f instanceof FMArchive)) {
            activity.loadPath(f.getAbsolutePath());
        } else {
            Intent i = new Intent(Intent.ACTION_VIEW);
            i.setDataAndType(Uri.fromFile(f.getFile()), FMFile.getMimeTypeFromFile(f));
            i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            try {
                getContext().startActivity(i);
            } catch (ActivityNotFoundException e) {
                Toast.makeText(getContext(), R.string.no_app_to_handle_file, LENGTH_SHORT).show();
            }
        }
    }

    /**
     * Gets the size of a file as formatted string.
     *
     * @param f the file
     * @return the size, formatted.
     */
    private String getSizeFormatted(FMFile f) {
        String[] units = new String[]{"B", "KiB", "MiB", "GiB", "TiB", "PiB"};
        Long length = f.getFile().length();
        if (Objects.equals(length, 0L)) {
            return "0";
        }
        Double number = Math.floor(Math.log(length) / Math.log(1024));
        Double pow = Math.pow(1024, Math.floor(number));
        Double d = length / pow;
        String formattedString = d.toString().substring(0, d.toString().indexOf(".") + 2);
        return formattedString + ' ' + units[number.intValue()];
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

        @SuppressLint("InflateParams") View v = LayoutInflater.from(activity).inflate(R.layout.layout_file, null); // works.

        if (f != null) {
            TextView fileNameView = v.findViewById(R.id.fileTitle);
            TextView filePermissions = v.findViewById(R.id.filePermissions);
            TextView fileDate = v.findViewById(R.id.fileDate);
            TextView fileSize = v.findViewById(R.id.fileSize);
            ImageView fileImage = v.findViewById(R.id.fileIcon);

            final String fileName = f.getName();
            if (fileNameView != null) {
                int maxLength = Integer.parseInt(new Pref<String>(FILENAME_LENGTH).getValue());
                if (fileName.length() >= maxLength) {
                    @SuppressLint("SetTextI18n") String output = fileName.substring(0, maxLength - 3) + "...";
                    fileNameView.setText(output); //shorten long names
                } else {
                    fileNameView.setText(fileName);
                }
            }
            if (filePermissions != null) {
                filePermissions.setText(f.getPermissions());
            }
            if (fileDate != null) {
                fileDate.setText(SimpleDateFormat.getDateTimeInstance().format(f.getLastModified()));
            }
            if (fileSize != null) {
                if (f.isDirectory()) {
                    fileSize.setVisibility(View.GONE);
                } else {
                    fileSize.setText(getSizeFormatted(f));
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
            if (f.isDirectory() || (f.isExplorableArchive() && new Pref<Boolean>(ZIPS_EXPLORABLE).getValue())) {
                v.setOnClickListener(v1 -> activity.loadPath(f.getFile().getAbsolutePath()));
            } else {
                v.setOnClickListener(v1 -> openFile(f));
            }
            v.setOnCreateContextMenuListener((menu, view, info) -> new ContextMenuUtil(activity, this).initializeContextMenu(f, fileName, menu));


            ImageButton contextButton = v.findViewById(R.id.contextMenuButton);
            contextButton.setOnClickListener(v1 -> {
                ((Vibrator) activity.getSystemService(Context.VIBRATOR_SERVICE)).vibrate(8);
                activity.getFileListView().showContextMenuForChild(v);
            });

            CheckBox checkBox = v.findViewById(R.id.fileCheckBox);
            checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isChecked && !isInContext(f)) {
                    activity.getFileOpContext().getSecond().add(f);
                } else if (!isChecked && isInContext(f)) {
                    removeFromContext(f);
                }
            });

            if (new Pref<Boolean>(SHOW_MENU_BUTTON_INSTEAD_OF_CHECKBOX).getValue()) {
                checkBox.setVisibility(View.INVISIBLE);
                contextButton.setVisibility(View.VISIBLE);
            } else {
                checkBox.setVisibility(View.VISIBLE);
                contextButton.setVisibility(View.GONE);
            }

            for (FMFile contextFile : activity.getFileOpContext().getSecond()) {
                if (contextFile.getFile().getAbsolutePath().equals(f.getFile().getAbsolutePath())) {
                    v.setBackgroundColor(activity.getColorByAttr(R.attr.colorPrimary));
                    checkBox.setChecked(true);
                } else {
                    checkBox.setChecked(false);
                }
            }
        }
        return v;
    }

    private boolean isInContext(FMFile f) {
        boolean res = false;
        //noinspection ForLoopReplaceableByForEach prevents ConcurrentModificationException
        for (Iterator<FMFile> iterator = activity.getFileOpContext().getSecond().iterator(); iterator.hasNext(); ) {
            FMFile contextFile = iterator.next();
            if (contextFile.getFile().getAbsolutePath().equals(f.getFile().getAbsolutePath())) {
                res = true;
            }
        }
        return res;
    }

    private void removeFromContext(FMFile f) {
        //noinspection ForLoopReplaceableByForEach prevents ConcurrentModificationException
        for (Iterator<FMFile> iterator = activity.getFileOpContext().getSecond().iterator(); iterator.hasNext(); ) {
            FMFile contextFile = iterator.next();
            if (contextFile.getFile().getAbsolutePath().equals(f.getFile().getAbsolutePath())) {
                activity.getFileOpContext().getSecond().remove(contextFile);
            }
        }
    }
}
