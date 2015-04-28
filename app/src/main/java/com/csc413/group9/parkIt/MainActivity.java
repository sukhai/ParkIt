package com.csc413.group9.parkIt;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Point;
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
import android.view.animation.Interpolator;
import android.view.animation.LinearInterpolator;
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
import com.google.android.gms.maps.Projection;
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

                if (mapLoaded)
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

        trackDeviceLocation(null);
    }

    /**
     * Return true if the map is loaded, otherwise false.
     * @return true if the map is loaded, otherwise false
     */
    public boolean isMapLoaded() {
        return mapLoaded;
    }

    /**
     * Show the warning timer window that allow the user to select how long they want to park
     * at that specific location.
     * @param view the view of the application
     */
    public void showWarningTimerWindow(View view) {

        // Get the warning timer view layout
        LayoutInflater layoutInflater = (LayoutInflater) getBaseContext().getSystemService(LAYOUT_INFLATER_SERVICE);
        final View timerView = layoutInflater.inflate(R.layout.window_warning_timer, null);

        if (mTimerWindow == null) {

            // Instantiate a popup window
            mTimerWindow = new PopupWindow(
                    timerView,
                    LayoutParams.WRAP_CONTENT,
                    LayoutParams.WRAP_CONTENT);

            // Instantiate a TimePicker and set its starting hour and minute to 0
            mTimePicker = (TimePicker) timerView.findViewById(R.id.timepicker_warning_timer);
            mTimePicker.setIs24HourView(true);
            mTimePicker.setCurrentHour(0);
            mTimePicker.setCurrentMinute(0);
            mTimePicker.setOnTimeChangedListener(new TimePicker.OnTimeChangedListener() {
                @Override
                public void onTimeChanged(TimePicker view, int hourOfDay, int minuteOfDay) {
                    mWarningTimer.setTime(hourOfDay, minuteOfDay);
                }
            });
        }

        // Show the popup window in the center of the screen
        mTimerWindow.showAtLocation(timerView, Gravity.CENTER, 0, 0);
    }

    /**
     * Store the current location and set the warning timer based on the time the user picks. After
     * that close the warning timer window.
     * @param view the view of the application
     */
    public void setWarningTimer(View view) {

        mWarningTimer.setLocation(mCLMarker.getPosition());
        mWarningTimer.setWarningTime();

        mTimerWindow.dismiss();
    }

    /**
     * Cancel and close the warning timer window. This method is called by a cancel button in
     * the warning timer window.
     * @param view the view of the application
     */
    public void cancelWarningTimer(View view) {
        mTimerWindow.dismiss();
    }

    /**
     * Track device's current location. The GPS will keep track of the location of the device
     * until somewhere on the map is clicked.
     * @param view The view of the application
     */
    public void trackDeviceLocation(View view) {

        // Start location tracking
        mCurrentLocation.startLocationUpdates();

        // If the GPS is available, then get the current location of the device
        if (mCurrentLocation.canGetLocation()) {

            Location location = mCurrentLocation.getLocation();

            if (location != null && mapLoaded) {
                placeMarkerOnMap(location, true);
            }

        } else {
            // Otherwise just get the last known location that is stored in the database

            if (mapLoaded) {
                String[] location = mCurrentLocation.getLastKnownLocation();

                Location loc = new Location(location[0]);
                loc.setLatitude(Double.parseDouble(location[1]));
                loc.setLongitude(Double.parseDouble(location[2]));

                placeMarkerOnMap(loc, true);
            }
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

                    animateMarker(new LatLng(location.getLatitude(), location.getLongitude()), false);

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

    public void animateMarker(final LatLng toPosition, final boolean hideMarker) {

        final Handler handler = new Handler();
        final long start = SystemClock.uptimeMillis();
        Projection projection = mMap.getProjection();
        Point startPoint = projection.toScreenLocation(mCLMarker.getPosition());
        final LatLng startLatLng = projection.fromScreenLocation(startPoint);
        final long duration = 500;

        final Interpolator interpolator = new LinearInterpolator();

        handler.post(new Runnable() {
            @Override
            public void run() {
                long elapsed = SystemClock.uptimeMillis() - start;
                float t = interpolator.getInterpolation((float) elapsed
                        / duration);
                double lng = t * toPosition.longitude + (1 - t)
                        * startLatLng.longitude;
                double lat = t * toPosition.latitude + (1 - t)
                        * startLatLng.latitude;
                mCLMarker.setPosition(new LatLng(lat, lng));
                mCLMarkerCircle.setCenter(new LatLng(lat, lng));

                if (t < 1.0) {
                    // Post again 16ms later.
                    handler.postDelayed(this, 16);
                } else {
                    if (hideMarker) {
                        mCLMarker.setVisible(false);
                        mCLMarkerCircle.setVisible(false);
                    } else {
                        mCLMarker.setVisible(true);
                        mCLMarkerCircle.setVisible(true);
                    }
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
