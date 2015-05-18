package com.csc413.group9.parkIt;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;

import com.csc413.group9.parkIt.SFPark.ParkingInformation;
import com.google.android.gms.maps.GoogleMap;

/**
 * Create a splash screen when the app just started. When this splash screen is shown, the app
 * will also fetch SFPark data at the back, so when the SFPark data will be ready to draw on the
 * map when this splash screen is gone.
 */
public class SplashActivity extends Activity {

    /**
     * The time to show this splash screen.
     */
    private final int SPLASH_LENGTH = 5000;

    /**
     * A reference to the ParkingInformation.
     */
    private ParkingInformation mParkingInfo;

    /**
     * A reference to the Google Map.
     */
    private GoogleMap mMap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        // Load SFPark data
        loadData();

        // Timer for the Splash screen
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                Intent mainIntent = new Intent(SplashActivity.this, MainActivity.class);
                SplashActivity.this.startActivity(mainIntent);
                SplashActivity.this.finish();
            }
        }, SPLASH_LENGTH);
    }

    /**
     * Fetch and load the SFPark data from the SFPark website.
     */
    private void loadData() {
        mParkingInfo = new ParkingInformation(this, mMap);

        mParkingInfo.getSFParkData();
    }
}