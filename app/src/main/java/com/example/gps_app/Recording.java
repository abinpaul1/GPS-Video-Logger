package com.example.gps_app;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.io.File;
import java.io.IOException;
import java.sql.Date;
import java.text.SimpleDateFormat;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.hardware.Camera.CameraInfo;
import android.media.CamcorderProfile;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.Toast;

public class Recording extends AppCompatActivity{

    private Camera mCamera;
    private CameraPreview mPreview;
    private MediaRecorder mediaRecorder;
    private Button captureButton;

    private boolean isRecording = false;

    private static final int MY_CAMERA_REQUEST_CODE = 100;
    private static final int MY_AUDIO_REQUEST_CODE = 200;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recording);


//        Checking permissions
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_DENIED){
            ActivityCompat.requestPermissions(this, new String[] {Manifest.permission.CAMERA}, MY_CAMERA_REQUEST_CODE);
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                == PackageManager.PERMISSION_DENIED){
            ActivityCompat.requestPermissions(this, new String[] {Manifest.permission.RECORD_AUDIO}, MY_AUDIO_REQUEST_CODE);
        }


        //Initializing

        captureButton = (Button) findViewById(R.id.button_capture);
        // Create an instance of Camera
        mCamera = getCameraInstance();

        // Create our Preview view and set it as the content of our activity.
        mPreview = new CameraPreview(this, mCamera);
        ConstraintLayout preview = (ConstraintLayout) findViewById(R.id.camera_preview);
        preview.addView(mPreview);


        captureButton.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (isRecording) {
                            // stop recording and release camera
                            mediaRecorder.stop();  // stop the recording
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
//        mediaRecorder.setOutputFile(getOutputMediaFile(MEDIA_TYPE_VIDEO).toString());


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


    public static final int MEDIA_TYPE_IMAGE = 1;
    public static final int MEDIA_TYPE_VIDEO = 2;

    /** Create a file Uri for saving an image or video */
    private static Uri getOutputMediaFileUri(int type){
        return Uri.fromFile(getOutputMediaFile(type));
    }

    /** Create a File for saving an image or video */
    private static File getOutputMediaFile(int type){
        // To be safe, you should check that the SDCard is mounted
        // using Environment.getExternalStorageState() before doing this.

        File mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES), "MyCameraApp");
        // This location works best if you want the created images to be shared
        // between applications and persist after your app has been uninstalled.

        // Create the storage directory if it does not exist
        if (! mediaStorageDir.exists()){
            if (! mediaStorageDir.mkdirs()){
                Log.d("MyCameraApp", "failed to create directory");
                return null;
            }
        }

        // Create a media file name
        File mediaFile;
        if (type == MEDIA_TYPE_IMAGE){
            mediaFile = new File(mediaStorageDir.getPath() + File.separator +
                    "IMG_"+".jpg");
        } else if(type == MEDIA_TYPE_VIDEO) {
            mediaFile = new File(mediaStorageDir.getPath() + File.separator +
                    "VID_"+".mp4");
        } else {
            return null;
        }

        return mediaFile;
    }

}

//public class Recording extends AppCompatActivity {
//
//
//    private static final int MY_CAMERA_REQUEST_CODE = 100;
//    private static final int MY_AUDIO_REQUEST_CODE = 200;
//
//    private Camera mCamera;
//    private CameraPreview mPreview;
//    private MediaRecorder mediaRecorder;
//    private Button capture, switchCamera;
//    private Context myContext;
//    private LinearLayout cameraPreview;
//
//    int numberOfCameras;
//    int cameraCurrentlyLocked;
//
//    // The first rear facing camera
//    int defaultCameraId;
//
//
//    @Override
//    public void onCreate(Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//        setContentView(R.layout.activity_recording);
//        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
//        myContext = this;
//
//
//        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
//                == PackageManager.PERMISSION_DENIED){
//            ActivityCompat.requestPermissions(this, new String[] {Manifest.permission.CAMERA}, MY_CAMERA_REQUEST_CODE);
//        }
//
//        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
//                == PackageManager.PERMISSION_DENIED){
//            ActivityCompat.requestPermissions(this, new String[] {Manifest.permission.RECORD_AUDIO}, MY_AUDIO_REQUEST_CODE);
//        }
//
//
//        initialize();
//    }
//
//
//
//
////    public void onResume() {
////        super.onResume();
////        Log.d("Cam",Integer.toString(defaultCameraId)+Integer.toString(numberOfCameras));
////        // Open the default i.e. the first rear facing camera.
////        mCamera = Camera.open(defaultCameraId);
////        cameraCurrentlyLocked = defaultCameraId;
////        mPreview.setCamera(mCamera);
////    }
//
//
//    public void initialize() {
//
//        // Find the total number of cameras available
//        numberOfCameras = Camera.getNumberOfCameras();
//
//        // Find the ID of the default camera
//        CameraInfo cameraInfo = new CameraInfo();
//        for (int i = 0; i < numberOfCameras; i++) {
//            Camera.getCameraInfo(i, cameraInfo);
//            if (cameraInfo.facing == CameraInfo.CAMERA_FACING_BACK) {
//                defaultCameraId = i;
//            }
//        }
//
//        cameraPreview = (LinearLayout) findViewById(R.id.camera_preview);
//
//        mPreview = new CameraPreview(myContext);
//        cameraPreview.addView(mPreview);
//
//        capture = (Button) findViewById(R.id.button_capture);
//        capture.setOnClickListener(captureListener);
//
//        switchCamera = (Button) findViewById(R.id.button_ChangeCamera);
//        switchCamera.setOnClickListener(switchCameraListener);
//
//        mCamera = Camera.open(defaultCameraId);
//        cameraCurrentlyLocked = defaultCameraId;
//        mPreview.setCamera(mCamera);
//        Log.d("Cam","B4 Previewed");
//        mCamera.startPreview();
//        Log.d("Cam","Previewed");
//    }
//
//
//    OnClickListener switchCameraListener = new OnClickListener() {
//        @Override
//        public void onClick(View v) {
//            // get the number of cameras
//            if (!recording) {
//                // check for availability of multiple cameras
//                if (numberOfCameras == 1) {
//                    AlertDialog.Builder builder = new AlertDialog.Builder(Recording.this);
//                    builder.setMessage("Alert")
//                            .setNeutralButton("Close", null);
//                    AlertDialog alert = builder.create();
//                    alert.show();
//                }
//
//                // OK, we have multiple cameras.
//                // Release this camera -> cameraCurrentlyLocked
//                if (mCamera != null) {
//                    mCamera.stopPreview();
//                    mPreview.setCamera(null);
//                    mCamera.release();
//                    mCamera = null;
//                }
//
//                // Acquire the next camera and request Preview to reconfigure
//                // parameters.
//                mCamera = Camera
//                        .open((cameraCurrentlyLocked + 1) % numberOfCameras);
//                cameraCurrentlyLocked = (cameraCurrentlyLocked + 1)
//                        % numberOfCameras;
//                mPreview.switchCamera(mCamera);
//
//                // Start the preview
//                mCamera.startPreview();
//            }
//        }
//    };
//
//
//
//    @Override
//    protected void onPause() {
//        super.onPause();
//
//        // Because the Camera object is a shared resource, it's very
//        // important to release it when the activity is paused.
//        if (mCamera != null) {
//            mPreview.setCamera(null);
//            mCamera.release();
//            mCamera = null;
//        }
//    }
//
//
//
//    boolean recording = false;
//    OnClickListener captureListener = new OnClickListener() {
//        @Override
//        public void onClick(View v) {
//            if (recording) {
//                // stop recording and release camera
//                mediaRecorder.stop(); // stop the recording
//                releaseMediaRecorder(); // release the MediaRecorder object
//                Toast.makeText(Recording.this, "Video captured!", Toast.LENGTH_LONG).show();
//                recording = false;
//            } else {
//                if (!prepareMediaRecorder()) {
//                    Toast.makeText(Recording.this, "Fail in prepareMediaRecorder()!\n - Ended -", Toast.LENGTH_LONG).show();
//                    finish();
//                }
//                // work on UiThread for better performance
//                runOnUiThread(new Runnable() {
//                    public void run() {
//                        // If there are stories, add them to the table
//
//                        try {
//                            mediaRecorder.start();
//                        } catch (final Exception ex) {
//                            // Log.i("---","Exception in thread");
//                        }
//                    }
//                });
//
//                recording = true;
//            }
//        }
//    };
//
//
//    private void releaseMediaRecorder() {
//        if (mediaRecorder != null) {
//            mediaRecorder.reset(); // clear recorder configuration
//            mediaRecorder.release(); // release the recorder object
//            mediaRecorder = null;
//            mCamera.lock(); // lock camera for later use
//        }
//    }
//
//
//    private boolean prepareMediaRecorder() {
//
//        mediaRecorder = new MediaRecorder();
//
//        mCamera.unlock();
//        mediaRecorder.setCamera(mCamera);
//
//        mediaRecorder.setAudioSource(MediaRecorder.AudioSource.CAMCORDER);
//        mediaRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);
//
//
//
//        CamcorderProfile profile =  CamcorderProfile.get(CamcorderProfile.QUALITY_LOW);
//        profile.fileFormat = MediaRecorder.OutputFormat.MPEG_4;
//        mediaRecorder.setProfile(profile);
//
//        File op = new File(getFilesDir().getAbsolutePath(),"Rec1.mp4");
//        mediaRecorder.setOutputFile(op);
//        mediaRecorder.setMaxDuration(6000000); // Set max duration 60 sec.
//        mediaRecorder.setMaxFileSize(500000000); // Set max file size 50M
//
//        try {
//            mediaRecorder.prepare();
//        } catch (IllegalStateException e) {
//            releaseMediaRecorder();
//            return false;
//        } catch (IOException e) {
//            releaseMediaRecorder();
//            return false;
//        }
//        return true;
//
//    }
//
//}
