package com.csc413.group9.parkIt.SFPark;

import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.app.ProgressDialog;
import android.content.res.AssetManager;
import android.graphics.Color;
import android.location.Location;
import android.os.AsyncTask;
import android.text.format.DateUtils;
import android.util.Log;

import com.csc413.group9.parkIt.MainActivity;
import com.csc413.group9.parkIt.R;
import com.csc413.group9.parkIt.SplashActivity;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

import org.apache.http.Header;
import org.apache.http.HeaderElement;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpResponseInterceptor;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.entity.HttpEntityWrapper;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.HttpContext;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.zip.GZIPInputStream;

/**
 * This class handles the parking information around the device's current location. This class will
 * load the data from the SFPark and display it on the Google map.
 *
 * Created by Su Khai Koh on 4/19/15.
 */
public class ParkingInformation {

    /**
     * The SFPark URL that contains the parking data this application needs.
     */
    private static final String SERVICE_URL = "http://api.sfpark.org/sfpark/rest/availabilityservice?radius=1000.0&response=json&version=1.0&pricing=yes";

    /**
     * SFSU URL parking lots containing parking data this application needs.
     */
    //private static final String google_URL = "https://docs.google.com/document/d/15QUYceBcLUVLk398dTuCuiHv98Nq32YT48pcYT-iUMg/edit";
    private static final String GPS_Path = "/GPS.txt";
    /**
     * content of SFSU Parking URL
     */
    private static String fileContent;
    /**
     * A reference to the MainActivity.
     */
    private MainActivity mMainActivity;

    /**
     * A reference to the Google map.
     */
    private GoogleMap mMap;

    /**
     * The content of the SFPark URL.
     */

    //Changed to static (only create one copy and share between activities)
    private static String uriContent;

    /**
     * A reference to the parking markers, which contains a list of on-street parking markers
     * and a list of off-street parking markers.
     */
    private ParkingMarkers parkingMarkers;

    /**
     *Flag for the SFSU Park data
     */
    private boolean sfsuParkDataReady = false;

    /**
     * Flag for the SFPark data readiness. Default to false (not ready).
     */
    private boolean sfParkDataReady = false;

    /**
     * Flag for whether the on-street parking has already been highlighted.
     */
    private boolean highlightedOnStreetParking;

    /**
     * Flag for whether the off-street parking (garage parking) has already been highlighted.
     */
    private boolean highlightedOffStreetParking;

    //Splash activity object
    private SplashActivity mSplashActivity;

    //Parking list for sf and sfsu
    private static List<ParkingLocation> sfParkingList = new ArrayList<>();
    private static List<ParkingLocation> sfsuParkingList = new ArrayList<>();


    /**
     * Constructor that setup the class members and references.
     * @param mainActivity a reference to the MainActivity
     * @param map the Google map
     */
    public ParkingInformation(MainActivity mainActivity, GoogleMap map) {

        mMainActivity = mainActivity;
        mMap = map;
    }

    /**
     * Constructor the setup the class members and references
     * @param splashActivity a reference to the SplashActivity
     * @param map the Google Map
     */
    public ParkingInformation(SplashActivity splashActivity, GoogleMap map) {

        mSplashActivity = splashActivity;
        mMap = map;
    }

    /**
     * Save the ParkingMarkers fragment and retain its state, which can retain the list of
     * both on-street and off-street parking markers when the device changes its state (orientation,
     * on pause, etc).
     */
    public void saveParkingMarkersFragment() {

        if (parkingMarkers == null) {
            parkingMarkers = new ParkingMarkers();
        }

        FragmentManager fragmentManager = mMainActivity.getFragmentManager();
        FragmentTransaction transaction = fragmentManager.beginTransaction();
        transaction.add(parkingMarkers, ParkingMarkers.TAG_PARKING_MARKERS).commit();
    }

    /**
     * Restore the ParkingMarkers fragment, which restore both the on-street and off-street parking
     * markers from the previous state (i.e change of device's orientation).
     */
    public void restoreParkingMarkersFragment() {

        FragmentManager fragmentManager = mMainActivity.getFragmentManager();
        parkingMarkers = (ParkingMarkers) fragmentManager.findFragmentByTag(ParkingMarkers.TAG_PARKING_MARKERS);
    }

    /**
     * Get the SFPark and SFSU parking data and display it on the Google map.
     */
    public void getSFParkData(){

        if (sfParkDataReady) {
            return;
        }

        //Added a condition: only call it inside the Main Activity
        if ((parkingMarkers == null) && (mMainActivity != null) ) {
            restoreParkingMarkersFragment();
        }

        LoadSFParkDataTask ls = new LoadSFParkDataTask();

        //Only in Splash Activity, start LoadSFParkDataTask
        if(mMainActivity == null){
            ls.execute();

        }else{

            //In Main Activity, update the markers for sf and sfsu

            for (ParkingLocation location: sfParkingList){
                if(location.isOnStreet()){
                    ls.initializeOnStreetParking(location);
                } else {
                    ls.initializeOffStreetParking(location);
                }
            }

            for (ParkingLocation location: sfsuParkingList){
                if(location.isOnStreet()){
                    ls.initializeOnStreetParking(location);
                }else {
                    ls.initializeOffStreetParking(location);
                }
            }

        }


    }
    /**
     * Determine if the SFSU data is ready.
     * @return true if the SFPark data is ready, false otherwise
     */
    public boolean isSFSUParkDataReady(){
       return sfsuParkDataReady;
    }

    /**
     * Determine if the SFPark data is ready.
     * @return true if the SFPark data is ready, false otherwise
     */
    public boolean isSFParkDataReady() {
        return sfParkDataReady;
    }

    /**
     * Set the flag for SFPark data. Set true if the SFPark data is ready, false otherwise.
     * @param ready true if the SFPark data is ready, false otherwise
     */
    public void setSfParkDataReady(boolean ready) {
        sfParkDataReady = ready;
    }

    /**
     * Highlight on-street and off-street parking locations based on the given parameters.
     * @param drawOnStreet whether to draw on-street parking or not (true = draw, false otherwise)
     * @param drawOffStreet whether to draw off-street parking or not (true = draw, false otherwise)
     */
    public void highlightStreet(boolean drawOnStreet, boolean drawOffStreet) {

        if (sfParkDataReady && drawOnStreet && !highlightedOnStreetParking) {
            highlightOnStreetParking();
            highlightedOnStreetParking = true;
        } else if (sfParkDataReady && !drawOnStreet) {
            removeOnStreetHighlighted();
            highlightedOnStreetParking = false;
        }

        if (sfParkDataReady && drawOffStreet && !highlightedOffStreetParking) {
            highlightOffStreetParking();
            highlightedOffStreetParking = true;
        } else if (sfParkDataReady && !drawOffStreet) {
            removeOffStreetHighlighted();
            highlightedOffStreetParking = false;
        }

        /*
        if(sfsuParkDataReady && drawOnStreet && !highlightedOnStreetParking){
            highlightOnStreetParking();
            highlightedOnStreetParking = true;
        }else if (sfsuParkDataReady && !drawOnStreet){
            removeOnStreetHighlighted();
        }

        if(sfsuParkDataReady && drawOffStreet && !highlightedOffStreetParking){
            highlightOffStreetParking();
            highlightedOffStreetParking = true;
        }else if (sfsuParkDataReady && !drawOffStreet){
            removeOffStreetHighlighted();
            highlightedOffStreetParking = true;
        }
        */
    }

    /**
     * Highlight on-street parking on the map by setting the markers to visible.
     */
    private void highlightOnStreetParking() {

        // Restore the ParkingMarkers object if it hasn't
        if (parkingMarkers == null || parkingMarkers.getOnStreetParkingMarkers() == null) {
            restoreParkingMarkersFragment();
        }

        ArrayList<Polyline> polylines = parkingMarkers.getOnStreetParkingMarkers();

        for (int i = 0; i < polylines.size(); i++) {
            polylines.get(i).setVisible(true);
        }
    }

    /**
     * Highlight the off-street parking (parking lot building) on the map by setting the markers
     * to visible.
     */
    private void highlightOffStreetParking() {

        // Restore the ParkingMarkers object if it hasn't
        if (parkingMarkers == null || parkingMarkers.getOffStreetParkingMarkers() == null) {
            restoreParkingMarkersFragment();
        }

        ArrayList<Marker> markers = parkingMarkers.getOffStreetParkingMarkers();

        for (int i = 0; i < markers.size(); i++) {
            markers.get(i).setVisible(true);
        }
    }

    /**
     * Remove the on-street parking highlight from the map by setting the markers to invisible.
     */
    private void removeOnStreetHighlighted() {

        // Restore the ParkingMarkers object if it hasn't
        if (parkingMarkers == null || parkingMarkers.getOnStreetParkingMarkers() == null) {
            restoreParkingMarkersFragment();
        }

        ArrayList<Polyline> polylines = parkingMarkers.getOnStreetParkingMarkers();

        for (int i = 0; i < polylines.size(); i++) {
            polylines.get(i).setVisible(false);
        }
    }

    /**
     * Remove the off-street parking highlight from the map by setting the markers to invisible.
     */
    private void removeOffStreetHighlighted() {

        // Restore the ParkingMarkers object if it hasn't
        if (parkingMarkers == null || parkingMarkers.getOffStreetParkingMarkers() == null) {
            restoreParkingMarkersFragment();
        }

        ArrayList<Marker> markers = parkingMarkers.getOffStreetParkingMarkers();

        for (int i = 0; i < markers.size(); i++) {
            markers.get(i).setVisible(false);
        }
    }

    /**
     * This class will handle the task of loading SFPark parking data into the map.
     */
    private class LoadSFParkDataTask extends AsyncTask<Void, ParkingLocation, Void> {

        // Constants for HTTP request
        /**
         * Default wait time for  HTTP connection.
         */
        private static final int SECOND_IN_MILLIS = (int) DateUtils.SECOND_IN_MILLIS;

        /**
         * Default socket buffer size for HTTP connection.
         */
        private static final int SOCKET_BUFFER_SIZE = 8192;

        /**
         * The progress dialog that will be shown when fetching the data from SFPark.
         */
        //private ProgressDialog progressDialog;

        /**
         * A date format with the format of HH:mm.
         */
        private DateFormat dateFormat = new SimpleDateFormat("HH:mm");

        /**
         * A reference to the current hour and minute
         */
        private Date currentTime;

        @Override
        protected void onPreExecute() {

            //We don't need the loading dialog box since we have the splash screen
            //to cover up the loading process

            try {
//                progressDialog = new ProgressDialog(mMainActivity);
//                progressDialog.setTitle("Please wait");
//                progressDialog.setMessage("Displaying parking data ...");
//                progressDialog.show();
//                progressDialog.setCancelable(false);

                currentTime = dateFormat.parse(dateFormat.format(new Date(System.currentTimeMillis())));

            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        @Override
        protected Void doInBackground(Void... voids) {

            loadSFParkData();
            readSFParkData();
            loadSFSUParkData();
            readSFSUParkData();

            return null;
        }

        @Override
        protected void onProgressUpdate(ParkingLocation... locations) {

//            if (locations[0].isOnStreet()) {
//                initializeOnStreetParking(locations[0]);
//            } else {
//                initializeOffStreetParking(locations[0]);
//            }
        }

        @Override
        protected void onPostExecute(Void v) {

            sfParkDataReady = true;
            sfsuParkDataReady = true;

            //progressDialog.dismiss();
        }

        /**
         * Load SFSU Parking data from the GSP.txt.
         */
        private void loadSFSUParkData() {

            if (fileContent != null)
                return;

            try {
                fileContent = getFileContent(GPS_Path);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }

        /**
         * Read the SFSU Park data after it is loaded into the application.
         */
        private void readSFSUParkData() {

            try {
                // Don't do anything if there is nothing available in the SFSU GPS.txt file
                if (fileContent == null || fileContent.equals("")) {
                    return;
                }
                System.out.println(fileContent);
                JSONObject fileObject = new JSONObject(fileContent);
                JSONArray jsonFile = fileObject.has("AVL") ? fileObject.getJSONArray("AVL") : null;
                if (jsonFile == null)
                    return;

                for (int i = 0; i < jsonFile.length(); i++) {
                    // Get the file value (a location JSON data) and parse it
                    JSONObject coordinate = jsonFile.getJSONObject(i);
                    ParkingLocation parkingLocation = new ParkingLocation(coordinate);

                    //Add the parking location into the parking list
                    sfsuParkingList.add(parkingLocation);
                    //publishProgress(parkingLocation); //don't need it
                }

            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }

        /**
         * Load SFPark data from the SFPark URL.
         */
        private void loadSFParkData() {

            if (uriContent != null)
                return;

            try {
                uriContent = getURIContent(SERVICE_URL);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }

        /**
         * Read the SFPark data after it is loaded into the application.
         */
        private void readSFParkData() {

            try {
                // Don't do anything if there is nothing available in the SFPark URL
                if (uriContent == null || uriContent.equals(""))
                    return;

                JSONObject rootObject = new JSONObject(uriContent);
                JSONArray jsonAVL = rootObject.has("AVL") ? rootObject.getJSONArray("AVL") : null;

                if (jsonAVL == null)
                    return;

                for (int i = 0; i < jsonAVL.length(); i++) {
                    // Get the AVL value (a location JSON data) and parse it
                    JSONObject location = jsonAVL.getJSONObject(i);
                    ParkingLocation parkingLocation = new ParkingLocation(location);

                    //Add the parking location into the parking list
                    sfParkingList.add(parkingLocation);

                    //publishProgress(parkingLocation); //don't need
                }

            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }

        /**
         * Initialize the on-street parking location and display it on the map.
         * @param parkingLocation the on-street parking location to be initialized
         */
        private void initializeOnStreetParking(ParkingLocation parkingLocation) {

            // If it is street cleaning time now, then don't show the line
            ParkingLocation.RateSchedule[] rateSchedules = parkingLocation.getRateSchedule();
            for (int i = 0; i < rateSchedules.length; i++) {
                ParkingLocation.RateSchedule rateSchedule = rateSchedules[i];

                if (rateSchedule.getRateRestriction().equals(ParkingLocation.VALUE_STREET_SWEEP)) {
                    if (!passedTime(rateSchedule.getEndTime())) {
                        return;
                    } else {
                        break;
                    }
                }
            }

            Location[] locations = parkingLocation.getLocation();

            // If there are 2 points, then draw a line from point1 to point2
            if (locations.length == 2) {
                // Draw a line
                PolylineOptions lineOptions = new PolylineOptions()
                        .add(new LatLng(locations[0].getLatitude(), locations[0].getLongitude()))
                        .add(new LatLng(locations[1].getLatitude(), locations[1].getLongitude()))
                        .color(Color.GREEN)
                        .width(7f)
                        .visible(mMainActivity.showOnStreetParking());

                Polyline polyline = mMap.addPolyline(lineOptions);
                parkingMarkers.addOnStreetParkingMarker(polyline);
            }
        }

        /**
         * Initialize off-street parking location (parking garage) and display it on the map.
         * @param parkingLocation the off-street parking location to be initialized
         */
        private void initializeOffStreetParking(ParkingLocation parkingLocation) {

            String name = parkingLocation.getName();
            String description = parkingLocation.getDescription();
            ParkingLocation.RateSchedule[] rateSchedules = parkingLocation.getRateSchedule();
            StringBuilder snippet = new StringBuilder();

            if (rateSchedules != null) {
                for (int i = 0; i < rateSchedules.length; i++) {

                    ParkingLocation.RateSchedule rate = rateSchedules[i];

                    if (!rate.getBeginTime().equals("")) {
                        snippet.append(rate.getBeginTime()).append("-")
                                .append(rate.getEndTime()).append(" : $")
                                .append(rate.getRate()).append(" ")
                                .append(rate.getRateQualifier()).append("\n");
                    }
                }
            }

            Location[] locations = parkingLocation.getLocation();

            MarkerOptions markerOptions = new MarkerOptions()
                    .icon(BitmapDescriptorFactory.fromResource(R.drawable.icon_offstreet_parking))
                    .position(new LatLng(locations[0].getLatitude(), locations[0].getLongitude()))
                    .title(name)
                    .snippet(description + "%" + snippet)
                    .visible(mMainActivity.showOffStreetParking());

            Marker marker = mMap.addMarker(markerOptions);
            parkingMarkers.addOffStreetParkingMarker(marker);
        }

        /**
         * Check whether the given time has passed the current time. The given time must be in the
         * format: HH:mm AM/PM. Return true if the given time has passed the current time, false
         * otherwise.
         * @param givenTime the given time in the format HH:mm AM/PM
         * @return true if the given time has passed the current time, false otherwise
         */
        private boolean passedTime(String givenTime) {

            // Parse out the given time from string into integer
            // Assuming the given time has the format: HH:mm AM
            String[] time = givenTime.split("\\s+");

            // Assume it is available to park if the given time is in invalid format
            if (time.length != 2) {
                return true;
            }

            String hourMinute = time[0];        // Should have the format: HH:mm
            String period = time[1];            // Should contain AM or PM

            String[] hm = hourMinute.split(":");

            if (hm.length != 2) {
                return true;
            }

            int hour = Integer.parseInt(hm[0]);

            String hhmm = period.equalsIgnoreCase("PM") ?
                    (Integer.toString(hour + 12) + ":" + hm[1]) :
                    ("0" + hour + ":" + hm[1]);

            try {
                Date givenDate = dateFormat.parse(hhmm);

                return currentTime.before(givenDate) ? false : true;

            } catch (Exception ex) {
                ex.printStackTrace();
            }

            return true;
        }

        /**
         * Get the URI content, which is the data from the SFPark.
         * @param file the SFSU Park file
         * @return a string that contains the content (SFSU data) from the GPS.txt file
         * @throws Exception any error when file content cannot be extracted
         */
        private String getFileContent(String file) throws Exception {

            AssetManager assetManager = mMainActivity.getResources().getAssets();
            InputStream inputStream = null;
            String line = "";
            StringBuilder sb = new StringBuilder();
            BufferedReader reader;
            String json = null;
            //mContext.getResources().openRawResource(R.id.);
            try {
                inputStream = assetManager.open("GPS.txt");
                System.out.printf("File Located");

                if (inputStream != null) {
                    reader = new BufferedReader(new InputStreamReader(inputStream));

                    while ((line = reader.readLine()) != null) {
                        sb.append(line);
                    }
                    json = sb.toString();
                }
            } catch (IOException ex) {
                ex.printStackTrace();
            } finally {
                assetManager.close();
            }
            return sb.toString();
        }

        /**
         * Get the URI content, which is the data from the SFPark.
         * @param uri the SFPark URI
         * @return a string that contains the content (SFPark data) from the SFPark website
         * @throws Exception any error when doing HTTP request
         */
        private String getURIContent(String uri) throws Exception {

            try {
                HttpGet request = new HttpGet();
                request.setURI(new URI(uri));
                request.addHeader("Accept-Encoding", "gzip");

                final HttpParams params = new BasicHttpParams();
                HttpConnectionParams.setConnectionTimeout(params, 30 * SECOND_IN_MILLIS);
                HttpConnectionParams.setSoTimeout(params, 30 * SECOND_IN_MILLIS);
                HttpConnectionParams.setSocketBufferSize(params, SOCKET_BUFFER_SIZE);

                final DefaultHttpClient client = new DefaultHttpClient(params);

                client.addResponseInterceptor(new HttpResponseInterceptor() {

                    @Override
                    public void process(HttpResponse response, HttpContext context) {

                        final HttpEntity entity = response.getEntity();
                        final Header encoding = entity.getContentEncoding();

                        if (encoding != null) {
                            for (HeaderElement element : encoding.getElements()) {
                                if (element.getName().equalsIgnoreCase("gzip")) {
                                    response.setEntity(new InflatingEntity(response.getEntity()));
                                    break;
                                }
                            }
                        }
                    }
                });

                return client.execute(request, new BasicResponseHandler());

            } finally {
                // No clean up code is needed
            }
        }

        /**
         * Base class for wrapping entities. Keeps a wrappedEntity and delegates all calls to it.
         */
        private class InflatingEntity extends HttpEntityWrapper {

            public InflatingEntity(HttpEntity wrapped) {
                super(wrapped);
            }

            @Override
            public InputStream getContent() throws IOException {
                return new GZIPInputStream(wrappedEntity.getContent());
            }

            @Override
            public long getContentLength() {
                return -1;
            }
        }
    }
}
