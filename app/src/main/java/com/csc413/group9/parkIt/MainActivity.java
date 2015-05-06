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
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import com.csc413.group9.parkIt.Database.DatabaseManager;
import com.csc413.group9.parkIt.Features.Search.Search;
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

    public static boolean startedFromNotification = false;

    private static final double SF_LATITUDE = 37.7833;
    private static final double SF_LONGITUDE = -122.4167;
    private static final float CAMERA_ZOOM_LEVEL = 18f;

    private GoogleMap mMap;
    private Marker mClickedLocationMarker;
    private Marker mParkingGarageMarker;
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
    private boolean mInfoWindowIsShown = false;
    private boolean mapLoaded = false;
    private boolean showOnStreetParking = true;
    private boolean showOffStreetParking = true;

    private Search mSearch;

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
        buildGoogleMap();

        mCurrentLocation = new CurrentLocation(this);
        mParkingInfo = new ParkingInformation(this, mMap);
        mSearch = new Search(this);
        mWarningTimer = new WarningTimer(this);
        mWarningTimer.bindService();

        if (startedFromNotification) {
            startedFromNotification = false;
            mWarningTimer.goToParkedLocation();
        }




    }



    /**
     * Build Google Map.
     */
    private synchronized void buildGoogleMap() {

        mMap = ((SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map))
                .getMap();

        mMap.setBuildingsEnabled(false);
        mMap.setIndoorEnabled(false);
        mMap.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
            @Override
            public void onMapClick(LatLng latLng) {

                if (mapLoaded) {
                    placeMarker(latLng);
                }

                mCurrentLocation.stopLocationUpdates();
            }
        });

        mMap.setOnMarkerClickListener(new MarkerClickedListener());
        mMap.setInfoWindowAdapter(new CustomInfoWindowAdapter());
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

        if (mTimerWindow != null) {
            mTimerWindow.dismiss();
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

        if (mCLMarker == null) {
            mTimerWindow.dismiss();
            return;
        }

        Location location = new Location("");
        location.setLatitude(mCLMarker.getPosition().latitude);
        location.setLongitude(mCLMarker.getPosition().longitude);

        mWarningTimer.setParkedLocation(getAddress(location), mCLMarker.getPosition());
        mWarningTimer.setWarningTime();

        mTimerWindow.dismiss();

        Toast.makeText(this,
                "Timer is set",
                Toast.LENGTH_SHORT)
                .show();
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
                placeCurrentLocationMarker(
                        new LatLng(location.getLatitude(), location.getLongitude()));
            }

        } else {
            // Otherwise just get the last known location that is stored in the database

            if (mapLoaded) {
                String[] location = mCurrentLocation.getLastKnownLocation();

                LatLng latLng = new LatLng(Double.parseDouble(location[1]), Double.parseDouble(location[2]));

                placeCurrentLocationMarker(latLng);
            }
        }
    }

    public void placeCurrentLocationMarker(LatLng location) {

        if (mMap == null) {
            buildGoogleMap();
        }

        if (mMap != null) {

            if (mParkingGarageMarker != null)
                mParkingGarageMarker.hideInfoWindow();

            if (mClickedLocationMarker != null)
                mClickedLocationMarker.remove();

            if (mCLMarker != null && mCLMarkerCircle != null) {
                // Show the marker
                mCLMarker.setVisible(true);
                mCLMarkerCircle.setVisible(true);

                animateMarker(location, false);

            } else {
                // Create the marker
                mCLMarker = mMap.addMarker(new MarkerOptions()
                        .title("")
                        .position(location)
                        .rotation(mMarkerRotation)
                        .anchor(0.5f, 0.75f)
                        .flat(true)
                        .icon(BitmapDescriptorFactory.fromResource(R.drawable.icon_user)));

                mCLMarkerCircle = mMap.addCircle(new CircleOptions()
                        .center(location)
                        .radius(45f)
                        .fillColor(Color.TRANSPARENT)
                        .strokeWidth(1.5f)
                        .strokeColor(0xFFE01368));
            }

            // Move the camera to the marker
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(location, CAMERA_ZOOM_LEVEL));
        }
    }

    public void placeMarker(LatLng location) {

        if (mMap == null) {
            buildGoogleMap();
        }

        if (mMap != null) {
            if (mCLMarker != null) {
                // Hide the current location marker
                mCLMarker.setVisible(false);
                mCLMarkerCircle.setVisible(false);
            }

            // We only want to remove the marker that is clicked on the map by the user, not
            // the garage parking marker
            if (mClickedLocationMarker != null && mClickedLocationMarker.getSnippet() == null)
                mClickedLocationMarker.remove();

            Location loc = new Location("");
            loc.setLatitude(location.latitude);
            loc.setLongitude(location.longitude);

            // Add the new marker to the map
            mClickedLocationMarker = mMap.addMarker(new MarkerOptions()
                    .position(location)
                    .title(getAddress(loc)));

            // Show the address of the marker's location
            mClickedLocationMarker.showInfoWindow();

            // Move the camera to the marker
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(location, CAMERA_ZOOM_LEVEL));
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
                } else {/*
                    if (hideMarker) {
                        mCLMarker.setVisible(false);
                        mCLMarkerCircle.setVisible(false);
                    } else {
                        mCLMarker.setVisible(true);
                        mCLMarkerCircle.setVisible(true);
                    }*/
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

    public void searchLocation(View view) throws Exception {

        mSearch.geolocate(view);
    }

    private class MarkerClickedListener implements GoogleMap.OnMarkerClickListener {

        @Override
        public boolean onMarkerClick(Marker marker) {

            if (marker.getSnippet() != null) {      // It's a garage parking marker

                if (mClickedLocationMarker != null) {
                    mClickedLocationMarker.remove();
                }

            } else {        // It's a random clicked location marker on the map

                // If it is a different marker than the previously stored marker, remove it
                if (mClickedLocationMarker != null && !mClickedLocationMarker.equals(marker))
                    mClickedLocationMarker.remove();

                mClickedLocationMarker = marker;
            }

            if (mInfoWindowIsShown) {
                mInfoWindowIsShown = false;
                marker.hideInfoWindow();

                // If this is the same parking garage marker that has been clicked, do nothing
                if (mParkingGarageMarker != null && mParkingGarageMarker.equals(marker)) {
                    return true;

                    // If this is a different parking garage marker, hide the current InfoWindow and
                    // show the new parking garage marker's InfoWindow
                } else if (mParkingGarageMarker != null && marker.getSnippet() != null) {
                    mParkingGarageMarker.hideInfoWindow();
                    mParkingGarageMarker = marker;
                    return false;

                    // If this is the first time user click on any parking garage marker, set this
                    // marker as the parking garage marker
                } else if (mParkingGarageMarker == null && marker.getSnippet() != null) {
                    mParkingGarageMarker = marker;
                    return true;
                }

                return true;
            } else {
                // No InfoWindow is shown on any of the markers on the map, so show it
                mInfoWindowIsShown = true;
                return false;
            }
        }
    }

    private class CustomInfoWindowAdapter implements GoogleMap.InfoWindowAdapter {

        private View mView;

        CustomInfoWindowAdapter() {
            mView = getLayoutInflater().inflate(R.layout.window_parking_info, null);
        }

        @Override
        public View getInfoWindow(Marker marker) {

            // If the title is an empty string, such as from the mCLMarker, then
            // we don't show any InfoWindow
            if (marker.getTitle().equals(""))
                return null;

            // Set the title for the InfoWindow
            TextView title = ((TextView) mView.findViewById(R.id.parking_name));
            title.setText(marker.getTitle());

            String snippet = marker.getSnippet();
            String address = null;
            String rate = null;

            if (snippet == null) {
                // This marker is a clicked marker on the map, and it's not the marker for
                // garage parking, so we just use default InfoWindow
                return null;
            }

            // Now we know this is the garage parking marker, it must have address and rate,
            // so we parse out the marker's snippet into address and rate
            // The address and rate is separate by '%' character
            for (int i = 0; i < snippet.length(); i++) {
                if (snippet.charAt(i) == '%') {
                    address = snippet.substring(0, i);
                    rate = snippet.substring(i + 1, snippet.length());
                    break;
                }
            }

            TextView textAddress = ((TextView) mView.findViewById(R.id.parking_address));
            textAddress.setText(address);

            TextView textRate = ((TextView) mView.findViewById(R.id.parking_snippet));
            textRate.setText(rate);

            return mView;
        }

        @Override
        public View getInfoContents(Marker marker) {
            // We want getInfoWindow(...) to set our custom info window, so return null here
            return null;
        }
    }
}