
package com.csc413.group9.parkIt.Features;
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

        @Override

        public void run() {

            System.out.println("Start time:" + new Date());

            doSomeWork();

            System.out.println("End time:" + new Date());

        }



                // simulate a time consuming task

        private void doSomeWork() {

            try {


                Thread.sleep(10000);

            } catch (InterruptedException e) {

                e.printStackTrace();

            }

        }



        public static void main(String args[]) {



            TimerTask timerTask = new WarningTimer();

            // running timer task as daemon thread

            Timer timer = new Timer(true);

            timer.scheduleAtFixedRate(timerTask, 0, 10 * 1000);

            System.out.println("TimerTask begins! :" + new Date());

            // cancel after sometime

            try {

                Thread.sleep(20000);

            } catch (InterruptedException e) {

                e.printStackTrace();

            }

            timer.cancel();

            System.out.println("TimerTask cancelled! :" + new Date());

            try {

                               Thread.sleep(30000);

            } catch (InterruptedException e) {

                e.printStackTrace();

            }

        }



    }

