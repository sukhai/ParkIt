
package com.csc413.group9.parkIt.Features.WarningTimer;

import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.IBinder;

import com.csc413.group9.parkIt.Database.DatabaseHelper;
import com.csc413.group9.parkIt.Database.DatabaseManager;
import com.csc413.group9.parkIt.MainActivity;
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
     * A reference to the MainActivity class.
     */
    private MainActivity mMainActivity;

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
     * The address on the map when this warning timer is set.
     */
    private String mAddress;

    /**
     * The location on the map when this warning timer is set.
     */
    private LatLng mLocation;

    /**
     * Setup all class members and the context of this application.
     * @param mainActivity the MainActivity class
     */
    public WarningTimer(MainActivity mainActivity) {

        mMainActivity = mainActivity;

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
        Intent intent = new Intent(mMainActivity, TimerNotificationScheduler.class);
        mMainActivity.bindService(intent, mServiceConnection, Context.BIND_AUTO_CREATE);
        mIsBound = true;
    }

    /**
     * Unbind the activity from the service.
     */
    public void unBindService() {

        // Unbind the connection from our service
        if (mIsBound) {
            mMainActivity.unbindService(mServiceConnection);
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
     * Set the parked address and location, which is the address and location when this warning
     * timer is set.
     * @param location the location to be set
     */
    public void setParkedLocation(String address, LatLng location) {
        mAddress = address;
        mLocation = location;
    }

    /**
     * Set the warning timer and start scheduling when the notification should be pushed to the
     * system.
     */
    public void setWarningTime() {

        if (mHour == 0 && mMinute == 0)
            return;

        storeLocationToDatabase();

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
     * Go to the parked location. The parked location is the latitude and longitude set by
     * setParkedLocation(...).
     */
    public void goToParkedLocation() {

        SQLiteDatabase db = DatabaseManager.getInstance().open();

        Cursor cursor = db.query(DatabaseHelper.TABLE_NAME_PARKED, new String[]{"*"},
                null, null, null, null, null, null);

        if (cursor != null) {
            if (cursor.moveToFirst()) {
                double latitude = cursor.getDouble(cursor.getColumnIndex(DatabaseHelper.COLUMN_PARKED_LATITUDE));
                double longitude = cursor.getDouble(cursor.getColumnIndex(DatabaseHelper.COLUMN_PARKED_LONGITUDE));
                mMainActivity.placeMarker(new LatLng(latitude, longitude));
            }

            cursor.close();
        }

        DatabaseManager.getInstance().close();
    }

    /**
     * Store the address, latitude, and longitude of the parked location to the database. The
     * address, latitude, and longitude are gathered when this warning timer is set.
     * The table for this parked location will only contains 1 row of data, that is, when a new
     * data is storing into this table, all the old data will be removed, and only the newest
     * data will remain in the table.
     */
    private void storeLocationToDatabase() {

        SQLiteDatabase db = DatabaseManager.getInstance().open();

        // Delete all entries
        db.delete(DatabaseHelper.TABLE_NAME_PARKED, null, null);

        ContentValues parkedLocation = new ContentValues();
        parkedLocation.put(DatabaseHelper.COLUMN_PARKED_ADDRESS, mAddress);
        parkedLocation.put(DatabaseHelper.COLUMN_PARKED_LATITUDE, mLocation.latitude);
        parkedLocation.put(DatabaseHelper.COLUMN_PARKED_LONGITUDE, mLocation.longitude);

        // Insert the new location into the database
        db.insert(DatabaseHelper.TABLE_NAME_PARKED, null, parkedLocation);

        DatabaseManager.getInstance().close();
    }
}

