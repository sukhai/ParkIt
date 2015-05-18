package com.csc413.group9.parkIt;

import android.content.Context;
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
import android.support.v4.app.FragmentActivity;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.view.animation.Interpolator;
import android.view.animation.LinearInterpolator;
import android.widget.CheckBox;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;
import android.widget.ViewSwitcher;

import com.csc413.group9.parkIt.Database.DatabaseManager;
import com.csc413.group9.parkIt.Features.Search.Search;
import com.csc413.group9.parkIt.Features.StreetHighlightSettings;
import com.csc413.group9.parkIt.Features.WarningTimer.WarningTimer;
import com.csc413.group9.parkIt.SFPark.ParkingInformation;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.Projection;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

/**
 * The main activity of the application. This app will have the basic functionality of Google Map,
 * SFPark data, and connectivity to the database. The special features of this app are Warning
 * Timer, Search, and Highlight Settings.
 */
public class MainActivity extends FragmentActivity implements
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        GoogleMap.OnMapLoadedCallback,
        SensorEventListener {

    /**
     * Key for SFPark data, if the data is ready or not. This key is used for saving and restoring
     * the state instance.
     */
    private static final String KEY_SFPARK_DATA_READY = "ParkIt.SFPark_Data_Ready";

    /**
     * Key for clicked location marker, if the data is ready or not. This key is used for saving
     * and restoring the state instance.
     */
    private static final String KEY_CLICKED_LOCATION_MARKER = "ParkIt.Clicked_Location_Marker";

    /**
     * Key for clicked marker's info window, if the data is ready or not. This key is used for
     * saving and restoring the state instance.
     */
    private static final String KEY_CLICKED_MARKER_INFO_WINDOW = "ParkIt.Clicked_Marker_Info_Window";

    /**
     * Key for parking marker's info window, if the data is ready or not. This key is used for
     * saving and restoring the state instance.
     */
    private static final String KEY_PARKING_MARKER_INFO_WINDOW = "ParkIt.Parking_Marker_Info_Window";

    /**
     * Key for camera's position, if the data is ready or not. This key is used for saving and
     * restoring the state instance.
     */
    private static final String KEY_CAMERA_POSITION = "ParkIt.Camera_Position";

    /**
     * Camera zoom level.
     */
    private static final float CAMERA_ZOOM_LEVEL = 18f;

    /**
     * Offset of y coordinate. This value will be used for showing the parking marker's info window.
     */
    private static final double OFFSET_Y = 0.005f;

    /**
     * Flag for whether this activity is started by clicking the notification from the notification
     * bar.
     */
    public static boolean mStartedFromNotification = false;

    /**
     * A reference to the ViewSwitcher, which will switch between splash screen and main activity
     * views.
     */
    private ViewSwitcher mViewSwitcher;

    /**
     * A reference to the Google map.
     */
    private GoogleMap mMap;

    /**
     * A reference to the Google API Client.
     */
    private GoogleApiClient mGoogleApiClient;

    /**
     * A reference to the Sensor Manager to check the direction of the device.
     */
    private SensorManager mSensorManager;

    /**
     * The clicked location marker (the red marker).
     */
    private Marker mClickedLocationMarker;

    /**
     * The parking garage marker (the P icon marker).
     */
    private Marker mParkingGarageMarker;

    /**
     * The current location marker (the purple marker).
     */
    private Marker mCurrentLocationMarker;

    /**
     * A reference to the ParkingInformation.
     */
    private ParkingInformation mParkingInfo;

    /**
     * A reference to the CurrentLocation.
     */
    private CurrentLocation mCurrentLocation;

    /**
     * A reference to the StreetHighlightSettings.
     */
    private StreetHighlightSettings mStreetHighlightSettings;

    /**
     * A reference to the Search.
     */
    private Search mSearch;

    /**
     * A reference to the WarningTimer.
     */
    private WarningTimer mWarningTimer;

    /**
     * The popup window for warning timer.
     */
    private PopupWindow mTimerWindow;

    /**
     * The popup window for settings window.
     */
    private PopupWindow mSettingWindow;

    /**
     * The rotation value for the current location's marker.
     */
    private float mMarkerRotation;

    /**
     * Flag for whether the parking marker's info window is shown.
     */
    private boolean mParkingMarkerInfoWindow = false;

    /**
     * Flag for whether the clicked marker's info window is shown.
     */
    private boolean mClickedMarkerInfoWindow = false;

    /**
     * Flag for whether the Google map has loaded.
     */
    private boolean mapLoaded = false;

    /**
     * Flag for whether to show on-street parking.
     */
    private boolean mShowOnStreetParking = true;

    /**
     * Flag for whether to show off-street parking.
     */
    private boolean mShowOffStreetParking = true;

    @Override
    protected void onStart() {
        super.onStart();

        // Load user's settings
        mStreetHighlightSettings = new StreetHighlightSettings();
        mShowOnStreetParking = mStreetHighlightSettings.isOnStreetHighlighted();
        mShowOffStreetParking = mStreetHighlightSettings.isOffStreetHighlighted();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        if (!servicesAvailable()) {
            finish();
        }

        // Initialize the ViewSwitcher object and add splash view and main activity view to the
        // ViewSwitcher
        mViewSwitcher = new ViewSwitcher(MainActivity.this);
        mViewSwitcher.addView(ViewSwitcher.inflate(MainActivity.this, R.layout.splash, null));
        mViewSwitcher.addView(ViewSwitcher.inflate(MainActivity.this, R.layout.activity_main, null));

        setContentView(mViewSwitcher);

        // Initialize the DatabaseManager
        DatabaseManager.initializeInstance(this);

        buildGoogleApiClient();
        buildSensorManager();
        buildGoogleMap();

        mParkingInfo = new ParkingInformation(this, mMap);

        // Load saved ParkingInformation's data if possible
        if (savedInstanceState == null) {
            mParkingInfo.saveParkingMarkersFragment();
            mParkingInfo.getSFParkData();
        } else {
            mParkingInfo.restoreParkingMarkersFragment();
            boolean ready = savedInstanceState.getBoolean(KEY_SFPARK_DATA_READY, false);
            mParkingInfo.setSfParkDataReady(ready);
            mParkingInfo.getSFParkData();
        }

        // Initialize and track the current location
        mCurrentLocation = new CurrentLocation(this);
        trackDeviceLocation(null);

        mSearch = new Search(this);

        mWarningTimer = new WarningTimer(this);
        mWarningTimer.bindService();

        if (mStartedFromNotification) {
            mStartedFromNotification = false;
            mWarningTimer.goToParkedLocation();
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        // Store the clicked marker's data if it exists
        if (mClickedLocationMarker != null) {
            MarkerOptions clickedLocationMarker = new MarkerOptions()
                    .title(mClickedLocationMarker.getTitle())
                    .position(mClickedLocationMarker.getPosition());

            mClickedLocationMarker.remove();
            outState.putParcelable(KEY_CLICKED_LOCATION_MARKER, clickedLocationMarker);
        }

        // Store the rest of the important data
        outState.putBoolean(KEY_SFPARK_DATA_READY, mParkingInfo.isSFParkDataReady());
        outState.putBoolean(KEY_CLICKED_MARKER_INFO_WINDOW, mClickedMarkerInfoWindow);
        outState.putBoolean(KEY_PARKING_MARKER_INFO_WINDOW, mParkingMarkerInfoWindow);
        outState.putParcelable(KEY_CAMERA_POSITION, mMap.getCameraPosition());
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);

        // Restore SFPark data
        boolean ready = savedInstanceState.getBoolean(KEY_SFPARK_DATA_READY);
        mParkingInfo.setSfParkDataReady(ready);
        mParkingInfo.getSFParkData();

        // Restore clicked location marker
        MarkerOptions clickedLocation =
                (MarkerOptions) savedInstanceState.getParcelable(KEY_CLICKED_LOCATION_MARKER);
        if (clickedLocation != null) {
            mClickedLocationMarker = mMap.addMarker(clickedLocation);
        }

        // Restore clicked marker's info window
        mClickedMarkerInfoWindow = savedInstanceState.getBoolean(KEY_CLICKED_MARKER_INFO_WINDOW);
        if (mClickedMarkerInfoWindow && mClickedLocationMarker != null) {
            mClickedLocationMarker.showInfoWindow();
        }

        // Restore parking garage's info window
        mParkingMarkerInfoWindow = savedInstanceState.getBoolean(KEY_PARKING_MARKER_INFO_WINDOW);
        if (mParkingMarkerInfoWindow && mParkingGarageMarker != null) {
            mParkingGarageMarker.showInfoWindow();
        }

        CameraPosition position = (CameraPosition) savedInstanceState.getParcelable(KEY_CAMERA_POSITION);
        mMap.moveCamera(CameraUpdateFactory.newCameraPosition(position));
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

        if (mSettingWindow != null) {
            mSettingWindow.dismiss();
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
    protected void onDestroy() {

        if (mCurrentLocation != null) {
            mCurrentLocation.hideNoGPSAlertMessage();
        }

        super.onDestroy();
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
    public void onSensorChanged(SensorEvent event) {

        // Rotate the current location's marker (purple marker) on the map when the direction of
        // the device has changed
        mMarkerRotation = event.values[0];

        if (mCurrentLocationMarker != null) {
            mCurrentLocationMarker.setRotation(mMarkerRotation);
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    @Override
    public void onMapLoaded() {

        mapLoaded = true;

        mMap.setLocationSource(mCurrentLocation);
    }

    /**
     * Determine if the Google map is loaded.
     * @return true if the Google map is loaded, false otherwise
     */
    public boolean isMapLoaded() {
        return mapLoaded;
    }

    /**
     * Determine whether the SFPark data is ready.
     * @return true if the SFPark data is ready, false otherwise
     */
    public boolean isSFParkDataReady() {
        return mParkingInfo == null ? false : mParkingInfo.isSFParkDataReady();
    }

    /**
     * Determine whether to show on-street parking on the Google map or not.
     * @return true if the Google map needs to show on-street parking, false otherwise
     */
    public boolean showOnStreetParking() {
        return mShowOnStreetParking;
    }

    /**
     * Determine whether to show off-street parking on the Google map or not.
     * @return true if the Google map needs to show off-street parking, false otherwise
     */
    public boolean showOffStreetParking() {
        return mShowOffStreetParking;
    }

    /**
     * Switch the ViewSwitcher to show the main activity view. This will also track the current
     * location once the view has switched.
     */
    public void showMainView() {
        if (mViewSwitcher != null) {
            mViewSwitcher.showNext();

            trackDeviceLocation(null);
        }
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
            TimePicker mTimePicker = (TimePicker) timerView.findViewById(R.id.timepicker_warning_timer);
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

        if (mCurrentLocationMarker == null) {
            mTimerWindow.dismiss();
            return;
        }

        Location location = new Location("");
        location.setLatitude(mCurrentLocationMarker.getPosition().latitude);
        location.setLongitude(mCurrentLocationMarker.getPosition().longitude);

        mWarningTimer.setParkedLocation(getAddress(location), mCurrentLocationMarker.getPosition());
        mWarningTimer.setWarningTime();

        // Close the warning timer window
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
     * Show the settings window.
     * @param view
     */
    public void showSettingWindow(View view) {

        // Get the setting view layout
        LayoutInflater layoutInflater = (LayoutInflater) getBaseContext().getSystemService(LAYOUT_INFLATER_SERVICE);
        final View settingView = layoutInflater.inflate(R.layout.window_settings, null);

        if (mSettingWindow == null) {

            // Instantiate a popup window
            mSettingWindow = new PopupWindow(
                    settingView,
                    LayoutParams.WRAP_CONTENT,
                    LayoutParams.WRAP_CONTENT);

            // Add listeners to checkboxes
            CheckBox checkBoxOnStreet = (CheckBox) settingView.findViewById(R.id.checkbox_onstreet);
            checkBoxOnStreet.setChecked(mShowOnStreetParking);
            checkBoxOnStreet.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (mParkingInfo.isSFParkDataReady()) {
                        mShowOnStreetParking = ((CheckBox) v).isChecked();
                        mParkingInfo.highlightStreet(mShowOnStreetParking, mShowOffStreetParking);

                        // Store settings
                        mStreetHighlightSettings.setHighlighted(mShowOnStreetParking, mShowOffStreetParking);
                    }
                }
            });

            CheckBox checkBoxOffStreet = (CheckBox) settingView.findViewById(R.id.checkbox_offstreet);
            checkBoxOffStreet.setChecked(mShowOffStreetParking);
            checkBoxOffStreet.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (mParkingInfo.isSFParkDataReady()) {
                        mShowOffStreetParking = ((CheckBox) v).isChecked();
                        mParkingInfo.highlightStreet(mShowOnStreetParking, mShowOffStreetParking);

                        // Store settings
                        mStreetHighlightSettings.setHighlighted(mShowOnStreetParking, mShowOffStreetParking);
                    }
                }
            });
        }

        // Show the popup window in the center of the screen
        mSettingWindow.showAtLocation(settingView, Gravity.CENTER, 0, 0);
    }

    /**
     * Close the settings window.
     * @param view the view of the application
     */
    public void closeSettingWindow(View view) {
        mSettingWindow.dismiss();
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
            if (isSFParkDataReady()) {
                mCurrentLocation.showNoGPSAlertMessage();
            }
        }
    }

    /**
     * Search the location on the Google map.
     * @param view the view of the application
     * @throws Exception any exception
     */
    public void searchLocation(View view) throws Exception {

        mSearch.geoLocate(view);
    }

    /**
     * Place the current location marker on the map (purple marker). This will hide all info windows
     * of other markers.
     * @param location the location of the marker should be placed
     */
    public void placeCurrentLocationMarker(LatLng location) {

        if (mMap == null) {
            buildGoogleMap();
        }

        if (mMap != null) {

            if (mParkingGarageMarker != null)
                mParkingGarageMarker.hideInfoWindow();

            if (mClickedLocationMarker != null)
                mClickedLocationMarker.remove();

            if (mCurrentLocationMarker != null) {
                // Show the marker
                mCurrentLocationMarker.setVisible(true);
                animateMarker(location);

            } else {
                // Create the marker
                mCurrentLocationMarker = mMap.addMarker(new MarkerOptions()
                        .title("")
                        .position(location)
                        .rotation(mMarkerRotation)
                        .anchor(0.5f, 0.75f)
                        .flat(true)
                        .icon(BitmapDescriptorFactory.fromResource(R.drawable.icon_user)));
            }

            // Move the camera to the marker
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(location, CAMERA_ZOOM_LEVEL));
        }
    }

    /**
     * Place a marker on the given location. The marker is red color, and will remove parking
     * marker's info window and the current location's marker from the Google map.
     * @param location the location of the marker should be placed
     */
    public void placeMarker(LatLng location) {

        if (mMap == null) {
            buildGoogleMap();
        }

        if (mMap != null) {

            mParkingMarkerInfoWindow = false;

            if (mCurrentLocationMarker != null) {
                // Hide the current location marker
                mCurrentLocationMarker.setVisible(false);
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

    /**
     * Animate the current location marker from the current position to the specified position.
     * This will make the current location move smoothly on the map.
     * @param toPosition the position to be moved to
     */
    public void animateMarker(final LatLng toPosition) {

        final Handler handler = new Handler();
        final long start = SystemClock.uptimeMillis();
        Projection projection = mMap.getProjection();
        Point startPoint = projection.toScreenLocation(mCurrentLocationMarker.getPosition());
        final LatLng startLatLng = projection.fromScreenLocation(startPoint);
        final long duration = 500;

        final Interpolator interpolator = new LinearInterpolator();

        handler.post(new Runnable() {
            @Override
            public void run() {
                long elapsed = SystemClock.uptimeMillis() - start;
                float t = interpolator.getInterpolation((float) elapsed
                        / duration);
                double lng = t * toPosition.longitude + (1 - t) * startLatLng.longitude;
                double lat = t * toPosition.latitude + (1 - t) * startLatLng.latitude;
                mCurrentLocationMarker.setPosition(new LatLng(lat, lng));

                if (t < 1.0) {
                    // Post again 16ms later.
                    handler.postDelayed(this, 16);
                }
            }
        });
    }

    /**
     * Get the address from the given Location object.
     * @param loc the location that stored the address
     * @return the address from the given Location object if the location exist, otherwise will
     *         return an empty string
     */
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

    /**
     * Build Google Map.
     */
    private synchronized void buildGoogleMap() {

        android.support.v4.app.FragmentManager fragmentManager = getSupportFragmentManager();

        RetainMapFragment mapFragment =
                (RetainMapFragment) fragmentManager.findFragmentById(R.id.map);

        mMap = mapFragment.getMap();

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

        // Move camera to San Francisco
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(37.7833, -122.4167), 12));
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

    /**
     * Hide all info windows. This includes both parking marker's info window and clicked location
     * marker's info window.
     */
    private void hideAllInfoWindows() {

        if (mClickedLocationMarker != null) {
            mClickedLocationMarker.hideInfoWindow();
        }

        if (mParkingGarageMarker != null) {
            mParkingGarageMarker.hideInfoWindow();
        }
    }

    /**
     * A class that handles the action when a marker on the map is clicked.
     */
    private class MarkerClickedListener implements GoogleMap.OnMarkerClickListener {

        @Override
        public boolean onMarkerClick(Marker marker) {

            // The clicked marker is a garage parking marker if the snippet is not null,
            // otherwise it is a user clicked marker (red marker)

            if (marker.getSnippet() != null) {      // Garage parking marker
                showGarageParkingMarker(marker);
            } else {                                // Clicked marker (red marker)
                showClickedMarker(marker);
            }

            // Always return true because we will handle showing and hiding infoWindow by ourself
            return true;
        }

        /**
         * Show garage parking marker. This will hide all other markers' info window on the Google
         * map.
         * @param marker the marker that is clicked
         */
        private void showGarageParkingMarker(Marker marker) {
            // Hide all info windows on the Google map
            hideAllInfoWindows();

            if (mParkingGarageMarker == null || !mParkingGarageMarker.equals(marker)) {

                // If this marker is different than the previous marker, then set the previous
                // marker to be the new marker and show the info window
                mParkingGarageMarker = marker;
                mParkingGarageMarker.showInfoWindow();
                mParkingMarkerInfoWindow = true;
                mClickedMarkerInfoWindow = false;

                // Get the location of this marker and move the camera to this marker
                LatLng position = new LatLng(mParkingGarageMarker.getPosition().latitude  + OFFSET_Y,
                        mParkingGarageMarker.getPosition().longitude);

                mMap.animateCamera(
                        CameraUpdateFactory.newLatLngZoom(position, mMap.getCameraPosition().zoom));

            } else if (mParkingGarageMarker.equals(marker) && !mParkingMarkerInfoWindow) {

                // If this clicked marker is the same as previous marker but the info window is not
                // shown, the show it and move the camera to this marker
                mClickedMarkerInfoWindow = false;
                mParkingMarkerInfoWindow = true;
                mParkingGarageMarker.showInfoWindow();

                LatLng position = new LatLng(mParkingGarageMarker.getPosition().latitude  + OFFSET_Y,
                        mParkingGarageMarker.getPosition().longitude);

                mMap.animateCamera(
                        CameraUpdateFactory.newLatLngZoom(position, mMap.getCameraPosition().zoom));

            } else {
                mParkingMarkerInfoWindow = false;
                mClickedMarkerInfoWindow = false;
            }
        }

        /**
         * Show clicked location marker. This will hide all other markers' info window on the Google
         * map.
         * @param marker the marker that is clicked
         */
        private void showClickedMarker(Marker marker) {
            // Hide all info windows on the Google map
            hideAllInfoWindows();

            if (mClickedLocationMarker == null || !mClickedLocationMarker.equals(marker)) {

                // If this marker is different than the previous marker, then set the previous
                // marker to be the new marker and show the info window
                mClickedLocationMarker = marker;
                mClickedLocationMarker.showInfoWindow();
                mClickedMarkerInfoWindow = true;
                mParkingMarkerInfoWindow = false;

                // Get the location of this marker and move the camera to this marker
                mMap.animateCamera(
                        CameraUpdateFactory.newLatLngZoom(mClickedLocationMarker.getPosition(),
                                mMap.getCameraPosition().zoom));

            } else if (mClickedLocationMarker.equals(marker) && !mClickedMarkerInfoWindow) {

                // If this clicked marker is the same as previous marker but the info window is not
                // shown, the show it and move the camera to this marker
                mParkingMarkerInfoWindow = false;
                mClickedMarkerInfoWindow = true;
                mClickedLocationMarker.showInfoWindow();

                mMap.animateCamera(
                        CameraUpdateFactory.newLatLngZoom(mClickedLocationMarker.getPosition(),
                                mMap.getCameraPosition().zoom));

            } else {
                mParkingMarkerInfoWindow = false;
                mClickedMarkerInfoWindow = false;
            }
        }
    }

    /**
     * A class that modify the info window of the markers.
     */
    private class CustomInfoWindowAdapter implements GoogleMap.InfoWindowAdapter {

        /**
         * A reference to the View.
         */
        private View mView;

        /**
         * Default constructor. Get the View of the custom info window.
         */
        CustomInfoWindowAdapter() {
            mView = getLayoutInflater().inflate(R.layout.window_parking_info, null);
        }

        @Override
        public View getInfoWindow(Marker marker) {

            // If the title is an empty string, such as from the mCurrentLocationMarker, then
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