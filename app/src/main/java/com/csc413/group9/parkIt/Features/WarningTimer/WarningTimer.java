
package com.csc413.group9.parkIt.Features.WarningTimer;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;

import com.google.android.gms.maps.model.LatLng;

import java.util.Calendar;

/**
 * A feature class that let the user set a timer that sends a notification to the user when the
 * time is up.
 *
 * Created by SaiKrishna on 4/24/2015.
 *
 * Modified by Su Khai Koh on 4/26/2015.
 */
public class WarningTimer {

    /**
     * The scheduler that set timer notification.
     */
    private TimerNotificationScheduler mScheduler;

    /**
     * The context of this application.
     */
    private Context mContext;

    /**
     * Determine if the service is bounded to the connection.
     */
    private boolean mIsBound;

    /**
     * The connection that connects the service with the system notification.
     */
    private ServiceConnection mServiceConnection;

    /**
     * The selected hour.
     */
    private int mHour;

    /**
     * The selected minute.
     */
    private int mMinute;

    /**
     * The location on the map when this warning timer is set.
     */
    private LatLng mLocation;

    /**
     * Setup all class members and the context of this application.
     * @param context the context of this application
     */
    public WarningTimer(Context context) {

        mContext = context;

        // Setup the connection to the system notification service
        mServiceConnection = new ServiceConnection() {

            public void onServiceConnected(ComponentName className, IBinder service) {
                mScheduler = ((TimerNotificationScheduler.ScheduleService) service).getService();
            }

            public void onServiceDisconnected(ComponentName className) {
                mScheduler = null;
            }
        };
    }

    /**
     * Bind the activity to the service.
     */
    public void bindService() {

        // Create an intent of the TimerNotificationScheduler and bind it to our service
        Intent intent = new Intent(mContext, TimerNotificationScheduler.class);
        mContext.bindService(intent, mServiceConnection, Context.BIND_AUTO_CREATE);
        mIsBound = true;
    }

    /**
     * Unbind the activity from the service.
     */
    public void unBindService() {

        // Unbind the connection from our service
        if (mIsBound) {
            mContext.unbindService(mServiceConnection);
            mIsBound = false;
        }
    }

    /**
     * Set the time.
     * @param hour the hour of the time
     * @param minute the minute of the time
     */
    public void setTime(int hour, int minute) {
        mHour = hour;
        mMinute = minute;
    }

    /**
     * Set the warning timer and start scheduling when the notification should be pushed to the
     * system.
     */
    public void setWarningTime() {

        if (mHour == 0 && mMinute == 0)
            return;

        // Get the current date and time
        Calendar calender = Calendar.getInstance();

        // Set the specified hour and minute future time and
        // schedule a notification for this time
        calender.add(Calendar.HOUR, mHour);
        calender.add(Calendar.MINUTE, mMinute);
        mScheduler.setNotificationSchedule(calender);

        // Set and schedule another notification that is 15 minutes before the time
        calender.add(Calendar.MINUTE, -15);

        // Don't schedule a notification if the time has already passed
        if (calender.getTimeInMillis() < Calendar.getInstance().getTimeInMillis())
            return;

        mScheduler.setNotificationSchedule(calender);
    }

    /**
     * Set the location when this warning timer is set.
     * @param location the location to be set
     */
    public void setLocation(LatLng location) {
        mLocation = location;

        // maybe need to store in SharedPreferences
    }
}

