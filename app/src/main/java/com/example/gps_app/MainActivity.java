package com.example.gps_app;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Looper;
import android.provider.Settings;
import android.util.Xml;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;

import org.xmlpull.v1.XmlSerializer;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.time.Instant;

import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.views.MapView;


public class MainActivity extends AppCompatActivity {

    int PERMISSION_ID = 44;
    FusedLocationProviderClient mFusedLocationClient;

    Button start_button,stop_button;
    TextView mlocation;

    FileOutputStream fos;
    XmlSerializer serializer;


    MapView map = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        start_button = (Button) findViewById(R.id.start_button);
        stop_button = (Button) findViewById(R.id.stop_button);
        mlocation = (TextView) findViewById(R.id.textView);

        start_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (checkPermissions()){
                    if (isLocationEnabled()){
                        //New XML
                        //Dedide naming convention for filename
                        String filename = "Rec1.gpx";
                        try {
                            fos = openFileOutput(filename,Context.MODE_PRIVATE);
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
                    else{
                        Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                        startActivity(intent);
                    }
                }
                else{
                    requestPermissions();
                }
            }
        });

        stop_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mFusedLocationClient.removeLocationUpdates(mLocationCallback);
                //Close XML
                try {
                    serializer.endTag(null,"trkseg");
                    serializer.endTag(null,"trk");
                    serializer.endTag(null,"gpx");

                    serializer.endDocument();
                    serializer.flush();

                    fos.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }

            }
        });
    }

    private LocationCallback mLocationCallback = new LocationCallback() {
        @Override
        public void onLocationResult(LocationResult locationResult) {
            Location mLastLocation = locationResult.getLastLocation();
            String text_disp = Instant.now() + " Latitude"+Double.toString(mLastLocation.getLatitude()) + "Longitude" + Double.toString(mLastLocation.getLongitude());
            mlocation.setText(text_disp);
            //Write to XML

            try {
                serializer.startTag(null, "trkpt");
                serializer.attribute(null,"lat", Double.toString(mLastLocation.getLatitude()));
                serializer.attribute(null,"lon", Double.toString(mLastLocation.getLongitude()));

                serializer.startTag(null,"ele");
                serializer.text("0.000000");
                serializer.endTag(null,"ele");

                serializer.startTag(null,"time");
                String time = String.valueOf(Instant.now());
                serializer.text(time);
                serializer.endTag(null,"time");

                serializer.endTag(null, "trkpt");
            } catch (IOException e) {
                e.printStackTrace();
            }

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
//        mLocationRequest.setFastestInterval(100);
//        mLocationRequest.setNumUpdates(1);

        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        mFusedLocationClient.requestLocationUpdates(
                mLocationRequest, mLocationCallback,
                Looper.myLooper()
        );
    }


    private boolean checkPermissions(){
        if( ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(this,Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            return true;
        }
        return false;
    }

    private void requestPermissions(){
        ActivityCompat.requestPermissions(
                this,
                new String[]{Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION},
                PERMISSION_ID
        );
    }

//    @Override
//    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults){
//        super.onRequestPermissionsResult(requestCode,permissions,grantResults);
//        if (requestCode==PERMISSION_ID){
//            if (grantResults.length>0 && grantResults[0]==PackageManager.PERMISSION_GRANTED){
//                //Start getting location info
//            }
//        }
//    }


    private boolean isLocationEnabled(){
        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) && locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
    }


}
