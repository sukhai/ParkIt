package com.csc413.group9.parkIt;

import android.content.Context;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.csc413.group9.parkIt.Database.DatabaseManager;
import com.csc413.group9.parkIt.SFPark.ParkingInformation;
import com.csc413.group9.parkIt.SFPark.ParkingLocation;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

import java.util.ArrayList;

public class MainActivity extends ActionBarActivity implements
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        GoogleMap.OnMapLoadedCallback,
        SensorEventListener {

    private static final float CAMERA_ZOOM_LEVEL = 18f;

    private GoogleMap mMap;
    private Marker mMarker;
    private GoogleApiClient mGoogleApiClient;
    private ParkingInformation mParkingInfo;
    private CurrentLocation mCurrentLocation;
    private SensorManager mSensorManager;
    private float mMarkerRotation;
    private boolean mapLoaded = false;
    private boolean showPrice = true;
    private boolean drawOnStreetParking = true;
    private boolean drawOffStreetParking = true;
    private boolean onStreetParkingIsDrawn = false;
    private boolean offStreetParkingIsDrawn = false;
    private ArrayList<Object> parkingIcons;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        if (!servicesAvailable()) {
            finish();
        }

        setContentView(R.layout.activity_main);

        DatabaseManager.initializeInstance(this);

        buildGoogleApiClient();
        buildSensorManager();

        mCurrentLocation = new CurrentLocation(this);
        mParkingInfo = new ParkingInformation(this, mCurrentLocation.getLastKnownLocation());
    }

    private synchronized void buildGoogleMap() {

        mMap = ((SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map))
                .getMap();

        mMap.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
            @Override
            public void onMapClick(LatLng latLng) {
                placeMarkerOnMap(latLng);

                mCurrentLocation.stopLocationUpdates();
            }
        });

        mMap.setOnMapLoadedCallback(this);
    }

    /**
     * Build the Google API client.
     */
    private synchronized void buildGoogleApiClient() {

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
    }

    /**
     * Build and initialize the SensorManager.
     */
    private synchronized void buildSensorManager() {

        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        mSensorManager.registerListener(this,
                mSensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION),
                SensorManager.SENSOR_DELAY_NORMAL);
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (mGoogleApiClient != null) {

            mGoogleApiClient.connect();
            mCurrentLocation.startLocationUpdates();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

        if (mGoogleApiClient != null && mGoogleApiClient.isConnected()) {

            mCurrentLocation.stopLocationUpdates();
            mGoogleApiClient.disconnect();
        }
    }

    @Override
    public void onConnected(Bundle dataBundle) {

        trackDeviceLocation(null);
    }

    @Override
    public void onConnectionSuspended(int i) {
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
 //       getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
 /*       int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }
*/
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {

        mMarkerRotation = event.values[0] - 170.0f;

        if (mMarker != null) {
            mMarker.setRotation(mMarkerRotation);
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    @Override
    public void onMapLoaded() {
        System.err.println("MAP LOADED");
        mapLoaded = true;
        updateMap();
    }

    /**
     * Track device's current location. The GPS will keep track of the location of the device
     * until somewhere on the map is clicked.
     * @param view The view of the application
     */
    public void trackDeviceLocation(View view) {

        // Start tracking
        mCurrentLocation.startLocationUpdates();

        // If the GPS is available, then get the current location of the device
        if (mCurrentLocation.canGetLocation()) {

            Location location = mCurrentLocation.getLocation();

            if (location != null) {
                placeMarkerOnMap(new LatLng(location.getLatitude(), location.getLongitude()));
            }

        } else {
            // Otherwise just get the last known location that is stored in the database

            String[] location = mCurrentLocation.getLastKnownLocation();

            double latitude = Double.parseDouble(location[1]);
            double longitude = Double.parseDouble(location[2]);

            placeMarkerOnMap(new LatLng(latitude, longitude));
        }
    }

    /**
     * Place a marker on the map on the specified latitude and longitude coordinate.
     * @param point The latitude and longitude coordinate on the map
     */
    public void placeMarkerOnMap(LatLng point) {

        // For testing purpose
        Toast.makeText(getApplicationContext(), point.latitude + ", " + point.longitude, Toast.LENGTH_SHORT).show();



        if (mMarker != null) {
            mMarker.remove();
        }

        // Do a null check to confirm that we have not already instantiated the map.
        if (mMap == null) {
            buildGoogleMap();
        }

        if (mMap != null) {
            mMarker = mMap.addMarker(new MarkerOptions()
                    .position(new LatLng(point.latitude, point.longitude))
                    .flat(true)
                    .anchor(0.5f, 0.5f)
                    .rotation(mMarkerRotation)
                    .visible(true));

            // Move the camera to the marker
            mMap.animateCamera(
                    CameraUpdateFactory.newLatLngZoom(mMarker.getPosition(), CAMERA_ZOOM_LEVEL));

            updateMap();
        }
    }

    public void updateMap() {
     //   if (mapLoaded) return;

        if (mapLoaded && (!onStreetParkingIsDrawn || !offStreetParkingIsDrawn)) {
            ArrayList<ParkingLocation> onStreet = mParkingInfo.getOnStreetParkingLocations();
            ArrayList<ParkingLocation> offStreet = mParkingInfo.getOffStreetParkingLocations();

            if (parkingIcons == null) {
                parkingIcons = new ArrayList<Object>();
            } else {
                parkingIcons.clear();
            }

            if (drawOnStreetParking && !onStreetParkingIsDrawn) {
                System.err.println("Draw on street parking");
                highlightOnStreetParking(onStreet);

                onStreetParkingIsDrawn = true;
            }

            if (drawOffStreetParking && !offStreetParkingIsDrawn) {
                System.out.println("draw off street");
                highlightOffStreetParking(offStreet);

                offStreetParkingIsDrawn = true;
            }
        }
    }

    private void highlightOnStreetParking(ArrayList<ParkingLocation> onStreet) {
        CircleOptions circleOptions = new CircleOptions()
                .radius(5f)
                .strokeColor(Color.GREEN)
                .fillColor(Color.GREEN);

        for (int i = 0; i < onStreet.size(); i++) {

            Location[] locations = onStreet.get(i).getLocation();

            // If there are 2 points, then draw a line from point1 to point2
            if (locations.length == 2) {
                // Draw a line
                PolylineOptions lineOptions = new PolylineOptions()
                        .add(new LatLng(locations[0].getLatitude(), locations[0].getLongitude()))
                        .add(new LatLng(locations[1].getLatitude(), locations[1].getLongitude()))
                        .color(Color.GREEN)
                        .width(5f);

                Polyline polyline = mMap.addPolyline(lineOptions);
                parkingIcons.add(polyline);
       //         System.out.println(locations[0] + " " + locations[1]);

            } else {
                // Draw a circle
                circleOptions.center(new LatLng(locations[0].getLatitude(), locations[0].getLongitude()));
                Circle circle = mMap.addCircle(circleOptions);
                parkingIcons.add(circle);
       //         System.out.println(locations[0]);
                //      onStreetMarkers[i] = circle;
            }
        }
    }

    private void highlightOffStreetParking(ArrayList<ParkingLocation> offStreet) {

        MarkerOptions markerOptions = new MarkerOptions()
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE));

        for (int i = 0; i < offStreet.size(); i++) {
            Location[] locations = offStreet.get(i).getLocation();
            markerOptions.position(new LatLng(locations[0].getLatitude(), locations[0].getLongitude()));

            parkingIcons.add(mMap.addMarker(markerOptions));


            System.out.println(locations[0].getLatitude() + ", " + locations[0].getLongitude());
        }
    }

    /**
     * Check if the Google Play Service is available. Return true if the Google Play Service is
     * available, otherwise false.
     * @return true if the Google Play Service is available, otherwise false
     */
    private boolean servicesAvailable() {

        int resultCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);

        if (ConnectionResult.SUCCESS == resultCode) {
            return true;
        }
        else {
            GooglePlayServicesUtil.getErrorDialog(resultCode, this, 0).show();
            return false;
        }
    }
}
