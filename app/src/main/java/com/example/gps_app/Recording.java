package com.example.gps_app;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.sql.Date;
import java.text.SimpleDateFormat;
import java.util.TimeZone;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.hardware.Camera.CameraInfo;
import android.location.Location;
import android.location.LocationManager;
import android.media.CamcorderProfile;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Looper;
import android.provider.Settings;
import android.util.Log;
import android.util.Xml;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;

import org.xmlpull.v1.XmlSerializer;

public class Recording extends AppCompatActivity{

    private Camera mCamera;
    private CameraPreview mPreview;
    MediaRecorder mediaRecorder;
    Button captureButton;

    private boolean isRecording = false;

    private static final int PERMISSION_ID = 44;
    FusedLocationProviderClient mFusedLocationClient;

    FileOutputStream fos;
    XmlSerializer serializer;
    SimpleDateFormat sdf;

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

        //Initializing
        captureButton = (Button) findViewById(R.id.button_capture);
        // Create an instance of Camera
        mCamera = getCameraInstance();

        // Create our Preview view and set it as the content of our activity.
        mPreview = new CameraPreview(this, mCamera);
        ConstraintLayout preview = (ConstraintLayout) findViewById(R.id.camera_preview);
        preview.addView(mPreview);

        //For capturing gps
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        //Setting time format
        sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));

        captureButton.setOnClickListener(
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

                            // inform the user that recording has stopped
                            captureButton.setText(R.string.capture);
                            isRecording = false;
                        } else {
                            // initialize video camera
                            if (prepareVideoRecorder()) {

                                // Camera is available and unlocked, MediaRecorder is prepared,
                                // now you can start recording
                                mediaRecorder.start();

                                //New GPX
                                String filename = "Rec1.gpx";
                                create_gpx_file(filename);


                                // inform the user that recording has started
                                captureButton.setText(R.string.stop);
                                isRecording = true;
                            } else {
                                // prepare didn't work, release the camera
                                releaseMediaRecorder();
                                // inform user
                            }
                        }
                    }
                }
        );



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


    private boolean prepareVideoRecorder(){

        mCamera = getCameraInstance();

        //Currently always setting as vertical--- use google function?
        mCamera.setDisplayOrientation(90);
        mediaRecorder = new MediaRecorder();

        // Step 1: Unlock and set camera to MediaRecorder
        mCamera.unlock();
        mediaRecorder.setCamera(mCamera);
        //Currently always set as vertical
        mediaRecorder.setOrientationHint(90);

        // Step 2: Set sources
        mediaRecorder.setAudioSource(MediaRecorder.AudioSource.CAMCORDER);
        mediaRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);

        // Step 3: Set a CamcorderProfile (requires API Level 8 or higher)
        CamcorderProfile profile =  CamcorderProfile.get(CamcorderProfile.QUALITY_LOW);
        profile.fileFormat = MediaRecorder.OutputFormat.MPEG_4;
        mediaRecorder.setProfile(profile);


        // Step 4: Set output file
//        File op = new File();
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


    @Override
    protected void onPause() {
        super.onPause();
        releaseMediaRecorder();       // if you are using MediaRecorder, release it first
        finish_gpx_file();
        releaseCamera();              // release the camera immediately on pause event
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


    //For new GPX --- storing while recording
    private void  create_gpx_file(String filename){
        //Dedide naming convention for filename
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

        requestNewLocationData();
    }



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



    private LocationCallback mLocationCallback = new LocationCallback() {
        @Override
        public void onLocationResult(LocationResult locationResult) {
            Location mLastLocation = locationResult.getLastLocation();

            //Write to XML
            update_location_gpx(mLastLocation.getLatitude(),mLastLocation.getLongitude());
        }
    };


    @SuppressLint("MissingPermission")
    private void getLastLocation(){
        if (checkPermissions()){
            if (isLocationEnabled()){
                requestNewLocationData();
            }
            else{
                Toast.makeText(this, "Turn on location", Toast.LENGTH_LONG).show();
                Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                startActivity(intent);
            }
        }
        else{
            requestPermissions();
        }
    }

    @SuppressLint("MissingPermission")
    private void requestNewLocationData(){

        LocationRequest mLocationRequest = new LocationRequest();
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        mLocationRequest.setInterval(500);

        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        mFusedLocationClient.requestLocationUpdates(
                mLocationRequest, mLocationCallback,
                Looper.myLooper()
        );
    }


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
