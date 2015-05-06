package com.csc413.group9.parkIt.Features.Search;

import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.location.Address;
import android.location.Geocoder;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;

import com.csc413.group9.parkIt.Database.DatabaseHelper;
import com.csc413.group9.parkIt.Database.DatabaseManager;
import com.csc413.group9.parkIt.MainActivity;
import com.csc413.group9.parkIt.R;
import com.google.android.gms.maps.model.LatLng;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Created by kshen on 4/24/15.
 */
public class Search {

    private MainActivity mMainActivity;

    //Variables for new updated Search bar
    private AutoCompleteTextView actv;
    private String[] places;
    private Set<String> set;
    private SharedPreferences storage;
    private final String SEARCH_KEY = "key";

    public Search(MainActivity mainActivity) {
        mMainActivity = mainActivity;

        storage = mMainActivity.getPreferences(Context.MODE_PRIVATE);
        actv = (AutoCompleteTextView) mMainActivity.findViewById(R.id.searchLocation);
        setAutoList();
    }

    private void setAutoList() {
        //set = new HashSet<String>();
        SharedPreferences.Editor edit = storage.edit();
        set = storage.getStringSet(SEARCH_KEY, null);
        if(set != null){
            Object[] objs = set.toArray();
            places = new String[objs.length];
            for(int i = 0; i < places.length; i++){
                places[i] = (String) objs[i];

            }
        }else{
            places = new String[]{""};
            set = new HashSet<String>();
        }
    }

    public void addAddressToSearchButtons(ArrayList<Button> buttons) {

        if (buttons == null) {
            return;
        }

        SQLiteDatabase db = DatabaseManager.getInstance().open();

        // Get the list of recent search data in the descending order, so the newest address
        // is the first data in the list
        Cursor cursor = db.query(DatabaseHelper.TABLE_NAME_RECENT, new String[] { "*" },
                null, null, null, null, null, DatabaseHelper.COLUMN_ID + " DESC");

        int i = 0;

        if (cursor != null) {
            while (cursor.moveToFirst()) {
                String storedAddress = cursor.getString(1);

                buttons.get(i).setText(storedAddress);
            }

            cursor.close();
        }

        DatabaseManager.getInstance().close();
    }

    public LatLng getLatLng(String address) {

        if (address == null || address.equals("")) {
            return null;
        }

        LatLng value = null;

        SQLiteDatabase db = DatabaseManager.getInstance().open();

        // Get the list of recent search data in the descending order, so the newest address
        // is the first data in the list
        Cursor cursor = db.query(DatabaseHelper.TABLE_NAME_RECENT, new String[] { "*" },
                null, null, null, null, null, DatabaseHelper.COLUMN_ID + " DESC");

        if (cursor != null) {
            while (cursor.moveToFirst()) {
                String storedAddress = cursor.getString(1);

                if (storedAddress.equalsIgnoreCase(address)) {
                    double latitude = cursor.getDouble(2);
                    double longitude = cursor.getDouble(3);
                    value = new LatLng(latitude, longitude);
                }
            }

            cursor.close();
        }

        DatabaseManager.getInstance().close();

        return value;
    }

    public void geoLocate(View v) throws IOException {

        hideKeyboard(v);

        boolean duplicated = false;
        AutoCompleteTextView actv = (AutoCompleteTextView) mMainActivity.findViewById(R.id.searchLocation);

        if (actv.getText() == null) {
            return;
        }

        String location = actv.getText().toString();

        for(String place: set){
            if (location.trim().equalsIgnoreCase(place))
                duplicated = true;
        }

        if(!duplicated){
            set.add(location);
            Object[] object = set.toArray();

            places = new String[object.length];
            for(int i = 0; i < places.length; i++){
                places[i] = (String) object[i];
            }

            SharedPreferences.Editor editor = storage.edit();
            editor.putStringSet(SEARCH_KEY, set);
            editor.commit();
            updateView();
        }

        Geocoder gc = new Geocoder(mMainActivity); //locate the location [Google Maps feature]

        //returns list of address from the literal location
        List<Address> addressList = gc.getFromLocationName(location, 1); //1 means gives you 1 address

        if (addressList == null || addressList.size() == 0) {
            return;
        }

        Address address = addressList.get(0); //that 1 address would be the first one from the list
        double lat = address.getLatitude(); //gets the latitude from the address
        double log = address.getLongitude(); //gets the longtitude from the address

        goToLocation(location, lat, log); //go to that location
    }

    private void updateView() {
        ArrayAdapter adapter = new ArrayAdapter(mMainActivity, android.R.layout.simple_list_item_1, places);
        actv.setAdapter(adapter);
    }

    //hides keyboard
    private void hideKeyboard(View v) {
        InputMethodManager imm = (InputMethodManager) mMainActivity.getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
    }

    //class that go to the searched location
    private void goToLocation(String address, double latitude, double longitude) {

    //    setLastSearchedLocation(address, latitude, longitude);

        LatLng mCurrentLatLng = new LatLng(latitude, longitude);

        mMainActivity.placeMarker(mCurrentLatLng);
    }

    private void setLastSearchedLocation(String address, double latitude, double longitude) {

        SQLiteDatabase db = DatabaseManager.getInstance().open();

        // Delete old entries
        db.execSQL(DatabaseHelper.SQL_DELETE_RECENT_ENTRIES);

        ContentValues location = new ContentValues();
        location.put(DatabaseHelper.COLUMN_RECENT_ADDRESS, address);
        location.put(DatabaseHelper.COLUMN_RECENT_LATITUDE, latitude);
        location.put(DatabaseHelper.COLUMN_RECENT_LONGITUDE, longitude);

        // Insert the new location into the database
        db.insert(DatabaseHelper.TABLE_NAME_RECENT, null, location);

        DatabaseManager.getInstance().close();
    }
}