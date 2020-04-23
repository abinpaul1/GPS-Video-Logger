package com.example.gps_video_logger;

import androidx.appcompat.app.AppCompatActivity;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.PorterDuff;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.VideoView;

import org.osmdroid.api.IMapController;
import org.osmdroid.config.Configuration;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.Polyline;
import org.xmlpull.v1.XmlPullParserException;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;
import java.util.Locale;

import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.views.MapView;

import io.ticofab.androidgpxparser.parser.GPXParser;
import io.ticofab.androidgpxparser.parser.domain.Gpx;
import io.ticofab.androidgpxparser.parser.domain.Track;
import io.ticofab.androidgpxparser.parser.domain.TrackPoint;
import io.ticofab.androidgpxparser.parser.domain.TrackSegment;


public class PlaybackActivity extends AppCompatActivity {

    //Error codes
    final private int INVALID_GPX_FILE = 1;
    final private int GPX_FILE_NOT_FOUND = 2;
    final private int EMPTY_GPX_FILE = 3;


    long current_delay = 500;
    long prev_time;

    long start_time;
    long end_time;

    FileInputStream fis;

    GPXParser parser;
    Gpx gpx_parsed;
    ListIterator<TrackPoint> gpxTrackPointsIterator;
    List<TrackPoint> gpxTrackPoints;
    TrackPoint currentGpxPoint;

    MapView map = null;
    IMapController mapController;
    Marker prev_marker = null;

    String filename;

    Boolean isLocationPlaying;
    Boolean isVideoPlaying;
    Boolean isPauseForScroll;


    Button play_button,stop_button;
    VideoView mVideoView;
    SeekBar mSeekBar;
    private Handler mHandler = new Handler();
    TextView mVideoTime;


    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);

        //Initializing GPX parser
        parser = new GPXParser();

        //Get filename of file to play
        Intent intent = getIntent();
        filename = intent.getStringExtra("filename");


        /*  Load/initialize the osmdroid configuration
        This can be done by setting this before the layout is inflated
        it 'should' ensure that the map has a writable location for the map cache, even without permissions
        if no tiles are displayed, you can try overriding the cache path using Configuration.getInstance().setCachePath
        Note, the load method also sets the HTTP User Agent to your application's package name */
        Context ctx = getApplicationContext();
        Configuration.getInstance().load(ctx, PreferenceManager.getDefaultSharedPreferences(ctx));

        org.osmdroid.config.IConfigurationProvider osmConf = org.osmdroid.config.Configuration.getInstance();
        File basePath = new File(getFilesDir().getAbsolutePath()+"/osmdroid");
        osmConf.setOsmdroidBasePath(basePath);
        File tileCache = new File(getFilesDir().getAbsolutePath()+"/osmdroid/tiles");
        osmConf.setOsmdroidTileCache(tileCache);


        //Inflating the layout
        setContentView(R.layout.activity_main);


        // Buttons and video view
        stop_button = findViewById(R.id.stop_button);
        play_button = findViewById(R.id.play_button);
        mVideoView = findViewById(R.id.videoView);
        mVideoTime = findViewById(R.id.timeView);
        mSeekBar = findViewById(R.id.seekView);
        mSeekBar.getProgressDrawable().setColorFilter(getResources().getColor(R.color.black_overlay), PorterDuff.Mode.SRC_ATOP);


        //Configuring map display
        map = findViewById(R.id.map);
        map.setTileSource(TileSourceFactory.MAPNIK);

        mapController = map.getController();
        mapController.setZoom(18.0);


        //Location of Video File
        Uri uri = Uri.parse(Environment.getExternalStorageDirectory() +
                File.separator + "GPS_Video_Logger" + File.separator + filename +".mp4");
        mVideoView.setVideoURI(uri);
        mVideoView.requestFocus();

        isLocationPlaying = false;
        isVideoPlaying = false;
        
        mSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean fromUser) {
                if (fromUser){
                    set_gpx_track(); //Setting map position
                    if (isVideoPlaying){
                        start_playback();
                    }
                    else{
                        // Set position in image and time since video is paused
                        mVideoView.seekTo(progressToTimer(mSeekBar.getProgress(),mVideoView.getDuration()));
                        mVideoTime.setText(convertMillisecondsToHMmSs(mVideoView.getCurrentPosition()));
                    }
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                // If video was playing , then it was paused for moving the seekbar
                // After scrolling in this case video should continue playing
                isPauseForScroll = isVideoPlaying;
                pause_playback();
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                set_gpx_track();
                mVideoView.seekTo(progressToTimer(mSeekBar.getProgress(),mVideoView.getDuration()));
                mVideoTime.setText(convertMillisecondsToHMmSs(mVideoView.getCurrentPosition()));
                if(isPauseForScroll){
                    start_playback();
                }
            }
        });


        play_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                set_gpx_track();
                start_playback();
            }
        });


        stop_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                pause_playback();
            }
        });

        mVideoView.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mediaPlayer) {
                stop_playback();
            }
        });


        if (open_gpx_file()) {
            draw_track();
            set_gpx_track();
            start_playback();
        }
    }


    @Override
    protected void onPause(){
        super.onPause();
        stop_gps_playback();
        mVideoView.stopPlayback();
        filename = null;
    }


    /**
     * Functions to set Seekbar and time elapsed
     * */

    private void startProgressBar() {
        mHandler.postDelayed(updateTimeTask, 100);
    }

    private void stopProgressBar(){
        mHandler.removeCallbacks(updateTimeTask);
    }

    // Timer task to update seekbar and video running time
    private Runnable updateTimeTask = new Runnable() {
        @Override
        public void run() {
            long totalDuration = mVideoView.getDuration();
            long currentDuration = mVideoView.getCurrentPosition();

            // update progress bar
            int progress = (getProgressPercentage(currentDuration,
                    totalDuration));
            mSeekBar.setProgress(progress);
            mVideoTime.setText(convertMillisecondsToHMmSs(currentDuration));
            mHandler.postDelayed(this, 100);
        }
    };




    /**
     * Function to open and start parsing gpx file
     * returns true on successful parsing of gpx
     * */
    private boolean open_gpx_file(){

        String gpx_filename = filename + ".gpx";

        Log.d("Fileo",Environment.getExternalStorageDirectory() +
                File.separator + "GPS_Video_Logger" + "/" + gpx_filename);

        try {
            fis = new FileInputStream(new File( Environment.getExternalStorageDirectory() +
                    File.separator + "GPS_Video_Logger", gpx_filename));
            gpx_parsed = parser.parse(fis);
        } catch (IOException | XmlPullParserException e) {
            e.printStackTrace();
            display_error_and_quit(GPX_FILE_NOT_FOUND);
            return false;
        }


        // We support playback of first segment of first track only
        if (gpx_parsed!=null){
            List<Track> tracks = gpx_parsed.getTracks();
            List<TrackSegment> trackSegments;
            if (tracks.size()>0) {
                trackSegments = tracks.get(0).getTrackSegments();
                if (trackSegments.size() > 0) {
                    gpxTrackPoints = trackSegments.get(0).getTrackPoints();
                    // Empty GPX file
                    if(gpxTrackPoints.size()==0) {
                        display_error_and_quit(EMPTY_GPX_FILE);
                        return false;
                    }
                    gpxTrackPointsIterator = gpxTrackPoints.listIterator();
                    start_time = gpxTrackPoints.get(0).getTime().getMillis();
                    end_time = gpxTrackPoints.get(gpxTrackPoints.size()-1).getTime().getMillis();
                    return true;
                }
            }
        }

        display_error_and_quit(INVALID_GPX_FILE);
        return false;
    }
    

    
    

    /**
     * Playback control functions
     * */
    

    private void start_playback(){
           if(!mVideoView.isPlaying() && !isLocationPlaying){
               start_video_playback();
               start_gps_playback();
               play_button.setVisibility(View.INVISIBLE);
               stop_button.setVisibility(View.VISIBLE);
           }
    }


    private void start_gps_playback(){
        isLocationPlaying = true;
        final Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if(isLocationPlaying){
                    update_map();
                    handler.postDelayed(this,current_delay);
                }
                else{
                    handler.removeCallbacks(this);
                }
            }
        }, current_delay);
    }

    private void start_video_playback(){
        //Start video playing
        mVideoView.seekTo(progressToTimer(mSeekBar.getProgress(),mVideoView.getDuration()));
        mVideoView.start();
        startProgressBar();
        isVideoPlaying = true;
    }

    
    

    private void stop_playback(){
        stop_gps_playback();
        //Video may be longer than gpx file
        stop_video_playback();
        set_gpx_track();
        stop_button.setVisibility(View.INVISIBLE);
        play_button.setVisibility(View.VISIBLE);
    }

    private void stop_gps_playback(){
        isLocationPlaying = false;
    }
    

    private void stop_video_playback(){
        if (mVideoView.isPlaying()) {
            mVideoView.pause();
        }
        mSeekBar.setProgress(0);
        stopProgressBar();
        mVideoView.seekTo(0);
        isVideoPlaying = false;
    }


    
    private void pause_playback(){
        stop_gps_playback();
        pause_video_playback();
        stop_button.setVisibility(View.INVISIBLE);
        play_button.setVisibility(View.VISIBLE);
    }
    
    private void pause_video_playback(){
        if (mVideoView.isPlaying()) {
            mVideoView.pause();
        }
        stopProgressBar();
        isVideoPlaying = false;
    }



    // Utitlity functions to update location on map


    /**
     * Function to plot the currentGpxPoint on the mapview
     * */
    private void update_map(){
        GeoPoint currentGeoPoint = getGeoPoint(currentGpxPoint);
        if (currentGeoPoint==null){
            stop_gps_playback();
            return;
        }
        mapController.setCenter(currentGeoPoint);

        if (prev_marker!=null){
            map.getOverlays().remove(prev_marker);
        }
        Marker marker = new Marker(map);
        marker.setPosition(currentGeoPoint);
        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
        map.getOverlays().add(marker);
        marker.setIcon(PlaybackActivity.this.getResources().getDrawable(R.drawable.center));
        prev_marker = marker;
        map.invalidate();
        get_next_location();
    }

    /** Utility funtion to get GeoPoint from a given TrackPoint
     * @param point -
     * returns the corresponding GeoPoint
     */
    private GeoPoint getGeoPoint(TrackPoint point){
        if (point==null) return null;

        Double Latitude =  point.getLatitude();
        Double Longitude = point.getLongitude();
        return new GeoPoint(Latitude,Longitude);
    }


    /** Funtion to find and update GpxTrackPoints iterator based on current state of Seekbar
     */
    private void set_gpx_track() {
        // Set start point based on current time estimated from seek bar

        int current_duration = progressToTimer(mSeekBar.getProgress(), mVideoView.getDuration());
        long current_time = start_time + (long) current_duration;

        for(int i =0 ; i<gpxTrackPoints.size(); ++i){
            long diff = gpxTrackPoints.get(i).getTime().getMillis() - current_time;
            if (diff >= 0){
                // Start from Beginning
                if (diff==0 && i==0){
                    gpxTrackPointsIterator = gpxTrackPoints.listIterator(0);
                    currentGpxPoint = gpxTrackPointsIterator.next();
                    prev_time = currentGpxPoint.getTime().getMillis();
                    update_map();
                    return;
                }

                // Start from between
                currentGpxPoint = gpxTrackPoints.get(i-1);
                gpxTrackPointsIterator = gpxTrackPoints.listIterator(i);
                update_map();
                // Overwrite current delay with remaining time
                current_delay = diff;
                return;
            }
        }

        // Video extends after last recorded point
        currentGpxPoint = gpxTrackPoints.get(gpxTrackPoints.size()-1);
        gpxTrackPointsIterator = gpxTrackPoints.listIterator(gpxTrackPoints.size());
        update_map();
    }
    

    
    /**
     * Function to connect the points in gpxTrackPoints and plot it 
     * */
    private void draw_track(){

        Polyline line = new Polyline(map);
        List<GeoPoint> pts = new ArrayList<>();

        for (int i =0; i< gpxTrackPoints.size(); ++i){
            Double lat = gpxTrackPoints.get(i).getLatitude();
            Double lon = gpxTrackPoints.get(i).getLongitude();
            pts.add(new GeoPoint(lat,lon));
        }

        line.setPoints(pts);
        map.getOverlayManager().add(line);
    }



    /**
     * Function to get next location from GPXTrackPoint Iterator
     * */
    private void get_next_location(){

        if (gpxTrackPointsIterator.hasNext()) {
            currentGpxPoint = gpxTrackPointsIterator.next();

            //Updating time to wait before fetching next location from gpx file
            long new_time = currentGpxPoint.getTime().getMillis();
            Log.d("time",Long.toString(new_time));
            Log.d("time",Long.toString(prev_time));
            current_delay =  new_time - prev_time; //Time value
            prev_time = new_time;
            Log.d("time",Long.toString(current_delay));
        }
        else
            currentGpxPoint = null;
    }





    /**
     * Function to display error alert message based on message code and finish activity
     * @param msg_code -
     * */
    private void display_error_and_quit(int msg_code){

        Log.d("Error func called","Here");
        String err_msg = "";
        if (msg_code == INVALID_GPX_FILE)
            err_msg = "Invalid GPX file!";
        else if (msg_code == GPX_FILE_NOT_FOUND)
            err_msg = "GPX file not Found! If you copied a new file into the app folder, ensure the video file and GPX file have the same name.";
        else if (msg_code == EMPTY_GPX_FILE)
            err_msg = "The GPX file does not have track data. This could happen if you recorded a very short video or GPS fix was lost after commencing recording. You can still access the video file in the app folder.";

        // No corresponding GPX file. Ensure same name, Show alert before quit
        // Setting Dialog Title
        AlertDialog.Builder alertBuilder = new AlertDialog.Builder(PlaybackActivity.this,R.style.DialogTheme);
        alertBuilder.setTitle("ERROR");

        // Setting Dialog Message
        alertBuilder.setMessage(err_msg);

        // Setting OK Button
        alertBuilder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                Log.d("Error func called","Here");
                finish();
            }
        });

        // Showing Alert Message
        alertBuilder.setCancelable(false); // cannot dismiss without Ok button

        AlertDialog dialog = alertBuilder.create();
        dialog.show();
    }


    



    // Utility functions to manage seekbar and time elapsed
    
    /**
     * Function to get progress percentage
     * @param currentDuration -
     * @param totalDuration
     * returns percentage
     * */
    public int getProgressPercentage(long currentDuration, long totalDuration){
        double percentage;
        percentage = Math.ceil((((double)currentDuration)/totalDuration)*100);
        return (int) percentage;
    }

    /**
     * Function to change progress to timer
     * @param progress -
     * @param totalDuration
     * returns current duration in milliseconds
     * */
    public int progressToTimer(int progress, int totalDuration) {
        // return current duration in milliseconds
        return  (int) ((((double)progress) / 100) * totalDuration);
    }

    /**
     * Function to covert milliseconds to time display format string
     * @param milliseconds -
     * returns time elapsed in display format
     * */
    public static String convertMillisecondsToHMmSs(long milliseconds) {
        long seconds = milliseconds/1000;
        long s = seconds % 60;
        long m = (seconds / 60) % 60;
        long h = (seconds / (60 * 60)) % 24;

        if (seconds<3600) {
            return String.format(Locale.getDefault(),"%02d:%02d",m,s);
        }
        else {
            return String.format(Locale.getDefault(),"%d:%02d:%02d", h,m,s);
        }
    }

}
