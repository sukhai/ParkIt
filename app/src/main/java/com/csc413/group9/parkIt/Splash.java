package com.csc413.group9.parkIt;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
/**
 * Created by khanh on 5/11/15.
 */
public class Splash extends MainActivity {
    /** Loading Waiting Time **/
    private final int SPLASH_DISPLAY_LENGTH = 4000;

    /** Called when Main activity is first created **/

    @Override
    public void onCreate(Bundle saveInstanceState){
        super.onCreate(saveInstanceState);
        setContentView(R.layout.splash);

        /* New Handler to start the Main Activity
        and close the splash screen after a set time in seconds
         */
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                /* Create an Intent that will start the Main Activity.*/
                Intent mainIntent = new Intent(Splash.this, MainActivity.class);
                Splash.this.startActivity(mainIntent);
                Splash.this.finish();
            }
        }, SPLASH_DISPLAY_LENGTH);
    }
}
