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
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.util.Log;
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

import org.osmdroid.api.IMapController;
import org.osmdroid.config.Configuration;
import org.osmdroid.config.IConfigurationProvider;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapController;
import org.osmdroid.views.overlay.Marker;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xmlpull.v1.XmlSerializer;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Map;
import java.util.TimeZone;

import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.views.MapView;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;


public class MainActivity extends AppCompatActivity {

    int PERMISSION_ID = 44;
    FusedLocationProviderClient mFusedLocationClient;

    Button start_button,stop_button;
    TextView mlocation;

    FileOutputStream fos;
    XmlSerializer serializer;

//    FileInputStream fis;
//    InputStreamReader isr;

    MapView map = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

//        load/initialize the osmdroid configuration, this can be done
//        setting this before the layout is inflated is a good idea
//        it 'should' ensure that the map has a writable location for the map cache, even without permissions
//        if no tiles are displayed, you can try overriding the cache path using Configuration.getInstance().setCachePath
//        see also StorageUtils
//        note, the load method also sets the HTTP User Agent to your application's package name, abusing osm's tile servers will get you banned based on this string
        Context ctx = getApplicationContext();
        Configuration.getInstance().load(ctx, PreferenceManager.getDefaultSharedPreferences(ctx));

//        File basePath = new File(getFilesDir().getAbsolutePath()+"/osmdroid");
//        Configuration.getInstance().setOsmdroidBasePath(basePath);
//        File tileCache = new File(getFilesDir().getAbsolutePath()+"/osmdroid/tiles");
//        Configuration.getInstance().setOsmdroidTileCache(tileCache);
//        Configuration.getInstance().setCacheSizes(...)
//        Configuration.getInstance().setOfflineMapsPath(this.getFilesDir().getAbsolutePath());
//        Configuration.getInstance().setUserAgentValue(...)



//        IConfigurationProvider osmConf = Configuration
        org.osmdroid.config.IConfigurationProvider osmConf = org.osmdroid.config.Configuration.getInstance();
        File basePath = new File(getFilesDir().getAbsolutePath()+"/osmdroid");
        osmConf.setOsmdroidBasePath(basePath);
        File tileCache = new File(getFilesDir().getAbsolutePath()+"/osmdroid/tiles");
        osmConf.setOsmdroidTileCache(tileCache);
//        Log.d("osm",Configuration.getIns)

        setContentView(R.layout.activity_main);

        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        start_button = (Button) findViewById(R.id.start_button);
        stop_button = (Button) findViewById(R.id.stop_button);
        mlocation = (TextView) findViewById(R.id.textView);

        map = (MapView) findViewById(R.id.map);
        map.setTileSource(TileSourceFactory.MAPNIK);


        IMapController mapController = map.getController();
        mapController.setZoom(18.0);


        //Parsing XML for display
//        String filename = "Rec1.gpx";
//        fis = this.getApplicationContext().openFileInput(filename);
//        isr = new InputStreamReader(fis);
//
//        char[] inputBuffer = new char[fis.available()];
//        isr.read(inputBuffer);
//
//        String data = new String(inputBuffer);
//
//        isr.close();
//        fis.close();
//
//        InputStream is = new ByteArrayInputStream(data.getBytes("UTF-8"));
//        ArrayList<XmlData> xmlDataList = new ArrayList<XmlData>();
//
//        XmlData xmlDataObj;
//        DocumentBuilderFactory dbf;
//        DocumentBuilder db;
//        NodeList items = null;
//        Document dom;
//
//        dbf = DocumentBuilderFactory.newInstance();
//        db = dbf.newDocumentBuilder();
//        dom = db.parse(is);
//
//        // Normalize the document
//        dom.getDocumentElement().normalize();
//
//        items = dom.getElementsByTagName("record");
//        ArrayList<String> arr = new ArrayList<String>();
//
//        for (int i = 0; i < items.getLength(); i++)
//        {
//            Node item = items.item(i);
//            arr.add(item.getNodeValue());
//        }




        GeoPoint startPoint = new GeoPoint(10.0251, 76.3459);
        mapController.setCenter(startPoint);
//        map.getController().animateTo(startPoint);

        Marker marker = new Marker(map);
        marker.setPosition(startPoint);
        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
        map.getOverlays().add(marker);
        marker.setIcon(this.getDrawable(R.drawable.center));
        map.invalidate();


        start_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d("oss",getFilesDir().getAbsolutePath());
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

            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
            sdf.setTimeZone(TimeZone.getTimeZone("UTC"));

            String text_disp = sdf.format(System.currentTimeMillis()) + " Latitude"+Double.toString(mLastLocation.getLatitude()) + "Longitude" + Double.toString(mLastLocation.getLongitude());
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

                sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
                String time = sdf.format(System.currentTimeMillis());

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
