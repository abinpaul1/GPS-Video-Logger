package com.example.gps_video_logger;


import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.hardware.Camera;
import android.location.GpsStatus;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.media.CamcorderProfile;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Environment;
import android.os.SystemClock;
import android.provider.Settings;
import android.util.Log;
import android.util.Xml;
import android.view.Surface;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.Toast;


import org.xmlpull.v1.XmlSerializer;

public class Recording extends AppCompatActivity{


    //Msg codes;
    private static final int INSTRUCTIONS = 1;
    private static final int FIX_PENDING = 2;
    private static final int FIX_INFO = 3;


    //Constants
    private int VIDEO_QUALITY = CamcorderProfile.QUALITY_480P;
    private int VIDEO_FORMAT = MediaRecorder.OutputFormat.MPEG_4;
    long FREQUENCY = 1000; //milli sec

    private String filename;

    Camera mCamera;
    int cameraId;
    CameraPreview mPreview;
    MediaRecorder mediaRecorder;

    FileOutputStream fos;
    XmlSerializer serializer;
    SimpleDateFormat sdf;

    private LocationManager mlocManager = null;
    private boolean isGPSLocationUpdatesActive = false;
    private long mLastLocationMillis;
    private boolean hasGPSFix = false;
    Location mLastLocation = null;


    Button recordButton;
    boolean isRecording = false;

    Button fileButton;
    Button aboutButton;

    // Imageview doesn't display tick because of issue with resolution-- bitmap issue most rpob
    Button tickView;
    Button mProgressBar;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
        setContentView(R.layout.activity_recording);

        //Check if location is enabled
        if(!isLocationEnabled()){
            Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
            startActivity(intent);
        }


        //Setting the time format
        sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SS'Z'");
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));


        //Initializing buttons and listeners
        recordButton = (Button) findViewById(R.id.record_button);
        fileButton = (Button) findViewById(R.id.files_button);
        aboutButton = (Button) findViewById(R.id.about_button);

        //GPS fix status progress bar
        tickView = (Button) findViewById(R.id.tickView);
        mProgressBar = (Button) findViewById(R.id.progressBar);

        //Toggling record button to start and stop recording
        recordButton.setOnClickListener(recordButtonOnClickListener);

        // Launch Filepicker activity on clicking
        fileButton.setOnClickListener(fileButtonOnClickListener);

        // Help/About Button
        aboutButton.setOnClickListener(aboutButtonOnClickListener);

        mProgressBar.setOnClickListener(GpsFixListener);
        tickView.setOnClickListener(GpsFixListener);

        initialize_app_folder();
        initialize_camera();
        initialize_location();
    }

    @Override
    protected void onPause() {
        super.onPause();
        releaseMediaRecorder();       // if you are using MediaRecorder, release it first
        setGPSLocationUpdates(false); //Stop location updates
        finish_gpx_file();
        stopPreview();
        recordButton.setBackgroundResource(R.drawable.rec);
        isRecording = false;
    }


    @Override
    protected void onResume() {
        super.onResume();
        //Restart location updates
        setGPSLocationUpdates(true);
        // Create an instance of Camera
        mCamera = getCameraInstance();

        //Setting orientation of camera
        int result = getCameraDisplayOrientation(this, cameraId, mCamera);
        mCamera.setDisplayOrientation(result);


        ConstraintLayout preview = (ConstraintLayout) findViewById(R.id.camera_preview);
        // Create our Preview view and set it as the content of our activity.
        mPreview = new CameraPreview(this, mCamera);
        preview.addView(mPreview);
    }

    @Override
    public void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        releaseCamera();              // release the camera only on destroy event
    }

    //Detecting and adjusting preview camera based on orientation
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        int result = getCameraDisplayOrientation(this, cameraId, mCamera);

        Log.d("Cam",Integer.toString(result));
        mCamera.setDisplayOrientation(result);
    }









    // OnClick listeners

    private View.OnClickListener recordButtonOnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (isRecording) {

                // stop recording and release camera
                mediaRecorder.stop();  // stop the recording

                //Close GPX File
                finish_gpx_file();

                releaseMediaRecorder(); // release the MediaRecorder object
                mCamera.lock();         // take camera access back from MediaRecorder

                // Inform the user that recording has stopped
                recordButton.setBackgroundResource(R.drawable.rec);
                isRecording = false;
                setGPSFixSpinner(); // Again display GPS fix status
            } else {

                filename = "REC-" + new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss").format(new Date());

                //Check if location is enabled
                if(!isLocationEnabled()){
                    Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                    startActivity(intent);

                }
                else{

                    if (hasGPSFix) {
                        // Initialize video camera
                        if (prepareVideoRecorder()) {

                            // Camera is available and unlocked, MediaRecorder is prepared,
                            // now you can start recording
                            mediaRecorder.start();

                            //Create new GPX file and start recording location data
                            create_gpx_file(filename + ".gpx");
                            isRecording = true;

                            // Inform the user that recording has started
                            recordButton.setBackgroundResource(R.drawable.stop);
//                            //Hide GPS fix status spinner while recording after integrating map matching
//                            mProgressBar.setVisibility(View.INVISIBLE);
//                            tickView.setVisibility(View.INVISIBLE);

                        } else {

                            // If prepare didn't work, release the camera
                            releaseMediaRecorder();
                            // Inform user
                            Toast.makeText(getApplicationContext(), "Some Error Occured", Toast.LENGTH_SHORT).show();
                        }
                    }
                    else{
                        display_alert(FIX_PENDING);
                    }
                }
            }
        }
    };

    private View.OnClickListener fileButtonOnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            if (isRecording){
                Toast.makeText(Recording.this,"Recording in progress",Toast.LENGTH_SHORT).show();
            }
            else{
                Intent intent = new Intent(Recording.this, FilePicker.class);
                startActivity(intent);
            }
        }
    };

    private View.OnClickListener aboutButtonOnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            // Display alert showing usage specifications
            if (!isRecording){
                display_alert(INSTRUCTIONS);
            }
        }
    };


    private View.OnClickListener GpsFixListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            display_alert(FIX_INFO);
        }
    };


    // Location listener
    //    https://stackoverflow.com/questions/2021176/how-can-i-check-the-current-status-of-the-gps-receiver
    private LocationListener mlocListener = new LocationListener() {
        @Override
        public void onLocationChanged(Location location) {
            if (location == null) return;
            mLastLocation = location;
            mLastLocationMillis = SystemClock.elapsedRealtime();
            Log.d("GPS","Loc changed");
            if (isRecording && hasGPSFix){
                update_location_gpx(location);
            }
        }

        @Override
        public void onStatusChanged(String s, int i, Bundle bundle) {

        }

        @Override
        public void onProviderEnabled(String s) {

        }

        @Override
        public void onProviderDisabled(String s) {

        }
    };


    // GpsStatus Listener
    //    https://stackoverflow.com/questions/2021176/how-can-i-check-the-current-status-of-the-gps-receiver
    private GpsStatus.Listener mGpsStatusListener = new GpsStatus.Listener() {
        @Override
        public void onGpsStatusChanged(int event) {
            switch (event) {
                case GpsStatus.GPS_EVENT_SATELLITE_STATUS:
                    if (mLastLocation != null)
                    {
                        if((SystemClock.elapsedRealtime() - mLastLocationMillis) < 5000)
                        {
                            if (!hasGPSFix)
                                Log.i("GPS","Fix Acquired");
                            setGPSFix(true);
                        }
                        else
                        {
                            if (hasGPSFix)
                            {
                                Log.i("GPS","Fix Lost (expired)");
                                mlocManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, FREQUENCY, 0, mlocListener);
                            }
                            setGPSFix(false);
                        }
                    }
                    break;
                case GpsStatus.GPS_EVENT_FIRST_FIX:
                    Log.i("GPS", "First Fix/ Refix");
                    setGPSFix(true);
                    break;
                case GpsStatus.GPS_EVENT_STARTED:
                    Log.i("GPS", "Started!");
                    break;
                case GpsStatus.GPS_EVENT_STOPPED:
                    Log.i("GPS", "Stopped");
                    break;
            }
        }
    };







    //Initialization functions

    private void initialize_camera(){
        // Create an instance of Camera
        mCamera = getCameraInstance();

        //Setting orientation of camera
        int result = getCameraDisplayOrientation(this, cameraId, mCamera);
        mCamera.setDisplayOrientation(result);

        // Create our Preview view and set it as the content of our activity.
        ConstraintLayout preview = (ConstraintLayout) findViewById(R.id.camera_preview);
        mPreview = new CameraPreview(this, mCamera);
        preview.addView(mPreview);

        //Finding back-camera id
        cameraId = getBackCameraID();

    }



    private void initialize_app_folder(){
        //Check for app folder
        File folder = new File(Environment.getExternalStorageDirectory() +
                File.separator + "GPS_Video_Logger");
        if (!folder.exists()) {
            //Handle error in making folder
            boolean success = folder.mkdir();
            Log.d("Folder-Creation",Boolean.toString(success));
        }
    }



    private void initialize_location(){
        //Initialising location manager
        mlocManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);


        //Setting up location requests
        setGPSLocationUpdates(true);
        setGPSFixSpinner();
    }


    public void setGPSLocationUpdates(boolean state) {
        if (!state && !isRecording && isGPSLocationUpdatesActive) {
            Log.d("GPS","Stopping");
            mlocManager.removeGpsStatusListener(mGpsStatusListener);
            mlocManager.removeUpdates(mlocListener);
            isGPSLocationUpdatesActive = false;
        }
        else if (state && !isGPSLocationUpdatesActive) {
            mlocManager.addGpsStatusListener(mGpsStatusListener);

//            Criteria myCriteria = new Criteria();
//            myCriteria.setAccuracy(Criteria.ACCURACY_HIGH);
//            mlocManager.requestLocationUpdates(FREQUENCY, 0, myCriteria, mlocListener, Looper.myLooper());
            mlocManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, FREQUENCY, 0, mlocListener);
            isGPSLocationUpdatesActive = true;
            Log.d("GPS","Started");
        }
    }


    // Update the spinner based on gps fix
    private void setGPSFixSpinner(){

        // Notifying users that fix is gone while recording???
//        if (!isRecording) {
            if (hasGPSFix) {
                mProgressBar.clearAnimation();
                mProgressBar.setVisibility(View.INVISIBLE);
                tickView.setVisibility(View.VISIBLE);
            } else {
                mProgressBar.startAnimation(
                        AnimationUtils.loadAnimation(this, R.anim.rotate) );
                mProgressBar.setVisibility(View.VISIBLE);
                tickView.setVisibility(View.INVISIBLE);
            }
//        }
    }

    private void setGPSFix(boolean state){
        hasGPSFix = state;
        setGPSFixSpinner();
    }








    // Configure MediaRecorder
    // https://developer.android.com/guide/topics/media/camera#configuring-mediarecorder
    private boolean prepareVideoRecorder(){

        mCamera = getCameraInstance();

        //Getting and setting orientation
        int result = getCameraDisplayOrientation(this, cameraId, mCamera);

        Log.d("Cam",Integer.toString(result));
        mCamera.setDisplayOrientation(result);


        mediaRecorder = new MediaRecorder();
        // Step 1: Unlock and set camera to MediaRecorder
        mCamera.unlock();
        mediaRecorder.setCamera(mCamera);
        mediaRecorder.setOrientationHint(result);

        // Step 2: Set sources
        mediaRecorder.setAudioSource(MediaRecorder.AudioSource.CAMCORDER);
        mediaRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);

        // Step 3: Set a CamcorderProfile
        CamcorderProfile profile;
        if(CamcorderProfile.hasProfile(VIDEO_QUALITY)){
            //Checking if the profile is available
            profile =  CamcorderProfile.get(VIDEO_QUALITY);
        }
        else{
            profile = CamcorderProfile.get(CamcorderProfile.QUALITY_LOW);
        }
        profile.fileFormat = VIDEO_FORMAT;
        mediaRecorder.setProfile(profile);


        // Step 4: Set output file
        mediaRecorder.setOutputFile(Environment.getExternalStorageDirectory() +
                File.separator + "GPS_Video_Logger" + File.separator + filename +".mp4");

        // Step 5: Set the preview output
        mediaRecorder.setPreviewDisplay(mPreview.getHolder().getSurface());

        // Step 6: Prepare configured MediaRecorder
        try {
            mediaRecorder.prepare();
        } catch (IllegalStateException e) {
            Log.d("Cam", "IllegalStateException preparing MediaRecorder: " + e.getMessage());
            releaseMediaRecorder();
            return false;
        } catch (IOException e) {
            Log.d("Cam", "IOException preparing MediaRecorder: " + e.getMessage());
            releaseMediaRecorder();
            return false;
        }
        return true;
    }






    // Camera Utility functions

    private int getBackCameraID(){
        int cameraId = -1;
        int numberOfCameras = Camera.getNumberOfCameras();
        for (int i = 0; i < numberOfCameras; i++) {
            Camera.CameraInfo info = new Camera.CameraInfo();
            Camera.getCameraInfo(i, info);
            if (info.facing == Camera.CameraInfo.CAMERA_FACING_BACK) {
                Log.d("Cam", "Camera found");
                cameraId = i;
                break;
            }
        }
        return cameraId;
    }

    public static Camera getCameraInstance(){
        Camera c = null;
        try {
            c = Camera.open(); // attempt to get a Camera instance
        }
        catch (Exception e){
            // Camera is not available (in use or does not exist)
            e.printStackTrace();
        }
        return c; // returns null if camera is unavailable
    }


    private void releaseMediaRecorder(){
        if (mediaRecorder != null) {
            mediaRecorder.reset();   // clear recorder configuration
            mediaRecorder.release(); // release the recorder object
            mediaRecorder = null;
            mCamera.lock();           // lock camera for later use
        }
    }

    private void stopPreview(){
        if (mCamera != null)
            mCamera.stopPreview();
    }

    private void releaseCamera(){
        if (mCamera != null){
            mCamera.stopPreview();
            mCamera.release();        // release the camera for other applications
            mCamera = null;
        }
    }


    // Utitliy function to get camera orientation based on device orientation
    // https://developer.android.com/reference/android/hardware/Camera#setDisplayOrientation
    public static int getCameraDisplayOrientation(Activity activity,
                                                   int cameraId, android.hardware.Camera camera) {
        android.hardware.Camera.CameraInfo info =
                new android.hardware.Camera.CameraInfo();
        android.hardware.Camera.getCameraInfo(cameraId, info);
        int rotation = activity.getWindowManager().getDefaultDisplay()
                .getRotation();
        int degrees = 0;
        switch (rotation) {
            case Surface.ROTATION_0: degrees = 0; break;
            case Surface.ROTATION_90: degrees = 90; break;
            case Surface.ROTATION_180: degrees = 180; break;
            case Surface.ROTATION_270: degrees = 270; break;
        }

        int result;
        if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            result = (info.orientation + degrees) % 360;
            result = (360 - result) % 360;  // compensate the mirror
        } else {  // back-facing
            result = (info.orientation - degrees + 360) % 360;
        }
        return result;
    }






    //GPX file handling functions

    // Create gpx file
    private void  create_gpx_file(String filename){
        Log.d("oss","Creating file");
        try {
            fos = new FileOutputStream(new File( Environment.getExternalStorageDirectory() +
                    File.separator + "GPS_Video_Logger", filename));
            Log.d("oss","Opened file");
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        serializer = Xml.newSerializer();

        try {
            serializer.setOutput(fos, "UTF-8");
            serializer.startDocument(null, Boolean.valueOf(true));

            serializer.setFeature("http://xmlpull.org/v1/doc/features.html#indent-output", true);

            serializer.startTag(null, "gpx");
            serializer.attribute(null, "version","1.0");
            serializer.attribute(null,"xmlns:xsi","http://www.w3.org/2001/XMLSchema-instance");

            serializer.startTag(null,"trk");

            serializer.startTag(null,"name");
            serializer.text("emulate");
            serializer.endTag(null,"name");

            serializer.startTag(null,"trkseg");
        } catch (IOException e) {
            e.printStackTrace();
        }

    }


    // Update provided location data into gpx file
    private void update_location_gpx(Location location){
        try {
            serializer.startTag(null, "trkpt");
            serializer.attribute(null,"lat", Double.toString(location.getLatitude()));
            serializer.attribute(null,"lon", Double.toString(location.getLongitude()));

            serializer.startTag(null,"ele");
            serializer.text(Double.toString(location.getAltitude()));
            serializer.endTag(null,"ele");
            serializer.startTag(null,"time");

            String time = sdf.format(System.currentTimeMillis());

            serializer.text(time);
            serializer.endTag(null,"time");

            serializer.endTag(null, "trkpt");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }    
    
    // Closing the gpx file
    private void finish_gpx_file(){
        try {

            if(serializer!=null){
                serializer.endTag(null,"trkseg");
                serializer.endTag(null,"trk");
                serializer.endTag(null,"gpx");

                serializer.endDocument();
                serializer.flush();

                fos.close();
                serializer = null;
                Log.d("oss","Closed file");
            }


        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    //Utility function to show alert messages
    private void display_alert(int msgCode){

        String msg = "";
        switch(msgCode){
            case INSTRUCTIONS:
                msg = "Video format is mp4" +"\n"+
                        "GPS track is saved in GPX file format" + "\n" +
                        "Both files would have the same name" + "\n" +
                        "The separate files can be found in the GPS_Video_Logger folder in your Internal Storage" + "\n" +
                        "Swipe to delete video file" + "\n" +
                        "Long press to rename videos";
                break;
            case FIX_PENDING:
                msg = "Please wait for the GPS to get a fix on your location." + "\n" +
                        "GPS Fix can be delayed if you are indoors or surrounded by tall building.";
                break;
            case FIX_INFO:
                msg = "GPS Fix Status" +"\n" +
                        "A GPS Fix means your device is in view of enough satellites to get a proper lock on your position" + "\n" +
                        "Even while recording, locations are saved only when a fix is present";
        }

        // No corresponding GPX file. Ensure same name, Show alert before quit
        // Setting Dialog Title
        AlertDialog.Builder alertBuilder = new AlertDialog.Builder(Recording.this,R.style.DialogTheme);
        alertBuilder.setTitle("GPS Video Logger v1.0.0");

        // Setting Dialog Message
        alertBuilder.setMessage(msg);


        AlertDialog dialog = alertBuilder.create();
        dialog.show();
    }


    // Utility function for checking if location is enabled

    private boolean isLocationEnabled(){
        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        // Logging if altitude data is supported by GPS on phone
        Log.d("Altitude available", Boolean.toString(locationManager.getProvider(LocationManager.GPS_PROVIDER).supportsAltitude()));

        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) && locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
    }

}
