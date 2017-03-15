package io.lerk.lrkfm;

import android.content.Context;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.ArrayList;
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
        if(locationFile.isDirectory()) {

            ProcessBuilder builder = new ProcessBuilder("ls", "-la", location);
            try {
                Process start = builder.start();
                InputStream inputStream = start.getInputStream();

                try (BufferedReader buffer = new BufferedReader(new InputStreamReader(inputStream))) {
                    String s = buffer.lines().collect(Collectors.joining("\n"));
                    parseLsOutput(s);
                }

            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            return loadLocationFiles(locationFile.getParent());
        }
    }

    private void parseLsOutput(String s) {

    }

}
