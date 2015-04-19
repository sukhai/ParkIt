package com.example.sukhai.part_it;

import com.example.sukhai.part_it.Database.DatabaseHelper;
import com.example.sukhai.part_it.Database.DatabaseManager;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.model.LatLng;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.view.View;

import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Created by Su Khai Koh on 4/17/15.
 */
public class CurrentLocation implements LocationListener,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener {

    static final long TIME_ONE_MIN = 1000 * 60;
    static final long TIME_TWO_MIN = TIME_ONE_MIN * 2;
    static final long TIME_FIVE_MIN = TIME_ONE_MIN * 5;
    static final long FREQUENCY_POLLING = 1000 * 30;
    static final long FREQUENCY_FASTEST_UPDATE = 1000 * 5;
    static final float ACCURACY_MIN = 25.0f;
    static final float ACCURACY_LAST_READ = 500.0f;
    static final float CAMERA_ZOOM_LEVEL = 18f;

    private MainActivity mMainActivity;
    private GoogleApiClient mGoogleApiClient;
    private LocationRequest mLocationRequest;
    private Location mLocation;
    private DatabaseManager mDatabaseManager;

    protected boolean mRequestingLocationUpdates;

    public CurrentLocation(MainActivity mainActivity, GoogleApiClient googleApiClient) {

        mRequestingLocationUpdates = true;

        mMainActivity = mainActivity;
        mGoogleApiClient = googleApiClient;

        mDatabaseManager = DatabaseManager.getInstance();

        if (mDatabaseManager.isEmpty(DatabaseHelper.TABLE_NAME_LOCATION)) {

            Location location = new Location(DatabaseHelper.DEFAULT_LOCATION_ADDRESS);
            location.setLatitude(DatabaseHelper.DEFAULT_LOCATION_LATITUDE);
            location.setLongitude(DatabaseHelper.DEFAULT_LOCATION_LONGITUDE);

            setLastKnownLocation(location);
        }

        createLocationRequest();
    }

    protected void createLocationRequest() {

        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(FREQUENCY_POLLING);
        mLocationRequest.setFastestInterval(FREQUENCY_FASTEST_UPDATE);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    }

    @Override
    public void onLocationChanged(Location location) {

        // Determine whether the new location is better than the current best estimate
        if (mLocation == null || mLocation.getAccuracy() > location.getAccuracy()) {

            mLocation = location;

            mMainActivity.placeMarkerOnMap(new LatLng(location.getLatitude(), location.getLongitude()));

            setLastKnownLocation(mLocation);

            if (mLocation.getAccuracy() < ACCURACY_MIN) {
                LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
            }
        }
    }

    @Override
    public void onConnected(Bundle bundle) {

        boolean locationAvailable = false;

        // Get first reading. Get additional location updates if necessary
        if (mRequestingLocationUpdates && mMainActivity.servicesAvailable()) {
            // Get best last location measurement meeting criteria
            mLocation = bestLastKnownLocation(ACCURACY_LAST_READ, TIME_FIVE_MIN);

            if (mLocation == null
                    || mLocation.getAccuracy() > ACCURACY_LAST_READ
                    || mLocation.getTime() < System.currentTimeMillis() - TIME_TWO_MIN) {

                startLocationUpdates();

                // Schedule a runnable to unregister location listeners
                Executors.newScheduledThreadPool(1).schedule(new Runnable() {

                    @Override
                    public void run() {
                        stopLocationUpdates();
                    }

                }, TIME_ONE_MIN, TimeUnit.MILLISECONDS);
            }

            // Place a marker on the map and store the new location if we have one
            if (mLocation != null) {
                mMainActivity.placeMarkerOnMap(new LatLng(mLocation.getLatitude(), mLocation.getLongitude()));
                locationAvailable = true;

                setLastKnownLocation(mLocation);
            }
        }

        if (!locationAvailable) {
            moveToCurrentLocation(null);
        }
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {

    }

    protected void startLocationUpdates() {

        if (mGoogleApiClient.isConnected())
            LocationServices.FusedLocationApi.requestLocationUpdates(
                    mGoogleApiClient, mLocationRequest, this);
    }

    protected void stopLocationUpdates() {

        LocationServices.FusedLocationApi.removeLocationUpdates(
                mGoogleApiClient, this);
    }

    public void moveToCurrentLocation(View view) {

        System.out.println("in move to current location");

        Location mLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);

        if (mLocation != null) {

            mMainActivity.placeMarkerOnMap(new LatLng(mLocation.getLatitude(), mLocation.getLongitude()));

            System.out.println("mLocation is not null");

            setLastKnownLocation(mLocation);

        } else {

            System.out.println("mLocation is null");

            String[] location = getLastKnownLocation();

            double latitude = Double.parseDouble(location[1]);
            double longitude = Double.parseDouble(location[2]);

            mMainActivity.placeMarkerOnMap(new LatLng(latitude, longitude));
        }
    }

    public synchronized void setLastKnownLocation(Location loc) {

        SQLiteDatabase db = mDatabaseManager.getInstance().open();

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

    private Location bestLastKnownLocation(float minAccuracy, long minTime) {

        Location bestResult = null;
        float bestAccuracy = Float.MAX_VALUE;
        long bestTime = Long.MIN_VALUE;

        // Get the best most recent location currently available
        Location currentLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);

        if (currentLocation != null) {
            float accuracy = currentLocation.getAccuracy();
            long time = currentLocation.getTime();

            if (accuracy < bestAccuracy) {
                bestResult = currentLocation;
                bestAccuracy = accuracy;
                bestTime = time;
            }
        }

        // Return best reading or null
        if (bestAccuracy > minAccuracy || bestTime < minTime) {
            return null;
        }
        else {

            setLastKnownLocation(bestResult);

            return bestResult;
        }
    }

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
            String address = addresses.get(0).getAddressLine(0);
            String city = getCity(addresses.get(0).getAddressLine(1));
            String postalCode = addresses.get(0).getPostalCode().split(" ")[0];
            String country = addresses.get(0).getCountryName();

            addr = address + ", " + city + ", " + postalCode + ", " + country;
        }

        return addr;
    }

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
}
