
package com.csc413.group9.parkIt.Features;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.TimerTask;

import java.util.Timer;
/*
import android.app.Activity;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.widget.TextView;

/**
 * Created by SaiKrishna on 4/24/2015.
 */
/*public class WarningTimer extends Activity {

        TextView tv; //textview to display the countdown

        /** Called when the activity is first created. */
       /* @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            tv = new TextView(this);
            this.setContentView(tv);

            //5000 is the starting number (in milliseconds)
            //1000 is the number to count down each time (in milliseconds)
            MyCount counter = new MyCount(5000,1000);

            counter.start();

        }

        //countdowntimer is an abstract class, so extend it and fill in methods
        public class MyCount extends CountDownTimer {

            public MyCount(long millisInFuture, long countDownInterval) {
                super(millisInFuture, countDownInterval);
            }

            @Override
            public void onFinish() {
                tv.setText("done!");
            }

            @Override
            public void onTick(long millisUntilFinished) {
                tv.setText("Left: " + millisUntilFinished/1000);

            }

        }
    }

*/


    //import java.util.TimerTask;

class WarningTimer extends TimerTask {

    Timer timer;
    TimerTask timerTask;

    //we are going to use a handler to be able to run in our TimerTask
    final Handler handler = new Handler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    @Override
    protected void onResume() {
        super.onResume();

        //onResume we start our timer so it can start when the app comes from the background
        startTimer();
    }

    public void startTimer() {
        //set a new Timer
        timer = new Timer();

        //initialize the TimerTask's job
        initializeTimerTask();

        //schedule the timer, after the first 5000ms the TimerTask will run every 10000ms
        timer.schedule(timerTask, 5000, 10000); //
    }

    public void stoptimertask(View v) {
        //stop the timer, if it's not already null
        if (timer != null) {
            timer.cancel();
            timer = null;
        }
    }

    public void initializeTimerTask() {

        timerTask = new TimerTask() {
            public void run() {

                //use a handler to run a toast that shows the current timestamp
                handler.post(new Runnable() {
                    public void run() {
                        //get the current timeStamp
                        Calendar calendar = Calendar.getInstance();
                        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("dd:MMMM:yyyy HH:mm:ss a");
                        final String strDate = simpleDateFormat.format(calendar.getTime());

                        //show the toast
                        int duration = Toast.LENGTH_SHORT;
                        Toast toast = Toast.makeText(getApplicationContext(), strDate, duration);
                        toast.show();
                    }
                });
            }
        };
    }


    }

