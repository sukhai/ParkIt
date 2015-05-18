package com.csc413.group9.parkIt.Features.Search;

import android.content.Context;
import android.content.SharedPreferences;
import android.location.Address;
import android.location.Geocoder;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;

import com.csc413.group9.parkIt.MainActivity;
import com.csc413.group9.parkIt.R;
import com.google.android.gms.maps.model.LatLng;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * This class will handle the search functionality of the app.
 * 
 * Created by kshen on 4/24/15.
 */
public class Search {

    /**
     * The key to store in SharedPreferences.
     */
    private static final String SEARCH_KEY = "KEY";

    /**
     * A reference to the MainActivity.
     */
    private MainActivity mMainActivity;

    /**
     * A reference to the AutoCompleteTextView.
     */
    private AutoCompleteTextView mActv;

    /**
     * The places that have been searched previously.
     */
    private String[] mPlaces;

    /**
     * A Set table.
     */
    private Set<String> mSet;

    /**
     * A reference to the SharedPreferences storage.
     */
    private SharedPreferences mStorage;

    /**
     * Constructor that store a reference to the MainActivity and setup the SharedPreferences
     * storage.
     * @param mainActivity
     */
    public Search(MainActivity mainActivity) {
        mMainActivity = mainActivity;

        mStorage = mMainActivity.getPreferences(Context.MODE_PRIVATE);
        mActv = (AutoCompleteTextView) mMainActivity.findViewById(R.id.searchLocation);
        setAutoList();
    }

    /**
     * Locate the address on the map. The address is the address that typed into the text field.
     * @param v the view of the application
     * @throws IOException any IO exception
     */
    public void geoLocate(View v) throws IOException {

        // Hide the keyboard when the "Go" button is pressed
        hideKeyboard(v);

        boolean duplicated = false;
        AutoCompleteTextView mActv = (AutoCompleteTextView) mMainActivity.findViewById(R.id.searchLocation);

        if (mActv.getText() == null) {
            return;
        }

        String location = mActv.getText().toString();

        // Check whether the location has been searched previously
        for (String place: mSet) {
            if (location.trim().equalsIgnoreCase(place))
                duplicated = true;
        }

        // If the location has not been searched previously, then add it to the Set table and
        // the list
        if (!duplicated) {
            mSet.add(location);
            Object[] object = mSet.toArray();

            mPlaces = new String[object.length];
            for (int i = 0; i < mPlaces.length; i++) {
                mPlaces[i] = (String) object[i];
            }

            SharedPreferences.Editor editor = mStorage.edit();
            editor.putStringSet(SEARCH_KEY, mSet);
            editor.commit();
            updateView();
        }

        // Locate the location
        Geocoder gc = new Geocoder(mMainActivity);

        // Returns list of address from the literal location
        List<Address> addressList = gc.getFromLocationName(location, 1); // 1 means gives you 1 address

        if (addressList == null || addressList.size() == 0) {
            return;
        }

        Address address = addressList.get(0); // that 1 address would be the first one from the list
        double lat = address.getLatitude();   // gets the latitude from the address
        double log = address.getLongitude();  // gets the longitude from the address

        goToLocation(lat, log);     //go to that location
    }

    /**
     * Set the auto list.
     */
    private void setAutoList() {

        SharedPreferences.Editor edit = mStorage.edit();
        mSet = mStorage.getStringSet(SEARCH_KEY, null);

        if (mSet != null) {

            Object[] objs = mSet.toArray();
            mPlaces = new String[objs.length];

            for(int i = 0; i < mPlaces.length; i++){
                mPlaces[i] = (String) objs[i];
            }

        } else {
            mPlaces = new String[]{""};
            mSet = new HashSet<String>();
        }
    }

    /**
     * Update the view of AutoCompleteTextView to show the address.
     */
    private void updateView() {

        ArrayAdapter adapter = new ArrayAdapter(mMainActivity,
                android.R.layout.simple_list_item_1, mPlaces);

        mActv.setAdapter(adapter);
    }

    /**
     * Hide the keyboard.
     * @param v
     */
    private void hideKeyboard(View v) {

        InputMethodManager imm =
                (InputMethodManager) mMainActivity.getSystemService(Context.INPUT_METHOD_SERVICE);

        imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
    }

    /**
     * Go to the location based on the given latitude and longitude.
     * @param latitude the latitude of the location
     * @param longitude the longitude of the location
     */
    private void goToLocation(double latitude, double longitude) {

        LatLng mCurrentLatLng = new LatLng(latitude, longitude);

        mMainActivity.placeMarker(mCurrentLatLng);
    }
}