package com.example.gps_video_logger;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.widget.TextView;

public class LauncherActivity extends AppCompatActivity {

    private int TIME_OUT = 500;
    private static final int PERMISSION_ID = 44;
    private String[] permissionString = new String[]{
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE };

    TextView permission_denied_text;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
//        overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
        setContentView(R.layout.activity_launcher);

        permission_denied_text = (TextView) findViewById(R.id.permission_text);

        if (!checkPermissions()){
            Log.d("Permission","Insufficient");
            requestPermissions();
        }
        else{

            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    Intent intent = new Intent(LauncherActivity.this, Recording.class);
                    startActivity(intent);
                    finish();
                }
            }, 200);
        }
    }


    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String[] permissions, int[] grantResults) {

        boolean all_permission_granted = true;
        if (grantResults.length == permissionString.length){
            for (int i=0;i < grantResults.length; ++i){
                if (grantResults[i] == PackageManager.PERMISSION_DENIED){
                    all_permission_granted = false;
                    break;
                }
            }

            if (all_permission_granted){
                //Start recording activity
                Intent intent = new Intent(LauncherActivity.this, Recording.class);
                startActivity(intent);
                finish();
            }
        }

        if (!all_permission_granted)
            permission_denied_text.setText("Please provide the necessary permissions to start using the app");
        return;
    }


    // Utility functions for checking permissions

    private boolean checkPermissions(){

        if( ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this,Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this,Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this,Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this,Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this,Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED ){
            return true;
        }
        return false;
    }


    private void requestPermissions(){
        ActivityCompat.requestPermissions(
                this,
                permissionString,
                PERMISSION_ID
        );
    }
}
