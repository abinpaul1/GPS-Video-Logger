package com.example.gps_video_logger;

import android.os.Bundle;


import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Environment;

import java.io.File;
import java.util.ArrayList;

public class FilePicker extends AppCompatActivity {

    private RecyclerView recyclerView;
    private RecyclerView.Adapter mAdapter;
    private RecyclerView.LayoutManager layoutManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_file_picker);
        recyclerView = (RecyclerView) findViewById(R.id.fileList);

        // use this setting to improve performance if you know that changes
        // in content do not change the layout size of the RecyclerView
        recyclerView.setHasFixedSize(true);

        // use a linear layout manager
        layoutManager = new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false);
        recyclerView.setLayoutManager(layoutManager);

        // specify an adapter (see also next example)
        mAdapter = new FileListAdapter(FetchFileList());
        recyclerView.setAdapter(mAdapter);
    }


    private ArrayList<String> FetchFileList() {

        ArrayList<String> filenames = new ArrayList<String>();
        String path = Environment.getExternalStorageDirectory()
                + File.separator + "GPS_Video_Logger";

        File directory = new File(path);
        File[] files = directory.listFiles();

        for (int i = 0; i < files.length; i++)
        {

            String file_name = files[i].getName();
            if (file_name.endsWith(".mp4"))
                filenames.add(file_name.split(".mp4")[0]);
        }
        return filenames;
    }
}
