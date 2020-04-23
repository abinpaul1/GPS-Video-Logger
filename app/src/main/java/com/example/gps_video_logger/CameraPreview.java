package com.example.gps_video_logger;

import android.content.Context;
import android.hardware.Camera;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.io.IOException;

/** A basic Camera preview class */
public class CameraPreview extends SurfaceView implements SurfaceHolder.Callback {
    private SurfaceHolder mHolder;
    private Camera mCamera;

    public CameraPreview(Context context, Camera camera) {
        super(context);
        mCamera = camera;

        //Currently always setting as vertical--- use google function?
        mCamera.setDisplayOrientation(90);
        // Install a SurfaceHolder.Callback so we get notified when the
        // underlying surface is created and destroyed.
        mHolder = getHolder();
        mHolder.addCallback(this);
        // deprecated setting, but required on Android versions prior to 3.0
    }

    public void surfaceCreated(SurfaceHolder holder) {
        // The Surface has been created, now tell the camera where to draw the preview.
        try {
            mCamera.setPreviewDisplay(holder);
            mCamera.startPreview();
        } catch (IOException e) {
            Log.d("Cam-prev", "Error setting camera preview: " + e.getMessage());
        }
    }

    public void surfaceDestroyed(SurfaceHolder holder) {
        // empty. Take care of releasing the Camera preview in your activity.
    }

    public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
        // If your preview can change or rotate, take care of those events here.
        // Make sure to stop the preview before resizing or reformatting it.

        if (mHolder.getSurface() == null){
            // preview surface does not exist
            return;
        }

        // stop preview before making changes
        try {
            mCamera.stopPreview();
        } catch (Exception e){
            // ignore: tried to stop a non-existent preview
        }

        // set preview size and make any resize, rotate or
        // reformatting changes here

        // start preview with new settings
        try {
            mCamera.setPreviewDisplay(mHolder);
            mCamera.startPreview();

        } catch (Exception e){
            Log.d("Cam-prev", "Error starting camera preview: " + e.getMessage());
        }
    }

//    Preview Callback to animate rotating progress bar
//    Additionally requires passing layout to constructor
//
//    Camera.PreviewCallback cb = new Camera.PreviewCallback() {
//        @Override
//        public void onPreviewFrame(byte[] bytes, Camera camera) {
//
//            if (currentProgress%50==0){
//                if (actualProgress == 100){
//                    actualProgress = 1;
//                    currentProgress = 0;
//                }
//                else{
//                    int ad = currentProgress%50;
//                    actualProgress+= ad;
//                }
//
//
//                mLayout.removeView(mProgressBar);
//                mProgressBar = new ProgressBar(getContext(), null, android.R.attr.progressBarStyleSmall);
//                mProgressBar.setProgress(actualProgress);
//                ConstraintSet constraintSet = new ConstraintSet();
//                ConstraintLayout.LayoutParams params = new ConstraintLayout.LayoutParams(35,35);
//                mLayout.addView(mProgressBar, params);
//            }
//
//            currentProgress+=1;
//        }
//
//    };

}