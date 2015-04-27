package com.csc413.group9.parkIt;

import android.app.Service;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.IBinder;
import android.widget.Toast;

import com.csc413.group9.parkIt.Database.DatabaseHelper;
import com.csc413.group9.parkIt.Database.DatabaseManager;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

/**
 * The current location of the device. The client can set whether to keep on tracking the current
 * location of the device or not.
 *
 * Created by Su Khai Koh on 4/17/15.
 */
public class CurrentLocation extends Service implements LocationListener {

    /**
     * The minimum time before getting a new location update. (5 seconds)
     */
    private static final long MIN_TIME = 1000 * 5;

    /**
     * The minimum distance before getting a new location update. (2 meters)
     */
    private static final long MIN_DISTANCE = 2;

    /**
     * The main activity.
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
     * Flag for network and GPS status.
     */
    private boolean canGetLocation = false;

    /**
     * Flag for keeping track on device's location.
     */
    private boolean keepTrack = true;

    /**
     * Setup class members.
     * @param mainActivity the main activity of this application
     */
    public CurrentLocation(MainActivity mainActivity) {

        mMainActivity = mainActivity;

        DatabaseManager mDatabaseManager = DatabaseManager.getInstance();

        // If the location table is empty, then provide the default location as the first entry
        if (mDatabaseManager.isEmpty(DatabaseHelper.TABLE_NAME_LOCATION)) {

            Location location = new Location(DatabaseHelper.DEFAULT_LOCATION_ADDRESS);
            location.setLatitude(DatabaseHelper.DEFAULT_LOCATION_LATITUDE);
            location.setLongitude(DatabaseHelper.DEFAULT_LOCATION_LONGITUDE);

            setLastKnownLocation(location);
        }

        getLocation();
    }

    /**
     * Get the current location of the device. This method return the current location of the
     * device if the network or GPS is enabled, otherwise return null.
     * @return the current location of the device if the network or GPS is enabled, otherwise null
     */
    public Location getLocation() {

        try {
            // Get the location service
            mLocationManager = (LocationManager) mMainActivity.getSystemService(LOCATION_SERVICE);

            // Check whether the network is enabled on the device
            boolean networkEnabled = mLocationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);

            // Check whether the GPS is enabled on the device
            boolean GPSEnabled = mLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);

            if ((GPSEnabled || networkEnabled) && keepTrack) {

                canGetLocation = true;
/*
                // If the network is enabled, then get the network location
                if (networkEnabled) {

                    mLocationManager.requestLocationUpdates(
                            LocationManager.NETWORK_PROVIDER,
                            MIN_TIME, MIN_DISTANCE, this);

                    if (mLocationManager != null) {
                        mLocation = mLocationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);

                        // Draw the current location on the map and save the coordinate to database
                        if (mLocation != null) {

                            mMainActivity.placeMarkerOnMap(mLocation, true);

                            setLastKnownLocation(mLocation);
                        }
                    }
                }
*/
                // If GPS is also enabled, then get the GPS location
                if (GPSEnabled) {

                    mLocationManager.requestLocationUpdates(
                            LocationManager.GPS_PROVIDER,
                            MIN_TIME, MIN_DISTANCE, this);

                    if (mLocationManager != null) {
                        mLocation = mLocationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);

                        // Draw the current location on the map and save the coordinate to database
                        if (mLocation != null) {

                            if (mMainActivity.isMapLoaded())
                                mMainActivity.placeMarkerOnMap(mLocation, true);

                            setLastKnownLocation(mLocation);
                        }
                    }
                }

                // If network or GPS is available but unable to get the location, then display
                // a message to indicate it is waiting for location
                if (mLocation == null) {
                    Toast.makeText(mMainActivity,
                            "Waiting for location ...",
                            Toast.LENGTH_SHORT)
                            .show();
                }

            }

        } catch (Exception ex) {
            // Do nothing
        }

        return mLocation;
    }

    /**
     * Start the location updates. The location updates will only start and work if the network or
     * GPS is enabled.
     */
    protected void startLocationUpdates() {

        keepTrack = true;

        getLocation();
    }

    /**
     * Stop the location updates.
     */
    protected void stopLocationUpdates() {

        keepTrack = false;

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

        return canGetLocation;
    }

    /**
     * Set the last known location of the device to the database Location table. The Location
     * table is only limited to one record. Therefore the given location will always replace
     * the old location in the database.
     * @param loc the new location to be inserted into the database Location table
     */
    public synchronized void setLastKnownLocation(Location loc) {

        SQLiteDatabase db = DatabaseManager.getInstance().open();

        // Delete all entries
        db.delete(DatabaseHelper.TABLE_NAME_LOCATION, null, null);

        ContentValues location = new ContentValues();
        location.put(DatabaseHelper.COLUMN_LOCATION_ADDRESS, getAddress(loc));
        location.put(DatabaseHelper.COLUMN_LOCATION_LATITUDE, loc.getLatitude());
        location.put(DatabaseHelper.COLUMN_LOCATION_LONGITUDE, loc.getLongitude());

        // Insert the new location into the database
        db.insert(DatabaseHelper.TABLE_NAME_LOCATION, null, location);

        DatabaseManager.getInstance().close();
    }

    /**
     * Get the last known location from the database Location table.
     * @return the last known location saved on the database Location table if the table has any
     *         records in it, otherwise return the default location
     */
    public String[] getLastKnownLocation() {

        SQLiteDatabase db = DatabaseManager.getInstance().open();

        // Set default location
        String[] location = {DatabaseHelper.DEFAULT_LOCATION_ADDRESS,
                Double.toString(DatabaseHelper.DEFAULT_LOCATION_LATITUDE),
                Double.toString(DatabaseHelper.DEFAULT_LOCATION_LONGITUDE)};

        Cursor cursor = db.query(DatabaseHelper.TABLE_NAME_LOCATION, new String[] { "*" },
                null, null, null, null, null, null);

        if (cursor != null) {
            if (cursor.moveToFirst()) {

                location = new String[3];

                location[0] = cursor.getString(1);      // Address
                location[1] = cursor.getString(2);      // Latitude
                location[2] = cursor.getString(3);      // Longitude
            }

            cursor.close();
        }

        DatabaseManager.getInstance().close();

        return location;
    }

    /**
     * Get the address from the given location. The result string consist of address, city, state,
     * postal code, and country name with each separated by a comma.
     * @param loc location
     * @return a string that consist of address, city, state, postal code, and country name with
     *         each separated by a comma
     */
    private String getAddress(Location loc) {

        String addr = "";

        Geocoder geocoder;
        List<Address> addresses = null;
        geocoder = new Geocoder(mMainActivity, Locale.getDefault());

        try {
            addresses = geocoder.getFromLocation(loc.getLatitude(), loc.getLongitude(), 1);
        } catch (IOException e) {
            // Unable to get the address, so we do nothing
        }

        if (addresses != null && addresses.size() > 0) {

            Address fullAddress = addresses.get(0);

            String address = fullAddress.getAddressLine(0) == null? "" : addresses.get(0).getAddressLine(0);
            String city = fullAddress.getAddressLine(1) == null ? "" : getCity(addresses.get(0).getAddressLine(1));
            String postalCode = fullAddress.getPostalCode() == null ? "" : addresses.get(0).getPostalCode().split(" ")[0];
            String country = fullAddress.getCountryName() == null ? "" : addresses.get(0).getCountryName();

            addr = address + ", " + city + ", " + postalCode + ", " + country;
        }

        return addr;
    }

    /**
     * Get the city name from the given address.
     * @param address the address
     * @return the city name from the given address
     */
    private String getCity(String address) {

        String addr = address;

        if (address.length() > 0 && Character.isDigit(address.charAt(address.length()-1))) {

            for (int i = address.length() - 1; i >= 0; i--) {
                if (address.charAt(i) == ' ') {
                    addr = address.substring(0, i);
                    break;
                }
            }
        }

        return addr;
    }

    @Override
    public void onLocationChanged(Location location) {

        if (keepTrack) {

            mLocation = location;

            if (mMainActivity.isMapLoaded())
                // Draw on map
                mMainActivity.placeMarkerOnMap(mLocation, true);

            // Save to database
            setLastKnownLocation(mLocation);
        }
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {

    }

    @Override
    public void onProviderEnabled(String provider) {
        canGetLocation = true;
    }

    @Override
    public void onProviderDisabled(String provider) {

    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
