package com.example.gps_video_logger;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;


import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Environment;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;

public class FilePicker extends AppCompatActivity {

    private RecyclerView recyclerView;
    private RecyclerView.Adapter mAdapter;
    private RecyclerView.LayoutManager layoutManager;
    ArrayList<String> filenames;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_file_picker);
        recyclerView = findViewById(R.id.fileList);

        // use this setting to improve performance if you know that changes
        // in content do not change the layout size of the RecyclerView
        recyclerView.setHasFixedSize(true);

        // use a linear layout manager
        layoutManager = new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false);
        recyclerView.setLayoutManager(layoutManager);

        filenames = FetchFileList();

        // specify an adapter (see also next example)
        mAdapter = new FileListAdapter(filenames);
        recyclerView.setAdapter(mAdapter);

        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(simpleItemTouchCallback);
        itemTouchHelper.attachToRecyclerView(recyclerView);
    }


    ItemTouchHelper.SimpleCallback simpleItemTouchCallback = new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT | ItemTouchHelper.DOWN | ItemTouchHelper.UP) {
        @Override
        public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder target) {
            return false;
        }

        @Override
        public void onSwiped(RecyclerView.ViewHolder viewHolder, int swipeDir) {
            //Remove swiped item from list and notify the RecyclerView
            final int position = viewHolder.getAdapterPosition();
            confirm_deletion(position);
        }
    };


    // Prompt for confirmation of deletion using alert dialog
    private void confirm_deletion(final int position){

        // Setting Dialog Title
        AlertDialog.Builder alertBuilder = new AlertDialog.Builder(FilePicker.this,R.style.DialogTheme);
        alertBuilder.setTitle("Confirm Deletion");

        // Setting OK Button
        alertBuilder.setPositiveButton("Confirm", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                remove_file(position);
            }
        });

        alertBuilder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                mAdapter.notifyItemChanged(position);
                dialog.dismiss();
            }
        });

        alertBuilder.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                mAdapter.notifyItemChanged(position);
                dialog.dismiss();
            }
        });

        AlertDialog dialog = alertBuilder.create();
        dialog.show();

    }


    private void remove_file(int position){
        String remove_file = filenames.get(position);
        String path = Environment.getExternalStorageDirectory()
                + File.separator + "GPS_Video_Logger" + File.separator;

        File mp4_file = new File(path + remove_file + ".mp4");
        if(mp4_file.exists())
            mp4_file.delete();

        File gpx_file = new File(path + remove_file + ".gpx");
        if (gpx_file.exists())
            gpx_file.delete();

        filenames.remove(position);
        mAdapter.notifyItemRemoved(position);
    }


    // Return names of video files in the directory
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
        Collections.sort(filenames,Collections.reverseOrder());
        return filenames;
    }
    
}
