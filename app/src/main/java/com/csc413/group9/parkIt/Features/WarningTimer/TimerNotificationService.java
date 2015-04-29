package com.csc413.group9.parkIt.Features.WarningTimer;

import android.R;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;

import com.csc413.group9.parkIt.MainActivity;
import com.google.android.gms.maps.model.LatLng;

/**
 * This is the notification service when the time has arrived and this notification will be shown
 * on the notification bar of the device. This notification will bring up the main activity when
 * it is clicked by the user.
 *
 * Created by Su Khai Koh on 4/26/15.
 */
public class TimerNotificationService extends Service {

    /**
     * Name of an intent extra, which is to identify if this service was started to create a
     * notification.
     */
    public static final String INTENT_NOTIFY = "com.csc413.group9.parkIt.Features.WarningTimer.INTENT_NOTIFY";

    /**
     * An unique ID for this notification.
     */
    private static final int NOTIFICATION_ID = 413413;

    /**
     * An object that receives interactions from TimerNotification.
     */
    private final IBinder mBinder = new ServiceBinder();

    /**
     * The system notification manager.
     */
    private NotificationManager mNotificationManager;

    /**
     * The location to be shown on the notification.
     */
    private LatLng location;

    @Override
    public void onCreate() {
        mNotificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

        // need a way to get location and store in this.location
        // probably use https://developer.android.com/training/basics/data-storage/shared-preferences.html method
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        // Show notification if this service was started by TimerNotification
        if(intent.getBooleanExtra(INTENT_NOTIFY, false))
            showNotification();

        return START_NOT_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    /**
     * Create a notification that will be shown on the notification bar of the device.
     */
    private void showNotification() {

        // Launch MainActivity if the user clicked on this notification
        PendingIntent contentIntent = PendingIntent.getActivity(this, 0, new Intent(this, MainActivity.class), 0);

        Notification notification = new NotificationCompat.Builder(this)
                .setContentTitle("Time is running out!")
                .setContentText("")
                .setWhen(System.currentTimeMillis())
                .setContentIntent(contentIntent)
                .setSmallIcon(R.drawable.ic_dialog_alert)
                .setAutoCancel(true)
                .setDefaults(Notification.DEFAULT_SOUND)
                .build();

        // Send the notification to the system.
        mNotificationManager.notify(NOTIFICATION_ID, notification);

        // Stop the service when we are finished
        stopSelf();
    }

    /**
     * The service binder.
     */
    public class ServiceBinder extends Binder {
        TimerNotificationService getService() {
            return TimerNotificationService.this;
        }
    }
}
