package com.csc413.group9.parkIt.SFPark;

import android.app.Fragment;
import android.os.Bundle;

import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.Polyline;

import java.util.ArrayList;

/**
 * A class that contains a list of on-street parking and off-street parking data from SFPark. The
 * data in this class will be retained when the device's state is changed, such as change of
 * device's orientation, HOME button is clicked, etc.
 *
 * Created by Su Khai Koh on 5/7/15.
 */
public class ParkingMarkers extends Fragment {

    public static final String TAG_PARKING_MARKERS = "ParkIt.ParkingInformation.ParkingMarkers";

    /**
     * A list of on-street parking location icons, which are Polylines on the Google map.
     */
    private ArrayList<Polyline> onStreetParkingMarkers;

    /**
     * A list of off-street parking location icons, which are Markers on the Google map.
     */
    private ArrayList<Marker> offStreetParkingMarkers;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // retain this fragment
        setRetainInstance(true);
    }

    public void addOnStreetParkingMarker(Polyline marker) {

        if (onStreetParkingMarkers == null) {
            onStreetParkingMarkers = new ArrayList<>();
        }

        onStreetParkingMarkers.add(marker);
    }

    public ArrayList<Polyline> getOnStreetParkingMarkers() {
        return onStreetParkingMarkers;
    }

    public void addOffStreetParkingMarker(Marker marker) {

        if (offStreetParkingMarkers == null) {
            offStreetParkingMarkers = new ArrayList<>();
        }

        offStreetParkingMarkers.add(marker);
    }

    public ArrayList<Marker> getOffStreetParkingMarkers() {
        return offStreetParkingMarkers;
    }
}
