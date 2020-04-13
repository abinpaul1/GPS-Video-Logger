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

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.hardware.Camera;
import android.location.Location;
import android.location.LocationManager;
import android.media.CamcorderProfile;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Environment;
import android.os.Looper;
import android.provider.Settings;
import android.util.Log;
import android.util.Xml;
import android.view.Surface;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;

import org.xmlpull.v1.XmlSerializer;

public class Recording extends AppCompatActivity{

    
    //Constants
    private int VIDEO_QUALITY = CamcorderProfile.QUALITY_480P;
    private int VIDEO_FORMAT = MediaRecorder.OutputFormat.MPEG_4;


    private String filename;

    Camera mCamera;
    int cameraId;
    CameraPreview mPreview;
    MediaRecorder mediaRecorder;

    FusedLocationProviderClient mFusedLocationClient;
    FileOutputStream fos;
    XmlSerializer serializer;
    SimpleDateFormat sdf;
    LocationRequest mLocationRequest;

    Button recordButton;
    boolean isRecording = false;

    Button fileButton;

    Button aboutButton;


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


        //Toggling record button to start and stop recording
        recordButton.setOnClickListener(recordButtonOnClickListener);

        // Launch Filepicker activity on clicking
        fileButton.setOnClickListener(fileButtonOnClickListener);

        // Help/About Button
        aboutButton.setOnClickListener(aboutButtonOnClickListener);

        initialize_app_folder();
        initialize_camera();
        initialize_location();
    }

    @Override
    protected void onPause() {
        super.onPause();
        releaseMediaRecorder();       // if you are using MediaRecorder, release it first
        mFusedLocationClient.removeLocationUpdates(mLocationCallback); //Stop location updates
        finish_gpx_file();
        stopPreview();
        recordButton.setBackgroundResource(R.drawable.rec);
        isRecording = false;
    }


    @Override
    protected void onResume() {
        super.onResume();
        // Create an instance of Camera
        mCamera = getCameraInstance();

        //Setting orientation of camera
        int result = getCameraDisplayOrientation(this, cameraId, mCamera);
        mCamera.setDisplayOrientation(result);

        // Create our Preview view and set it as the content of our activity.
        mPreview = new CameraPreview(this, mCamera);
        ConstraintLayout preview = (ConstraintLayout) findViewById(R.id.camera_preview);
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
                mFusedLocationClient.removeLocationUpdates(mLocationCallback);
                finish_gpx_file();

                releaseMediaRecorder(); // release the MediaRecorder object
                mCamera.lock();         // take camera access back from MediaRecorder

                // Inform the user that recording has stopped
                recordButton.setBackgroundResource(R.drawable.rec);
                isRecording = false;

            } else {

                filename = "REC-" + new SimpleDateFormat("yyyy-MM-dd-hh-mm-ss").format(new Date());

                //Check if location is enabled
                if(!isLocationEnabled()){
                    Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                    startActivity(intent);

                }
                else{
                    // Initialize video camera
                    if (prepareVideoRecorder()) {

                        // Camera is available and unlocked, MediaRecorder is prepared,
                        // now you can start recording
                        mediaRecorder.start();

                        //Create new GPX file and start recording location data
                        create_gpx_file(filename+".gpx");
                        recordLocationData();


                        // Inform the user that recording has started
                        recordButton.setBackgroundResource(R.drawable.stop);
                        isRecording = true;
                    } else {

                        // If prepare didn't work, release the camera
                        releaseMediaRecorder();
                        // Inform user
                        Toast.makeText(getApplicationContext(),"Some Error Occured",Toast.LENGTH_SHORT).show();
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
                // No corresponding GPX file. Ensure same name, Show alert before quit
                // Setting Dialog Title
                AlertDialog.Builder alertBuilder = new AlertDialog.Builder(Recording.this,R.style.DialogTheme);
                alertBuilder.setTitle("GPS Video Logger v1.0.0");

                // Setting Dialog Message
                alertBuilder.setMessage("Video format is mp4" +"\n"+
                        "GPS track is saved in GPX file format" + "\n" +
                        "Both files would have the same name" + "\n" +
                        "The separate files can be found in the GPS_Video_Logger folder in your Internal Storage" + "\n" +
                        "Swipe to delete video file" + "\n" +
                        "Long press to rename videos");
                // Setting OK Button
                alertBuilder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                    }
                });

                AlertDialog dialog = alertBuilder.create();
                dialog.show();
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
        mPreview = new CameraPreview(this, mCamera);
        ConstraintLayout preview = (ConstraintLayout) findViewById(R.id.camera_preview);
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
        //Initialising location provider
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);


        //Setting up location request objects
        mLocationRequest = new LocationRequest();
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        mLocationRequest.setInterval(1000);
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
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
    private void update_location_gpx(Double Latitude, Double Longitude, Double Altitude){
        try {
            serializer.startTag(null, "trkpt");
            serializer.attribute(null,"lat", Double.toString(Latitude));
            serializer.attribute(null,"lon", Double.toString(Longitude));

            serializer.startTag(null,"ele");
            serializer.text(Double.toString(Altitude));
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





    //Location Related functions
    private LocationCallback mLocationCallback = new LocationCallback() {
        @Override
        public void onLocationResult(LocationResult locationResult) {
            Location mLastLocation = locationResult.getLastLocation();
            //Write location to GPX file
            Log.d("Accuracy",Float.toString(mLastLocation.getAccuracy()));
            update_location_gpx(mLastLocation.getLatitude(),mLastLocation.getLongitude(),mLastLocation.getAltitude());
        }
    };


    // Function for getting and recording new location into gpx file
    @SuppressLint("MissingPermission")
    private void recordLocationData(){
        mFusedLocationClient.requestLocationUpdates(
                mLocationRequest, mLocationCallback,
                Looper.myLooper()
        );
    }





    // Utility functions for checking if location is enabled

    private boolean isLocationEnabled(){
        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        // Logging if altitude data is supported by GPS on phone
        Log.d("Altitude available", Boolean.toString(locationManager.getProvider(LocationManager.GPS_PROVIDER).supportsAltitude()));

        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) && locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
    }

}
