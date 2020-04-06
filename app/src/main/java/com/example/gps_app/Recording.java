package com.example.gps_app;


import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.ActivityCompat;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.TimeZone;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.hardware.Camera;
import android.location.Location;
import android.location.LocationManager;
import android.media.CamcorderProfile;
import android.media.MediaRecorder;
import android.os.Bundle;
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
    private static final int PERMISSION_ID = 44;
    private int VIDEO_QUALITY = CamcorderProfile.QUALITY_480P;
    private int VIDEO_FORMAT = MediaRecorder.OutputFormat.MPEG_4;
    

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



    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recording);


        //Checking permissions
        if (!checkPermissions()){
            requestPermissions();
        }

        //Check if location is enabled
        if(!isLocationEnabled()){
            Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
            startActivity(intent);
        }

        //Setting the time format
        sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));


        //Initializing button
        recordButton = (Button) findViewById(R.id.record_button);

        // Create an instance of Camera
        mCamera = getCameraInstance();

        //Setting orientation of camera
        int result = getCameraDisplayOrientation(this, cameraId, mCamera);
        mCamera.setDisplayOrientation(result);

        // Create our Preview view and set it as the content of our activity.
        mPreview = new CameraPreview(this, mCamera);
        ConstraintLayout preview = (ConstraintLayout) findViewById(R.id.camera_preview);
        preview.addView(mPreview);

        //Initialising location provider
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);


        //Toggling record button to start and stop recording
        recordButton.setOnClickListener(
                new View.OnClickListener() {
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
                            recordButton.setText(R.string.record);
                            isRecording = false;

                        } else {

                            // Initialize video camera
                            if (prepareVideoRecorder()) {

                                // Camera is available and unlocked, MediaRecorder is prepared,
                                // now you can start recording
                                mediaRecorder.start();

                                //Create new GPX file and start recording location data
                                String filename = "Rec1.gpx";
                                create_gpx_file(filename);
                                recordLocationData();


                                // Inform the user that recording has started
                                recordButton.setText(R.string.record_stop);
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
        );

        //Finding back-camera id
        cameraId = getBackCameraID();


        //Setting up location request objects
        mLocationRequest = new LocationRequest();
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        mLocationRequest.setInterval(500);
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

    }

    @Override
    protected void onPause() {
        super.onPause();
        releaseMediaRecorder();       // if you are using MediaRecorder, release it first
        finish_gpx_file();
        releaseCamera();              // release the camera immediately on pause event
    }
    
    
    //Detecting and adjusting preview camera based on orientation
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        int result = getCameraDisplayOrientation(this, cameraId, mCamera);

        Log.d("Cam",Integer.toString(result));
        mCamera.setDisplayOrientation(result);
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
        CamcorderProfile profile =  CamcorderProfile.get(VIDEO_QUALITY);
        profile.fileFormat = VIDEO_FORMAT;
        mediaRecorder.setProfile(profile);


        // Step 4: Set output file
        mediaRecorder.setOutputFile(getFilesDir().getAbsolutePath()+"/Rec1.mp4");

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
    
    

    // Create gpx file
    private void  create_gpx_file(String filename){
        //Decide naming convention for filename
        Log.d("oss","Trying crete file");
        try {
            fos = new FileOutputStream(new File(getFilesDir(), filename));
            Log.d("oss","Opened file"+getFilesDir());
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
    private void update_location_gpx(Double Latitude, Double Longitude){
        try {
            serializer.startTag(null, "trkpt");
            serializer.attribute(null,"lat", Double.toString(Latitude));
            serializer.attribute(null,"lon", Double.toString(Longitude));

            serializer.startTag(null,"ele");
            serializer.text("0.000000");
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

    
    private LocationCallback mLocationCallback = new LocationCallback() {
        @Override
        public void onLocationResult(LocationResult locationResult) {
            Location mLastLocation = locationResult.getLastLocation();
            //Write location to GPX file
            update_location_gpx(mLastLocation.getLatitude(),mLastLocation.getLongitude());
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



    
    // Utility functions for checking permissions

    private boolean isLocationEnabled(){
        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) && locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
    }


    private boolean checkPermissions(){

        if( ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this,Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this,Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this,Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            return true;
        }
        return false;
    }


    private void requestPermissions(){
        ActivityCompat.requestPermissions(
                this,
                new String[]{Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO },
                PERMISSION_ID
        );
    }

}
