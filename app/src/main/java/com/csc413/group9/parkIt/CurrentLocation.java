package com.csc413.group9.parkIt;

import android.app.AlertDialog;
import android.app.Service;
import android.content.DialogInterface;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.Settings;
import android.widget.Toast;

import com.google.android.gms.maps.LocationSource;
import com.google.android.gms.maps.model.LatLng;

/**
 * The current location of the device. The user can set whether to keep on tracking the current
 * location of the device or not.
 *
 * Created by Su Khai Koh on 4/17/15.
 */
public class CurrentLocation extends Service implements LocationListener, LocationSource {

    /**
     * The minimum time before getting a new location update. (3 seconds)
     */
    private static final long MIN_TIME = 3000;

    /**
     * The minimum distance before getting a new location update. (2 meters)
     */
    private static final long MIN_DISTANCE = 2;

    /**
     * A reference to the main activity of the app.
     */
    private final MainActivity mMainActivity;

    /**
     * The location of the device.
     */
    private Location mLocation;

    /**
     * The system location manager.
     */
    private LocationManager mLocationManager;

    /**
     * Flag for network provider is enable.
     */
    private boolean mNetworkEnabled;

    /**
     * Flag for GPS provider is enable.
     */
    private boolean mGPSEnabled;

    /**
     * Flag for network and GPS status.
     */
    private boolean mCanGetLocation = false;

    /**
     * Flag for keeping track on device's location.
     */
    private boolean mKeepTrack = true;

    /**
     * A reference to an alert dialog, which is uses for displaying a dialog box about the GPS
     * setting.
     */
    private AlertDialog mAlert;

    /**
     * Setup class members and start the location updates.
     * @param mainActivity the main activity of this application
     */
    public CurrentLocation(MainActivity mainActivity) {

        mMainActivity = mainActivity;

        startLocationUpdates();

        getLocation();
    }

    /**
     * Start the location updates.
     */
    public void startLocationUpdates() {

        mKeepTrack = true;

        mLocationManager = (LocationManager) mMainActivity.getSystemService(LOCATION_SERVICE);
    }

    /**
     * Stop the location updates.
     */
    public void stopLocationUpdates() {

        mKeepTrack = false;

        if (mLocationManager != null) {
            mLocationManager.removeUpdates(this);
        }
    }

    /**
     * Check whether the network or GPS is enabled and can get the current location of the device.
     * @return true if the GPS is enabled and it is ready to get the current location of the
     *         device, otherwise false.
     */
    public boolean canGetLocation() {

        mCanGetLocation = isProviderAvailable();

        return mCanGetLocation;
    }

    /**
     * Get the current location of the device. This method return the current location of the
     * device if the network or GPS is enabled, otherwise return null.
     * @return the current location of the device if the network or GPS is enabled, otherwise null
     */
    public Location getLocation() {

        try {
            if (isProviderAvailable()) {

                mCanGetLocation = true;

                // If the network is enabled, then get the network location
                if (mNetworkEnabled) {
                    useNetworkLocation();
                }

                // If GPS is also enabled, then get the GPS location
                if (mGPSEnabled) {
                    useGPSLocation();
                }

                // If network or GPS is available but unable to get the location, then display
                // a message to indicate it is waiting for location
                if (mLocation == null) {
                    Toast.makeText(mMainActivity,
                            "Waiting for location ...",
                            Toast.LENGTH_SHORT)
                            .show();
                }

            } else if (!mGPSEnabled) {
                // Only show alert dialog if the SFPark data is already ready on the map
                if (mMainActivity.isSFParkDataReady()) {
                    showNoGPSAlertMessage();
                }
            }

        } catch (Exception ex) {
            ex.printStackTrace();
        }

        return mLocation;
    }

    /**
     * Show the No-GPS alert message box.
     */
    public void showNoGPSAlertMessage() {

        if (mAlert != null && mAlert.isShowing()) {
            return;
        }

        AlertDialog.Builder alertDialog = new AlertDialog.Builder(mMainActivity);

        alertDialog.setCancelable(false);
        alertDialog.setTitle("GPS Settings");
        alertDialog.setMessage("Your GPS seems to be disabled, do you want to enable it?");

        // Add YES button
        alertDialog.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                mMainActivity.startActivity(intent);
            }
        });

        // Add NO button
        alertDialog.setNegativeButton("No", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
                mMainActivity.finish();
            }
        });

        mAlert = alertDialog.create();
        mAlert.show();
    }

    /**
     * Hide the No-GPS alert message box if it is currently showing on the screen.
     */
    public void hideNoGPSAlertMessage() {
        if (mAlert != null) {
            mAlert.dismiss();
        }
    }

    /**
     * Determine if any provider is available. The provider can either be GPS or network provider.
     * This method will return true if either GPS or network provider is available and
     * startLocationUpdates() has already called, false if stopLocationUpdates() is called or none
     * of the provider is available.
     * @return true if startLocationUpdates() has already called and either GPS or network provider
     *         is available, false otherwise
     */
    private boolean isProviderAvailable() {

        // Get the location service
        mLocationManager = (LocationManager) mMainActivity.getSystemService(LOCATION_SERVICE);

        // Check whether the network is enabled on the device
        mNetworkEnabled = mLocationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);

        // Check whether the GPS is enabled on the device
        mGPSEnabled = mLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);

        return ((mNetworkEnabled || mGPSEnabled) && mKeepTrack);
    }

    /**
     * Use the network provider to provide current location.
     */
    private void useNetworkLocation() {

        mLocationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER,
                MIN_TIME, MIN_DISTANCE, this);

        if (mLocationManager != null) {
            mLocation = mLocationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);

            // Draw the current location on the map and save the coordinate to database
            if (mLocation != null && mMainActivity.isMapLoaded()) {

                mMainActivity.placeCurrentLocationMarker(
                        new LatLng(mLocation.getLatitude(), mLocation.getLongitude()));
            }
        }
    }

    /**
     * Use GPS provider to provide current location.
     */
    private void useGPSLocation() {

        mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,
                MIN_TIME, MIN_DISTANCE, this);

        if (mLocationManager != null) {
            mLocation = mLocationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);

            // Draw the current location on the map and save the coordinate to database
            if (mLocation != null && mMainActivity.isMapLoaded()) {

                mMainActivity.placeCurrentLocationMarker(
                        new LatLng(mLocation.getLatitude(), mLocation.getLongitude()));
            }
        }
    }

    @Override
    public void onLocationChanged(Location location) {

        if (mKeepTrack) {

            mLocation = location;

            if (mMainActivity.isMapLoaded()) {
                // Place the marker on the map
                mMainActivity.placeCurrentLocationMarker(
                        new LatLng(mLocation.getLatitude(), mLocation.getLongitude()));
            }
        }
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {

    }

    @Override
    public void onProviderEnabled(String provider) {

        // Hide the NO-GPS alert dialog box when the GPS is enabled and set the flag to true
        mCanGetLocation = true;
        hideNoGPSAlertMessage();
    }

    @Override
    public void onProviderDisabled(String provider) {

        // Show the NO-GPS alert dialog box when the GPS is not enabled and set the flag to false
        mCanGetLocation = false;
        showNoGPSAlertMessage();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void activate(OnLocationChangedListener onLocationChangedListener) {

    }

    @Override
    public void deactivate() {

    }
}