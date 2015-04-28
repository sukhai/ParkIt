package com.csc413.group9.parkIt.Features.WarningTimer;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;

import java.util.Calendar;

/**
 * A scheduler that scheduler a time to send notification to the user.
 *
 * Created by Su Khai Koh on 4/26/15.
 */
public class TimerNotificationScheduler extends Service {

    /**
     * An object that receives interactions from WarningTimer.
     */
    private final IBinder mIBinder = new ScheduleService();

    @Override
    public IBinder onBind(Intent intent) {
        return mIBinder;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // Make this service to continue running until it is explicitly stopped
        return START_STICKY;
    }

    /**
     * Set the schedule to send a timer notification.
     * @param calendar the time to send the notification
     */
    public void setNotificationSchedule(Calendar calendar) {
        // Start a new thread to set the alarm
        new TimerNotification(this, calendar).run();
    }

    /**
     * The schedule service.
     */
    public class ScheduleService extends Binder {
        TimerNotificationScheduler getService() {
            return TimerNotificationScheduler.this;
        }
    }

    /**
     * The timer notification that runs on different thread.
     */
    public class TimerNotification implements Runnable {

        /**
         * The date (time) to send the notification.
         */
        private final Calendar mDate;

        /**
         * The system alarm manager.
         */
        private final AlarmManager mAlarmManager;

        /**
         * The context of this application
         */
        private final Context mContext;

        /**
         * Setup class members.
         * @param context the context of this application
         * @param date the date to send a notification
         */
        public TimerNotification(Context context, Calendar date) {
            mContext = context;
            mDate = date;
            mAlarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        }

        @Override
        public void run() {

            // Set an alarm that will push a notification to the notification bar
            // when the date has arrived
            Intent intent = new Intent(mContext, TimerNotificationService.class);
            intent.putExtra(TimerNotificationService.INTENT_NOTIFY, true);
            intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            PendingIntent pendingIntent = PendingIntent.getService(mContext, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);

            // Set the alarm
            mAlarmManager.set(AlarmManager.RTC, mDate.getTimeInMillis(), pendingIntent);
        }
    }
}
