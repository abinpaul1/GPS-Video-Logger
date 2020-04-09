package com.example.gps_app;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.media.ThumbnailUtils;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import java.io.File;
import java.util.ArrayList;

public class FileListAdapter extends RecyclerView.Adapter<FileListAdapter.ViewHolder> {

    private ArrayList<String> mDataset;

    private String selectedFilename;

    // Provide a reference to the views for each data item
    // Complex data items may need more than one view per item, and
    // you provide access to all the views for a data item in a view holder
    public static class ViewHolder extends RecyclerView.ViewHolder {
        // each data item is just a string in this case
        public View mView;
        public final ImageView mImageView;
        public final TextView mTextView;

        public ViewHolder(View v) {
            super(v);
            mView = v;
            mImageView = v.findViewById(R.id.item_image);
            mTextView = v.findViewById(R.id.item_name);
        }
    }


    // Provide a suitable constructor (depends on the kind of dataset)
    public FileListAdapter(ArrayList<String> myDataset) {
        mDataset = myDataset;
    }


    // Create new views (invoked by the layout manager)
    @Override
    public FileListAdapter.ViewHolder onCreateViewHolder(ViewGroup parent,
                                                           int viewType) {
        // create a new view
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.file_list_view, parent, false);

        return new ViewHolder(view);
    }



    // Replace the contents of a view (invoked by the layout manager)
    @Override
    public void onBindViewHolder(final ViewHolder holder, int position) {
        // - get element from your dataset at this position
        // - replace the contents of the view with that element

        holder.mTextView.setText(mDataset.get(position));

        Bitmap bMap = ThumbnailUtils.createVideoThumbnail(Environment.getExternalStorageDirectory()
                + File.separator + "GPS_Video_Logger" + File.separator + mDataset.get(position) + ".mp4", MediaStore.Video.Thumbnails.MINI_KIND);
        holder.mImageView.setImageBitmap(bMap);


        //On click launch playback activity with selected file
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d("Fileo",Integer.toString(holder.getAdapterPosition()));
                selectedFilename = mDataset.get(holder.getAdapterPosition());
                Log.d("Fileo",selectedFilename);
                Intent PlaybackIntent = new Intent( view.getContext() , MainActivity.class);
                PlaybackIntent.putExtra("filename",selectedFilename);
                view.getContext().startActivity(PlaybackIntent);
            }
        });
    }

    // Return the size of your dataset (invoked by the layout manager)
    @Override
    public int getItemCount() {
        return mDataset.size();
    }
}
