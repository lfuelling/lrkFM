package io.lerk.lrkfm;

import android.content.Context;
import android.util.Log;

import org.apache.commons.io.FileSystemUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.Date;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Created by lfuelling on 15.03.17.
 */

public class FileView {

    private final Context context;

    private final String location;

    private final ArrayList<FMFile> files;

    public FileView(Context context, String location) {
        this.context = context;
        this.location = location;
        files = loadLocationFiles(this.location);
        initializeFileList();
    }

    private void initializeFileList() {
        //TODO build list of 'layout_file' views
    }

    private ArrayList<FMFile> loadLocationFiles(String location) {
        File locationFile = new File(location);
        if (locationFile.isDirectory()) {

            ArrayList<FMFile> result = new ArrayList<>();

            Arrays.asList(locationFile.listFiles()).forEach((f) -> {

                String permissionString = ((f.isDirectory()) ? "d" : "-") +
                        ((f.canRead()) ? "r" : "-") +
                        ((f.canWrite()) ? "w" : "-") +
                        ((f.canExecute()) ? "x" : "-"); // lol

                FMFile file = new FMFile(f.getName(), permissionString, new Date(f.lastModified()));
                result.add(file);
            });

            return result;
        } else {
            return loadLocationFiles(locationFile.getParent());
        }
    }


}
