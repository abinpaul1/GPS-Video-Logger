package com.example.gps_video_logger;

import androidx.appcompat.app.AppCompatActivity;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

import org.osmdroid.api.IMapController;
import org.osmdroid.config.Configuration;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.Polyline;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

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

    FileInputStream fis;
    XmlPullParserFactory factory;
    
    GPXParser parser;
    Gpx gpx_parsed;
    ListIterator<TrackPoint> gpxTrackPointsIterator;
    List<TrackPoint> gpxTrackPoints;

    MapView map = null;
    IMapController mapController;
    Marker prev_marker = null;

    String filename;

    Boolean play;


    Button play_button,pause_button;
    VideoView mVideoView;
    TextView filenameTextView;


    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);

        //Initializing GPX parser
        parser = new GPXParser();

        //Get filename of file to play
        Intent intent = getIntent();
        filename = intent.getStringExtra("filename");

//        Load/initialize the osmdroid configuration, this can be done
//        setting this before the layout is inflated is a good idea
//        it 'should' ensure that the map has a writable location for the map cache, even without permissions
//        if no tiles are displayed, you can try overriding the cache path using Configuration.getInstance().setCachePath
//        see also StorageUtils
//        note, the load method also sets the HTTP User Agent to your application's package name, abusing osm's tile servers will get you banned based on this string
        Context ctx = getApplicationContext();
        Configuration.getInstance().load(ctx, PreferenceManager.getDefaultSharedPreferences(ctx));

        org.osmdroid.config.IConfigurationProvider osmConf = org.osmdroid.config.Configuration.getInstance();
        File basePath = new File(getFilesDir().getAbsolutePath()+"/osmdroid");
        osmConf.setOsmdroidBasePath(basePath);
        File tileCache = new File(getFilesDir().getAbsolutePath()+"/osmdroid/tiles");
        osmConf.setOsmdroidTileCache(tileCache);


        setContentView(R.layout.activity_main);


        // Buttons and video view
        pause_button = (Button) findViewById(R.id.pause_button);
        play_button = (Button) findViewById(R.id.play_button);
        mVideoView = (VideoView) findViewById(R.id.videoView);
        filenameTextView = (TextView) findViewById(R.id.textView);


        //Configuring map display
        map = (MapView) findViewById(R.id.map);
        map.setTileSource(TileSourceFactory.MAPNIK);

        mapController = map.getController();
        mapController.setZoom(18.0);


        //Location of Video File
        Uri uri = Uri.parse(Environment.getExternalStorageDirectory() +
                File.separator + "GPS_Video_Logger" + File.separator + filename +".mp4");
        mVideoView.setVideoURI(uri);
        mVideoView.requestFocus();

        //Setting filename into textview
        filenameTextView.setText(filename);


        play_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                start_playback();
            }
        });


        pause_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                stop_gps_playback();
                //Video may be longer than gpx file
                stop_video_playback();
            }
        });

        mVideoView.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mediaPlayer) {
                stop_video_playback();
            }
        });

        start_playback();
    }

    @Override
    protected void onPause(){
        super.onPause();
        stop_gps_playback();
        mVideoView.stopPlayback();
        filename = null;
    }


    // Open and start reading from GPX file
    private boolean open_gpx_read(){

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
                    gpxTrackPointsIterator = gpxTrackPoints.listIterator();
                    return true;
                }
            }
        }
        
        display_error_and_quit(INVALID_GPX_FILE);
        return false;
    }


    //Update map with current co-ordinates
    private void update_map(){
        GeoPoint nextGeoPoint = get_next_location();
        if (nextGeoPoint==null){
                stop_gps_playback();
                return;
        }
        mapController.setCenter(nextGeoPoint);

        if (prev_marker!=null){
            map.getOverlays().remove(prev_marker);
        }
        Marker marker = new Marker(map);
        marker.setPosition(nextGeoPoint);
        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
        map.getOverlays().add(marker);
        marker.setIcon(PlaybackActivity.this.getResources().getDrawable(R.drawable.center));
        prev_marker = marker;
        map.invalidate();
    }


    // Function to begin playing of video and gps data
    private void start_playback(){

           if(!mVideoView.isPlaying() && gpx_parsed==null){

               if(open_gpx_read()){
                   play = true;

                   draw_track();
                   start_video_playback();
                   //Update to initial position
                   update_map();
                   update_map();

                   final Handler handler = new Handler();
                   handler.postDelayed(new Runnable() {
                       @Override
                       public void run() {
                           if(play){
                               update_map();
                               handler.postDelayed(this,current_delay);
                           }
                           else{
                               handler.removeCallbacks(this);
                           }
                       }
                   }, current_delay);
               }
           }
           else{
                 Toast.makeText(getApplicationContext(),"Playback already started",Toast.LENGTH_SHORT).show();
           }
    }


    private void stop_gps_playback(){
        play = false;
        gpx_parsed = null;
        factory = null;
    }


    private void start_video_playback(){
        //Start video playing
        mVideoView.seekTo(0);
        mVideoView.start();
    }


    private void stop_video_playback(){
        if (mVideoView.isPlaying()){
            mVideoView.pause();
        }
    }



    private void draw_track(){

        Polyline line = new Polyline(map);
        line.setWidth(20f);
        List<GeoPoint> pts = new ArrayList<>();

        for (int i =0; i< gpxTrackPoints.size(); ++i){
            Double lat = gpxTrackPoints.get(i).getLatitude();
            Double lon = gpxTrackPoints.get(i).getLongitude();
            pts.add(new GeoPoint(lat,lon));
        }

        line.setPoints(pts);
        map.getOverlayManager().add(line);
    }



    //Get next location from currently open gpx file
    private GeoPoint get_next_location(){

        TrackPoint current_track;
        if (gpxTrackPointsIterator.hasNext())
            current_track = gpxTrackPointsIterator.next();
        else
            return null;


        Double Latitude =  current_track.getLatitude();
        Double Longitude = current_track.getLongitude();
        GeoPoint nextGeoPoint = new GeoPoint(Latitude,Longitude);

        //Updating time to wait before fetching next location from gpx file
        long new_time = current_track.getTime().getMillis();
        Log.d("time",Long.toString(new_time));
        Log.d("time",Long.toString(prev_time));
        current_delay =  new_time - prev_time; //Time value
        prev_time = new_time;
        Log.d("time",Long.toString(current_delay));

        return nextGeoPoint;
    }


    // Display error with alert box and finish activity
    private void display_error_and_quit(int msg){

        Log.d("Error func called","Here");
        String err_msg = "";
        if (msg == INVALID_GPX_FILE)
            err_msg = "Invalid GPX file!";
        else if (msg == GPX_FILE_NOT_FOUND)
            err_msg = "GPX file not Found! If you copied a new file into the app folder, ensure the video file and GPX file have the same name.";
        else if (msg == EMPTY_GPX_FILE)
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

}
