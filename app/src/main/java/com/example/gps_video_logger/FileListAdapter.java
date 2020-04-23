package com.example.gps_video_logger;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.media.ThumbnailUtils;
import android.os.Environment;
import android.provider.MediaStore;
import android.text.InputType;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import java.io.File;
import java.util.ArrayList;

public class FileListAdapter extends RecyclerView.Adapter<FileListAdapter.ViewHolder> {

    private ArrayList<String> mDataset;


    // Provide a reference to the views for each data item
    // Complex data items may need more than one view per item, and
    // you provide access to all the views for a data item in a view holder
    static class ViewHolder extends RecyclerView.ViewHolder {
        // each data item is just a string in this case
        View mView;
        final ImageView mImageView;
        final TextView mTextView;

        ViewHolder(View v) {
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
    public void onBindViewHolder(final ViewHolder holder, final int position) {
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
                String selectedFilename = mDataset.get(holder.getAdapterPosition());
                Log.d("Fileo",selectedFilename);
                Intent PlaybackIntent = new Intent( view.getContext() , PlaybackActivity.class);
                PlaybackIntent.putExtra("filename",selectedFilename);
                view.getContext().startActivity(PlaybackIntent);
            }
        });

        // Rename on long click
        holder.itemView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {

                AlertDialog.Builder builder = new AlertDialog.Builder(view.getContext(),R.style.DialogTheme);
                builder.setTitle("Rename Journey");

                // Set up the input
                final EditText input = new EditText(view.getContext());
                // Specify the type of input expected; this, for example, sets the input as a password, and will mask the text
                input.setInputType(InputType.TYPE_CLASS_TEXT);
                input.setTextColor(view.getResources().getColor(R.color.colorText));
                input.setText(mDataset.get(holder.getAdapterPosition()));

                builder.setView(input);

                // Set up the buttons
                builder.setPositiveButton("Confirm", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String m_Text = input.getText().toString();
                        String oldName = mDataset.get(holder.getAdapterPosition());
                        rename_file(oldName, m_Text, holder.getAdapterPosition());
                        notifyItemChanged(holder.getAdapterPosition());
                    }
                });
                builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                });

                builder.show();
                return true;
            }
        });

    }





    private void rename_file(String old_name, String new_name, int position){
        String path = Environment.getExternalStorageDirectory()
                + File.separator + "GPS_Video_Logger" + File.separator;

        if (new_name.length() > 0){
            File new_mp4_file = new File(path + new_name + ".mp4");
            new File(path + old_name + ".mp4").renameTo(new_mp4_file);

            File new_gpx_file = new File(path + new_name + ".gpx");
            new File(path + old_name + ".gpx").renameTo(new_gpx_file);

            mDataset.set(position,new_name);
        }

    }

    // Return the size of your dataset (invoked by the layout manager)
    @Override
    public int getItemCount() {
        return mDataset.size();
    }
}
