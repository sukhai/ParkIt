package com.csc413.group9.parkIt;

import android.content.Context;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.support.v7.app.ActionBarActivity;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.Interpolator;
import android.widget.PopupWindow;
import android.widget.TimePicker;
import android.widget.Toast;

import com.csc413.group9.parkIt.Database.DatabaseManager;
import com.csc413.group9.parkIt.Features.WarningTimer.WarningTimer;
import com.csc413.group9.parkIt.SFPark.ParkingInformation;
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

import java.io.IOException;
import java.util.List;
import java.util.Locale;

public class MainActivity extends ActionBarActivity implements
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        GoogleMap.OnMapLoadedCallback,
        SensorEventListener {

    private static final double SF_LATITUDE = 37.7833;
    private static final double SF_LONGITUDE = -122.4167;
    private static final float CAMERA_ZOOM_LEVEL = 18f;
    private GoogleMap mMap;
    private Marker mClickedLocationMarker;
    private Marker mCLMarker;
    private Circle mCLMarkerCircle;
    private GoogleApiClient mGoogleApiClient;
    private ParkingInformation mParkingInfo;
    private CurrentLocation mCurrentLocation;
    private WarningTimer mWarningTimer;
    private PopupWindow mTimerWindow;
    private TimePicker mTimePicker;
    private SensorManager mSensorManager;
    private float mMarkerRotation;
    private boolean mapLoaded = false;
    private boolean showPrice = true;
    private boolean showOnStreetParking = true;
    private boolean showOffStreetParking = true;

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

        buildGoogleMap();

        mParkingInfo = new ParkingInformation(this, mMap);

        mWarningTimer = new WarningTimer(this);
        mWarningTimer.bindService();
    }

    /**
     * Build Google Map.
     */
    private synchronized void buildGoogleMap() {

        mMap = ((SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map))
                .getMap();

        mMap.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
            @Override
            public void onMapClick(LatLng latLng) {

                Location location = new Location("Clicked Location");
                location.setLatitude(latLng.latitude);
                location.setLongitude(latLng.longitude);

                placeMarkerOnMap(location, false);

                mCurrentLocation.stopLocationUpdates();
            }
        });

        mMap.setOnMapLoadedCallback(this);
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(SF_LATITUDE, SF_LONGITUDE), 10));
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
    protected void onStop() {

        if (mWarningTimer != null) {
            mWarningTimer.unBindService();
        }

        super.onStop();
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

        mMarkerRotation = event.values[0];

        if (mCLMarker != null) {
            mCLMarker.setRotation(mMarkerRotation);
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    @Override
    public void onMapLoaded() {

        mapLoaded = true;

        if (mParkingInfo.isSFParkDataReady())
            mParkingInfo.highlightStreet(showOnStreetParking, showOffStreetParking);
    }

    /**
     * Show the warning timer window.
     * @param view the view of the application
     */
    public void showWarningTimerWindow(View view) {

        LayoutInflater layoutInflater = (LayoutInflater) getBaseContext().getSystemService(LAYOUT_INFLATER_SERVICE);
        final View timerView = layoutInflater.inflate(R.layout.window_warning_timer, null);

        if (mTimerWindow == null) {

            mTimerWindow = new PopupWindow(
                    timerView,
                    LayoutParams.WRAP_CONTENT,
                    LayoutParams.WRAP_CONTENT);

            mTimePicker = (TimePicker) timerView.findViewById(R.id.timepicker_warning_timer);
            mTimePicker.setIs24HourView(true);
            mTimePicker.setOnTimeChangedListener(new TimePicker.OnTimeChangedListener() {
                @Override
                public void onTimeChanged(TimePicker view, int hourOfDay, int minuteOfDay) {
                    mWarningTimer.setTime(hourOfDay, minuteOfDay);
                }
            });
        }

        mTimerWindow.showAtLocation(timerView, Gravity.CENTER, 0, 0);

    //    mWarningTimer.showWindow();
/*
        TimePickerDialog timePickerDialog = new TimePickerDialog(this, new TimePickerDialog.OnTimeSetListener() {

            @Override
            public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
            }
        }, 0, 0, true);

        timePickerDialog.show();*/
    }

    public void setWarningTimer(View view) {

        mWarningTimer.setWarningTime();

        mTimerWindow.dismiss();
    }

    public void cancelWarningTimer(View view) {
        mTimerWindow.dismiss();
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
                placeMarkerOnMap(location, true);
            }

        } else {
            // Otherwise just get the last known location that is stored in the database

            String[] location = mCurrentLocation.getLastKnownLocation();

         //   double latitude = Double.parseDouble(location[1]);
         //   double longitude = Double.parseDouble(location[2]);
            Location loc = new Location(location[0]);
            loc.setLatitude(Double.parseDouble(location[1]));
            loc.setLongitude(Double.parseDouble(location[2]));

            placeMarkerOnMap(loc, true);
        }
    }

    /**
     * Place a marker on the map on the specified location on the map.
     * @param location The location of the marker to be placed on the map
     * @param isCurrentLocationMarker true if this marker is for current location, false otherwise
     */
    public void placeMarkerOnMap(Location location, boolean isCurrentLocationMarker) {

        // For testing purpose
        Toast.makeText(getApplicationContext(), location.getLatitude() + ", " + location.getLongitude(), Toast.LENGTH_SHORT).show();

        // Do a null check to confirm that we have not already instantiated the map.
        if (mMap == null)
            buildGoogleMap();

        if (mMap != null) {
            // The marker is for current location
            if (isCurrentLocationMarker) {
                if (mClickedLocationMarker != null)
                    mClickedLocationMarker.remove();

                if (mCLMarker != null && mCLMarkerCircle != null) {
                    // Show the marker
                    mCLMarker.setVisible(true);
                    mCLMarkerCircle.setVisible(true);

                    Location current = new Location(mCLMarker.getTitle());

                    // If the next location is less than 10 meters away, then animate to that new location
  //                  if (current.distanceTo(location) <= 10f) {
                        animateMarker(current, location);
  //                  } else {
  //                      // Otherwise just move the marker to that new location without animation
  //                      mCLMarker.setPosition(new LatLng(location.getLatitude(), location.getLongitude()));
                        mCLMarker.setRotation(mMarkerRotation);
  //                      mCLMarkerCircle.setCenter(new LatLng(location.getLatitude(), location.getLongitude()));
  //                  }

                } else {
                    // Create the marker
                    mCLMarker = mMap.addMarker(new MarkerOptions()
                            .position(new LatLng(location.getLatitude(), location.getLongitude()))
                            .rotation(mMarkerRotation)
                            .anchor(0.5f, 0.75f)
                            .flat(true)
                            .icon(BitmapDescriptorFactory.fromResource(R.drawable.icon_user)));

                    mCLMarkerCircle = mMap.addCircle(new CircleOptions()
                            .center(new LatLng(location.getLatitude(), location.getLongitude()))
                            .radius(45f)
                            .fillColor(Color.TRANSPARENT)
                            .strokeWidth(1.5f)
                            .strokeColor(0xFFE01368));
                }

            } else {
                // The marker is not for current location, so just place a regular marker on the
                // specified location

                if (mCLMarker != null) {
                    // Hide the marker
                    mCLMarker.setVisible(false);
                    mCLMarkerCircle.setVisible(false);
                }

                if (mClickedLocationMarker != null)
                    mClickedLocationMarker.remove();

                mClickedLocationMarker = mMap.addMarker(new MarkerOptions()
                        .position(new LatLng(location.getLatitude(), location.getLongitude()))
                        .title(getAddress(location)));

                // Show the address of the clicked location
                mClickedLocationMarker.showInfoWindow();
            }

            // Move the camera to the marker
            mMap.animateCamera(
                    CameraUpdateFactory.newLatLngZoom(
                            new LatLng(location.getLatitude(), location.getLongitude()),
                            CAMERA_ZOOM_LEVEL));
        }
    }

    private void animateMarker(Location from, Location to) {

        // Convert Location objects to LatLng objects
        double fromLatitude = from.getLatitude();
        double fromLongitude = from.getLongitude();
        double toLatitude = to.getLatitude();
        double toLongitude = to.getLongitude();

        // Setting up values for animation
        final LatLng startPosition = new LatLng(fromLatitude, fromLongitude);
        final LatLng finalPosition = new LatLng(toLatitude, toLongitude);

        // Start time
        final long start = SystemClock.uptimeMillis();

        // Animate using interpolator
        final Interpolator interpolator = new AccelerateDecelerateInterpolator();

        // Complete the animation over 1.5s
        final float durationInMs = 1500;

        final Handler handler = new Handler();

        handler.post(new Runnable() {

            long elapsed;
            float t;
            float v;

            @Override
            public void run() {
                // Calculate progress using interpolator
                elapsed = SystemClock.uptimeMillis() - start;
                t = elapsed / durationInMs;
                v = interpolator.getInterpolation(t);

                LatLng currentPosition = new LatLng(
                        startPosition.latitude*(1-t)+finalPosition.latitude*t,
                        startPosition.longitude*(1-t)+finalPosition.longitude*t);

                mCLMarker.setPosition(currentPosition);
                mCLMarkerCircle.setCenter(currentPosition);

                // Repeat until the animation is completed.
                if (t < 1) {
                    // Post again 16ms later.
                    handler.postDelayed(this, 16);
                }
            }
        });
    }

    private String getAddress(Location loc) {

        String addr = "";

        Geocoder geocoder;
        List<Address> addresses = null;
        geocoder = new Geocoder(this, Locale.getDefault());

        try {
            addresses = geocoder.getFromLocation(loc.getLatitude(), loc.getLongitude(), 1);
        } catch (IOException e) {
            // Unable to get the address, so we do nothing
        }

        if (addresses != null && addresses.size() > 0) {

            String address = addresses.get(0).getAddressLine(0) == null ? "" :
                             addresses.get(0).getAddressLine(0);

            String postalCode = addresses.get(0).getPostalCode() == null ? "" :
                                addresses.get(0).getPostalCode().split(" ")[0];

            addr = address + ", " + postalCode;
        }

        return addr;
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
