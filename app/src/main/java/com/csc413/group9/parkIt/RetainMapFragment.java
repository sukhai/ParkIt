package com.csc413.group9.parkIt;

import android.os.Bundle;

import com.google.android.gms.maps.SupportMapFragment;

/**
 * A class that retain the SupportMapFragment, which can retain its state when the device's state
 * is changed, such as change of orientation, HOME button is clicked, etc.
 *
 * Created by Su Khai Koh on 5/7/15.
 */
public class RetainMapFragment extends SupportMapFragment {

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {

        super.onActivityCreated(savedInstanceState);
        setRetainInstance(true);
    }
}
