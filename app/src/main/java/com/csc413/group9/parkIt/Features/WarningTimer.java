
package com.csc413.group9.parkIt.Features;

import android.content.Context;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.Button;
import android.widget.PopupWindow;
import android.widget.TimePicker;

import com.csc413.group9.parkIt.MainActivity;
import com.csc413.group9.parkIt.R;

/**
 * Created by SaiKrishna on 4/24/2015.
 */
public class WarningTimer {

    private View timerView;
    private PopupWindow popupWindow;
    private TimePicker timePicker;
    private Button buttonOk;
    private Button buttonCancel;
    private MainActivity mainActivity;

    private int hour;
    private int minute;

    public WarningTimer(MainActivity activity) {

        mainActivity = activity;

        hour = 23;
        minute = 0;

        addButtonListeners();
    }

    public void setWarningTimer() {

        setTime();

        startAlarmManager();

        showInfo();
    }

    private void addButtonListeners() {

        final LayoutInflater factory = mainActivity.getLayoutInflater();
        timerView = factory.inflate(R.layout.window_warning_timer, null);

        timePicker = (TimePicker) timerView.findViewById(R.id.timepicker_warning_timer);
        timePicker.setIs24HourView(true);
    }

    private void setTime() {

        /*
        need to get the current selected time instead
         */
        hour = timePicker.getCurrentHour();
        minute = timePicker.getCurrentMinute();
    }

    private void startAlarmManager() {

        /*
        need to implement a way to push the notification at the specified time (hour, minute)
         */
    }

    public void showWindow() {

        if (popupWindow == null) {
            final LayoutInflater layoutInflater = (LayoutInflater) mainActivity.getBaseContext()
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);

            timerView = layoutInflater.inflate(R.layout.window_warning_timer, null);

            popupWindow = new PopupWindow(timerView,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    LayoutParams.WRAP_CONTENT);
        }

        popupWindow.showAtLocation(timerView, Gravity.CENTER, 0, 0);
    }




    private void showInfo() {
        System.err.println("Hour: " + hour);
        System.err.println("Minute: " + minute);
    }
}

