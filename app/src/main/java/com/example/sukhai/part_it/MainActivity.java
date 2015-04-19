package com.example.sukhai.part_it;

import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.example.sukhai.part_it.Database.DatabaseManager;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

public class MainActivity extends ActionBarActivity implements
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener {

    private static final float CAMERA_ZOOM_LEVEL = 18f;

    private GoogleMap mMap;
    private Marker mMarker;
    private GoogleApiClient mGoogleApiClient;
    private CurrentLocation mCurrentLocation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        if (!servicesAvailable()) {
            finish();
        }

        setContentView(R.layout.activity_main);

        DatabaseManager.initializeInstance(this);

        buildGoogleApiClient();

        mCurrentLocation = new CurrentLocation(this, mGoogleApiClient);
    }

    protected synchronized void buildGoogleApiClient() {

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (mGoogleApiClient != null) {
            mGoogleApiClient.connect();

            if (mCurrentLocation.mRequestingLocationUpdates)
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

        if (servicesAvailable())
            mCurrentLocation.onConnected(dataBundle);
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

    public void moveToCurrentLocation(View view) {
        mCurrentLocation.moveToCurrentLocation(view);
    }

    public void placeMarkerOnMap(LatLng point) {

        Toast.makeText(getApplicationContext(), point.latitude + ", " + point.longitude, Toast.LENGTH_SHORT).show();

        if (mMarker != null) {
            mMarker.remove();
        }

        // Do a null check to confirm that we have not already instantiated the map.
        if (mMap == null) {
            // Try to obtain the map from the SupportMapFragment.
            mMap = ((SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map))
                    .getMap();

            mMap.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
                @Override
                public void onMapClick(LatLng latLng) {
                    placeMarkerOnMap(latLng);
                }
            });
        }

        if (mMap != null) {
            mMarker = mMap.addMarker(new MarkerOptions()
                    .position(new LatLng(point.latitude, point.longitude))
                    .draggable(true)
                    .visible(true));

            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(mMarker.getPosition(), CAMERA_ZOOM_LEVEL));
        }
    }

    public boolean servicesAvailable() {

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
